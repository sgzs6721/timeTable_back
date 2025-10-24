package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.TrialScheduleRequest;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.service.ScheduleService;
import com.timetable.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * 通用排课控制器（不需要timetableId的接口）
 */
@RestController
@RequestMapping("/schedules")
public class GeneralScheduleController {

    private static final Logger logger = LoggerFactory.getLogger(GeneralScheduleController.class);

    private final ScheduleService scheduleService;
    private final UserService userService;

    @Autowired
    public GeneralScheduleController(ScheduleService scheduleService, UserService userService) {
        this.scheduleService = scheduleService;
        this.userService = userService;
    }

    /**
     * 查询指定时间段有空闲的教练列表
     */
    @GetMapping("/available-coaches")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAvailableCoaches(
            @RequestParam String date,
            @RequestParam String startTime,
            @RequestParam String endTime,
            Authentication authentication) {
        
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在"));
        }

        try {
            LocalDate scheduleDate = LocalDate.parse(date);
            LocalTime start = LocalTime.parse(startTime);
            LocalTime end = LocalTime.parse(endTime);
            
            List<Map<String, Object>> availableCoaches = scheduleService.findAvailableCoaches(scheduleDate, start, end);
            return ResponseEntity.ok(ApiResponse.success("查询成功", availableCoaches));
        } catch (Exception e) {
            logger.error("查询有空教练失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 创建体验课程
     */
    @PostMapping("/trial")
    public ResponseEntity<ApiResponse<String>> createTrialSchedule(
            @Valid @RequestBody TrialScheduleRequest request,
            Authentication authentication) {
        
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在"));
        }

        try {
            scheduleService.createTrialSchedule(request, user);
            return ResponseEntity.ok(ApiResponse.success("体验课创建成功", null));
        } catch (Exception e) {
            logger.error("创建体验课失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("创建失败: " + e.getMessage()));
        }
    }

    /**
     * 查询学生的体验课程信息
     */
    @GetMapping("/trial/{studentName}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTrialSchedule(
            @PathVariable String studentName,
            Authentication authentication) {
        
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在"));
        }

        try {
            Map<String, Object> trialSchedule = scheduleService.findTrialScheduleByStudentName(studentName);
            if (trialSchedule == null) {
                return ResponseEntity.ok(ApiResponse.success("暂无体验课程", null));
            }
            return ResponseEntity.ok(ApiResponse.success("查询成功", trialSchedule));
        } catch (Exception e) {
            logger.error("查询体验课程失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }
}

