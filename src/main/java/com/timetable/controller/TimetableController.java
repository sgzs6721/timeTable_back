package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.TimetableRequest;
import com.timetable.model.Timetable;
import com.timetable.model.User;
import com.timetable.service.TimetableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 课表控制器
 */
@RestController
@RequestMapping("/timetables")
@Validated
public class TimetableController {
    
    @Autowired
    private TimetableService timetableService;
    
    /**
     * 获取用户的课表列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Timetable>>> getUserTimetables(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<Timetable> timetables = timetableService.getUserTimetables(user.getId());
        
        return ResponseEntity.ok(ApiResponse.success("获取课表列表成功", timetables));
    }
    
    /**
     * 创建新课表
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Timetable>> createTimetable(
            @Valid @RequestBody TimetableRequest request,
            Authentication authentication) {
        
        User user = (User) authentication.getPrincipal();
        
        // 验证日期范围课表的时间
        if (!request.getIsWeekly() && 
            (request.getStartDate() == null || request.getEndDate() == null)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("日期范围课表必须指定开始和结束日期"));
        }
        
        if (!request.getIsWeekly() && 
            request.getStartDate().isAfter(request.getEndDate())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("开始日期不能晚于结束日期"));
        }
        
        Timetable timetable = timetableService.createTimetable(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("创建课表成功", timetable));
    }
    
    /**
     * 获取课表详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Timetable>> getTimetable(
            @PathVariable Long id,
            Authentication authentication) {
        
        User user = (User) authentication.getPrincipal();
        Timetable timetable = timetableService.getTimetable(id, user.getId());
        
        if (timetable == null) {
            return ResponseEntity.notFound()
                    .build();
        }
        
        return ResponseEntity.ok(ApiResponse.success("获取课表详情成功", timetable));
    }
    
    /**
     * 更新课表
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Timetable>> updateTimetable(
            @PathVariable Long id,
            @Valid @RequestBody TimetableRequest request,
            Authentication authentication) {
        
        User user = (User) authentication.getPrincipal();
        
        // 验证日期范围课表的时间
        if (!request.getIsWeekly() && 
            (request.getStartDate() == null || request.getEndDate() == null)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("日期范围课表必须指定开始和结束日期"));
        }
        
        if (!request.getIsWeekly() && 
            request.getStartDate().isAfter(request.getEndDate())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("开始日期不能晚于结束日期"));
        }
        
        Timetable timetable = timetableService.updateTimetable(id, user.getId(), request);
        
        if (timetable == null) {
            return ResponseEntity.notFound()
                    .build();
        }
        
        return ResponseEntity.ok(ApiResponse.success("更新课表成功", timetable));
    }
    
    /**
     * 删除课表
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteTimetable(
            @PathVariable Long id,
            Authentication authentication) {
        
        User user = (User) authentication.getPrincipal();
        boolean deleted = timetableService.deleteTimetable(id, user.getId());
        
        if (!deleted) {
            return ResponseEntity.notFound()
                    .build();
        }
        
        return ResponseEntity.ok(ApiResponse.success("课表删除成功"));
    }
} 