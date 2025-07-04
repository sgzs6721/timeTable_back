package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.MergeTimetablesRequest;
import com.timetable.dto.AdminTimetableDTO;
import com.timetable.dto.BatchTimetableInfoRequest;
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
import java.util.Map;

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
    public ResponseEntity<ApiResponse<List<AdminTimetableDTO>>> getAllTimetables() {
        List<AdminTimetableDTO> timetables = timetableService.getAllTimetablesWithUser();
        return ResponseEntity.ok(ApiResponse.success("获取所有课表成功", timetables));
    }
    
    /**
     * 批量获取课表信息（包含用户信息）- 用于合并预览
     */
    @PostMapping("/timetables/batch-info")
    public ResponseEntity<ApiResponse<List<AdminTimetableDTO>>> getBatchTimetablesInfo(
            @Valid @RequestBody BatchTimetableInfoRequest request) {
        
        if (request.getTimetableIds().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("课表ID列表不能为空"));
        }
        
        List<AdminTimetableDTO> timetables = timetableService.getTimetablesByIds(request.getTimetableIds());
        return ResponseEntity.ok(ApiResponse.success("获取课表信息成功", timetables));
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
    
    /**
     * 获取所有用户列表
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllUsers() {
        List<Map<String, Object>> users = userService.getAllUsersForAdmin();
        return ResponseEntity.ok(ApiResponse.success("获取用户列表成功", users));
    }
    
    /**
     * 更新用户权限
     */
    @PutMapping("/users/{userId}/role")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody Map<String, String> request) {
        
        String newRole = request.get("role");
        if (newRole == null || (!newRole.equals("USER") && !newRole.equals("ADMIN"))) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("角色必须是USER或ADMIN"));
        }
        
        Map<String, Object> updatedUser = userService.updateUserRole(userId, newRole);
        
        if (updatedUser == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在或更新失败"));
        }
        
        return ResponseEntity.ok(ApiResponse.success("用户权限更新成功", updatedUser));
    }
} 