package com.timetable.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    public AiNlpService(WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    public Mono<List<ScheduleInfo>> extractScheduleInfoFromText(String text, String timetableType) {
        String prompt = buildPrompt(text, timetableType);

        ChatRequest request = new ChatRequest(
                model,
                Collections.singletonList(new ChatMessage("user", prompt))
        );

        try {
            logger.info("Sending AI request with body: {}", objectMapper.writeValueAsString(request));
        } catch (IOException e) {
            logger.error("Error serializing AI request body", e);
        }

        return webClient.post()
                .uri(apiUrl + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .doOnError(WebClientResponseException.class, ex -> {
                    logger.error("AI API call failed with status code: {}", ex.getRawStatusCode());
                    logger.error("AI API response body: {}", ex.getResponseBodyAsString());
                })
                .map(ChatResponse::getFirstChoiceContent)
                .flatMap(this::parseResponseToList);
    }

    private String buildPrompt(String text, String timetableType) {
        String jsonFormat;
        String typeDescription;
        String negativeInstruction;

        if ("WEEKLY".equalsIgnoreCase(timetableType)) {
            typeDescription = "这是一个**周固定课表**。";
            negativeInstruction = "你**只能**提取 `dayOfWeek` (星期几)，**绝对不能**包含 `date` (日期) 字段。如果用户提到了具体的日期（如\\\"8月15日\\\"），请忽略它，只关注星期几。";
            jsonFormat = "{\n" +
                         "  \"studentName\": \"学生姓名\",\n" +
                         "  \"time\": \"HH:mm-HH:mm\",\n" +
                         "  \"dayOfWeek\": \"MONDAY\"\n" +
                         "}";
        } else { // DATE_RANGE
            typeDescription = "这是一个**日期范围课表**。";
            negativeInstruction = "你**只能**提取 `date` (具体日期)，**绝对不能**包含 `dayOfWeek` (星期几) 字段。如果用户提到了星期几（如\\\"周三\\\"），请忽略它，专注于识别具体的日期。";
            jsonFormat = "{\n" +
                         "  \"studentName\": \"学生姓名\",\n" +
                         "  \"time\": \"HH:mm-HH:mm\",\n" +
                         "  \"date\": \"YYYY-MM-DD\"\n" +
                         "}";
        }

        return String.format(
                "你是一个严格的课程安排解析助手。请从以下文本中提取所有课程安排信息: \"%s\"\n\n" +
                "### 任务要求\n" +
                "1. **课表类型**: %s\n" +
                "2. **用户输入内容**: 用户的输入通常只会包含**日期、星期、时间和人名**相关的信息。请专注于从这些信息中解析。\n" +
                "3. **年份处理**: 如果用户输入的日期没有指定年份（例如只说了"8月15日"），你必须默认使用**当前年份**。\n" +
                "4. **关键指令**: %s\n" +
                "5. **输出格式**: 必须是严格的JSON数组格式，数组中的每个对象代表一个排课。不要返回任何其他说明文字或代码标记。\n" +
                "6. **无关内容**: 如果文本与课程无关，请返回一个空的JSON数组 `[]`。\n\n" +
                "### JSON对象格式\n%s",
                text, typeDescription, negativeInstruction, jsonFormat);
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