package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.ScheduleRequest;
import com.timetable.dto.TextInputRequest;
import com.timetable.model.Schedule;
import com.timetable.model.User;
import com.timetable.service.ScheduleService;
import com.timetable.service.TimetableService;
import com.timetable.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
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
    public ResponseEntity<ApiResponse<List<Schedule>>> getTimetableSchedules(
            @PathVariable Long timetableId,
            @RequestParam(required = false) Integer week,
            Authentication authentication) {
        
        User user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在"));
        }
        
        // 检查课表是否属于当前用户
        if (!timetableService.isUserTimetable(timetableId, user.getId())) {
            return ResponseEntity.notFound().build();
        }
        
        List<Schedule> schedules = scheduleService.getTimetableSchedules(timetableId, week);
        return ResponseEntity.ok(ApiResponse.success("获取排课列表成功", schedules));
    }
    
    /**
     * 创建新排课
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Schedule>> createSchedule(
            @PathVariable Long timetableId,
            @Valid @RequestBody ScheduleRequest request,
            Authentication authentication) {
        
        User user = userService.findByUsername(authentication.getName());
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
        
        Schedule schedule = scheduleService.createSchedule(timetableId, request);
        return ResponseEntity.ok(ApiResponse.success("创建排课成功", schedule));
    }
    
    /**
     * 更新排课
     */
    @PutMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<Schedule>> updateSchedule(
            @PathVariable Long timetableId,
            @PathVariable Long scheduleId,
            @Valid @RequestBody ScheduleRequest request,
            Authentication authentication) {
        
        User user = userService.findByUsername(authentication.getName());
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
        
        Schedule schedule = scheduleService.updateSchedule(timetableId, scheduleId, request);
        
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
        
        User user = userService.findByUsername(authentication.getName());
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
    public ResponseEntity<ApiResponse<Schedule>> createScheduleByVoice(
            @PathVariable Long timetableId,
            @RequestParam("audio") MultipartFile audioFile,
            Authentication authentication) {
        
        User user = userService.findByUsername(authentication.getName());
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
            Schedule schedule = scheduleService.createScheduleByVoice(timetableId, audioData);
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
    public ResponseEntity<ApiResponse<Schedule>> createScheduleByText(
            @PathVariable Long timetableId,
            @Valid @RequestBody TextInputRequest request,
            Authentication authentication) {
        
        User user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在"));
        }
        
        // 检查课表是否属于当前用户
        if (!timetableService.isUserTimetable(timetableId, user.getId())) {
            return ResponseEntity.notFound().build();
        }
        
        Schedule schedule = scheduleService.createScheduleByText(timetableId, request.getText());
        return ResponseEntity.ok(ApiResponse.success("文本创建排课成功", schedule));
    }
} 