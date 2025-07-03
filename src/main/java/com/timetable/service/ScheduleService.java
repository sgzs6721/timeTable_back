package com.timetable.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.timetable.dto.ScheduleRequest;
import com.timetable.dto.ai.ScheduleInfo;
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
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.Iterator;
import java.time.temporal.TemporalAdjusters;

/**
 * 排课服务
 */
@Service
public class ScheduleService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleService.class);

    private final ScheduleRepository scheduleRepository;
    private final AiNlpService aiNlpService;
    private final TimetableRepository timetableRepository;
    private final SiliconFlowService siliconFlowService;
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
        Timetables timetable = timetableRepository.findById(timetableId);
        if (timetable == null) {
            logger.warn("Timetable with ID {} not found.", timetableId);
            return Collections.emptyList();
        }

        // For WEEKLY timetables, week-based filtering is based on the week_number field.
        if (timetable.getIsWeekly() == 1) {
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

        // For date-range timetables, weekNumber from request is ignored and set to null.
        // For weekly timetables, we use the one from the request.
        Timetables timetable = timetableRepository.findById(timetableId);
        if (timetable != null && timetable.getIsWeekly() == 0) {
            schedule.setWeekNumber(null);
        } else {
            schedule.setWeekNumber(request.getWeekNumber());
        }

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
                        return Mono.just(Collections.<ScheduleInfo>emptyList());
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

    public List<ScheduleInfo> parseTextWithRules(String text, String type) {
        List<ScheduleInfo> scheduleInfoList = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            scheduleInfoList.addAll(parseSingleLine(line, type));
        }

        // 对时间进行平铺处理
        return expandTimeSlots(scheduleInfoList);
    }

    private List<ScheduleInfo> parseSingleLine(String line, String type) {
        List<ScheduleInfo> results = new ArrayList<>();
        String[] parts = line.trim().split("[\\s，,]+");

        if (parts.length < 2 || parts.length > 4) {
            return results; // Expect 2-4 parts
        }

        List<String> partList = new ArrayList<>(Arrays.asList(parts));
        String timePart = null;
        String dayPart = null;
        String studentName = null;

        // Identify Time Part
        for (Iterator<String> iterator = partList.iterator(); iterator.hasNext();) {
            String part = iterator.next();
            if (isTimePart(part)) {
                timePart = part;
                iterator.remove();
                break;
            }
        }

        // Identify Day/Date Part
        for (Iterator<String> iterator = partList.iterator(); iterator.hasNext();) {
            String part = iterator.next();
            if (isDayPart(part, type)) {
                dayPart = part;
                iterator.remove();
                break;
            }
        }

        // The remaining parts should be the student name (could be multiple parts)
        if (!partList.isEmpty()) {
            studentName = String.join("", partList); // Join remaining parts as student name
        }

        // If we couldn't identify day/date part, but we have time and name,
        // try to use a default or infer from context
        if (dayPart == null && timePart != null && studentName != null) {
            // For DATE_RANGE type, if no specific date is found, we might need to handle this differently
            // For now, let's skip this case
            return results;
        }

        if (studentName == null || timePart == null) {
            return results; // Must have at least student name and time
        }

        // If dayPart is null, we might be dealing with a different format
        if (dayPart == null) {
            return results;
        }

        List<String> days = parseDays(dayPart, type);
        String timeRange = parseTime(timePart);

        for (String day : days) {
            if ("WEEKLY".equalsIgnoreCase(type)) {
                results.add(new ScheduleInfo(studentName, timeRange, day, null, "格式解析"));
            } else { // DATE_RANGE
                results.add(new ScheduleInfo(studentName, timeRange, null, day, "格式解析"));
            }
        }
        return results;
    }

    private boolean isTimePart(String part) {
        String cleanPart = part.trim().replaceAll("点", "");
        return cleanPart.matches("^\\d{1,2}([-~]\\d{1,2})?$") || cleanPart.matches("^\\d{1,2}:\\d{2}-\\d{1,2}:\\d{2}$");
    }

    private boolean isDayPart(String part, String type) {
        if ("WEEKLY".equalsIgnoreCase(type)) {
            String cleanPart = part.trim().replaceAll("周|星期", "");
            String[] dayTokens = cleanPart.split("[-~至]");
            if (dayTokens.length < 1 || dayTokens.length > 2) return false;
            for (String token : dayTokens) {
                if (!weekDayMap.containsKey(token)) return false;
            }
            return true;
        } else { // DATE_RANGE
            String[] dateTokens = part.split("[-~至]");
            if (dateTokens.length == 1) {
                // A single date part must be in M.D format
                return dateTokens[0].matches("^\\d{1,2}[月./]\\d{1,2}$");
            }
            if (dateTokens.length == 2) {
                // For a range, the start must be M.D, the end can be M.D or just D.
                boolean startOk = dateTokens[0].matches("^\\d{1,2}[月./]\\d{1,2}$");
                boolean endOk = dateTokens[1].matches("^\\d{1,2}[月./]\\d{1,2}$") || dateTokens[1].matches("^\\d{1,2}$");
                return startOk && endOk;
            }
            return false;
        }
    }

    private List<String> parseDays(String dayPart, String type) {
        dayPart = dayPart.trim().replaceAll("周", "").replaceAll("星期", "");
        if ("WEEKLY".equalsIgnoreCase(type)) {
            return parseDayOfWeekRanges(dayPart);
        } else {
            return parseDateRanges(dayPart);
        }
    }

    private List<String> parseDayOfWeekRanges(String part) {
        List<String> resultDays = new ArrayList<>();
        String[] dayParts = part.split("[-~至]");

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
        return resultDays;
    }

    private List<String> parseDateRanges(String part) {
        List<String> resultDates = new ArrayList<>();
        String[] dateParts = part.split("[-~至]");
        int currentYear = LocalDate.now().getYear();

        if (dateParts.length == 2) {
            LocalDate startDate = parseSingleDate(dateParts[0], currentYear);
            if (startDate != null) {
                LocalDate endDate;
                // If end date is just a number, assume it's a day in the same month as the start date.
                if (dateParts[1].matches("^\\d{1,2}$")) {
                    try {
                        endDate = LocalDate.of(startDate.getYear(), startDate.getMonth(), Integer.parseInt(dateParts[1]));
                    } catch (Exception e) {
                        endDate = null;
                    }
                } else {
                    endDate = parseSingleDate(dateParts[1], currentYear);
                }

                if (endDate != null && !startDate.isAfter(endDate)) {
                    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                        resultDates.add(date.format(DateTimeFormatter.ISO_LOCAL_DATE));
                    }
                }
            }
        } else if (dateParts.length == 1) {
            LocalDate date = parseSingleDate(dateParts[0], currentYear);
            if (date != null) {
                resultDates.add(date.format(DateTimeFormatter.ISO_LOCAL_DATE));
            }
        }
        return resultDates;
    }

    private LocalDate parseSingleDate(String dateStr, int year) {
        try {
            String[] parts = dateStr.trim().split("[月./]");
            int month = Integer.parseInt(parts[0]);
            int day = Integer.parseInt(parts[1]);
            return LocalDate.of(year, month, day);
        } catch (Exception e) {
            return null;
        }
    }

    private String parseTime(String timePart) {
        timePart = timePart.trim().replaceAll("点", "").replaceAll("[~]", "-");

        // Handles "15:00-16:00" - pass through for now
        if (timePart.matches("\\d{1,2}:\\d{2}-\\d{1,2}:\\d{2}")) {
            return timePart;
        }

        final int MIN_HOUR = 8;
        final int MAX_HOUR = 20;

        try {
            int startHourInput, endHourInput;

            if (timePart.matches("\\d{1,2}-\\d{1,2}")) {
                String[] parts = timePart.split("-");
                startHourInput = Integer.parseInt(parts[0]);
                endHourInput = Integer.parseInt(parts[1]);
            } else if (timePart.matches("\\d{1,2}")) {
                startHourInput = Integer.parseInt(timePart);
                endHourInput = startHourInput + 1;
            } else {
                return "09:00-10:00"; // Fallback for unknown format
            }

            // --- Disambiguation Logic ---

            // Interpretation 1: PM-preferred (e.g., 3 -> 15:00, 8 -> 20:00)
            int start1 = (startHourInput >= 1 && startHourInput <= 8) ? startHourInput + 12 : startHourInput;
            int end1 = (endHourInput >= 1 && endHourInput <= 8) ? endHourInput + 12 : endHourInput;
            if (end1 < start1) { // Adjust for ranges like "8-9" -> 20:00-21:00
                end1 += 12;
            }

            // Check if Interpretation 1 is valid
            if (start1 >= MIN_HOUR && end1 <= MAX_HOUR) {
                return String.format("%02d:00-%02d:00", start1, end1);
            }

            // Interpretation 2: As-is / AM-preferred (e.g., 8 -> 08:00)
            int start2 = startHourInput;
            int end2 = endHourInput;

            // Check if Interpretation 2 is valid
            if (start2 >= MIN_HOUR && end2 <= MAX_HOUR) {
                return String.format("%02d:00-%02d:00", start2, end2);
            }

        } catch (NumberFormatException e) {
            // Fallthrough to default if parsing fails
        }

        return "09:00-10:00"; // Fallback if no valid interpretation found
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

    public boolean deleteSingleSchedule(Long scheduleId) {
        try {
            scheduleRepository.deleteById(scheduleId);
            return true;
        } catch (Exception e) {
            logger.error("Failed to delete schedule with ID: {}", scheduleId, e);
            return false;
        }
    }
}