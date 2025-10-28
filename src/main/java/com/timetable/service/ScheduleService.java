package com.timetable.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.timetable.dto.ScheduleRequest;
import com.timetable.dto.ai.ScheduleInfo;
import com.timetable.dto.ConflictInfo;
import com.timetable.dto.ConflictCheckResult;
import com.timetable.entity.WeeklyInstance;
import com.timetable.generated.tables.pojos.Schedules;
import com.timetable.generated.tables.pojos.Timetables;
import com.timetable.repository.ScheduleRepository;
import com.timetable.repository.TimetableRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.Iterator;
import java.time.temporal.TemporalAdjusters;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.HashMap;

/**
 * 排课服务
 */
@Service
public class ScheduleService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleService.class);

    private final ScheduleRepository scheduleRepository;
    private final AiNlpService aiNlpService;
    private final TimetableRepository timetableRepository;
    private final WeeklyInstanceService weeklyInstanceService;

    private final AIService aiService;
    private final ObjectMapper objectMapper;

    private static final Map<String, DayOfWeek> weekDayMap = new LinkedHashMap<>();
    static {
        weekDayMap.put("一", DayOfWeek.MONDAY);
        weekDayMap.put("1", DayOfWeek.MONDAY);
        weekDayMap.put("二", DayOfWeek.TUESDAY);
        weekDayMap.put("2", DayOfWeek.TUESDAY);
        weekDayMap.put("三", DayOfWeek.WEDNESDAY);
        weekDayMap.put("3", DayOfWeek.WEDNESDAY);
        weekDayMap.put("四", DayOfWeek.THURSDAY);
        weekDayMap.put("4", DayOfWeek.THURSDAY);
        weekDayMap.put("五", DayOfWeek.FRIDAY);
        weekDayMap.put("5", DayOfWeek.FRIDAY);
        weekDayMap.put("六", DayOfWeek.SATURDAY);
        weekDayMap.put("6", DayOfWeek.SATURDAY);
        weekDayMap.put("日", DayOfWeek.SUNDAY);
        weekDayMap.put("天", DayOfWeek.SUNDAY);
        weekDayMap.put("7", DayOfWeek.SUNDAY);
    }

    private static class ChineseNumberConverter {
        private static final Map<Character, Integer> CN_NUM_MAP = new HashMap<>();
        static {
            CN_NUM_MAP.put('零', 0); CN_NUM_MAP.put('一', 1); CN_NUM_MAP.put('二', 2);
            CN_NUM_MAP.put('三', 3); CN_NUM_MAP.put('四', 4); CN_NUM_MAP.put('五', 5);
            CN_NUM_MAP.put('六', 6); CN_NUM_MAP.put('日', 7); CN_NUM_MAP.put('天', 7); // '日'和'天'在星期中特殊处理
            CN_NUM_MAP.put('七', 7); CN_NUM_MAP.put('八', 8); CN_NUM_MAP.put('九', 9);
            CN_NUM_MAP.put('十', 10);

            CN_NUM_MAP.put('壹', 1); CN_NUM_MAP.put('贰', 2); CN_NUM_MAP.put('叁', 3);
            CN_NUM_MAP.put('肆', 4); CN_NUM_MAP.put('伍', 5); CN_NUM_MAP.put('陆', 6);
            CN_NUM_MAP.put('柒', 7); CN_NUM_MAP.put('捌', 8); CN_NUM_MAP.put('玖', 9);
            CN_NUM_MAP.put('拾', 10);
        }

        public static String convert(String chineseNumber) {
            StringBuilder sb = new StringBuilder();
            for (char c : chineseNumber.toCharArray()) {
                if (CN_NUM_MAP.containsKey(c)) {
                    sb.append(CN_NUM_MAP.get(c));
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }
    }

    @Autowired
    public ScheduleService(ScheduleRepository scheduleRepository, AiNlpService aiNlpService,
                          TimetableRepository timetableRepository,
                          WeeklyInstanceService weeklyInstanceService,
                          AIService aiService) {
        this.scheduleRepository = scheduleRepository;
        this.aiNlpService = aiNlpService;
        this.timetableRepository = timetableRepository;
        this.weeklyInstanceService = weeklyInstanceService;

        this.aiService = aiService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 获取课表的排课列表
     */
    public List<Schedules> getTimetableSchedules(Long timetableId, Integer week) {
        return getTimetableSchedules(timetableId, week, false);
    }

    /**
     * 获取今日课程（从实例数据中获取）
     */
    public List<Schedules> getTodaySchedules(Long timetableId) {
        Timetables timetable = timetableRepository.findById(timetableId);
        if (timetable == null) {
            logger.warn("Timetable with ID {} not found.", timetableId);
            return Collections.emptyList();
        }

        LocalDate today = LocalDate.now();
        
        if (timetable.getIsWeekly() == 1) {
            // 周固定课表：从周实例数据获取今日课程
            try {
                List<com.timetable.entity.WeeklyInstanceSchedule> instanceSchedules = 
                    weeklyInstanceService.getSchedulesByDate(timetableId, today);
                
                // 转换为 Schedules 格式
                return instanceSchedules.stream()
                    .map(this::convertWeeklyInstanceScheduleToSchedule)
                    .collect(Collectors.toList());
            } catch (Exception e) {
                logger.warn("Failed to get today's schedules from weekly instance for timetable {}: {}", 
                    timetableId, e.getMessage());
                return Collections.emptyList();
            }
        } else {
            // 日期范围课表：获取今日课程
            return scheduleRepository.findByTimetableIdAndScheduleDate(timetableId, today);
        }
    }

    /**
     * 获取明日课程（从实例数据中获取）
     */
    public List<Schedules> getTomorrowSchedules(Long timetableId) {
        Timetables timetable = timetableRepository.findById(timetableId);
        if (timetable == null) {
            logger.warn("Timetable with ID {} not found.", timetableId);
            return Collections.emptyList();
        }

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        
        if (timetable.getIsWeekly() == 1) {
            // 周固定课表：从周实例数据获取明日课程
            try {
                List<com.timetable.entity.WeeklyInstanceSchedule> instanceSchedules = 
                    weeklyInstanceService.getSchedulesByDate(timetableId, tomorrow);
                
                // 转换为 Schedules 格式
                return instanceSchedules.stream()
                    .map(this::convertWeeklyInstanceScheduleToSchedule)
                    .collect(Collectors.toList());
            } catch (Exception e) {
                logger.warn("Failed to get tomorrow's schedules from weekly instance for timetable {}: {}", 
                    timetableId, e.getMessage());
                return Collections.emptyList();
            }
        } else {
            // 日期范围课表：获取明日课程
            return scheduleRepository.findByTimetableIdAndScheduleDate(timetableId, tomorrow);
        }
    }

    /**
     * 获取本周课程（从实例数据中获取）
     */
    public List<Schedules> getThisWeekSchedules(Long timetableId) {
        Timetables timetable = timetableRepository.findById(timetableId);
        if (timetable == null) {
            logger.warn("Timetable with ID {} not found.", timetableId);
            return Collections.emptyList();
        }

        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate sunday = today.with(DayOfWeek.SUNDAY);
        
        if (timetable.getIsWeekly() == 1) {
            // 周固定课表：从周实例数据获取本周课程
            try {
                // 先确保存在当前周实例
                WeeklyInstance currentInstance = weeklyInstanceService.getCurrentWeekInstance(timetableId);
                if (currentInstance == null) {
                    try {
                        currentInstance = weeklyInstanceService.generateCurrentWeekInstance(timetableId);
                    } catch (Exception ignore) {}
                }
                // 注释掉强制同步，避免删除实例课程后又被同步回来
                // if (currentInstance != null) {
                //     weeklyInstanceService.syncTemplateToCurrentInstanceSelectively(currentInstance);
                // }
                // 再获取本周实例课程
                List<com.timetable.entity.WeeklyInstanceSchedule> instanceSchedules = 
                    weeklyInstanceService.getCurrentWeekInstanceSchedules(timetableId);
                
                // 转换为 Schedules 格式，并过滤掉请假的课程
                return instanceSchedules.stream()
                    .filter(s -> s.getIsOnLeave() == null || !s.getIsOnLeave()) // 过滤掉请假的课程
                    .map(this::convertWeeklyInstanceScheduleToSchedule)
                    .collect(Collectors.toList());
            } catch (Exception e) {
                logger.warn("Failed to get this week's schedules from weekly instance for timetable {}: {}", 
                    timetableId, e.getMessage());
                return Collections.emptyList();
            }
        } else {
            // 日期范围课表：获取本周课程（日期范围课表没有请假功能，直接返回）
            return scheduleRepository.findByTimetableIdAndScheduleDateBetween(timetableId, monday, sunday);
        }
    }

    /**
     * 获取固定课表模板（只获取模板数据）
     */
    public List<Schedules> getTemplateSchedules(Long timetableId) {
        Timetables timetable = timetableRepository.findById(timetableId);
        if (timetable == null) {
            logger.warn("Timetable with ID {} not found.", timetableId);
            return Collections.emptyList();
        }

        if (timetable.getIsWeekly() == 1) {
            // 周固定课表：只获取模板数据（scheduleDate为null的记录）
            return scheduleRepository.findTemplateSchedulesByTimetableId(timetableId);
        } else {
            // 日期范围课表：按当前周获取
            LocalDate startDate = LocalDate.parse(timetable.getStartDate().toString());
            LocalDate currentDate = LocalDate.now();
            int currentWeek = (int) ChronoUnit.WEEKS.between(startDate, currentDate) + 1;
            return scheduleRepository.findByTimetableIdAndWeekNumber(timetableId, currentWeek);
        }
    }

    /**
     * 统一汇总：今日/明日/本周/固定
     */
    public com.timetable.dto.SchedulesOverviewResponse getSchedulesOverview(Long timetableId) {
        com.timetable.dto.SchedulesOverviewResponse overview = new com.timetable.dto.SchedulesOverviewResponse();
        overview.setToday(getTodaySchedules(timetableId));
        overview.setTomorrow(getTomorrowSchedules(timetableId));
        overview.setThisWeek(getThisWeekSchedules(timetableId));
        overview.setTemplate(getTemplateSchedules(timetableId));
        return overview;
    }
    /**
     * 获取课表的排课列表（支持只获取模板数据）
     */
    public List<Schedules> getTimetableSchedules(Long timetableId, Integer week, Boolean templateOnly) {
        Timetables timetable = timetableRepository.findById(timetableId);
        if (timetable == null) {
            logger.warn("Timetable with ID {} not found.", timetableId);
            return Collections.emptyList();
        }

        // For WEEKLY timetables, week-based filtering is based on the week_number field.
        if (timetable.getIsWeekly() == 1) {
            if (templateOnly != null && templateOnly) {
                // 只获取模板数据：scheduleDate为null的记录
                return scheduleRepository.findTemplateSchedulesByTimetableId(timetableId);
            }
            if (week != null && week > 0) {
                return scheduleRepository.findByTimetableIdAndWeekNumber(timetableId, week);
            }
            return scheduleRepository.findByTimetableId(timetableId);
        }

        // For DATE-RANGE timetables, a "week" is defined as the calendar week (Monday-Sunday) that contains the timetable's start date.
        if (week != null && week > 0) {
            LocalDate timetableStartDate = timetable.getStartDate();
            if (timetableStartDate == null) {
                // This is an invalid state for a date-range timetable.
                logger.error("Date-range timetable {} has a null start date.", timetableId);
                return Collections.emptyList();
            }

            // Find the Monday of the week where the timetable officially starts. This is our anchor for all week calculations.
            LocalDate anchorMonday = timetableStartDate.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

            // Calculate the start and end date for the requested week (N).
            LocalDate weekStartDate = anchorMonday.plusWeeks(week - 1);
            LocalDate weekEndDate = weekStartDate.plusDays(6); // Monday + 6 days = Sunday

            return scheduleRepository.findByTimetableIdAndScheduleDateBetween(timetableId, weekStartDate, weekEndDate);
        }

        // If 'week' is not provided for a date-range timetable, return all its schedules.
        return scheduleRepository.findByTimetableId(timetableId);
    }

    /**
     * 根据学生姓名获取课表的排课列表
     */
    public List<Schedules> getTimetableSchedulesByStudent(Long timetableId, String studentName, Integer week) {
        Timetables timetable = timetableRepository.findById(timetableId);
        if (timetable == null) {
            logger.warn("Timetable with ID {} not found.", timetableId);
            return Collections.emptyList();
        }

        // For WEEKLY timetables, week-based filtering is based on the week_number field.
        if (timetable.getIsWeekly() == 1) {
            if (week != null && week > 0) {
                return scheduleRepository.findByTimetableIdAndStudentNameAndWeek(timetableId, studentName, week);
            }
            return scheduleRepository.findByTimetableIdAndStudentName(timetableId, studentName);
        }

        // For DATE-RANGE timetables, a "week" is defined as the calendar week (Monday-Sunday) that contains the timetable's start date.
        if (week != null && week > 0) {
            LocalDate timetableStartDate = timetable.getStartDate();
            if (timetableStartDate == null) {
                // This is an invalid state for a date-range timetable.
                logger.error("Date-range timetable {} has a null start date.", timetableId);
                return Collections.emptyList();
            }

            // Find the Monday of the week where the timetable officially starts. This is our anchor for all week calculations.
            LocalDate anchorMonday = timetableStartDate.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

            // Calculate the start and end date for the requested week (N).
            LocalDate weekStartDate = anchorMonday.plusWeeks(week - 1);
            LocalDate weekEndDate = weekStartDate.plusDays(6); // Monday + 6 days = Sunday

            // 先获取指定周数的所有课程，然后按学生姓名过滤
            List<Schedules> weekSchedules = scheduleRepository.findByTimetableIdAndScheduleDateBetween(timetableId, weekStartDate, weekEndDate);
            return weekSchedules.stream()
                    .filter(schedule -> studentName.equals(schedule.getStudentName()))
                    .collect(Collectors.toList());
        }

        // If 'week' is not provided for a date-range timetable, return all schedules for the student.
        return scheduleRepository.findByTimetableIdAndStudentName(timetableId, studentName);
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

        // 对不同类型课表的字段进行规范化：
        // - 日期范围课表：忽略 weekNumber，允许具体 scheduleDate
        // - 周固定课表：允许 weekNumber；强制 scheduleDate = null（写入模板）
        Timetables timetable = timetableRepository.findById(timetableId);
        if (timetable != null && timetable.getIsWeekly() != null && timetable.getIsWeekly() == 1) {
            schedule.setWeekNumber(request.getWeekNumber());
            schedule.setScheduleDate(null); // 强制写入模板
        } else {
            schedule.setWeekNumber(null);
            schedule.setScheduleDate(request.getScheduleDate());
        }
        schedule.setNote(request.getNote());
        // 设置创建和更新时间
        schedule.setCreatedAt(LocalDateTime.now());
        schedule.setUpdatedAt(LocalDateTime.now());
        scheduleRepository.save(schedule);

        // 周固定课表：如果是模板课程（scheduleDate为空），选择性同步到“当前周实例”的未来时段
        try {
            if (timetable != null && timetable.getIsWeekly() != null && timetable.getIsWeekly() == 1
                    && schedule.getScheduleDate() == null) {
                weeklyInstanceService.syncSpecificTemplateSchedulesToCurrentInstanceByTime(timetableId,
                        java.util.Collections.singletonList(schedule));
            }
        } catch (Exception e) {
            logger.warn("Selective sync to current week instance failed after create. timetableId={}, scheduleId={}",
                    timetableId, schedule.getId());
        }
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

        // 周固定课表模板：仅影响当前周实例中未来时段
        try {
            Timetables timetable = timetableRepository.findById(timetableId);
            if (timetable != null && timetable.getIsWeekly() != null && timetable.getIsWeekly() == 1
                    && schedule.getScheduleDate() == null) {
                weeklyInstanceService.syncSpecificTemplateSchedulesToCurrentInstanceByTime(timetableId,
                        java.util.Collections.singletonList(schedule));
            }
        } catch (Exception e) {
            logger.warn("Selective sync to current week instance failed after update. timetableId={}, scheduleId={}",
                    timetableId, scheduleId);
        }
        return schedule;
    }

    /**
     * 部分更新排课，仅修改传入的字段
     */
    public Schedules updateSchedule(Long timetableId, Long scheduleId, com.timetable.dto.UpdateScheduleRequest request) {
        Schedules schedule = scheduleRepository.findByIdAndTimetableId(scheduleId, timetableId);
        if (schedule == null) {
            return null;
        }

        if (request.getStudentName() != null) {
            schedule.setStudentName(request.getStudentName());
        }
        if (request.getSubject() != null) {
            schedule.setSubject(request.getSubject());
        }
        if (request.getDayOfWeek() != null) {
            schedule.setDayOfWeek(request.getDayOfWeek().name());
        }
        if (request.getStartTime() != null) {
            schedule.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            schedule.setEndTime(request.getEndTime());
        }
        if (request.getWeekNumber() != null) {
            schedule.setWeekNumber(request.getWeekNumber());
        }
        // 对不同类型课表的字段进行规范化更新
        Timetables timetable = timetableRepository.findById(timetableId);
        if (timetable != null && timetable.getIsWeekly() != null && timetable.getIsWeekly() == 1) {
            // 周固定课表：强制 scheduleDate 维持为 null（模板）
            schedule.setScheduleDate(null);
        } else {
            if (request.getScheduleDate() != null) {
                schedule.setScheduleDate(request.getScheduleDate());
            }
        }
        if (request.getNote() != null) {
            schedule.setNote(request.getNote());
        }
        if (request.getIsTrial() != null) {
            schedule.setIsTrial(request.getIsTrial() ? (byte) 1 : (byte) 0);
        }

        schedule.setUpdatedAt(LocalDateTime.now());
        scheduleRepository.update(schedule);

        // 周固定课表模板：仅影响当前周实例中未来时段
        try {
            if (timetable != null && timetable.getIsWeekly() != null && timetable.getIsWeekly() == 1
                    && schedule.getScheduleDate() == null) {
                weeklyInstanceService.syncSpecificTemplateSchedulesToCurrentInstanceByTime(timetableId,
                        java.util.Collections.singletonList(schedule));
            }
        } catch (Exception e) {
            logger.warn("Selective sync to current week instance failed after partial update. timetableId={}, scheduleId={}",
                    timetableId, scheduleId);
        }
        return schedule;
    }

    /**
     * 删除排课
     */
    public boolean deleteSchedule(Long timetableId, Long scheduleId) {
        Schedules schedule = scheduleRepository.findByIdAndTimetableId(scheduleId, timetableId);
        if (schedule == null) {
            return false;
        }

        // 周固定课表模板：仅删除当前周实例未来时段的对应实例课程
        try {
            Timetables timetable = timetableRepository.findById(timetableId);
            if (timetable != null && timetable.getIsWeekly() != null && timetable.getIsWeekly() == 1
                    && schedule.getScheduleDate() == null) {
                weeklyInstanceService.deleteCurrentWeekInstanceScheduleIfFuture(timetableId, schedule);
            }
        } catch (Exception e) {
            logger.warn("Selective delete from current week instance failed before delete. timetableId={}, scheduleId={}",
                    timetableId, scheduleId);
        }

        scheduleRepository.deleteById(scheduleId);
        return true;
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
     * 检查冲突并部分创建无冲突的排课
     * 返回结果包含：已创建的排课 + 冲突的排课信息
     */
    public ConflictCheckResult checkConflictsWithPartialCreation(Long timetableId, List<ScheduleRequest> requests) {
        List<Schedules> createdSchedules = new ArrayList<>();
        List<ConflictInfo> conflicts = new ArrayList<>();

        // 获取现有排课
        List<Schedules> existingSchedules = scheduleRepository.findByTimetableId(timetableId);

        for (ScheduleRequest request : requests) {
            boolean hasConflict = false;

            // 检查与现有排课的冲突
            for (Schedules existing : existingSchedules) {
                if (hasTimeConflict(request, existing)) {
                    ConflictInfo conflict = new ConflictInfo();
                    conflict.setConflictType(request.getStudentName().equals(existing.getStudentName()) ?
                        "STUDENT_TIME_CONFLICT" : "TIME_SLOT_CONFLICT");
                    conflict.setConflictDescription(generateConflictDescription(request, existing));
                    conflict.setNewSchedule(request);
                    conflict.setExistingSchedule(existing);
                    conflicts.add(conflict);
                    hasConflict = true;
                    break; // 找到一个冲突就够了
                }
            }

            // 检查与已创建排课的冲突
            if (!hasConflict) {
                for (Schedules created : createdSchedules) {
                    if (hasTimeConflict(request, created)) {
                        ConflictInfo conflict = new ConflictInfo();
                        conflict.setConflictType(request.getStudentName().equals(created.getStudentName()) ?
                            "STUDENT_TIME_CONFLICT" : "TIME_SLOT_CONFLICT");
                        conflict.setConflictDescription(generateConflictDescription(request, created));
                        conflict.setNewSchedule(request);
                        conflict.setExistingSchedule(created);
                        conflicts.add(conflict);
                        hasConflict = true;
                        break;
                    }
                }
            }

            // 如果没有冲突，直接创建
            if (!hasConflict) {
                try {
                    Schedules newSchedule = createSchedule(timetableId, request);
                    createdSchedules.add(newSchedule);
                    // 同时添加到现有排课列表中，以便后续冲突检查
                    existingSchedules.add(newSchedule);
                } catch (Exception e) {
                    logger.error("创建排课失败: {}", e.getMessage(), e);
                    // 创建失败也当作冲突处理
                    ConflictInfo conflict = new ConflictInfo();
                    conflict.setConflictType("CREATION_ERROR");
                    conflict.setConflictDescription("创建失败: " + e.getMessage());
                    conflict.setNewSchedule(request);
                    conflicts.add(conflict);
                }
            }
        }

        ConflictCheckResult result = new ConflictCheckResult();
        result.setHasConflicts(!conflicts.isEmpty());
        result.setConflicts(conflicts);
        result.setCreatedSchedules(createdSchedules);

        return result;
    }

    /**
     * 强制批量创建排课（智能覆盖）
     * - 如果是不同学员的时间冲突：删除原有排课，添加新排课
     * - 如果是同一学员的重复排课：保留原有排课，添加新排课
     */
    public List<Schedules> createSchedulesWithOverride(Long timetableId, List<ScheduleRequest> requests) {
        List<Schedules> result = new ArrayList<>();

        // 获取现有的排课数据
        List<Schedules> existingSchedules = scheduleRepository.findByTimetableId(timetableId);

        for (ScheduleRequest request : requests) {
            // 查找与当前请求冲突的现有排课
            List<Schedules> conflictingSchedules = new ArrayList<>();

            for (Schedules existing : existingSchedules) {
                if (hasTimeConflict(request, existing)) {
                    // 如果是不同学员的冲突，需要删除原有排课
                    if (!request.getStudentName().equals(existing.getStudentName())) {
                        conflictingSchedules.add(existing);
                    }
                    // 如果是同一学员，不删除，允许重复
                }
            }

            // 删除冲突的排课（仅限不同学员）
            for (Schedules conflicting : conflictingSchedules) {
                scheduleRepository.deleteById(conflicting.getId());
                logger.info("删除冲突排课: 学生={}, 时间={}-{}, ID={}",
                    conflicting.getStudentName(),
                    conflicting.getStartTime(),
                    conflicting.getEndTime(),
                    conflicting.getId());
            }

            // 创建新的排课
            Schedules newSchedule = createSchedule(timetableId, request);
            result.add(newSchedule);
        }

        return result;
    }

    /**
     * 检测排课冲突
     */
    public ConflictCheckResult checkScheduleConflicts(Long timetableId, List<ScheduleRequest> requests) {
        ConflictCheckResult result = new ConflictCheckResult();
        List<ConflictInfo> conflicts = new ArrayList<>();

        try {
            // 获取现有的排课数据
            List<Schedules> existingSchedules = scheduleRepository.findByTimetableId(timetableId);

            for (int i = 0; i < requests.size(); i++) {
                ScheduleRequest request = requests.get(i);

                // 检查与现有排课的冲突
                for (Schedules existing : existingSchedules) {
                    if (hasTimeConflict(request, existing)) {
                        ConflictInfo conflict = new ConflictInfo();
                        conflict.setNewScheduleIndex(i);
                        conflict.setNewSchedule(request);
                        conflict.setExistingSchedule(existing);
                        conflict.setConflictType(determineConflictType(request, existing));
                        conflict.setConflictDescription(generateConflictDescription(request, existing));
                        conflicts.add(conflict);
                    }
                }

                // 检查新排课之间的冲突
                for (int j = i + 1; j < requests.size(); j++) {
                    ScheduleRequest other = requests.get(j);
                    if (hasTimeConflict(request, other)) {
                        ConflictInfo conflict = new ConflictInfo();
                        conflict.setNewScheduleIndex(i);
                        conflict.setNewSchedule(request);
                        conflict.setOtherNewScheduleIndex(j);
                        conflict.setOtherNewSchedule(other);
                        conflict.setConflictType("NEW_SCHEDULE_CONFLICT");
                        conflict.setConflictDescription(generateConflictDescription(request, other));
                        conflicts.add(conflict);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("检测排课冲突时发生错误", e);
            // 发生错误时返回无冲突结果，让业务继续进行
        }

        result.setHasConflicts(!conflicts.isEmpty());
        result.setConflicts(conflicts);
        result.setTotalConflicts(conflicts.size());

        return result;
    }

    /**
     * 按条件批量删除排课
     */
    public int deleteSchedulesByCondition(Long timetableId, ScheduleRequest request) {
        return scheduleRepository.deleteByCondition(timetableId, request);
    }

    /**
     * 调换两个课程
     */
    public boolean swapSchedules(Long timetableId, Long scheduleId1, Long scheduleId2) {
        try {
            // 获取两个课程
            Schedules schedule1 = scheduleRepository.findByIdAndTimetableId(scheduleId1, timetableId);
            Schedules schedule2 = scheduleRepository.findByIdAndTimetableId(scheduleId2, timetableId);
            
            if (schedule1 == null || schedule2 == null) {
                logger.warn("调换课程失败：课程不存在，scheduleId1={}, scheduleId2={}, timetableId={}", 
                    scheduleId1, scheduleId2, timetableId);
                return false;
            }
            
            // 交换学生姓名
            String tempStudentName = schedule1.getStudentName();
            schedule1.setStudentName(schedule2.getStudentName());
            schedule2.setStudentName(tempStudentName);
            
            // 更新备注
            schedule1.setNote("调换课程");
            schedule2.setNote("调换课程");
            schedule1.setUpdatedAt(LocalDateTime.now());
            schedule2.setUpdatedAt(LocalDateTime.now());
            
            // 保存更新
            scheduleRepository.update(schedule1);
            scheduleRepository.update(schedule2);
            
            logger.info("课程调换成功：{} <-> {}, timetableId={}", 
                schedule1.getStudentName(), schedule2.getStudentName(), timetableId);
            
            return true;
        } catch (Exception e) {
            logger.error("调换课程失败", e);
            return false;
        }
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

    /**
     * 批量按ID删除排课
     */
    public int deleteSchedulesByIds(List<Long> scheduleIds) {
        int deletedCount = 0;
        for (Long scheduleId : scheduleIds) {
            try {
                scheduleRepository.deleteById(scheduleId);
                deletedCount++;
            } catch (Exception e) {
                // 记录错误但继续删除其他课程
                logger.error("删除课程失败，ID: {}, 错误: {}", scheduleId, e.getMessage());
            }
        }
        return deletedCount;
    }

    public List<ScheduleInfo> parseTextWithRules(String text, String type) {
        List<ScheduleInfo> scheduleInfoList = new ArrayList<>();
        // 预处理：替换所有分隔符和关键词，并将中文数字转为阿拉伯数字
        String preprocessedText = preprocessText(text);
        String[] lines = preprocessedText.split("\\r?\\n");

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            scheduleInfoList.addAll(parseSingleLine(line, type));
        }

        // 对时间进行平铺处理
        return expandTimeSlots(scheduleInfoList);
    }

    private String preprocessText(String text) {
        return ChineseNumberConverter.convert(text)
                .replaceAll("[至到~——]", "-") // 统一范围分隔符
                .replaceAll("[，]", ",") // 统一逗号
                .replaceAll("周|星期", ""); // 移除星期的前缀
    }

    private List<ScheduleInfo> parseSingleLine(String line, String type) {
        List<ScheduleInfo> results = new ArrayList<>();
        String originalLine = line;

        // 1. Extract all day/date parts
        List<String> dayParts = new ArrayList<>();
        Pattern dayPattern = "WEEKLY".equalsIgnoreCase(type)
                ? Pattern.compile("\\b([1-7](-[1-7])?)\\b")
                : Pattern.compile("(\\d{1,2}[./]\\d{1,2}(-\\d{1,2}([./]\\d{1,2})?)?)");
        Matcher dayMatcher = dayPattern.matcher(line);
        while (dayMatcher.find()) {
            dayParts.add(dayMatcher.group());
        }
        line = dayMatcher.replaceAll("").trim(); // Remove day parts

        // 2. Extract all time parts
        List<String> timeParts = new ArrayList<>();
        Pattern timePattern = Pattern.compile("(\\d{1,2}(:\\d{2})?-\\d{1,2}(:\\d{2})?|\\d{1,2}-\\d{1,2}|\\d{1,2})");
        Matcher timeMatcher = timePattern.matcher(line);
        while (timeMatcher.find()) {
            timeParts.add(timeMatcher.group());
        }
        line = timeMatcher.replaceAll("").trim(); // Remove time parts from line

        // 3. The rest is student name
        String studentName = line.replaceAll("[,\\s]+", "");

        boolean isSuccess = !studentName.isEmpty() && !timeParts.isEmpty() && !dayParts.isEmpty();

        if (isSuccess) {
            // 4. Generate combinations for successful parsing
            List<String> parsedDays = dayParts.stream()
                    .flatMap(dayPart -> parseDays(dayPart, type).stream())
                    .collect(Collectors.toList());

            List<String> parsedTimes = timeParts.stream()
                    .map(this::parseTime)
                    .collect(Collectors.toList());

            for (String day : parsedDays) {
                for (String time : parsedTimes) {
                    if ("WEEKLY".equalsIgnoreCase(type)) {
                        results.add(new ScheduleInfo(studentName, time, day, null, null));
                    } else { // DATE_RANGE
                        results.add(new ScheduleInfo(studentName, time, null, day, null));
                    }
                }
            }
        } else if (!originalLine.trim().isEmpty()) {
            // 5. Handle parsing failure and generate a detailed error message
            StringBuilder errorMsg = new StringBuilder("解析失败: ");
            List<String> missing = new ArrayList<>();
            if (studentName.isEmpty()) missing.add("姓名");
            if (dayParts.isEmpty()) missing.add("日期/星期");
            if (timeParts.isEmpty()) missing.add("时间");
            errorMsg.append("缺失 [").append(String.join(", ", missing)).append("] 信息。");

            List<String> found = new ArrayList<>();
            if (!studentName.isEmpty()) found.add("姓名: " + studentName);
            if (!dayParts.isEmpty()) found.add("日期/星期: " + String.join(", ", dayParts));
            if (!timeParts.isEmpty()) found.add("时间: " + String.join(", ", timeParts));

            if (!found.isEmpty()) {
                errorMsg.append(" (已识别: ").append(String.join("; ", found)).append(")");
            }
            
            // Create a special ScheduleInfo object to carry the error
            ScheduleInfo errorInfo = new ScheduleInfo(originalLine, null, null, null, errorMsg.toString());
            results.add(errorInfo);
        }

        return results;
    }

    private List<String> parseDays(String dayPart, String type) {
        if ("WEEKLY".equalsIgnoreCase(type)) {
            return parseDayOfWeekRanges(dayPart);
        } else {
            return parseDateRanges(dayPart);
        }
    }

    private List<String> parseDayOfWeekRanges(String part) {
        List<String> resultDays = new ArrayList<>();
        String[] dayParts = part.split("-");

        try {
            if (dayParts.length == 2) {
                DayOfWeek startDay = weekDayMap.get(dayParts[0]);
                DayOfWeek endDay = weekDayMap.get(dayParts[1]);
                if (startDay != null && endDay != null && startDay.getValue() <= endDay.getValue()) {
                    for (int i = startDay.getValue(); i <= endDay.getValue(); i++) {
                        resultDays.add(DayOfWeek.of(i).toString());
                    }
                }
            } else if (dayParts.length == 1 && weekDayMap.containsKey(dayParts[0])) {
                resultDays.add(weekDayMap.get(dayParts[0]).toString());
            }
        } catch (Exception e) {
             logger.error("Error parsing day of week range: {}", part, e);
        }
        return resultDays;
    }

    private List<String> parseDateRanges(String part) {
        List<String> resultDates = new ArrayList<>();
        String[] dateParts = part.split("-");
        int currentYear = LocalDate.now().getYear();

        try {
            if (dateParts.length >= 1) {
                LocalDate startDate = parseSingleDate(dateParts[0], currentYear);
                LocalDate endDate = startDate; // Default to single day

                if (dateParts.length == 2) {
                    // If end date is just a number, assume it's a day in the same month.
                    if (dateParts[1].matches("^\\d{1,2}$")) {
                        endDate = LocalDate.of(startDate.getYear(), startDate.getMonth(), Integer.parseInt(dateParts[1]));
                    } else {
                        endDate = parseSingleDate(dateParts[1], currentYear);
                    }
                }

                if (startDate != null && endDate != null && !startDate.isAfter(endDate)) {
                    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                        resultDates.add(date.format(DateTimeFormatter.ISO_LOCAL_DATE));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing date range: {}", part, e);
        }
        return resultDates;
    }

    private LocalDate parseSingleDate(String dateStr, int year) {
        try {
            String[] parts = dateStr.trim().split("[./]");
            if (parts.length != 2) return null;
            int month = Integer.parseInt(parts[0]);
            int day = Integer.parseInt(parts[1]);
            return LocalDate.of(year, month, day);
        } catch (Exception e) {
            return null;
        }
    }

    private String parseTime(String timePart) {
        // Handles "15:00-16:00"
        if (timePart.matches("\\d{1,2}:\\d{2}-\\d{1,2}:\\d{2}")) {
            return timePart;
        }

        final int MIN_HOUR = 8;
        final int MAX_HOUR = 21; // Can end at 21:00

        try {
            int startHour, endHour;

            if (timePart.contains("-")) {
                String[] parts = timePart.split("-");
                startHour = Integer.parseInt(parts[0]);
                endHour = Integer.parseInt(parts[1]);
            } else {
                startHour = Integer.parseInt(timePart);
                endHour = startHour + 1;
            }

            // Disambiguation: 1-8 are likely PM
            if (startHour >= 1 && startHour <= 8) startHour += 12;
            if (endHour >= 1 && endHour <= 9) endHour += 12; // 9 might mean 9pm (21:00)

            if (startHour >= MIN_HOUR && endHour <= MAX_HOUR && startHour < endHour) {
                 return String.format("%02d:00-%02d:00", startHour, endHour);
            }

        } catch (NumberFormatException e) {
            logger.error("Error parsing time part: {}", timePart, e);
        }

        return "09:00-10:00"; // Fallback
    }

    /**
     * 将时间范围平铺为1小时的时间段
     */
    private List<ScheduleInfo> expandTimeSlots(List<ScheduleInfo> scheduleInfoList) {
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

        return expandedList;
    }

    /**
     * 检查两个排课是否有时间冲突
     */
    private boolean hasTimeConflict(ScheduleRequest request, Schedules existing) {
        // 检查日期/星期是否匹配
        if (!isSameDateOrWeek(request, existing)) {
            return false;
        }

        // 检查时间是否重叠
        return isTimeOverlap(request.getStartTime(), request.getEndTime(),
                           existing.getStartTime(), existing.getEndTime());
    }

    /**
     * 检查两个新排课是否有时间冲突
     */
    private boolean hasTimeConflict(ScheduleRequest request1, ScheduleRequest request2) {
        // 检查日期/星期是否匹配
        if (!isSameDateOrWeek(request1, request2)) {
            return false;
        }

        // 检查时间是否重叠
        return isTimeOverlap(request1.getStartTime(), request1.getEndTime(),
                           request2.getStartTime(), request2.getEndTime());
    }

    /**
     * 检查是否是同一日期或星期
     */
    private boolean isSameDateOrWeek(ScheduleRequest request, Schedules existing) {
        // 如果都有具体日期，比较日期
        if (request.getScheduleDate() != null && existing.getScheduleDate() != null) {
            return request.getScheduleDate().equals(existing.getScheduleDate());
        }

        // 如果都有星期，比较星期
        if (request.getDayOfWeek() != null && existing.getDayOfWeek() != null) {
            return request.getDayOfWeek().name().equals(existing.getDayOfWeek());
        }

        return false;
    }

    /**
     * 检查是否是同一日期或星期（两个新排课）
     */
    private boolean isSameDateOrWeek(ScheduleRequest request1, ScheduleRequest request2) {
        // 如果都有具体日期，比较日期
        if (request1.getScheduleDate() != null && request2.getScheduleDate() != null) {
            return request1.getScheduleDate().equals(request2.getScheduleDate());
        }

        // 如果都有星期，比较星期
        if (request1.getDayOfWeek() != null && request2.getDayOfWeek() != null) {
            return request1.getDayOfWeek().equals(request2.getDayOfWeek());
        }

        return false;
    }

    /**
     * 检查时间是否重叠
     */
    private boolean isTimeOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        // 时间重叠的条件：start1 < end2 && start2 < end1
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    /**
     * 确定冲突类型
     */
    private String determineConflictType(ScheduleRequest request, Schedules existing) {
        // 如果学生姓名相同，是学生时间冲突
        if (request.getStudentName().equals(existing.getStudentName())) {
            return "STUDENT_TIME_CONFLICT";
        }
        // 否则是时间段占用冲突
        return "TIME_SLOT_CONFLICT";
    }

    /**
     * 生成冲突描述（新排课 vs 现有排课）
     */
    private String generateConflictDescription(ScheduleRequest request, Schedules existing) {
        StringBuilder desc = new StringBuilder();

        // 时间信息
        String timeInfo = String.format("%s-%s",
            request.getStartTime().toString(),
            request.getEndTime().toString());

        // 日期/星期信息
        String dateInfo = "";
        if (request.getScheduleDate() != null) {
            dateInfo = request.getScheduleDate().toString();
        } else if (request.getDayOfWeek() != null) {
            dateInfo = convertDayOfWeekToChinese(request.getDayOfWeek().name());
        }

        if (request.getStudentName().equals(existing.getStudentName())) {
            desc.append(String.format("学生 %s 在 %s %s 已有课程安排",
                request.getStudentName(), dateInfo, timeInfo));
        } else {
            desc.append(String.format("时间段 %s %s 已被学生 %s 占用，新学生 %s 产生冲突",
                dateInfo, timeInfo, existing.getStudentName(), request.getStudentName()));
        }

        return desc.toString();
    }

    /**
     * 生成冲突描述（两个新排课）
     */
    private String generateConflictDescription(ScheduleRequest request1, ScheduleRequest request2) {
        StringBuilder desc = new StringBuilder();

        // 时间信息
        String timeInfo = String.format("%s-%s",
            request1.getStartTime().toString(),
            request1.getEndTime().toString());

        // 日期/星期信息
        String dateInfo = "";
        if (request1.getScheduleDate() != null) {
            dateInfo = request1.getScheduleDate().toString();
        } else if (request1.getDayOfWeek() != null) {
            dateInfo = convertDayOfWeekToChinese(request1.getDayOfWeek().name());
        }

        if (request1.getStudentName().equals(request2.getStudentName())) {
            desc.append(String.format("学生 %s 在 %s %s 有重复的课程安排",
                request1.getStudentName(), dateInfo, timeInfo));
        } else {
            desc.append(String.format("时间段 %s %s 被学生 %s 和 %s 同时占用",
                dateInfo, timeInfo, request1.getStudentName(), request2.getStudentName()));
        }

        return desc.toString();
    }

    /**
     * 将英文星期转换为中文
     */
    private String convertDayOfWeekToChinese(String dayOfWeek) {
        if (dayOfWeek == null) return "";

        switch (dayOfWeek.toUpperCase()) {
            case "MONDAY": return "周一";
            case "TUESDAY": return "周二";
            case "WEDNESDAY": return "周三";
            case "THURSDAY": return "周四";
            case "FRIDAY": return "周五";
            case "SATURDAY": return "周六";
            case "SUNDAY": return "周日";
            default: return dayOfWeek;
        }
    }

    public boolean deleteSingleSchedule(Long scheduleId) {
        try {
            scheduleRepository.deleteById(scheduleId);
            return true;
        } catch (Exception e) {
            logger.error("Failed to delete schedule with ID: {}", scheduleId, e);
            return false;
        }
    }

    /**
     * 清空课表的所有课程
     */
    public int clearTimetableSchedules(Long timetableId) {
        try {
            // 获取删除前的课程数量
            List<Schedules> existingSchedules = scheduleRepository.findByTimetableId(timetableId);
            int count = existingSchedules.size();
            
            // 批量删除所有课程
            scheduleRepository.deleteByTimetableId(timetableId);
            
            logger.info("清空课表成功，课表ID: {}, 删除课程数量: {}", timetableId, count);
            return count;
        } catch (Exception e) {
            logger.error("清空课表失败，课表ID: {}", timetableId, e);
            throw new RuntimeException("清空课表失败: " + e.getMessage());
        }
    }

    /**
     * 将 WeeklyInstanceSchedule 转换为 Schedules 格式
     */
    private Schedules convertWeeklyInstanceScheduleToSchedule(com.timetable.entity.WeeklyInstanceSchedule instanceSchedule) {
        Schedules schedule = new Schedules();
        schedule.setId(instanceSchedule.getId());
        
        // 需要获取原始课表ID而不是实例ID
        // 通过WeeklyInstance查找对应的模板课表ID
        WeeklyInstance instance = weeklyInstanceService.findById(instanceSchedule.getWeeklyInstanceId());
        if (instance != null) {
            schedule.setTimetableId(instance.getTemplateTimetableId());
        } else {
            // 如果找不到实例，使用实例ID作为fallback（虽然这不是最佳方案）
            schedule.setTimetableId(instanceSchedule.getWeeklyInstanceId());
        }
        
        schedule.setStudentName(instanceSchedule.getStudentName());
        schedule.setSubject(instanceSchedule.getSubject());
        schedule.setDayOfWeek(instanceSchedule.getDayOfWeek());
        schedule.setStartTime(instanceSchedule.getStartTime());
        schedule.setEndTime(instanceSchedule.getEndTime());
        schedule.setScheduleDate(instanceSchedule.getScheduleDate());
        schedule.setNote(instanceSchedule.getNote());
        schedule.setCreatedAt(instanceSchedule.getCreatedAt());
        schedule.setUpdatedAt(instanceSchedule.getUpdatedAt());
        return schedule;
    }

    /**
     * 查询指定时间段有空闲的教练列表
     * @param scheduleDate 日期
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 有空闲的教练列表
     */
    public List<Map<String, Object>> findAvailableCoaches(LocalDate scheduleDate, LocalTime startTime, LocalTime endTime) {
        List<Map<String, Object>> availableCoaches = new ArrayList<>();
        
        try {
            logger.info("开始查询有空教练: 日期={}, 时间={}-{}", scheduleDate, startTime, endTime);
            
            // 1. 获取所有教练（普通用户且职位为教练，或管理员，已批准且未删除的用户）
            List<com.timetable.generated.tables.pojos.Users> allCoaches = 
                scheduleRepository.findAllCoaches();
            
            logger.info("找到符合条件的用户总数: {}", allCoaches.size());
            
            // 2. 只返回有活动课表且该时间段无冲突的教练
            for (com.timetable.generated.tables.pojos.Users coach : allCoaches) {
                logger.info("检查教练: {} (ID={})", coach.getNickname() != null ? coach.getNickname() : coach.getUsername(), coach.getId());
                
                // 获取教练的活动课表
                Timetables activeTimetable = timetableRepository.findActiveTimetableByUserId(coach.getId());
                
                if (activeTimetable == null) {
                    // 没有活动课表，跳过此教练
                    logger.info("教练 {} 没有活动课表，跳过", coach.getNickname() != null ? coach.getNickname() : coach.getUsername());
                    continue;
                }
                
                logger.info("教练 {} 的活动课表: {} (ID={}, 是否周固定={})", 
                    coach.getNickname() != null ? coach.getNickname() : coach.getUsername(),
                    activeTimetable.getName(), activeTimetable.getId(), activeTimetable.getIsWeekly());
                
                // 检查该时间段是否有冲突
                boolean hasConflict = false;
                
                // 判断是周固定课表还是日期范围课表
                if (activeTimetable.getIsWeekly() != null && activeTimetable.getIsWeekly() == 1) {
                    // 周固定课表：只检查该日期所在周的实例
                    WeeklyInstance targetInstance = weeklyInstanceService.findInstanceByDate(activeTimetable.getId(), scheduleDate);
                    
                    if (targetInstance != null) {
                        logger.info("找到周实例: ID={}, 周开始={}", targetInstance.getId(), targetInstance.getWeekStartDate());
                        // 查询该实例在指定日期和时间段的课程
                        List<Schedules> instanceSchedules = scheduleRepository.findByInstanceAndDateTime(
                            targetInstance.getId(), scheduleDate, startTime, endTime);
                        hasConflict = !instanceSchedules.isEmpty();
                        logger.info("周实例中该日期该时间段课程数: {}", instanceSchedules.size());
                        if (!instanceSchedules.isEmpty()) {
                            for (Schedules schedule : instanceSchedules) {
                                logger.info("  - 课程: 学员={}, 时间={}-{}, 日期={}", 
                                    schedule.getStudentName(), 
                                    schedule.getStartTime(), 
                                    schedule.getEndTime(),
                                    schedule.getScheduleDate());
                            }
                        }
                        logger.info("有冲突: {}", hasConflict);
                    } else {
                        // 没有该周的实例，说明该周没有排课，有空
                        logger.info("没有找到该周的实例，教练有空");
                        hasConflict = false;
                    }
                    
                } else {
                    // 日期范围课表：直接查询课表的课程
                    List<Schedules> existingSchedules = scheduleRepository.findByTimetableAndDateTime(
                        activeTimetable.getId(), scheduleDate, startTime, endTime);
                    hasConflict = !existingSchedules.isEmpty();
                    logger.info("日期范围课表，该时间段课程数: {}, 有冲突: {}", existingSchedules.size(), hasConflict);
                }
                
                // 如果没有冲突，说明有空
                if (!hasConflict) {
                    logger.info("教练 {} 有空", coach.getNickname() != null ? coach.getNickname() : coach.getUsername());
                    Map<String, Object> coachInfo = new HashMap<>();
                    coachInfo.put("id", coach.getId());
                    coachInfo.put("username", coach.getUsername());
                    coachInfo.put("nickname", coach.getNickname());
                    coachInfo.put("timetableId", activeTimetable.getId());
                    coachInfo.put("timetableName", activeTimetable.getName());
                    availableCoaches.add(coachInfo);
                } else {
                    logger.info("教练 {} 该时间段有课，不可用", coach.getNickname() != null ? coach.getNickname() : coach.getUsername());
                }
            }
            
            logger.info("查询完成，有空教练数: {}", availableCoaches.size());
            
        } catch (Exception e) {
            logger.error("查询有空教练失败", e);
            throw new RuntimeException("查询失败: " + e.getMessage());
        }
        
        return availableCoaches;
    }

    /**
     * 创建体验课程
     * @param request 体验课请求
     * @param createdBy 创建者
     */
    public void createTrialSchedule(com.timetable.dto.TrialScheduleRequest request, com.timetable.generated.tables.pojos.Users createdBy) {
        try {
            // 1. 获取教练的活动课表
            Timetables activeTimetable = timetableRepository.findActiveTimetableByUserId(request.getCoachId());
            
            if (activeTimetable == null) {
                throw new RuntimeException("教练没有活动课表，无法安排体验课");
            }
            
            LocalDate scheduleDate = LocalDate.parse(request.getScheduleDate());
            LocalTime startTime = LocalTime.parse(request.getStartTime());
            LocalTime endTime = LocalTime.parse(request.getEndTime());
            
            // 2. 再次检查时间冲突
            boolean hasConflict = false;
            Long targetId = null; // 用于保存课程的目标ID（课表ID或实例ID）
            
            if (activeTimetable.getIsWeekly() != null && activeTimetable.getIsWeekly() == 1) {
                // 周固定课表：需要保存到当前周实例
                WeeklyInstance currentInstance = weeklyInstanceService.getCurrentWeekInstance(activeTimetable.getId());
                
                if (currentInstance == null) {
                    // 如果没有当前周实例，创建一个
                    currentInstance = weeklyInstanceService.generateCurrentWeekInstance(activeTimetable.getId());
                }
                
                if (currentInstance != null) {
                    targetId = currentInstance.getId();
                    List<Schedules> existingSchedules = scheduleRepository.findByInstanceAndDateTime(
                        currentInstance.getId(), scheduleDate, startTime, endTime);
                    hasConflict = !existingSchedules.isEmpty();
                }
            } else {
                // 日期范围课表：直接保存到课表
                targetId = activeTimetable.getId();
                List<Schedules> existingSchedules = scheduleRepository.findByTimetableAndDateTime(
                    activeTimetable.getId(), scheduleDate, startTime, endTime);
                hasConflict = !existingSchedules.isEmpty();
            }
            
            if (hasConflict) {
                throw new RuntimeException("该时间段已有课程，无法添加体验课");
            }
            
            // 3. 计算星期几
            DayOfWeek dayOfWeek = scheduleDate.getDayOfWeek();
            String noteText = request.getCustomerPhone() != null ? "联系电话: " + request.getCustomerPhone() : null;
            boolean isTrial = request.getIsTrial() != null && request.getIsTrial();
            
            // 4. 保存课程
            if (activeTimetable.getIsWeekly() != null && activeTimetable.getIsWeekly() == 1) {
                // 保存到周实例 - 使用WeeklyInstanceSchedules对象
                com.timetable.generated.tables.pojos.WeeklyInstanceSchedules instanceSchedule = 
                    new com.timetable.generated.tables.pojos.WeeklyInstanceSchedules();
                instanceSchedule.setWeeklyInstanceId(targetId);
                instanceSchedule.setStudentName(request.getStudentName());
                instanceSchedule.setDayOfWeek(dayOfWeek.name());
                instanceSchedule.setScheduleDate(scheduleDate);
                instanceSchedule.setStartTime(startTime);
                instanceSchedule.setEndTime(endTime);
                instanceSchedule.setNote(noteText);
                instanceSchedule.setCreatedAt(LocalDateTime.now());
                instanceSchedule.setUpdatedAt(LocalDateTime.now());
                scheduleRepository.insertInstanceSchedule(instanceSchedule, isTrial, request.getCustomerId());
            } else {
                // 保存到课表 - 使用Schedules对象
                Schedules schedule = new Schedules();
                schedule.setTimetableId(targetId);
                schedule.setStudentName(request.getStudentName());
                schedule.setDayOfWeek(dayOfWeek.name());
                schedule.setScheduleDate(scheduleDate);
                schedule.setStartTime(startTime);
                schedule.setEndTime(endTime);
                schedule.setNote(noteText);
                schedule.setCreatedAt(LocalDateTime.now());
                schedule.setUpdatedAt(LocalDateTime.now());
                scheduleRepository.insertSchedule(schedule, isTrial, request.getCustomerId());
            }
            
            logger.info("体验课创建成功: 教练ID={}, 学员={}, 日期={}, 时间={}-{}",
                request.getCoachId(), request.getStudentName(), scheduleDate, startTime, endTime);
                
        } catch (Exception e) {
            logger.error("创建体验课失败", e);
            throw new RuntimeException("创建失败: " + e.getMessage());
        }
    }

    public Map<String, Object> findTrialScheduleByStudentName(String studentName) {
        try {
            // 优先从状态流转记录中查询体验时间
            // 通过原生SQL查询最新的待体验状态记录
            String sql = "SELECT csh.trial_schedule_date, csh.trial_start_time, csh.trial_end_time, csh.trial_coach_id, " +
                        "c.id as customer_id " +
                        "FROM customer_status_history csh " +
                        "JOIN customers c ON csh.customer_id = c.id " +
                        "WHERE c.child_name = ? AND csh.to_status = 'SCHEDULED' " +
                        "AND csh.trial_schedule_date IS NOT NULL " +
                        "ORDER BY csh.created_at DESC " +
                        "LIMIT 1";
            
            List<Map<String, Object>> statusRecords = scheduleRepository.queryForMaps(sql, studentName);
            if (statusRecords != null && !statusRecords.isEmpty()) {
                Map<String, Object> statusRecord = statusRecords.get(0);
                Map<String, Object> result = new HashMap<>();
                result.put("scheduleDate", statusRecord.get("trial_schedule_date"));
                result.put("startTime", statusRecord.get("trial_start_time"));
                result.put("endTime", statusRecord.get("trial_end_time"));
                result.put("coachId", statusRecord.get("trial_coach_id"));
                return result;
            }
            
            // 如果状态记录中没有，再从课表中查询
            List<Map<String, Object>> trialSchedules = scheduleRepository.findTrialSchedulesByStudentName(studentName);
            List<Map<String, Object>> instanceTrialSchedules = scheduleRepository.findTrialSchedulesInInstancesByStudentName(studentName);
            
            List<Map<String, Object>> allTrials = new ArrayList<>();
            allTrials.addAll(trialSchedules);
            allTrials.addAll(instanceTrialSchedules);
            
            if (allTrials.isEmpty()) {
                return null;
            }
            
            // 按日期倒序排序
            allTrials.sort((a, b) -> {
                Object dateA = a.get("schedule_date");
                Object dateB = b.get("schedule_date");
                if (dateA == null && dateB == null) return 0;
                if (dateA == null) return 1;
                if (dateB == null) return -1;
                return dateB.toString().compareTo(dateA.toString());
            });
            
            Map<String, Object> latestTrial = allTrials.get(0);
            Map<String, Object> result = new HashMap<>();
            result.put("scheduleDate", latestTrial.get("schedule_date"));
            result.put("startTime", latestTrial.get("start_time"));
            result.put("endTime", latestTrial.get("end_time"));
            result.put("note", latestTrial.get("note"));
            result.put("coachId", latestTrial.get("coach_id"));
            
            return result;
        } catch (Exception e) {
            logger.error("查询体验课程失败: studentName={}", studentName, e);
            return null;
        }
    }
}