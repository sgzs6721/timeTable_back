package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.ScheduleRequest;
import com.timetable.dto.TextInputRequest;
import com.timetable.dto.ConflictCheckResult;
import com.timetable.dto.ai.ScheduleInfo;
import com.timetable.dto.UpdateScheduleRequest;
import com.timetable.dto.SwapSchedulesRequest;
import com.timetable.generated.tables.pojos.Schedules;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.service.ScheduleService;
import com.timetable.service.TimetableService;
import com.timetable.service.UserService;
import com.timetable.service.WeeklyInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import javax.validation.Validator;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;

/**
 * 排课控制器
 */
@RestController
@RequestMapping("/timetables/{timetableId}/schedules")
@Validated
public class ScheduleController {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleController.class);

    private final ScheduleService scheduleService;
    private final TimetableService timetableService;
    private final UserService userService;
    private final WeeklyInstanceService weeklyInstanceService;
    private final Validator validator;

    @Autowired
    public ScheduleController(ScheduleService scheduleService, TimetableService timetableService, UserService userService, WeeklyInstanceService weeklyInstanceService, Validator validator) {
        this.scheduleService = scheduleService;
        this.timetableService = timetableService;
        this.userService = userService;
        this.weeklyInstanceService = weeklyInstanceService;
        this.validator = validator;
    }

    /**
     * 获取课表的排课列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Schedules>>> getTimetableSchedules(
            @PathVariable Long timetableId,
            @RequestParam(required = false) Integer week,
            @RequestParam(required = false, defaultValue = "false") Boolean templateOnly,
            Authentication authentication) {

        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在"));
        }

        if (!"MANAGER".equals(user.getPosition())) {
            if (!timetableService.isUserTimetable(timetableId, user.getId())) {
                return ResponseEntity.notFound().build();
            }
        }

        List<Schedules> schedules = scheduleService.getTimetableSchedules(timetableId, week, templateOnly);
        return ResponseEntity.ok(ApiResponse.success("获取排课列表成功", schedules));
    }

    /**
     * 获取今日课程
     */
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<List<Schedules>>> getTodaySchedules(
            @PathVariable Long timetableId,
            Authentication authentication) {

        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在"));
        }

        if (!"MANAGER".equals(user.getPosition())) {
            if (!timetableService.isUserTimetable(timetableId, user.getId())) {
                return ResponseEntity.notFound().build();
            }
        }

        List<Schedules> schedules = scheduleService.getTodaySchedules(timetableId);
        return ResponseEntity.ok(ApiResponse.success("获取今日课程成功", schedules));
    }

    /**
     * 获取明日课程
     */
    @GetMapping("/tomorrow")
    public ResponseEntity<ApiResponse<List<Schedules>>> getTomorrowSchedules(
            @PathVariable Long timetableId,
            Authentication authentication) {

        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在"));
        }

        if (!"MANAGER".equals(user.getPosition())) {
            if (!timetableService.isUserTimetable(timetableId, user.getId())) {
                return ResponseEntity.notFound().build();
            }
        }

        List<Schedules> schedules = scheduleService.getTomorrowSchedules(timetableId);
        return ResponseEntity.ok(ApiResponse.success("获取明日课程成功", schedules));
    }

    /**
     * 获取本周课程
     */
    @GetMapping("/this-week")
    public ResponseEntity<ApiResponse<List<Schedules>>> getThisWeekSchedules(
            @PathVariable Long timetableId,
            Authentication authentication) {

        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在"));
        }

        if (!"MANAGER".equals(user.getPosition())) {
            if (!timetableService.isUserTimetable(timetableId, user.getId())) {
                return ResponseEntity.notFound().build();
            }
        }

        List<Schedules> schedules = scheduleService.getThisWeekSchedules(timetableId);
        return ResponseEntity.ok(ApiResponse.success("获取本周课程成功", schedules));
    }

    /**
     * 获取固定课表模板
     */
    @GetMapping("/template")
    public ResponseEntity<ApiResponse<List<Schedules>>> getTemplateSchedules(
            @PathVariable Long timetableId,
            Authentication authentication) {

        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在"));
        }

        if (!"MANAGER".equals(user.getPosition())) {
            if (!timetableService.isUserTimetable(timetableId, user.getId())) {
                return ResponseEntity.notFound().build();
            }
        }

        List<Schedules> schedules = scheduleService.getTemplateSchedules(timetableId);
        return ResponseEntity.ok(ApiResponse.success("获取固定课表成功", schedules));
    }

    /**
     * 统一接口：一次返回 今日/明日/本周/固定
     */
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<com.timetable.dto.SchedulesOverviewResponse>> getSchedulesOverview(
            @PathVariable Long timetableId,
            Authentication authentication) {

        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在"));
        }

        if (!"MANAGER".equals(user.getPosition())) {
            if (!timetableService.isUserTimetable(timetableId, user.getId())) {
                return ResponseEntity.notFound().build();
            }
        }

        com.timetable.dto.SchedulesOverviewResponse overview = scheduleService.getSchedulesOverview(timetableId);
        return ResponseEntity.ok(ApiResponse.success("获取概览成功", overview));
    }

    /**
     * 根据学生姓名获取课表的排课列表
     */
    @GetMapping("/student/{studentName}")
    public ResponseEntity<ApiResponse<List<Schedules>>> getTimetableSchedulesByStudent(
            @PathVariable Long timetableId,
            @PathVariable String studentName,
            @RequestParam(required = false) Integer week,
            Authentication authentication) {

        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在"));
        }

        if (!"MANAGER".equals(user.getPosition())) {
            if (!timetableService.isUserTimetable(timetableId, user.getId())) {
                return ResponseEntity.notFound().build();
            }
        }

        List<Schedules> schedules = scheduleService.getTimetableSchedulesByStudent(timetableId, studentName, week);
        return ResponseEntity.ok(ApiResponse.success("获取学生排课列表成功", schedules));
    }

    /**
     * 创建新排课
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Schedules>> createSchedule(
            @PathVariable Long timetableId,
            @Valid @RequestBody ScheduleRequest request,
            Authentication authentication) {

        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在"));
        }

        if (!"MANAGER".equals(user.getPosition())) {
            if (!timetableService.isUserTimetable(timetableId, user.getId())) {
                return ResponseEntity.notFound().build();
            }
        }

        // 验证时间逻辑
        if (request.getStartTime().isAfter(request.getEndTime())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("开始时间不能晚于结束时间"));
        }

        Schedules schedule = scheduleService.createSchedule(timetableId, request);

        // 固定模板的修改：首先在固定模板中体现，然后根据时间判断是否同步到实例
        try {
            com.timetable.generated.tables.pojos.Timetables t = timetableService.getTimetableById(timetableId);
            if (t != null && t.getIsWeekly() != null && t.getIsWeekly() == 1) {
                // 根据课程时间判断是否同步到当前周实例
                java.util.List<com.timetable.generated.tables.pojos.Schedules> list = new java.util.ArrayList<>();
                list.add(schedule);
                weeklyInstanceService.syncSpecificTemplateSchedulesToCurrentInstanceByTime(timetableId, list);
                
                // 同时同步到未来的实例
                weeklyInstanceService.syncTemplateChangesToFutureInstances(timetableId);
            }
        } catch (Exception e) {
            logger.warn("同步模板到实例失败，课表ID: {}, 错误: {}", timetableId, e.getMessage());
        }
        
        return ResponseEntity.ok(ApiResponse.success("创建排课成功", schedule));
    }

    /**
     * 更新排课
     */
    @PutMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<Schedules>> updateSchedule(
            @PathVariable Long timetableId,
            @PathVariable Long scheduleId,
            @Valid @RequestBody UpdateScheduleRequest request,
            Authentication authentication) {

        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在"));
        }

        if (!"MANAGER".equals(user.getPosition())) {
            if (!timetableService.isUserTimetable(timetableId, user.getId())) {
                return ResponseEntity.notFound().build();
            }
        }

        // 如果同时提供了开始和结束时间，则验证逻辑
        if (request.getStartTime() != null && request.getEndTime() != null &&
                request.getStartTime().isAfter(request.getEndTime())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("开始时间不能晚于结束时间"));
        }

        Schedules schedule = scheduleService.updateSchedule(timetableId, scheduleId, request);

        if (schedule == null) {
            return ResponseEntity.notFound().build();
        }

        // 同步到周实例
        syncToWeeklyInstances(timetableId);

        return ResponseEntity.ok(ApiResponse.success("更新排课成功", schedule));
    }

    /**
     * 删除排课
     */
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<String>> deleteSchedule(
            @PathVariable Long timetableId,
            @PathVariable Long scheduleId,
            Authentication authentication) {

        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在"));
        }

        if (!"MANAGER".equals(user.getPosition())) {
            if (!timetableService.isUserTimetable(timetableId, user.getId())) {
                return ResponseEntity.notFound().build();
            }
        }

        boolean deleted = scheduleService.deleteSchedule(timetableId, scheduleId);

        if (!deleted) {
            return ResponseEntity.notFound().build();
        }

        // 同步到周实例
        syncToWeeklyInstances(timetableId);

        return ResponseEntity.ok(ApiResponse.success("排课删除成功"));
    }



    /**
     * 通过文本输入创建排课
     */
    @PostMapping("/text")
    public ResponseEntity<ApiResponse<List<ScheduleInfo>>> createScheduleByText(
            @PathVariable Long timetableId,
            @Valid @RequestBody TextInputRequest request,
            Authentication authentication) {

        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在"));
        }

        if (!"MANAGER".equals(user.getPosition())) {
            if (!timetableService.isUserTimetable(timetableId, user.getId())) {
                return ResponseEntity.notFound().build();
            }
        }

        try {
            List<ScheduleInfo> scheduleInfoList = scheduleService.extractScheduleInfoFromText(request.getText(), request.getType())
                    .block(Duration.ofSeconds(60)); // Block for up to 60 seconds

            if (scheduleInfoList == null || scheduleInfoList.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("无法从文本中解析出排课信息"));
            }
            return ResponseEntity.ok(ApiResponse.success("文本解析成功", scheduleInfoList));
        } catch (Exception e) {
            // This will catch the timeout exception from .block() or any other error
            return ResponseEntity.internalServerError().body(ApiResponse.error("处理文本解析时出错: " + e.getMessage()));
        }
    }

    /**
     * 通过格式化文本创建排课
     */
    @PostMapping("/format")
    public ResponseEntity<ApiResponse<List<ScheduleInfo>>> createScheduleByFormat(
            @PathVariable Long timetableId,
            @Valid @RequestBody TextInputRequest request,
            Authentication authentication) {

        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在"));
        }

        if (!"MANAGER".equals(user.getPosition())) {
            if (!timetableService.isUserTimetable(timetableId, user.getId())) {
                return ResponseEntity.notFound().build();
            }
        }

        try {
            List<ScheduleInfo> scheduleInfoList = scheduleService.parseTextWithRules(request.getText(), request.getType());

            if (scheduleInfoList == null || scheduleInfoList.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("无法从文本中解析出排课信息"));
            }
            return ResponseEntity.ok(ApiResponse.success("文本解析成功", scheduleInfoList));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("处理文本解析时出错: " + e.getMessage()));
        }
    }

    /**
     * 批量创建排课
     */
    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<List<Schedules>>> createSchedulesBatch(
            @PathVariable Long timetableId,
            @RequestBody List<ScheduleRequest> requests,
            Authentication authentication) {
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }
        if (!"MANAGER".equals(user.getPosition())) {
            if (!timetableService.isUserTimetable(timetableId, user.getId())) {
                return ResponseEntity.notFound().build();
            }
        }
        // 校验每个排课的时间
        for (ScheduleRequest request : requests) {
            if (request.getStartTime().isAfter(request.getEndTime())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("开始时间不能晚于结束时间"));
            }
        }
        List<Schedules> result = scheduleService.createSchedules(timetableId, requests);
        
        // 同步到周实例
        syncToWeeklyInstances(timetableId);
        
        return ResponseEntity.ok(ApiResponse.success("批量创建排课成功", result));
    }

    /**
     * 检查排课冲突
     */
    @PostMapping("/batch/check-conflicts")
    public ResponseEntity<ApiResponse<ConflictCheckResult>> checkScheduleConflicts(
            @PathVariable Long timetableId,
            @RequestBody List<ScheduleRequest> requests,
            Authentication authentication) {
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }

        if (!"MANAGER".equals(user.getPosition())) {
            if (!timetableService.isUserTimetable(timetableId, user.getId())) {
                return ResponseEntity.notFound().build();
            }
        }

        // 校验每个排课的时间
        for (ScheduleRequest request : requests) {
            if (request.getStartTime().isAfter(request.getEndTime())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("开始时间不能晚于结束时间"));
            }
        }

        try {
            ConflictCheckResult result = scheduleService.checkConflictsWithPartialCreation(timetableId, requests);

            if (result.isHasConflicts()) {
                logger.warn("发现冲突: 总数={}, 冲突数={}, 已创建数={}",
                    requests.size(), result.getConflicts().size(),
                    result.getCreatedSchedules() != null ? result.getCreatedSchedules().size() : 0);
                return ResponseEntity.ok(ApiResponse.success("部分创建成功，发现冲突", result));
            } else {
                logger.info("全部创建成功: 数量={}",
                    result.getCreatedSchedules() != null ? result.getCreatedSchedules().size() : 0);
                return ResponseEntity.ok(ApiResponse.success("全部创建成功", result));
            }
        } catch (Exception e) {
            logger.error("检查冲突失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("检查冲突失败: " + e.getMessage()));
        }
    }

    /**
     * 强制批量创建排课（忽略冲突）
     */
    @PostMapping("/batch/force")
    public ResponseEntity<ApiResponse<List<Schedules>>> createSchedulesBatchForce(
            @PathVariable Long timetableId,
            @RequestBody List<ScheduleRequest> requests,
            Authentication authentication) {
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }
        if (!"MANAGER".equals(user.getPosition())) {
            if (!timetableService.isUserTimetable(timetableId, user.getId())) {
                return ResponseEntity.notFound().build();
            }
        }
        // 校验每个排课的时间
        for (ScheduleRequest request : requests) {
            if (request.getStartTime().isAfter(request.getEndTime())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("开始时间不能晚于结束时间"));
            }
        }
        try {
            // 使用智能覆盖逻辑：删除不同学员的冲突排课，保留同学员的重复排课
            List<Schedules> schedules = scheduleService.createSchedulesWithOverride(timetableId, requests);
            
            // 同步到周实例
            syncToWeeklyInstances(timetableId);
            
            return ResponseEntity.ok(ApiResponse.success("强制创建排课成功", schedules));
        } catch (Exception e) {
            logger.error("强制创建排课失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("创建排课失败: " + e.getMessage()));
        }
    }

    /**
     * 按条件批量删除排课
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Integer>> deleteSchedulesByCondition(
            @PathVariable Long timetableId,
            @RequestBody ScheduleRequest request,
            Authentication authentication) {
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }
        if (!"MANAGER".equals(user.getPosition())) {
            if (!timetableService.isUserTimetable(timetableId, user.getId())) {
                return ResponseEntity.notFound().build();
            }
        }
        int deleted = scheduleService.deleteSchedulesByCondition(timetableId, request);
        
        // 同步到周实例
        syncToWeeklyInstances(timetableId);
        
        return ResponseEntity.ok(ApiResponse.success("删除排课成功", deleted));
    }

    /**
     * 批量按条件删除排课
     */
    @DeleteMapping("/batch")
    public ResponseEntity<ApiResponse<Integer>> deleteSchedulesBatch(
            @PathVariable Long timetableId,
            @RequestBody List<ScheduleRequest> requests,
            Authentication authentication) {
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }
        if (!"MANAGER".equals(user.getPosition())) {
            if (!timetableService.isUserTimetable(timetableId, user.getId())) {
                return ResponseEntity.notFound().build();
            }
        }
        int deleted = scheduleService.deleteSchedulesBatch(timetableId, requests);
        
        // 同步到周实例
        syncToWeeklyInstances(timetableId);
        
        return ResponseEntity.ok(ApiResponse.success("批量删除排课成功", deleted));
    }

    /**
     * 批量按ID删除排课
     */
    @DeleteMapping("/batch/ids")
    public ResponseEntity<ApiResponse<Integer>> deleteSchedulesByIds(
            @PathVariable Long timetableId,
            @RequestBody List<Long> scheduleIds,
            Authentication authentication) {
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }
        if (!"MANAGER".equals(user.getPosition())) {
            if (!timetableService.isUserTimetable(timetableId, user.getId())) {
                return ResponseEntity.notFound().build();
            }
        }
        
        int deleted = scheduleService.deleteSchedulesByIds(scheduleIds);
        
        // 同步到周实例
        syncToWeeklyInstances(timetableId);
        
        return ResponseEntity.ok(ApiResponse.success("批量删除排课成功", deleted));
    }

    /**
     * 清空课表的所有课程
     */
    @DeleteMapping(value = "/clear", produces = "application/json")
    public ResponseEntity<ApiResponse<String>> clearTimetableSchedules(
            @PathVariable("timetableId") Long timetableId,
            @RequestParam(value = "alsoClearCurrentWeek", required = false, defaultValue = "false") boolean alsoClearCurrentWeek,
            Authentication authentication) {
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }
        if (!"MANAGER".equals(user.getPosition())) {
            if (!timetableService.isUserTimetable(timetableId, user.getId())) {
                return ResponseEntity.notFound().build();
            }
        }
        
        int deleted = scheduleService.clearTimetableSchedules(timetableId);

        int instanceDeleted = 0;
        if (alsoClearCurrentWeek) {
            try {
                instanceDeleted = weeklyInstanceService.clearCurrentWeekInstanceSchedules(timetableId);
            } catch (Exception e) {
                logger.warn("清空当前周实例课程失败，课表ID: {}, 错误: {}", timetableId, e.getMessage());
            }
        } else {
            // 未选择清空当前周，仅同步模板变化
            syncToWeeklyInstances(timetableId);
        }

        String messageText = instanceDeleted > 0
                ? String.format("清空课表成功(模板:%d, 当前周:%d)", deleted, instanceDeleted)
                : String.format("清空课表成功(模板:%d)", deleted);

        return ResponseEntity.ok(ApiResponse.success(messageText, String.valueOf(deleted + instanceDeleted)));
    }

    /**
     * 调换两个课程
     */
    @PostMapping("/swap")
    public ResponseEntity<ApiResponse<String>> swapSchedules(
            @PathVariable Long timetableId,
            @Valid @RequestBody SwapSchedulesRequest request,
            Authentication authentication) {

        logger.info("调换课程请求 - timetableId: {}, scheduleId1: {}, scheduleId2: {}", 
                    timetableId, request.getScheduleId1(), request.getScheduleId2());

        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            logger.error("用户不存在: {}", authentication.getName());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在"));
        }

        if (!"MANAGER".equals(user.getPosition())) {
            if (!timetableService.isUserTimetable(timetableId, user.getId())) {
                return ResponseEntity.notFound().build();
            }
        }

        try {
            boolean success = scheduleService.swapSchedules(timetableId, request.getScheduleId1(), request.getScheduleId2());
            
            if (success) {
                // 同步到周实例
                syncToWeeklyInstances(timetableId);
                return ResponseEntity.ok(ApiResponse.success("课程调换成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("课程调换失败"));
            }
        } catch (Exception e) {
            logger.error("调换课程失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("课程调换失败: " + e.getMessage()));
        }
    }

    /**
     * 同步固定课表变化到周实例
     */
    private void syncToWeeklyInstances(Long timetableId) {
        try {
            // 检查是否为周固定课表
            com.timetable.generated.tables.pojos.Timetables timetable = timetableService.getTimetableById(timetableId);
            if (timetable != null && timetable.getIsWeekly() != null && timetable.getIsWeekly() == 1) {
                // 只同步当前时间之后的未来实例
                weeklyInstanceService.syncTemplateChangesToFutureInstances(timetableId);
            }
        } catch (Exception e) {
            // 记录错误但不影响主要操作
            logger.warn("同步到周实例失败，课表ID: {}, 错误: {}", timetableId, e.getMessage());
        }
    }
}
