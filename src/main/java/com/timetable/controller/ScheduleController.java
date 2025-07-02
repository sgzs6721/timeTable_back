package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.ScheduleRequest;
import com.timetable.dto.TextInputRequest;
import com.timetable.dto.ai.ScheduleInfo;
import com.timetable.generated.tables.pojos.Schedules;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.service.ScheduleService;
import com.timetable.service.TimetableService;
import com.timetable.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.time.Duration;
import java.util.List;

/**
 * 排课控制器
 */
@RestController
@RequestMapping("/timetables/{timetableId}/schedules")
@Validated
public class ScheduleController {
    
    @Autowired
    private ScheduleService scheduleService;
    
    @Autowired
    private TimetableService timetableService;
    
    @Autowired
    private UserService userService;
    
    /**
     * 获取课表的排课列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Schedules>>> getTimetableSchedules(
            @PathVariable Long timetableId,
            @RequestParam(required = false) Integer week,
            Authentication authentication) {
        
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在"));
        }
        
        // 检查课表是否属于当前用户
        if (!timetableService.isUserTimetable(timetableId, user.getId())) {
            return ResponseEntity.notFound().build();
        }
        
        List<Schedules> schedules = scheduleService.getTimetableSchedules(timetableId, week);
        return ResponseEntity.ok(ApiResponse.success("获取排课列表成功", schedules));
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
        
        // 检查课表是否属于当前用户
        if (!timetableService.isUserTimetable(timetableId, user.getId())) {
            return ResponseEntity.notFound().build();
        }
        
        // 验证时间逻辑
        if (request.getStartTime().isAfter(request.getEndTime())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("开始时间不能晚于结束时间"));
        }
        
        Schedules schedule = scheduleService.createSchedule(timetableId, request);
        return ResponseEntity.ok(ApiResponse.success("创建排课成功", schedule));
    }
    
    /**
     * 更新排课
     */
    @PutMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<Schedules>> updateSchedule(
            @PathVariable Long timetableId,
            @PathVariable Long scheduleId,
            @Valid @RequestBody ScheduleRequest request,
            Authentication authentication) {
        
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在"));
        }
        
        // 检查课表是否属于当前用户
        if (!timetableService.isUserTimetable(timetableId, user.getId())) {
            return ResponseEntity.notFound().build();
        }
        
        // 验证时间逻辑
        if (request.getStartTime().isAfter(request.getEndTime())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("开始时间不能晚于结束时间"));
        }
        
        Schedules schedule = scheduleService.updateSchedule(timetableId, scheduleId, request);
        
        if (schedule == null) {
            return ResponseEntity.notFound().build();
        }
        
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
        
        // 检查课表是否属于当前用户
        if (!timetableService.isUserTimetable(timetableId, user.getId())) {
            return ResponseEntity.notFound().build();
        }
        
        boolean deleted = scheduleService.deleteSchedule(timetableId, scheduleId);
        
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(ApiResponse.success("排课删除成功"));
    }
    
    /**
     * 通过语音输入创建排课
     */
    @PostMapping("/voice")
    public ResponseEntity<ApiResponse<Schedules>> createScheduleByVoice(
            @PathVariable Long timetableId,
            @RequestParam("audio") MultipartFile audioFile,
            Authentication authentication) {
        
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在"));
        }
        
        // 检查课表是否属于当前用户
        if (!timetableService.isUserTimetable(timetableId, user.getId())) {
            return ResponseEntity.notFound().build();
        }
        
        if (audioFile.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("音频文件不能为空"));
        }
        
        try {
            byte[] audioData = audioFile.getBytes();
            Schedules schedule = scheduleService.createScheduleByVoice(timetableId, audioData);
            return ResponseEntity.ok(ApiResponse.success("语音创建排课成功", schedule));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("处理音频文件失败"));
        }
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
        
        // 检查课表是否属于当前用户
        if (!timetableService.isUserTimetable(timetableId, user.getId())) {
            return ResponseEntity.notFound().build();
        }
        
        try {
            List<ScheduleInfo> scheduleInfoList = scheduleService.extractScheduleInfoFromText(request.getText())
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
     * 批量创建排课
     */
    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<List<Schedules>>> createSchedulesBatch(
            @PathVariable Long timetableId,
            @Valid @RequestBody List<ScheduleRequest> requests,
            Authentication authentication) {
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在"));
        }
        // 检查课表是否属于当前用户
        if (!timetableService.isUserTimetable(timetableId, user.getId())) {
            return ResponseEntity.notFound().build();
        }
        // 校验每个排课的时间
        for (ScheduleRequest request : requests) {
            if (request.getStartTime().isAfter(request.getEndTime())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("开始时间不能晚于结束时间"));
            }
        }
        List<Schedules> result = scheduleService.createSchedules(timetableId, requests);
        return ResponseEntity.ok(ApiResponse.success("批量创建排课成功", result));
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
        if (!timetableService.isUserTimetable(timetableId, user.getId())) {
            return ResponseEntity.notFound().build();
        }
        int deleted = scheduleService.deleteSchedulesByCondition(timetableId, request);
        return ResponseEntity.ok(ApiResponse.success("删除排课成功", deleted));
    }
} 