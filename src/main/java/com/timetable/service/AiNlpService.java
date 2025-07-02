package com.timetable.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.timetable.dto.ai.ChatMessage;
import com.timetable.dto.ai.ChatRequest;
import com.timetable.dto.ai.ChatResponse;
import com.timetable.dto.ai.ScheduleInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;

@Service
public class AiNlpService {

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
                List.of(new ChatMessage("user", prompt))
        );

        return webClient.post()
                .uri(apiUrl + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .map(ChatResponse::getFirstChoiceContent)
                .flatMap(this::parseResponse);
    }

    private String buildPrompt(String text) {
        return """
                请从以下文本中提取课程安排信息: "%s"

                你需要识别这是固定周课表还是按日期的课表。
                - 如果是固定周课表，请提取星期几、时间（例如 "14:00-15:00"）和姓名。
                - 如果是日期类课表，请提取日期（格式 YYYY-MM-DD）、时间（例如 "14:00-15:00"）和姓名。

                请将结果以JSON格式返回，不要包含任何其他说明文字或代码标记。
                如果文本与课程无关，请返回一个空的JSON对象 {}。

                JSON格式如下:
                对于固定周课表:
                {
                  "type": "weekly",
                  "studentName": "学生姓名",
                  "time": "HH:mm-HH:mm",
                  "dayOfWeek": "MONDAY"
                }

                对于日期类课表:
                {
                  "type": "dated",
                  "studentName": "学生姓名",
                  "time": "HH:mm-HH:mm",
                  "date": "YYYY-MM-DD"
                }
                """.formatted(text);
    }

    private Mono<ScheduleInfo> parseResponse(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.isBlank() || jsonResponse.trim().equals("{}")) {
            return Mono.empty();
        }
        try {
            // The response from the AI might be wrapped in ```json ... ```
            String cleanJson = jsonResponse.trim().replace("```json", "").replace("```", "").trim();
            ScheduleInfo scheduleInfo = objectMapper.readValue(cleanJson, ScheduleInfo.class);
            return Mono.just(scheduleInfo);
        } catch (IOException e) {
            // Log the error and the problematic JSON
            System.err.println("Failed to parse AI response: " + jsonResponse);
            return Mono.error(new RuntimeException("Failed to parse AI response", e));
        }
    }
} 