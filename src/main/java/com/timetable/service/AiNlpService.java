package com.timetable.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.timetable.dto.ai.ChatMessage;
import com.timetable.dto.ai.ChatRequest;
import com.timetable.dto.ai.ChatResponse;
import com.timetable.dto.ai.ScheduleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class AiNlpService {

    private static final Logger logger = LoggerFactory.getLogger(AiNlpService.class);
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.nlp.api-url}")
    private String apiUrl;

    @Value("${ai.nlp.api-key}")
    private String apiKey;

    @Value("${ai.nlp.model}")
    private String model;

    @Value("${ai.nlp.provider}")
    private String provider;

    public AiNlpService(WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    public Mono<List<ScheduleInfo>> extractScheduleInfoFromText(String text, String timetableType) {
        if ("gemini".equalsIgnoreCase(provider)) {
            return extractScheduleInfoWithGemini(text, timetableType);
        } else {
            // Default to siliconflow or other providers
            return extractScheduleInfoWithSiliconFlow(text, timetableType);
        }
    }

    private Mono<List<ScheduleInfo>> extractScheduleInfoWithSiliconFlow(String text, String timetableType) {
        String prompt = buildPrompt(text, timetableType);
        ChatRequest request = new ChatRequest(model, Collections.singletonList(new ChatMessage("user", prompt)));

        try {
            logger.info("Sending SiliconFlow AI request with body: {}", objectMapper.writeValueAsString(request));
        } catch (IOException e) {
            logger.error("Error serializing SiliconFlow AI request body", e);
        }

        return webClient.post()
                .uri(apiUrl + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("User-Agent", "Java-WebClient/11") // Add a User-Agent header
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .doOnError(WebClientResponseException.class, ex -> {
                    logger.error("SiliconFlow API call failed with status code: {}", ex.getRawStatusCode());
                    logger.error("SiliconFlow API response body: {}", ex.getResponseBodyAsString());
                })
                .map(ChatResponse::getFirstChoiceContent)
                .flatMap(this::parseResponseToList);
    }

    private Mono<List<ScheduleInfo>> extractScheduleInfoWithGemini(String text, String timetableType) {
        String prompt = buildPrompt(text, timetableType);
        
        // 创建包含max_tokens参数的完整请求
        try {
            String escapedPrompt = objectMapper.writeValueAsString(prompt);
            String requestJson = String.format(
                "{\"model\":\"%s\",\"messages\":[{\"role\":\"user\",\"content\":%s}],\"max_tokens\":8000,\"temperature\":0}",
                model,
                escapedPrompt
            );
            logger.info("Generated request JSON for Gemini API");
            return createWebClientRequest(requestJson);
        } catch (Exception e) {
            logger.error("Failed to create request JSON", e);
            return Mono.just(Collections.emptyList());
        }
    }

    private Mono<List<ScheduleInfo>> createWebClientRequest(String requestJson) {
        logger.info("Sending AI request to: {}/v1/chat/completions", apiUrl);

        return webClient.post()
                .uri(apiUrl + "/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("User-Agent", "Timetable-Backend/1.0")
                .bodyValue(requestJson)
                .retrieve()
                .onStatus(status -> status.is5xxServerError(), response -> {
                    logger.warn("Server error response: {}", response.statusCode());
                    return response.bodyToMono(String.class)
                        .doOnNext(body -> logger.warn("Server error body: {}", body))
                        .then(Mono.error(new RuntimeException("Server error: " + response.statusCode())));
                })
                .bodyToMono(String.class)
                .doOnNext(responseBody -> {
                    // 检查响应是否是有效的JSON
                    if (responseBody.trim().startsWith("The deployment failed") || 
                        responseBody.trim().startsWith("the edge function timed out")) {
                        logger.error("Edge function deployment/timeout error: {}", responseBody);
                        throw new RuntimeException("Edge function error: " + responseBody);
                    }
                })
                .map(this::parseOpenAIResponse)
                .timeout(Duration.ofSeconds(45)) // 增加到45秒超时
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(throwable -> {
                            // 重试网络连接错误和Edge Function错误
                            String message = throwable.getMessage();
                            return message != null && (
                                message.contains("Connection reset by peer") ||
                                message.contains("Connection timed out") ||
                                message.contains("ConnectTimeoutException") ||
                                message.contains("Edge function error") ||
                                message.contains("Server error") ||
                                message.contains("TimeoutException")
                            );
                        })
                        .doBeforeRetry(retrySignal -> 
                            logger.warn("Retrying AI API call, attempt: {}, error: {}", 
                                retrySignal.totalRetries() + 1, 
                                retrySignal.failure().getMessage())))
                .doOnError(ex -> logger.error("AI API call failed after retries: {}", ex.getMessage()))
                .flatMap(this::parseResponseToList)
                .onErrorReturn(Collections.emptyList()); // 移到flatMap之后，现在类型匹配了
    }

    /**
     * 解析OpenAI格式的响应
     */
    private String parseOpenAIResponse(String responseBody) {
        try {
            // 先检查是否是错误页面
            if (responseBody.trim().startsWith("<!DOCTYPE html") || 
                responseBody.trim().startsWith("<html") ||
                responseBody.trim().startsWith("The deployment failed") ||
                responseBody.trim().startsWith("the edge function timed out") ||
                responseBody.trim().contains("Application error")) {
                logger.error("Received error page instead of JSON: {}", responseBody.substring(0, Math.min(200, responseBody.length())));
                return "[]";
            }

            JsonNode jsonNode = objectMapper.readTree(responseBody);
            JsonNode choices = jsonNode.path("choices");
            
            if (choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).path("message");
                String content = message.path("content").asText();
                
                // 检查content是否为null或"null"
                if ("null".equals(content) || content == null || content.trim().isEmpty()) {
                    logger.warn("AI returned null or empty content");
                    return "[]";
                }
                
                logger.info("AI response content: {}", content);
                return content;
            }
            
            logger.warn("No choices found in AI response: {}", responseBody);
            return "[]";
        } catch (Exception e) {
            logger.error("Failed to parse OpenAI response: {}", responseBody.substring(0, Math.min(500, responseBody.length())), e);
            return "[]";
        }
    }


    public Mono<List<ScheduleInfo>> extractScheduleInfo(String prompt, String type) {
        // This method is now a wrapper. The logic is moved to provider-specific methods.
        return extractScheduleInfoFromText(prompt, type);
    }

    private String buildPrompt(String text, String type) {
        String jsonFormat;
        String typeDescription;

        if ("WEEKLY".equalsIgnoreCase(type)) {
            typeDescription = "周课表，提取姓名、星期(MONDAY/TUESDAY/WEDNESDAY/THURSDAY/FRIDAY/SATURDAY/SUNDAY)、时间";
            jsonFormat = "[{\"studentName\":\"姓名\",\"dayOfWeek\":\"MONDAY\",\"time\":\"10:00-11:00\"}]";
        } else { // DATE_RANGE
            typeDescription = "日期课表，提取姓名、日期(YYYY-MM-DD)、时间";
            jsonFormat = "[{\"studentName\":\"姓名\",\"date\":\"2024-08-15\",\"time\":\"10:00-11:00\"}]";
        }

        String commonPrompt = "任务：从以下文本中提取课程安排信息\n" +
                "输入文本：%s\n\n" +
                "%s\n\n" +
                "课程时间范围：上午10:00到晚上20:00（只有这个时间段内的课程有效）\n\n" +
                "解析示例：\n" +
                "1. 小王周二三点到四点 -> {\"studentName\":\"小王\",\"dayOfWeek\":\"TUESDAY\",\"time\":\"15:00-16:00\"}\n" +
                "2. 张三周一十点到十一点 -> {\"studentName\":\"张三\",\"dayOfWeek\":\"MONDAY\",\"time\":\"10:00-11:00\"}\n" +
                "3. 李四周三七点到八点 -> {\"studentName\":\"李四\",\"dayOfWeek\":\"WEDNESDAY\",\"time\":\"19:00-20:00\"}\n" +
                "4. 小王周二三点到四点小张周三四点到五点 -> 解析为两个学生的信息\n\n" +
                "时间转换规则（24小时制，课程时间10:00-20:00）：\n" +
                "- 十点/10点 = 10:00, 十一点/11点 = 11:00\n" +
                "- 一点/1点 = 13:00, 二点/2点 = 14:00\n" +
                "- 三点/3点 = 15:00, 四点/4点 = 16:00\n" +
                "- 五点/5点 = 17:00, 六点/6点 = 18:00\n" +
                "- 七点/7点 = 19:00, 八点/8点 = 20:00\n" +
                "- 九点到十点 = 09:00-10:00（注意：9点在课程时间范围外）\n" +
                "- 三点到四点 = 15:00-16:00\n" +
                "- 17-18 = 17:00-18:00\n\n" +
                "星期转换：\n" +
                "- 周一/星期一 = MONDAY\n" +
                "- 周二/星期二 = TUESDAY\n" +
                "- 周三/星期三 = WEDNESDAY\n" +
                "- 周四/星期四 = THURSDAY\n" +
                "- 周五/星期五 = FRIDAY\n" +
                "- 周六/星期六 = SATURDAY\n" +
                "- 周日/星期日 = SUNDAY\n\n" +
                "注意事项：\n" +
                "1. 如果时间超出10:00-20:00范围，请在errorMessage中说明\n" +
                "2. 如果多个学生信息连在一起没有分隔符，请仔细识别每个学生的完整信息（姓名+星期+时间）\n" +
                "3. 所有时间都转换为24小时制格式\n\n" +
                "请准确提取每个学生的信息，输出严格的JSON格式：\n%s";

        return String.format(
                commonPrompt,
                text, typeDescription, jsonFormat);
    }

    private Mono<List<ScheduleInfo>> parseResponseToList(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.trim().isEmpty() || jsonResponse.trim().equals("[]")) {
            return Mono.just(Collections.emptyList());
        }
        try {
            // The response from the AI might be wrapped in ```json ... ```
            String cleanJson = jsonResponse.trim().replace("```json", "").replace("```", "").trim();
            List<ScheduleInfo> scheduleInfoList = objectMapper.readValue(cleanJson, new TypeReference<List<ScheduleInfo>>() {});

            List<ScheduleInfo> expandedList = new ArrayList<>();
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

            for (ScheduleInfo schedule : scheduleInfoList) {
                if (schedule.getTime() == null || !schedule.getTime().contains("-")) {
                    expandedList.add(schedule); // Add as is if no time or invalid format
                    continue;
                }

                try {
                    String[] timeParts = schedule.getTime().split("-");
                    LocalTime startTime = LocalTime.parse(timeParts[0].trim());
                    LocalTime endTime = LocalTime.parse(timeParts[1].trim());

                    if (startTime.isAfter(endTime) || startTime.equals(endTime)) {
                        logger.warn("Invalid time range (start is not before end): {}. Adding schedule as is.", schedule.getTime());
                        expandedList.add(schedule);
                        continue;
                    }

                    LocalTime currentTime = startTime;
                    while (currentTime.isBefore(endTime)) {
                        LocalTime nextHour = currentTime.plusHours(1);
                        
                        // Ensure we don't go past the original end time
                        if (nextHour.isAfter(endTime)) {
                            nextHour = endTime;
                        }
                        
                        // Create a new ScheduleInfo for the one-hour slot
                        ScheduleInfo newSchedule = new ScheduleInfo(
                            schedule.getStudentName(),
                            currentTime.format(timeFormatter) + "-" + nextHour.format(timeFormatter),
                            schedule.getDayOfWeek(),
                            schedule.getDate(),
                            null // No error message for successful parsing
                        );
                        expandedList.add(newSchedule);

                        currentTime = nextHour;
                    }
                } catch (DateTimeParseException | ArrayIndexOutOfBoundsException e) {
                    // Log the error and add the original schedule
                    logger.warn("Could not parse time slot: '{}'. Adding schedule as is.", schedule.getTime(), e);
                    expandedList.add(schedule);
                }
            }

            return Mono.just(expandedList);
        } catch (IOException e) {
            // Log the error and the problematic JSON
            logger.error("Failed to parse AI response: " + jsonResponse, e);
            return Mono.error(new RuntimeException("Failed to parse AI response", e));
        }
    }
} 