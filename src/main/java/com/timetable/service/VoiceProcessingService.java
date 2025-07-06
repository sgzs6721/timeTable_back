package com.timetable.service;

import com.timetable.generated.tables.pojos.Schedules;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 语音处理服务
 * 集成AI功能进行语音识别和自然语言处理
 */
@Service
public class VoiceProcessingService {
    
    @Value("${ai.speech.provider:google}")
    private String speechProvider;
    
    @Value("${ai.speech.api-key}")
    private String speechApiKey;
    
    @Value("${ai.nlp.provider:google}")
    private String nlpProvider;
    
    @Value("${ai.nlp.api-key}")
    private String nlpApiKey;
    
    @Value("${ai.nlp.model:gemini-2.5-flash}")
    private String nlpModel;
    
    private final AIService aiService;
    
    public VoiceProcessingService(AIService aiService) {
        this.aiService = aiService;
    }
    
    /**
     * 处理语音输入，转换为课程安排
     */
    public List<Schedules> processVoiceInput(Long timetableId, MultipartFile audioFile) throws IOException {
        // 1. 语音转文字
        String transcribedText = transcribeAudio(audioFile);
        
        // 2. 使用AI处理自然语言，提取课程信息
        return processTextToSchedules(timetableId, transcribedText);
    }
    
    /**
     * 处理文本输入，转换为课程安排
     */
    public List<Schedules> processTextInput(Long timetableId, String inputText) {
        return processTextToSchedules(timetableId, inputText);
    }
    
    /**
     * 语音转文字
     */
    private String transcribeAudio(MultipartFile audioFile) throws IOException {
        // 使用AI服务进行语音识别
        return aiService.transcribeAudio(audioFile);
    }
    
    /**
     * 处理文本，提取课程安排信息
     */
    private List<Schedules> processTextToSchedules(Long timetableId, String text) {
        try {
            // 使用AI进行自然语言处理
            String aiResponse = aiService.processScheduleText(text);
            
            // 解析AI返回的结构化数据
            return parseAIResponse(timetableId, aiResponse);
        } catch (Exception e) {
            // 如果AI处理失败，使用规则引擎作为备用方案
            return parseTextWithRules(timetableId, text);
        }
    }
    
    /**
     * 解析AI返回的结构化数据
     */
    private List<Schedules> parseAIResponse(Long timetableId, String aiResponse) {
        List<Schedules> schedules = new ArrayList<>();
        
        try {
            // 简化的解析逻辑，实际应该使用JSON解析
            // 这里假设AI返回格式类似：
            // "学生姓名：张三，科目：数学，时间：周一9:00-10:00"
            
            String[] lines = aiResponse.split("\n");
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                
                Schedules schedule = parseScheduleLine(timetableId, line);
                if (schedule != null) {
                    schedules.add(schedule);
                }
            }
        } catch (Exception e) {
            // 解析失败时的处理
            throw new RuntimeException("AI响应解析失败: " + e.getMessage());
        }
        
        return schedules;
    }
    
    /**
     * 使用规则引擎解析文本（备用方案）
     */
    private List<Schedules> parseTextWithRules(Long timetableId, String text) {
        List<Schedules> schedules = new ArrayList<>();
        
        // 常见的时间表达式模式
        Pattern timePattern = Pattern.compile("(\\d{1,2})[:.：]?(\\d{0,2})\\s*[至到—\\-~]\\s*(\\d{1,2})[:.：]?(\\d{0,2})");
        Pattern dayPattern = Pattern.compile("(周[一二三四五六日天]|星期[一二三四五六日天]|Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday)");
        Pattern namePattern = Pattern.compile("([\\u4e00-\\u9fa5]{2,4})(同学|学生)?");
        Pattern subjectPattern = Pattern.compile("(数学|语文|英语|物理|化学|生物|历史|地理|政治|音乐|美术|体育|科学|编程|钢琴|小提琴|舞蹈)课?");
        
        // 提取时间
        Matcher timeMatcher = timePattern.matcher(text);
        String timeStr = "";
        LocalTime startTime = null;
        LocalTime endTime = null;
        
        if (timeMatcher.find()) {
            int startHour = Integer.parseInt(timeMatcher.group(1));
            int startMin = timeMatcher.group(2).isEmpty() ? 0 : Integer.parseInt(timeMatcher.group(2));
            int endHour = Integer.parseInt(timeMatcher.group(3));
            int endMin = timeMatcher.group(4).isEmpty() ? 0 : Integer.parseInt(timeMatcher.group(4));
            
            startTime = LocalTime.of(startHour, startMin);
            endTime = LocalTime.of(endHour, endMin);
        }
        
        // 提取星期
        Matcher dayMatcher = dayPattern.matcher(text);
        java.time.DayOfWeek dayOfWeek = null;
        
        if (dayMatcher.find()) {
            String dayStr = dayMatcher.group(1);
            dayOfWeek = convertChineseDayToEnum(dayStr);
        }
        
        // 提取学生姓名
        Matcher nameMatcher = namePattern.matcher(text);
        String studentName = "";
        if (nameMatcher.find()) {
            studentName = nameMatcher.group(1);
        }
        
        // 提取科目
        Matcher subjectMatcher = subjectPattern.matcher(text);
        String subject = "";
        if (subjectMatcher.find()) {
            subject = subjectMatcher.group(1);
        }
        
        // 创建课程安排
        if (startTime != null && endTime != null && dayOfWeek != null && !studentName.isEmpty()) {
            Schedules schedule = new Schedules();
            schedule.setTimetableId(timetableId);
            schedule.setStudentName(studentName);
            schedule.setSubject(subject);
            schedule.setDayOfWeek(dayOfWeek.name());
            schedule.setStartTime(startTime);
            schedule.setEndTime(endTime);
            // 设置创建和更新时间
            schedule.setCreatedAt(java.time.LocalDateTime.now());
            schedule.setUpdatedAt(java.time.LocalDateTime.now());
            schedules.add(schedule);
        }
        
        return schedules;
    }
    
    /**
     * 解析单行课程信息
     */
    private Schedules parseScheduleLine(Long timetableId, String line) {
        // 简化的解析逻辑
        // 实际应该根据AI返回的具体格式进行解析
        try {
            // 示例解析逻辑
            String[] parts = line.split("，|,");
            String studentName = "";
            String subject = "";
            java.time.DayOfWeek dayOfWeek = null;
            LocalTime startTime = null;
            LocalTime endTime = null;
            
            for (String part : parts) {
                part = part.trim();
                if (part.contains("学生") || part.contains("姓名")) {
                    studentName = extractValue(part);
                } else if (part.contains("科目") || part.contains("课程")) {
                    subject = extractValue(part);
                } else if (part.contains("时间")) {
                    // 解析时间信息
                    String timeInfo = extractValue(part);
                    String[] timeComponents = parseTimeInfo(timeInfo);
                    if (timeComponents.length >= 3) {
                        dayOfWeek = convertChineseDayToEnum(timeComponents[0]);
                        startTime = LocalTime.parse(timeComponents[1], DateTimeFormatter.ofPattern("H:mm"));
                        endTime = LocalTime.parse(timeComponents[2], DateTimeFormatter.ofPattern("H:mm"));
                    }
                }
            }
            
            if (!studentName.isEmpty() && dayOfWeek != null && startTime != null && endTime != null) {
                Schedules schedule = new Schedules();
                schedule.setTimetableId(timetableId);
                schedule.setStudentName(studentName);
                schedule.setSubject(subject);
                schedule.setDayOfWeek(dayOfWeek.name());
                schedule.setStartTime(startTime);
                schedule.setEndTime(endTime);
                // 设置创建和更新时间
                schedule.setCreatedAt(java.time.LocalDateTime.now());
                schedule.setUpdatedAt(java.time.LocalDateTime.now());
                return schedule;
            }
        } catch (Exception e) {
            // 解析失败，忽略该行
        }
        
        return null;
    }
    
    /**
     * 从字符串中提取值
     */
    private String extractValue(String text) {
        int colonIndex = text.indexOf("：");
        if (colonIndex == -1) {
            colonIndex = text.indexOf(":");
        }
        if (colonIndex != -1 && colonIndex + 1 < text.length()) {
            return text.substring(colonIndex + 1).trim();
        }
        return text.trim();
    }
    
    /**
     * 解析时间信息
     */
    private String[] parseTimeInfo(String timeInfo) {
        // 示例：周一9:00-10:00
        Pattern pattern = Pattern.compile("(周[一二三四五六日天])(\\d{1,2}:\\d{2})-(\\d{1,2}:\\d{2})");
        Matcher matcher = pattern.matcher(timeInfo);
        
        if (matcher.find()) {
            return new String[]{matcher.group(1), matcher.group(2), matcher.group(3)};
        }
        
        return new String[0];
    }
    
    /**
     * 转换中文星期到枚举
     */
    private java.time.DayOfWeek convertChineseDayToEnum(String chineseDay) {
        Map<String, java.time.DayOfWeek> dayMap = new HashMap<>();
        dayMap.put("周一", java.time.DayOfWeek.MONDAY);
        dayMap.put("周二", java.time.DayOfWeek.TUESDAY);
        dayMap.put("周三", java.time.DayOfWeek.WEDNESDAY);
        dayMap.put("周四", java.time.DayOfWeek.THURSDAY);
        dayMap.put("周五", java.time.DayOfWeek.FRIDAY);
        dayMap.put("周六", java.time.DayOfWeek.SATURDAY);
        dayMap.put("周日", java.time.DayOfWeek.SUNDAY);
        dayMap.put("周天", java.time.DayOfWeek.SUNDAY);
        dayMap.put("星期一", java.time.DayOfWeek.MONDAY);
        dayMap.put("星期二", java.time.DayOfWeek.TUESDAY);
        dayMap.put("星期三", java.time.DayOfWeek.WEDNESDAY);
        dayMap.put("星期四", java.time.DayOfWeek.THURSDAY);
        dayMap.put("星期五", java.time.DayOfWeek.FRIDAY);
        dayMap.put("星期六", java.time.DayOfWeek.SATURDAY);
        dayMap.put("星期日", java.time.DayOfWeek.SUNDAY);
        dayMap.put("星期天", java.time.DayOfWeek.SUNDAY);
        
        return dayMap.getOrDefault(chineseDay, java.time.DayOfWeek.MONDAY);
    }
} 