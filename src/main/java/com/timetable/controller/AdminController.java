package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.AdminTimetableDTO;
import com.timetable.dto.BatchTimetableInfoRequest;
import com.timetable.dto.PendingUserDTO;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.service.TimetableService;
import com.timetable.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
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
     * 更新课表状态（例如，设为活动）
     */
    @PutMapping("/timetables/{id}")
    public ResponseEntity<ApiResponse<Void>> updateTimetableStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {
        
        if (updates.containsKey("isActive")) {
            boolean isActive = (boolean) updates.get("isActive");
            if (isActive) {
                try {
                    timetableService.setTimetableActive(id);
                    return ResponseEntity.ok(ApiResponse.success("课表状态更新成功"));
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
                }
            }
        }
        
        // 如果有其他状态需要更新，可以在这里添加逻辑
        
        return ResponseEntity.badRequest().body(ApiResponse.error("无效的更新请求"));
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
    
    /**
     * 重置用户密码
     */
    @PutMapping("/users/{userId}/password")
    public ResponseEntity<ApiResponse<Void>> resetUserPassword(
            @PathVariable Long userId,
            @Valid @RequestBody Map<String, String> request) {
        
        String newPassword = request.get("password");
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("密码不能为空"));
        }
        
        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("密码至少6个字符"));
        }
        
        boolean success = userService.resetUserPassword(userId, newPassword);
        
        if (!success) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在或重置失败"));
        }
        
        return ResponseEntity.ok(ApiResponse.success("密码重置成功"));
    }

    /**
     * 更新用户昵称
     */
    @PutMapping("/users/{userId}/nickname")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateUserNickname(
            @PathVariable Long userId,
            @Valid @RequestBody Map<String, String> request) {
        
        String nickname = request.get("nickname");
        if (nickname != null && nickname.length() > 50) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("昵称长度不能超过50个字符"));
        }
        
        Users updatedUser = userService.updateUserNickname(userId, nickname);
        
        if (updatedUser == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在或更新失败"));
        }
        
        Map<String, Object> userDTO = new HashMap<>();
        userDTO.put("id", updatedUser.getId());
        userDTO.put("username", updatedUser.getUsername());
        userDTO.put("nickname", updatedUser.getNickname());
        userDTO.put("role", updatedUser.getRole());
        userDTO.put("createdAt", updatedUser.getCreatedAt());
        userDTO.put("updatedAt", updatedUser.getUpdatedAt());
        
        return ResponseEntity.ok(ApiResponse.success("昵称更新成功", userDTO));
    }
    
    /**
     * 软删除用户
     */
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId, Authentication authentication) {
        String currentUsername = authentication.getName();
        try {
            userService.softDeleteUser(userId, currentUsername);
            return ResponseEntity.ok(ApiResponse.success("用户删除成功"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 获取所有待审批的用户注册申请
     */
    @GetMapping("/users/pending")
    public ResponseEntity<ApiResponse<List<PendingUserDTO>>> getPendingUsers() {
        List<PendingUserDTO> pendingUsers = userService.getPendingUsers();
        return ResponseEntity.ok(ApiResponse.success("获取待审批用户列表成功", pendingUsers));
    }

    /**
     * 审批用户注册申请
     */
    @PutMapping("/users/{userId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveUserRegistration(@PathVariable Long userId) {
        boolean success = userService.approveUserRegistration(userId);
        
        if (!success) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在或状态不是待审批"));
        }
        
        return ResponseEntity.ok(ApiResponse.success("用户注册申请已批准"));
    }

    /**
     * 拒绝用户注册申请
     */
    @PutMapping("/users/{userId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectUserRegistration(@PathVariable Long userId) {
        boolean success = userService.rejectUserRegistration(userId);
        
        if (!success) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在或状态不是待审批"));
        }
        
        return ResponseEntity.ok(ApiResponse.success("用户注册申请已拒绝"));
    }
} 