package com.timetable.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.timetable.dto.ai.ScheduleInfo;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * SiliconFlow AI服务集成
 * 提供语音转文字和自然语言处理功能
 */
@Service
public class SiliconFlowService {

    private static final Logger logger = LoggerFactory.getLogger(SiliconFlowService.class);

    @Value("${ai.speech.api-url}")
    private String apiUrl;

    @Value("${ai.speech.api-key}")
    private String apiKey;

    @Value("${ai.nlp.model}")
    private String nlpModel;

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SiliconFlowService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 使用SiliconFlow的语音转文字API
     */
    public String transcribeAudio(MultipartFile audioFile) throws IOException {
        try {
            logger.info("开始调用SiliconFlow语音转文字API");

            // 构建multipart请求
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("model", "FunAudioLLM/SenseVoiceSmall")
                    .addFormDataPart("file", audioFile.getOriginalFilename(),
                            RequestBody.create(audioFile.getBytes(), MediaType.parse(audioFile.getContentType())))
                    .build();

            Request request = new Request.Builder()
                    .url(apiUrl + "/audio/transcriptions")
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "未知错误";
                    logger.error("语音转文字API调用失败，状态码: {}, 错误信息: {}", response.code(), errorBody);
                    throw new RuntimeException("语音转文字失败: " + errorBody);
                }

                String responseBody = response.body().string();
                logger.debug("语音转文字API响应: {}", responseBody);

                // 解析响应
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                String transcribedText = jsonNode.path("text").asText();

                if (transcribedText.isEmpty()) {
                    throw new RuntimeException("语音转文字结果为空");
                }

                logger.info("语音转文字成功: {}", transcribedText);
                return transcribedText;
            }
        } catch (Exception e) {
            logger.error("语音转文字处理失败", e);
            throw new IOException("语音转文字处理失败: " + e.getMessage());
        }
    }

    public Mono<List<ScheduleInfo>> extractScheduleInfoFromText(String text, String timetableType) {
        String prompt = buildPrompt(text, timetableType);

        String requestJson = String.format("{\"model\": \"%s\", \"messages\": [{\"role\": \"system\", \"content\": \"%s\"}, {\"role\": \"user\", \"content\": \"%s\"}], \"temperature\": 0.3, \"max_tokens\": 1024}",
                nlpModel, "You are a helpful assistant that extracts schedule information into JSON format.", prompt.replace("\"", "\\\""));

        RequestBody body = RequestBody.create(requestJson, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(apiUrl + "/chat/completions")
                .post(body)
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "未知错误";
                logger.error("AI API call failed with status code: {}, body: {}", response.code(), errorBody);
                return Mono.error(new IOException("AI API call failed: " + errorBody));
            }
            String jsonResponse = response.body().string();
            return parseResponseToList(jsonResponse);
        } catch (IOException e) {
            logger.error("Failed to call AI API", e);
            return Mono.error(e);
        }
    }

    private String buildPrompt(String text, String timetableType) {
        String jsonFormat;
        String typeDescription;
        String negativeInstruction;

        if ("WEEKLY".equalsIgnoreCase(timetableType)) {
            typeDescription = "这是一个**周固定课表**。";
            negativeInstruction = "你**只能**提取 `dayOfWeek` (星期几)，**绝对不能**包含 `date` (日期) 字段。如果用户提到了具体的日期（如"8月15日"），请忽略它，只关注星期几。";
            jsonFormat = "{\\\"studentName\\\": \\\"学生姓名\\\", \\\"time\\\": \\\"HH:mm-HH:mm\\\", \\\"dayOfWeek\\\": \\\"MONDAY\\\"}";
        } else { // DATE_RANGE
            typeDescription = "这是一个**日期范围课表**。";
            negativeInstruction = "你**只能**提取 `date` (具体日期)，**绝对不能**包含 `dayOfWeek` (星期几) 字段。如果用户提到了星期几（如"周三"），请忽略它，专注于识别具体的日期。";
            jsonFormat = "{\\\"studentName\\\": \\\"学生姓名\\\", \\\"time\\\": \\\"HH:mm-HH:mm\\\", \\\"date\\\": \\\"YYYY-MM-DD\\\"}";
        }

        return String.format(
                "你是一个严格的课程安排解析助手。请从以下文本中提取所有课程安排信息: \\\"%s\\\"\\n\\n" +
                "### 任务要求\\n" +
                "1. **课表类型**: %s\\n" +
                "2. **关键指令**: %s\\n" +
                "3. **输出格式**: 必须是严格的JSON数组格式，数组中的每个对象代表一个排课。不要返回任何其他说明文字或代码标记。\\n" +
                "4. **无关内容**: 如果文本与课程无关，请返回一个空的JSON数组 `[]`。\\n\\n" +
                "### JSON对象格式\\n%s",
                text, typeDescription, negativeInstruction, jsonFormat);
    }
    
    private Mono<List<ScheduleInfo>> parseResponseToList(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            String content = root.path("choices").get(0).path("message").path("content").asText();
            
            // The response from the AI might be wrapped in ```json ... ```
            String cleanJson = content.trim().replace("```json", "").replace("```", "").trim();
            
            if (cleanJson.isEmpty() || cleanJson.equals("[]")) {
                return Mono.just(Collections.emptyList());
            }
            
            List<ScheduleInfo> scheduleInfoList = objectMapper.readValue(cleanJson, new com.fasterxml.jackson.core.type.TypeReference<List<ScheduleInfo>>() {});
            return Mono.just(scheduleInfoList);
            
        } catch (IOException e) {
            logger.error("Failed to parse AI response: " + jsonResponse, e);
            return Mono.error(new RuntimeException("Failed to parse AI response", e));
        }
    }
}