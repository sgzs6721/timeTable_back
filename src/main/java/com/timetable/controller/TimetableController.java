package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.TimetableRequest;
import com.timetable.generated.tables.pojos.Timetables;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.service.TimetableService;
import com.timetable.service.UserService;
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
    
    @Autowired
    private UserService userService;
    
    /**
     * 获取用户的课表列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Timetables>>> getUserTimetables(Authentication authentication) {
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在"));
        }
        
        List<Timetables> timetables = timetableService.getUserTimetables(user.getId());
        return ResponseEntity.ok(ApiResponse.success("获取课表列表成功", timetables));
    }
    
    /**
     * 创建新课表
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Timetables>> createTimetable(
            @Valid @RequestBody TimetableRequest request,
            Authentication authentication) {
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在"));
        }
        // 校验课表类型和日期
        if (request.getType() == TimetableRequest.TimetableType.DATE_RANGE) {
            if (request.getStartDate() == null || request.getEndDate() == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("日期范围课表必须指定开始和结束日期"));
            }
            if (request.getStartDate().isAfter(request.getEndDate())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("开始日期不能晚于结束日期"));
            }
        }
        Timetables timetable = timetableService.createTimetable(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("创建课表成功", timetable));
    }
    
    /**
     * 获取课表详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Timetables>> getTimetable(
            @PathVariable Long id,
            Authentication authentication) {
        
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在"));
        }
        
        Timetables timetable;
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            timetable = timetableService.getTimetableById(id);
        } else {
            timetable = timetableService.getTimetable(id, user.getId());
        }
        
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
    public ResponseEntity<ApiResponse<Timetables>> updateTimetable(
            @PathVariable Long id,
            @Valid @RequestBody TimetableRequest request,
            Authentication authentication) {
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在"));
        }
        // 校验课表类型和日期
        if (request.getType() == TimetableRequest.TimetableType.DATE_RANGE) {
            if (request.getStartDate() == null || request.getEndDate() == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("日期范围课表必须指定开始和结束日期"));
            }
            if (request.getStartDate().isAfter(request.getEndDate())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("开始日期不能晚于结束日期"));
            }
        }
        Timetables timetable = timetableService.updateTimetable(id, user.getId(), request);
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
        
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在"));
        }
        
        boolean deleted = timetableService.deleteTimetable(id, user.getId());
        
        if (!deleted) {
            return ResponseEntity.notFound()
                    .build();
        }
        
        return ResponseEntity.ok(ApiResponse.success("课表删除成功"));
    }
} 