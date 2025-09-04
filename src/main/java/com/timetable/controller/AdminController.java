package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.AdminTimetableDTO;
import com.timetable.dto.BatchTimetableInfoRequest;
import com.timetable.dto.CopyTimetableRequest;
import com.timetable.dto.PendingUserDTO;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.generated.tables.pojos.Timetables;
import com.timetable.service.TimetableService;
import com.timetable.service.UserService;
import com.timetable.service.ScheduleService;
import com.timetable.service.WeeklyInstanceService;
import com.timetable.task.WeeklyInstanceScheduledTask;
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
import java.util.stream.Collectors;

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

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private WeeklyInstanceScheduledTask weeklyInstanceScheduledTask;
    @Autowired
    private WeeklyInstanceService weeklyInstanceService;
    
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
     * 获取所有活动课表的指定日期课程信息
     */
    @GetMapping("/active-timetables/schedules")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getActiveSchedulesByDate(
            @RequestParam String date) {

        try {
            Map<String, Object> result = timetableService.getActiveSchedulesByDate(date);
            return ResponseEntity.ok(ApiResponse.success("获取活动课表课程成功", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取课程失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新课表状态（例如，设为活动、归档等）
     */
    @PutMapping("/timetables/{id}")
    public ResponseEntity<ApiResponse<Void>> updateTimetableStatus(
            @PathVariable Long id,
            @RequestBody Object updates) {
        
        try {
            // 处理字符串类型的更新请求（如 'ARCHIVED'）
            if (updates instanceof String) {
                String status = (String) updates;
                if ("ARCHIVED".equals(status)) {
                    boolean ok = timetableService.archiveTimetableByAdmin(id);
                    if (ok) {
                        return ResponseEntity.ok(ApiResponse.success("课表已归档"));
                    } else {
                        return ResponseEntity.badRequest().body(ApiResponse.error("归档失败，课表不存在或已删除"));
                    }
                }
            }
            
            // 处理Map类型的更新请求
            if (updates instanceof Map) {
                Map<String, Object> updateMap = (Map<String, Object>) updates;
                
                if (updateMap.containsKey("isActive")) {
                    boolean isActive = (boolean) updateMap.get("isActive");
                    if (isActive) {
                        timetableService.setTimetableActive(id);
                        return ResponseEntity.ok(ApiResponse.success("课表状态更新成功"));
                    }
                }
                
                if (updateMap.containsKey("isArchived")) {
                    boolean isArchived = (boolean) updateMap.get("isArchived");
                    if (isArchived) {
                        boolean ok = timetableService.archiveTimetableByAdmin(id);
                        if (ok) {
                            return ResponseEntity.ok(ApiResponse.success("课表已归档"));
                        } else {
                            return ResponseEntity.badRequest().body(ApiResponse.error("归档失败，课表不存在或已删除"));
                        }
                    }
                }
            }
            
            return ResponseEntity.badRequest().body(ApiResponse.error("无效的更新请求"));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("操作失败: " + e.getMessage()));
        }
    }

    
    /**
     * 获取所有用户列表（只返回APPROVED状态）
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllUsers() {
        List<Users> users = userService.getAllApprovedUsers();
        List<Map<String, Object>> userDTOs = users.stream()
            .map(user -> {
                Map<String, Object> dto = new HashMap<>();
                dto.put("id", user.getId());
                dto.put("username", user.getUsername());
                dto.put("nickname", user.getNickname());
                dto.put("role", user.getRole());
                dto.put("createdAt", user.getCreatedAt());
                dto.put("updatedAt", user.getUpdatedAt());
                return dto;
            })
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("获取用户列表成功", userDTOs));
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
     * 获取所有注册申请记录（包括已处理的）
     */
    @GetMapping("/users/registration-requests")
    public ResponseEntity<ApiResponse<List<PendingUserDTO>>> getAllRegistrationRequests() {
        List<PendingUserDTO> allRequests = userService.getAllRegistrationRequests();
        return ResponseEntity.ok(ApiResponse.success("获取所有注册申请记录成功", allRequests));
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

    /**
     * 手动触发生成当前周实例任务
     */
    @PostMapping("/tasks/generate-weekly-instances")
    public ResponseEntity<ApiResponse<String>> manualGenerateWeeklyInstances(Authentication authentication) {
        Users user = userService.findByUsername(authentication.getName());
        if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("权限不足"));
        }

        try {
            weeklyInstanceScheduledTask.manualGenerateWeeklyInstances();
            return ResponseEntity.ok(ApiResponse.success("当前周实例生成任务执行成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("生成任务执行失败: " + e.getMessage()));
        }
    }

    /**
     * 复制课表到指定用户
     */
    @PostMapping("/timetables/copy")
    public ResponseEntity<ApiResponse<Map<String, Object>>> copyTimetable(
            @Valid @RequestBody CopyTimetableRequest request) {
        
        try {
            Timetables copiedTimetable = timetableService.copyTimetableToUser(
                request.getSourceTimetableId(), 
                request.getTargetUserId(), 
                request.getNewTimetableName()
            );
            
            // 获取目标用户信息
            Users targetUser = userService.findById(request.getTargetUserId());
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("timetable", copiedTimetable);
            responseData.put("targetUserName", targetUser.getNickname() != null ? targetUser.getNickname() : targetUser.getUsername());
            
            return ResponseEntity.ok(ApiResponse.success("课表复制成功", responseData));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("复制课表失败: " + e.getMessage()));
        }
    }

    /**
     * 管理员删除课表
     */
    @DeleteMapping("/timetables/{id}")
    public ResponseEntity<ApiResponse<String>> deleteTimetable(@PathVariable Long id) {
        try {
            boolean deleted = timetableService.deleteTimetableByAdmin(id);
            if (!deleted) {
                return ResponseEntity.badRequest().body(ApiResponse.error("课表不存在或已删除"));
            }
            return ResponseEntity.ok(ApiResponse.success("课表删除成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("删除失败: " + e.getMessage()));
        }
    }

    /**
     * 管理员清空课表的所有课程
     */
    @DeleteMapping("/timetables/{id}/schedules/clear")
    public ResponseEntity<ApiResponse<String>> clearTimetableSchedules(@PathVariable Long id,
                                                                       @RequestParam(value = "alsoClearCurrentWeek", required = false, defaultValue = "false") boolean alsoClearCurrentWeek) {
        try {
            int deleted = scheduleService.clearTimetableSchedules(id);
            int instanceDeleted = 0;
            if (alsoClearCurrentWeek) {
                instanceDeleted = weeklyInstanceService.clearCurrentWeekInstanceSchedules(id);
            }
            String messageText = instanceDeleted > 0
                    ? String.format("清空课表成功(模板:%d, 当前周:%d)", deleted, instanceDeleted)
                    : String.format("清空课表成功(模板:%d)", deleted);
            return ResponseEntity.ok(ApiResponse.success(messageText, String.valueOf(deleted + instanceDeleted)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("清空失败: " + e.getMessage()));
        }
    }
} 