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
                .onErrorReturn(Collections.emptyList()) // 失败时返回空列表而不是抛异常
                .flatMap(this::parseResponseToList);
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
        String positiveInstruction;

        String commonPrompt = "提取课程信息：%s\n%s\n时间转换：'3-4'='15:00-16:00'，'17点-18点'='17:00-18:00'\n输出JSON：%s";

        if ("WEEKLY".equalsIgnoreCase(type)) {
            typeDescription = "周课表，提取姓名、星期(MONDAY/TUESDAY/WEDNESDAY/THURSDAY/FRIDAY/SATURDAY/SUNDAY)、时间";
            jsonFormat = "[{\"studentName\":\"姓名\",\"dayOfWeek\":\"MONDAY\",\"time\":\"10:00-11:00\"}]";
        } else { // DATE_RANGE
            typeDescription = "日期课表，提取姓名、日期(YYYY-MM-DD)、时间";
            jsonFormat = "[{\"studentName\":\"姓名\",\"date\":\"2024-08-15\",\"time\":\"10:00-11:00\"}]";
        }

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