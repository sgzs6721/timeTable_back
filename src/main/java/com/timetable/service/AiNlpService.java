package com.timetable.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.timetable.dto.ai.ChatMessage;
import com.timetable.dto.ai.ChatRequest;
import com.timetable.dto.ai.ChatResponse;
import com.timetable.dto.ai.ResponseFormat;
import com.timetable.dto.ai.ScheduleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.io.IOException;
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

    public Mono<ScheduleInfo> extractScheduleInfo(String text) {
        String prompt = buildPrompt(text);

        ChatRequest request = new ChatRequest(
                model,
                Collections.singletonList(new ChatMessage("user", prompt)),
                new ResponseFormat("json_object")
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
                .flatMap(this::parseResponse);
    }

    private String buildPrompt(String text) {
        return String.format(
                "请从以下文本中提取课程安排信息: \"%s\"\n\n" +
                "你需要识别这是固定周课表还是按日期的课表。\n" +
                "- 如果是固定周课表,请提取星期几、时间(例如 \"14:00-15:00\")和姓名。\n" +
                "- 如果是日期类课表,请提取日期(格式 YYYY-MM-DD)、时间(例如 \"14:00-15:00\")和姓名。\n\n" +
                "请将结果以JSON格式返回,不要包含任何其他说明文字或代码标记。\n" +
                "如果文本与课程无关,请返回一个空的JSON对象 {}。\n\n" +
                "JSON格式如下:\n" +
                "对于固定周课表(不需要dayOfWeek字段时请省略):\n" +
                "{\n" +
                "  \"studentName\": \"学生姓名\",\n" +
                "  \"time\": \"HH:mm-HH:mm\",\n" +
                "  \"dayOfWeek\": \"MONDAY\"\n" +
                "}\n\n" +
                "对于日期类课表(不需要date字段时请省略):\n" +
                "{\n" +
                "  \"studentName\": \"学生姓名\",\n" +
                "  \"time\": \"HH:mm-HH:mm\",\n" +
                "  \"date\": \"YYYY-MM-DD\"\n" +
                "}",
                text);
    }

    private Mono<ScheduleInfo> parseResponse(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.trim().isEmpty() || jsonResponse.trim().equals("{}")) {
            return Mono.empty();
        }
        try {
            // The response from the AI might be wrapped in ```json ... ```
            String cleanJson = jsonResponse.trim().replace("```json", "").replace("```", "").trim();
            ScheduleInfo scheduleInfo = objectMapper.readValue(cleanJson, ScheduleInfo.class);
            return Mono.just(scheduleInfo);
        } catch (IOException e) {
            // Log the error and the problematic JSON
            logger.error("Failed to parse AI response: " + jsonResponse, e);
            return Mono.error(new RuntimeException("Failed to parse AI response", e));
        }
    }
} 