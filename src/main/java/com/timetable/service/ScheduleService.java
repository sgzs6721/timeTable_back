package com.timetable.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.timetable.dto.ScheduleRequest;
import com.timetable.dto.ai.ScheduleInfo;
import com.timetable.generated.tables.pojos.Schedules;
import com.timetable.generated.tables.pojos.Timetables;
import com.timetable.repository.ScheduleRepository;
import com.timetable.repository.TimetableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 排课服务
 */
@Service
public class ScheduleService {
    
    private final ScheduleRepository scheduleRepository;
    private final AiNlpService aiNlpService;
    private final TimetableRepository timetableRepository;
    private final SiliconFlowService siliconFlowService;
    private final ObjectMapper objectMapper;

    @Autowired
    public ScheduleService(ScheduleRepository scheduleRepository, AiNlpService aiNlpService, 
                          TimetableRepository timetableRepository, SiliconFlowService siliconFlowService) {
        this.scheduleRepository = scheduleRepository;
        this.aiNlpService = aiNlpService;
        this.timetableRepository = timetableRepository;
        this.siliconFlowService = siliconFlowService;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 获取课表的排课列表
     */
    public List<Schedules> getTimetableSchedules(Long timetableId, Integer week) {
        if (week != null) {
            return scheduleRepository.findByTimetableIdAndWeekNumber(timetableId, week);
        }
        return scheduleRepository.findByTimetableId(timetableId);
    }
    
    /**
     * 创建新排课
     */
    public Schedules createSchedule(Long timetableId, ScheduleRequest request) {
        Schedules schedule = new Schedules();
        schedule.setTimetableId(timetableId);
        schedule.setStudentName(request.getStudentName());
        schedule.setSubject(request.getSubject());
        schedule.setDayOfWeek(request.getDayOfWeek() == null ? null : request.getDayOfWeek().name());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setWeekNumber(request.getWeekNumber());
        schedule.setScheduleDate(request.getScheduleDate());
        schedule.setNote(request.getNote());
        scheduleRepository.save(schedule);
        return schedule;
    }
    
    /**
     * 更新排课
     */
    public Schedules updateSchedule(Long timetableId, Long scheduleId, ScheduleRequest request) {
        Schedules schedule = scheduleRepository.findByIdAndTimetableId(scheduleId, timetableId);
        if (schedule == null) {
            return null;
        }
        
        schedule.setStudentName(request.getStudentName());
        schedule.setSubject(request.getSubject());
        schedule.setDayOfWeek(request.getDayOfWeek() == null ? null : request.getDayOfWeek().name());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setWeekNumber(request.getWeekNumber());
        schedule.setScheduleDate(request.getScheduleDate());
        schedule.setNote(request.getNote());
        schedule.setUpdatedAt(LocalDateTime.now());
        scheduleRepository.update(schedule);
        return schedule;
    }
    
    /**
     * 删除排课
     */
    public boolean deleteSchedule(Long timetableId, Long scheduleId) {
        if (!scheduleRepository.existsByIdAndTimetableId(scheduleId, timetableId)) {
            return false;
        }
        
        scheduleRepository.deleteById(scheduleId);
        return true;
    }
    
    /**
     * 通过语音输入创建排课
     */
    public Mono<List<ScheduleInfo>> createScheduleByVoice(Long timetableId, MultipartFile audioFile, String type) {
        if (timetableRepository.findById(timetableId) == null) {
            return Mono.error(new IllegalArgumentException("Timetable not found"));
        }

        return Mono.fromCallable(() -> siliconFlowService.transcribeAudio(audioFile))
                .flatMap(transcribedText -> {
                    if (transcribedText == null || transcribedText.trim().isEmpty()) {
                        return Mono.just(Collections.emptyList());
                    }
                    return this.extractScheduleInfoFromText(transcribedText, type);
                })
                .onErrorResume(e -> {
                    logger.error("Error processing voice input for timetableId: " + timetableId, e);
                    return Mono.just(Collections.singletonList(createFallbackScheduleInfo("语音处理失败: " + e.getMessage())));
                });
    }
    
    /**
     * 解析AI返回的JSON响应并创建排课记录
     */
    private Schedules parseAiResponseAndCreateSchedule(Long timetableId, String aiResponse, String originalText) throws Exception {
        try {
            // 尝试从AI响应中提取JSON
            String jsonStr = extractJsonFromAiResponse(aiResponse);
            JsonNode jsonNode = objectMapper.readTree(jsonStr);
            
            // 创建排课对象
            Schedules schedule = new Schedules();
            schedule.setTimetableId(timetableId);
            schedule.setStudentName(jsonNode.path("studentName").asText("未知学生"));
            schedule.setSubject(jsonNode.path("subject").asText("未知科目"));
            
            // 解析星期
            String dayOfWeekStr = jsonNode.path("dayOfWeek").asText("MONDAY");
            schedule.setDayOfWeek(dayOfWeekStr);
            
            // 解析时间
            String startTimeStr = jsonNode.path("startTime").asText("09:00");
            String endTimeStr = jsonNode.path("endTime").asText("10:00");
            schedule.setStartTime(LocalTime.parse(startTimeStr, DateTimeFormatter.ofPattern("HH:mm")));
            schedule.setEndTime(LocalTime.parse(endTimeStr, DateTimeFormatter.ofPattern("HH:mm")));
            
            // 设置备注
            String note = jsonNode.path("note").asText("");
            schedule.setNote("语音输入: " + originalText + (note.isEmpty() ? "" : " | 备注: " + note));
            schedule.setCreatedAt(LocalDateTime.now());
            schedule.setUpdatedAt(LocalDateTime.now());
            
            // 保存到数据库
            scheduleRepository.save(schedule);
            return schedule;
            
        } catch (Exception e) {
            // 如果JSON解析失败，尝试使用规则解析
            return parseTextWithRulesAndCreate(timetableId, originalText);
        }
    }
    
    /**
     * 从AI响应中提取JSON字符串
     */
    private String extractJsonFromAiResponse(String aiResponse) {
        // 查找JSON开始和结束标记
        int startIndex = aiResponse.indexOf("{");
        int endIndex = aiResponse.lastIndexOf("}");
        
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return aiResponse.substring(startIndex, endIndex + 1);
        }
        
        throw new RuntimeException("无法从AI响应中提取JSON");
    }
    
    /**
     * 使用规则解析文本并创建排课
     */
    private Schedules parseTextWithRulesAndCreate(Long timetableId, String text) {
        Schedules schedule = new Schedules();
        schedule.setTimetableId(timetableId);
        schedule.setNote("语音输入: " + text);
        schedule.setCreatedAt(LocalDateTime.now());
        schedule.setUpdatedAt(LocalDateTime.now());
        
        // 简单的规则解析（可以根据需要扩展）
        schedule.setStudentName(extractStudentName(text));
        schedule.setSubject(extractSubject(text));
        schedule.setDayOfWeek(extractDayOfWeek(text).name());
        
        LocalTime[] times = extractTimes(text);
        schedule.setStartTime(times[0]);
        schedule.setEndTime(times[1]);
        
        scheduleRepository.save(schedule);
        return schedule;
    }
    
    /**
     * 创建备用排课记录（当所有解析都失败时使用）
     */
    private ScheduleInfo createFallbackScheduleInfo(String errorMsg) {
        return new ScheduleInfo("待确认", "09:00-10:00", "MONDAY", null, errorMsg);
    }
    
    /**
     * 从文本中提取学生姓名
     */
    private String extractStudentName(String text) {
        // 简单的正则匹配中文姓名
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([\\u4e00-\\u9fa5]{2,4})(同学|学生)?");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "未知学生";
    }
    
    /**
     * 从文本中提取科目
     */
    private String extractSubject(String text) {
        String[] subjects = {"数学", "语文", "英语", "物理", "化学", "生物", "历史", "地理", "政治", "音乐", "美术", "体育", "科学", "编程", "钢琴", "小提琴", "舞蹈"};
        for (String subject : subjects) {
            if (text.contains(subject)) {
                return subject;
            }
        }
        return "未知科目";
    }
    
    /**
     * 从文本中提取星期
     */
    private DayOfWeek extractDayOfWeek(String text) {
        String[] days = {"周一", "星期一", "周二", "星期二", "周三", "星期三", "周四", "星期四", "周五", "星期五", "周六", "星期六", "周日", "星期日", "周天"};
        for (String day : days) {
            if (text.contains(day)) {
                return DayOfWeek.valueOf(day.replace("星期", "").replace("周", "").replace("天", ""));
            }
        }
        return DayOfWeek.MONDAY;
    }
    
    /**
     * 从文本中提取时间
     */
    private LocalTime[] extractTimes(String text) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d{1,2})[:.：]?(\\d{0,2})\\s*[至到\\-~]\\s*(\\d{1,2})[:.：]?(\\d{0,2})");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            int startHour = Integer.parseInt(matcher.group(1));
            int startMin = matcher.group(2).isEmpty() ? 0 : Integer.parseInt(matcher.group(2));
            int endHour = Integer.parseInt(matcher.group(3));
            int endMin = matcher.group(4).isEmpty() ? 0 : Integer.parseInt(matcher.group(4));
            
            return new LocalTime[]{LocalTime.of(startHour, startMin), LocalTime.of(endHour, endMin)};
        }
        
        // 默认时间
        return new LocalTime[]{LocalTime.of(9, 0), LocalTime.of(10, 0)};
    }

    /**
     * 通过文本输入提取排课信息
     */
    public Mono<List<ScheduleInfo>> extractScheduleInfoFromText(String text, String type) {
        return aiNlpService.extractScheduleInfoFromText(text, type);
    }
    
    /**
     * 检查排课是否属于指定课表
     */
    public boolean isScheduleInTimetable(Long scheduleId, Long timetableId) {
        return scheduleRepository.existsByIdAndTimetableId(scheduleId, timetableId);
    }
    
    /**
     * 批量创建排课
     */
    public List<Schedules> createSchedules(Long timetableId, List<ScheduleRequest> requests) {
        List<Schedules> result = new java.util.ArrayList<>();
        for (ScheduleRequest req : requests) {
            result.add(createSchedule(timetableId, req));
        }
        return result;
    }
    
    /**
     * 按条件批量删除排课
     */
    public int deleteSchedulesByCondition(Long timetableId, ScheduleRequest request) {
        return scheduleRepository.deleteByCondition(timetableId, request);
    }
    
    /**
     * 批量按条件删除排课
     */
    public int deleteSchedulesBatch(Long timetableId, List<ScheduleRequest> requests) {
        int total = 0;
        for (ScheduleRequest req : requests) {
            total += scheduleRepository.deleteByCondition(timetableId, req);
        }
        return total;
    }
} 