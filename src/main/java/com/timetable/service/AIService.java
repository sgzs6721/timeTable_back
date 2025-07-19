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
import java.util.Base64;

/**
 * AI服务类，集成Google Gemini API
 * 提供语音识别和自然语言处理功能
 */
@Service
public class AIService {
    
    private static final Logger logger = LoggerFactory.getLogger(AIService.class);
    
    @Value("${ai.speech.api-url}")
    private String speechApiUrl;
    
    @Value("${ai.speech.api-key}")
    private String speechApiKey;
    
    @Value("${ai.nlp.api-url}")
    private String nlpApiUrl;
    
    @Value("${ai.nlp.api-key}")
    private String nlpApiKey;
    
    @Value("${ai.nlp.model:gemini-2.5-flash}")
    private String nlpModel;
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public AIService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 使用Netlify代理的OpenAI兼容API进行语音转文字
     */
    public String transcribeAudio(MultipartFile audioFile) throws IOException {
        try {
            logger.info("开始调用Gemini语音转文字API (通过Netlify代理)");
            
            // 使用multipart/form-data格式上传音频文件
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("model", "whisper-1") // OpenAI兼容的语音模型名
                    .addFormDataPart("file", audioFile.getOriginalFilename(),
                            RequestBody.create(audioFile.getBytes(), MediaType.parse("audio/wav")))
                    .build();

            Request request = new Request.Builder()
                    .url(speechApiUrl + "/v1/audio/transcriptions")
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer " + speechApiKey)
                    .addHeader("User-Agent", "Timetable-Backend/1.0")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "未知错误";
                    logger.error("语音转文字API调用失败，状态码: {}, 错误信息: {}", response.code(), errorBody);
                    throw new RuntimeException("语音转文字失败: " + errorBody);
                }

                String responseBody = response.body().string();
                logger.debug("语音转文字API响应: {}", responseBody);

                // 解析OpenAI格式的响应
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
     * 处理课程安排文本，提取结构化信息
     */
    public String processScheduleText(String inputText) throws IOException {
        try {
            // 构建提示文本
            String promptText = "请从以下文本中提取课程安排信息，并以结构化格式返回：\\n\\n" +
                    "输入文本：\"" + inputText + "\"\\n\\n" +
                    "请提取以下信息：\\n" +
                    "1. 学生姓名\\n" +
                    "2. 科目/课程名称\\n" +
                    "3. 上课时间（星期几、开始时间、结束时间）\\n" +
                    "4. 备注信息（如果有）\\n\\n" +
                    "输出格式要求：\\n" +
                    "- 每个课程安排占一行\\n" +
                    "- 格式：学生姓名：[姓名]，科目：[科目]，时间：[星期几][开始时间]-[结束时间]，备注：[备注]\\n" +
                    "- 时间格式使用24小时制，如：9:00-10:00\\n" +
                    "- 星期格式使用中文，如：周一、周二等\\n\\n" +
                    "示例输出：\\n" +
                    "学生姓名：张三，科目：数学，时间：周一9:00-10:00，备注：无\\n" +
                    "学生姓名：李四，科目：英语，时间：周二14:00-15:00，备注：需要准备教材\\n\\n" +
                    "如果无法提取完整信息，请尽量提取可用的部分。";
            
            // 构建请求JSON
            String requestJson = "{" +
                    "\"contents\": [{" +
                    "\"parts\": [{" +
                    "\"text\": \"" + promptText.replace("\"", "\\\"") + "\"" +
                    "}]" +
                    "}]" +
                    "}";
            
            // 发送请求
            String apiUrl = nlpApiUrl + "models/" + nlpModel + ":generateContent?key=" + nlpApiKey;
            RequestBody body = RequestBody.create(requestJson, MediaType.get("application/json"));
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("自然语言处理API调用失败: {}", response.body().string());
                    throw new RuntimeException("自然语言处理失败");
                }
                
                String responseBody = response.body().string();
                logger.debug("自然语言处理API响应: {}", responseBody);
                
                // 解析响应
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                JsonNode candidates = jsonNode.path("candidates");
                
                if (candidates.isArray() && candidates.size() > 0) {
                    JsonNode content = candidates.get(0).path("content");
                    JsonNode parts = content.path("parts");
                    
                    if (parts.isArray() && parts.size() > 0) {
                        String processedText = parts.get(0).path("text").asText();
                        logger.info("自然语言处理成功: {}", processedText);
                        return processedText;
                    }
                }
                
                throw new RuntimeException("自然语言处理响应格式异常");
            }
        } catch (Exception e) {
            logger.error("自然语言处理失败", e);
            throw new IOException("自然语言处理失败: " + e.getMessage());
        }
    }
    
    /**
     * 智能分析课程安排冲突
     */
    public String analyzeScheduleConflicts(String existingSchedules, String newSchedule) throws IOException {
        try {
            // 构建提示文本
            String promptText = "请分析以下课程安排是否存在时间冲突：\\n\\n" +
                    "现有课程安排：\\n" + existingSchedules + "\\n\\n" +
                    "新增课程安排：\\n" + newSchedule + "\\n\\n" +
                    "请检查：\\n" +
                    "1. 时间段是否重叠\\n" +
                    "2. 同一时间是否安排了多个学生\\n" +
                    "3. 是否违反了常见的排课规则\\n\\n" +
                    "如果发现冲突，请详细说明冲突内容和建议的解决方案。\\n" +
                    "如果没有冲突，请回复\"无冲突\"。";
            
            // 构建请求JSON
            String requestJson = "{" +
                    "\"contents\": [{" +
                    "\"parts\": [{" +
                    "\"text\": \"" + promptText.replace("\"", "\\\"") + "\"" +
                    "}]" +
                    "}]" +
                    "}";
            
            // 发送请求
            String apiUrl = nlpApiUrl + "models/" + nlpModel + ":generateContent?key=" + nlpApiKey;
            RequestBody body = RequestBody.create(requestJson, MediaType.get("application/json"));
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("冲突分析API调用失败");
                }
                
                String responseBody = response.body().string();
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                JsonNode candidates = jsonNode.path("candidates");
                
                if (candidates.isArray() && candidates.size() > 0) {
                    JsonNode content = candidates.get(0).path("content");
                    JsonNode parts = content.path("parts");
                    
                    if (parts.isArray() && parts.size() > 0) {
                        return parts.get(0).path("text").asText();
                    }
                }
                
                return "分析失败";
            }
        } catch (Exception e) {
            logger.error("冲突分析失败", e);
            throw new IOException("冲突分析失败: " + e.getMessage());
        }
    }
} 