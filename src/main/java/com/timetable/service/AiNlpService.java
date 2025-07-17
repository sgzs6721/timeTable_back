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

import java.io.IOException;
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
        
        // 临时使用简单消息测试 502 错误是否与 prompt 大小有关
        String testPrompt = "Hello";
        
        // The request body should now conform to the OpenAI format, which is handled by ChatRequest.
        ChatRequest request = new ChatRequest(model, Collections.singletonList(new ChatMessage("user", testPrompt)));

        try {
            logger.info("Sending Gemini (OpenAI-compatible) AI request with body: {}", objectMapper.writeValueAsString(request));
            logger.info("Request URI: {}", apiUrl + "/v1/chat/completions");
            logger.info("API Key (first 10 chars): {}", apiKey.substring(0, Math.min(10, apiKey.length())));
        } catch (IOException e) {
            logger.error("Error serializing Gemini (OpenAI-compatible) AI request body", e);
        }

        // The endpoint and headers should be consistent with other OpenAI-compatible services.
        return webClient.post()
                .uri(apiUrl + "/v1/chat/completions") // Use the standard chat completions endpoint
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("User-Agent", "Java-WebClient/11") // Add a User-Agent header
                .header("Host", "velvety-cupcake-6a525e.netlify.app") // 设置正确的Host头用于SSL验证
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatResponse.class) // Expect an OpenAI-compatible response
                .doOnError(WebClientResponseException.class, ex -> {
                    logger.error("Gemini (OpenAI-compatible) API call failed with status code: {}", ex.getRawStatusCode());
                    logger.error("Gemini (OpenAI-compatible) API response body: {}", ex.getResponseBodyAsString());
                })
                .doOnError(Exception.class, ex -> {
                    logger.error("Unexpected error during Gemini API call: {}", ex.getMessage(), ex);
                })
                .map(ChatResponse::getFirstChoiceContent) // Use the same response parsing logic
                .flatMap(response -> {
                    logger.info("Received response from Gemini: {}", response);
                    // 如果使用测试 prompt，直接返回空列表
                    if ("Hello".equals(testPrompt)) {
                        return Mono.just(Collections.emptyList());
                    }
                    return parseResponseToList(response);
                });
    }


    public Mono<List<ScheduleInfo>> extractScheduleInfo(String prompt, String type) {
        // This method is now a wrapper. The logic is moved to provider-specific methods.
        return extractScheduleInfoFromText(prompt, type);
    }

    private String buildPrompt(String text, String type) {
        String jsonFormat;
        String typeDescription;
        String positiveInstruction;

        String commonPrompt = "你是一个严格的课程安排解析助手。你的任务是从用户提供的文本中，精准地提取出排课信息。\n\n" +
                "### 用户文本\n" +
                "```text\n" +
                "%s\n" + // user input text
                "```\n\n" +
                "### 解析规则\n" +
                "1. **课表类型**: %s\n" +
                "2. **核心解析任务**: %s\n" + // This will be the new positive instruction
                "3. **时间解析**: \n" +
                "   - **有效范围**: 只处理上午9点到晚上8点(20:00)之间的时间。\n" +
                "   - **智能识别**: '3-4' 应解析为 `15:00-16:00`。'9-11' 应解析为 `09:00-11:00`。\n" +
                "4. **输出要求**: 必须返回一个严格的JSON数组。如果无法解析或内容无关，返回空数组 `[]`。不要添加任何解释。\n\n" +
                "### JSON输出格式\n" +
                "```json\n" +
                "%s\n" + // json format
                "```";

        if ("WEEKLY".equalsIgnoreCase(type)) {
            typeDescription = "这是一个**周固定课表**。";
            positiveInstruction = "你需要为每个学员解析出他们的 **姓名 (studentName)**、**上课星期 (dayOfWeek)** 和 **时间 (time)**。";
            jsonFormat = "[\n" +
                         "  {\n" +
                         "    \"studentName\": \"学生姓名\",\n" +
                         "    \"dayOfWeek\": \"MONDAY\",\n" +
                         "    \"time\": \"HH:mm-HH:mm\"\n" +
                         "  }\n" +
                         "]";
        } else { // DATE_RANGE
            typeDescription = "这是一个**日期范围课表**。";
            positiveInstruction = "你需要为每个学员解析出他们的 **姓名 (studentName)**、**上课日期 (date)** 和 **时间 (time)**。";
            jsonFormat = "[\n" +
                         "  {\n" +
                         "    \"studentName\": \"学生姓名\",\n" +
                         "    \"date\": \"YYYY-MM-DD\",\n" +
                         "    \"time\": \"HH:mm-HH:mm\"\n" +
                         "  }\n" +
                         "]";
        }

        return String.format(
                commonPrompt,
                text, typeDescription, positiveInstruction, jsonFormat);
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