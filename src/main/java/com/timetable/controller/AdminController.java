package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.MergeTimetablesRequest;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.generated.tables.pojos.Timetables;
import com.timetable.service.TimetableService;
import com.timetable.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 管理员控制器
 */
@RestController
@RequestMapping("/admin")
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    @Autowired
    private TimetableService timetableService;
    
    @Autowired
    private UserService userService;
    
    /**
     * 获取所有用户的课表
     */
    @GetMapping("/timetables")
    public ResponseEntity<ApiResponse<List<Timetables>>> getAllTimetables() {
        List<Timetables> timetables = timetableService.getAllTimetables();
        return ResponseEntity.ok(ApiResponse.success("获取所有课表成功", timetables));
    }
    
    /**
     * 合并课表
     */
    @PostMapping("/timetables/merge")
    public ResponseEntity<ApiResponse<Timetables>> mergeTimetables(
            @Valid @RequestBody MergeTimetablesRequest request,
            Authentication authentication) {
        
        Users admin = userService.findByUsername(authentication.getName());
        if (admin == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在"));
        }
        
        if (request.getTimetableIds().size() < 2) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("至少需要选择2个课表进行合并"));
        }
        
        Timetables mergedTimetable = timetableService.mergeTimetables(
                request.getTimetableIds(),
                request.getMergedName(),
                request.getDescription(),
                admin.getId()
        );
        
        if (mergedTimetable == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("合并失败，请检查课表ID是否有效"));
        }
        
        return ResponseEntity.ok(ApiResponse.success("合并课表成功", mergedTimetable));
    }
} 