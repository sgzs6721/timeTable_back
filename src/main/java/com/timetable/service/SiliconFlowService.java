package com.timetable.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    /**
     * 使用SiliconFlow的聊天completion API进行自然语言处理
     */
    public String processTextForSchedule(String transcribedText) throws IOException {
        try {
            logger.info("开始调用SiliconFlow聊天API进行排课信息提取");

            // 构建聊天请求
            String systemPrompt = "你是一个专业的课程安排助手。请从用户的语音转文字结果中提取排课信息，并以JSON格式返回。" +
                    "返回格式：{\"studentName\": \"学生姓名\", \"subject\": \"科目\", \"dayOfWeek\": \"MONDAY/TUESDAY/WEDNESDAY/THURSDAY/FRIDAY/SATURDAY/SUNDAY\", " +
                    "\"startTime\": \"HH:MM\", \"endTime\": \"HH:MM\", \"note\": \"备注信息\"}";

            String userPrompt = "请从以下文本中提取排课信息：" + transcribedText;

            String requestJson = "{\n" +
                    "  \"model\": \"" + nlpModel + "\",\n" +
                    "  \"messages\": [\n" +
                    "    {\n" +
                    "      \"role\": \"system\",\n" +
                    "      \"content\": \"" + systemPrompt.replace("\"", "\\\"") + "\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"role\": \"user\",\n" +
                    "      \"content\": \"" + userPrompt.replace("\"", "\\\"") + "\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"temperature\": 0.3,\n" +
                    "  \"max_tokens\": 500\n" +
                    "}";

            RequestBody body = RequestBody.create(requestJson, MediaType.get("application/json"));
            Request request = new Request.Builder()
                    .url(apiUrl + "/chat/completions")
                    .post(body)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "未知错误";
                    logger.error("聊天API调用失败，状态码: {}, 错误信息: {}", response.code(), errorBody);
                    throw new RuntimeException("自然语言处理失败: " + errorBody);
                }

                String responseBody = response.body().string();
                logger.debug("聊天API响应: {}", responseBody);

                // 解析响应
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                JsonNode choices = jsonNode.path("choices");

                if (choices.isArray() && choices.size() > 0) {
                    JsonNode message = choices.get(0).path("message");
                    String content = message.path("content").asText();

                    logger.info("自然语言处理成功: {}", content);
                    return content;
                }

                throw new RuntimeException("聊天API响应格式异常");
            }
        } catch (Exception e) {
            logger.error("自然语言处理失败", e);
            throw new IOException("自然语言处理失败: " + e.getMessage());
        }
    }
}