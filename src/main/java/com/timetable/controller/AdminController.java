package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.AdminTimetableDTO;
import com.timetable.dto.BatchTimetableInfoRequest;
import com.timetable.dto.CopyTimetableRequest;
import com.timetable.dto.PendingUserDTO;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.generated.tables.pojos.Timetables;
import com.timetable.generated.tables.pojos.Schedules;
import com.timetable.entity.WeeklyInstance;
import com.timetable.entity.WeeklyInstanceSchedule;
import com.timetable.repository.ScheduleRepository;
import com.timetable.repository.WeeklyInstanceScheduleRepository;
import com.timetable.repository.WeeklyInstanceRepository;
import com.timetable.service.TimetableService;
import com.timetable.service.UserService;
import com.timetable.service.ScheduleService;
import com.timetable.service.WeeklyInstanceService;
import com.timetable.task.WeeklyInstanceScheduledTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.timetable.service.WeeklyInstanceService;

import javax.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    
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

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private WeeklyInstanceScheduleRepository weeklyInstanceScheduleRepository;
    
    @Autowired
    private WeeklyInstanceRepository weeklyInstanceRepository;
    
    /**
     * 获取所有用户的课表
     */
    @GetMapping("/timetables")
    public ResponseEntity<ApiResponse<List<AdminTimetableDTO>>> getAllTimetables(
            @RequestParam(value = "activeOnly", required = false, defaultValue = "false") boolean activeOnly) {
        List<AdminTimetableDTO> timetables = timetableService.getAllTimetablesWithUser(activeOnly);
        return ResponseEntity.ok(ApiResponse.success("获取所有课表成功", timetables));
    }

    /**
     * 有课表（活动或归档）的教练列表，按注册时间倒序
     */
    @GetMapping("/coaches/with-timetables")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getCoachesWithTimetables() {
        java.util.List<com.timetable.generated.tables.pojos.Users> users = userService.getAllApprovedUsers();
        java.util.List<com.timetable.dto.AdminTimetableDTO> all = timetableService.getAllTimetablesWithUser(false);
        java.util.Set<Long> userIds = all.stream().map(com.timetable.dto.AdminTimetableDTO::getUserId).collect(java.util.stream.Collectors.toSet());
        java.util.List<java.util.Map<String, Object>> list = users.stream()
                .filter(u -> userIds.contains(u.getId()))
                .sorted((a,b) -> {
                    java.time.LocalDateTime atA = a.getCreatedAt();
                    java.time.LocalDateTime atB = b.getCreatedAt();
                    if (atA == null && atB == null) return 0;
                    if (atA == null) return 1;
                    if (atB == null) return -1;
                    return atB.compareTo(atA);
                })
                .map(u -> {
                    java.util.Map<String, Object> m = new java.util.HashMap<>();
                    m.put("id", u.getId());
                    m.put("name", u.getNickname() != null ? u.getNickname() : u.getUsername());
                    m.put("createdAt", u.getCreatedAt());
                    return m;
                })
                .collect(java.util.stream.Collectors.toList());

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("list", list);
        data.put("total", list.size());
        return ResponseEntity.ok(ApiResponse.success("获取教练列表成功", data));
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
     * 获取所有活动课表列表
     */
    @GetMapping("/active-timetables")
    public ResponseEntity<ApiResponse<List<AdminTimetableDTO>>> getActiveTimetables() {
        try {
            List<AdminTimetableDTO> activeTimetables = timetableService.getActiveTimetables();
            return ResponseEntity.ok(ApiResponse.success("获取活动课表成功", activeTimetables));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取活动课表失败: " + e.getMessage()));
        }
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
     * 获取所有活动课表的本周课程信息（优化版，一次性返回所有数据）
     */
    @GetMapping("/active-timetables/this-week")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getActiveTimetablesThisWeek() {
        try {
            List<Map<String, Object>> result = timetableService.getActiveTimetablesThisWeekSchedules();
            return ResponseEntity.ok(ApiResponse.success("获取活动课表本周课程成功", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取本周课程失败: " + e.getMessage()));
        }
    }

    /**
     * 获取所有活动课表的模板课程信息（优化版，一次性返回所有数据）
     */
    @GetMapping("/active-timetables/templates")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getActiveTimetablesTemplates() {
        try {
            List<Map<String, Object>> result = timetableService.getActiveTimetablesTemplateSchedules();
            return ResponseEntity.ok(ApiResponse.success("获取活动课表模板课程成功", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取模板课程失败: " + e.getMessage()));
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
     * 管理员更新课表信息（名称、描述等）
     */
    @PutMapping("/timetables/{id}/details")
    public ResponseEntity<ApiResponse<com.timetable.generated.tables.pojos.Timetables>> updateTimetableDetails(
            @PathVariable Long id,
            @Valid @RequestBody com.timetable.dto.TimetableRequest request) {
        
        try {
            com.timetable.generated.tables.pojos.Timetables updatedTimetable = timetableService.updateTimetableByAdmin(id, request);
            if (updatedTimetable == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("课表不存在或更新失败"));
            }
            return ResponseEntity.ok(ApiResponse.success("课表更新成功", updatedTimetable));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("更新失败: " + e.getMessage()));
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
     * 管理员编辑用户信息（用户名/昵称/角色）
     */
    @PutMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateUserInfo(
            @PathVariable Long userId,
            @Valid @RequestBody Map<String, String> request) {
        String role = request.get("role");
        String username = request.get("username");
        String nickname = request.get("nickname");

        Users user = userService.findById(userId);
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在或更新失败"));
        }

        // 校验并更新角色（可选，但如果传了必须合法）
        if (role != null) {
            if (!"USER".equals(role) && !"ADMIN".equals(role)) {
                return ResponseEntity.badRequest().body(ApiResponse.error("角色必须是USER或ADMIN"));
            }
            user.setRole(role);
        }

        // 更新用户名（可选）
        if (username != null && !username.equals(user.getUsername())) {
            // 复用用户服务中的校验逻辑
            try {
                // 这里直接调用底层仓库更新，或在UserService中新增方法：updateUsernameByAdmin
                // 为避免重复代码，这里简单做唯一性检查
                if (userService.existsByUsername(username)) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("新用户名已存在"));
                }
                user.setUsername(username);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
            }
        }

        // 更新昵称（可选）
        if (nickname != null) {
            if (nickname.length() > 50) {
                return ResponseEntity.badRequest().body(ApiResponse.error("昵称长度不能超过50个字符"));
            }
            user.setNickname(nickname);
        }

        user.setUpdatedAt(java.time.LocalDateTime.now());
        userService.getUserRepository().update(user); // 需要在UserService暴露仓库或封装一个保存方法

        Map<String, Object> dto = new java.util.HashMap<>();
        dto.put("id", user.getId());
        dto.put("username", user.getUsername());
        dto.put("nickname", user.getNickname());
        dto.put("role", user.getRole());
        dto.put("createdAt", user.getCreatedAt());
        dto.put("updatedAt", user.getUpdatedAt());

        return ResponseEntity.ok(ApiResponse.success("用户信息更新成功", dto));
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

    /**
     * 获取所有教练的课程统计信息（管理员概览）
     */
    @GetMapping("/coaches/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCoachesStatistics() {
        try {
            Map<String, Object> statistics = new HashMap<>();
            
            // 获取所有已审批通过且未删除的用户（包括管理员，但只统计有活动课表的）
            List<Users> allUsers = userService.getAllApprovedUsers();
            
            // 过滤出有活动课表的用户（包括管理员）
            List<Users> coaches = allUsers.stream()
                    .filter(user -> {
                        List<Timetables> userTimetables = timetableService.getUserTimetables(user.getId())
                            .stream()
                            .filter(t -> t.getIsDeleted() == null || t.getIsDeleted() == 0)
                            .filter(t -> t.getIsArchived() == null || t.getIsArchived() == 0)
                            .filter(t -> t.getIsActive() != null && t.getIsActive() == 1)
                            .collect(Collectors.toList());
                        return !userTimetables.isEmpty(); // 只统计有活动课表的用户
                    })
                    .collect(Collectors.toList());
            
            List<Map<String, Object>> coachStats = coaches.stream().map(coach -> {
                Map<String, Object> coachStat = new HashMap<>();
                coachStat.put("id", coach.getId());
                coachStat.put("username", coach.getUsername());
                coachStat.put("nickname", coach.getNickname());
                
                // 获取该教练的活动课表（只取第一个，因为每个教练只有一个活动课表）
                List<Timetables> coachTimetables = timetableService.getUserTimetables(coach.getId())
                    .stream()
                    .filter(t -> t.getIsDeleted() == null || t.getIsDeleted() == 0)
                    .filter(t -> t.getIsArchived() == null || t.getIsArchived() == 0)
                    .filter(t -> t.getIsActive() != null && t.getIsActive() == 1)
                    .collect(Collectors.toList());
                
                // 只统计第一个活动课表（业务规则：每个教练只有一个活动课表）
                Timetables activeTimetable = coachTimetables.isEmpty() ? null : coachTimetables.get(0);
                coachStat.put("timetableCount", activeTimetable != null ? 1 : 0);
                
                // 如果杨教练有多个活动课表，记录警告
                if (coachTimetables.size() > 1 && (coach.getNickname() != null && coach.getNickname().contains("杨"))) {
                    logger.warn("警告：杨教练有{}个活动课表，只统计第一个！", coachTimetables.size());
                }
                
                // 添加调试日志
                logger.info("=== 调试教练: {} ===", coach.getNickname() != null ? coach.getNickname() : coach.getUsername());
                logger.info("活动课表数量: {}", coachTimetables.size());
                for (int i = 0; i < coachTimetables.size(); i++) {
                    Timetables t = coachTimetables.get(i);
                    logger.info("活动课表[{}]: ID={}, 名称={}, isWeekly={}", 
                        i, t.getId(), t.getName(), t.getIsWeekly());
                }
                if (activeTimetable != null) {
                    logger.info("使用课表ID: {}, 课表名: {}, isWeekly: {}", 
                        activeTimetable.getId(), activeTimetable.getName(), activeTimetable.getIsWeekly());
                }
                
                // 计算当天课程数
                LocalDate today = LocalDate.now();
                String todayStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                int todayCourses = 0;
                int todayLeaves = 0;
                
                // 计算当周课程数（仅统计本周范围内的课程）
                int weeklyCourses = 0;
                java.time.LocalDate startOfWeek = today.with(java.time.DayOfWeek.MONDAY);
                java.time.LocalDate endOfWeek = today.with(java.time.DayOfWeek.SUNDAY);
                
                // 收集今日课程明细
                java.util.List<java.util.Map<String, Object>> todayCourseDetails = new java.util.ArrayList<>();
                
                if (activeTimetable != null) {
                    if (activeTimetable.getIsWeekly() != null && activeTimetable.getIsWeekly().byteValue() == 1) {
                        // 周固定课表：检查当前周实例
                        try {
                            // 为统计请假，需要获取包含请假的完整列表
                            List<WeeklyInstanceSchedule> instanceSchedules = weeklyInstanceService.getCurrentWeekInstanceSchedulesIncludingLeaves(activeTimetable.getId());
                            // 周实例课程当前实现为物理删除，这里直接统计
                            List<WeeklyInstanceSchedule> validInstanceSchedules = instanceSchedules;
                            weeklyCourses = validInstanceSchedules.size();
                            
                            logger.info("周固定课表 - 本周总课程数: {}", weeklyCourses);
                            logger.info("周固定课表 - 今日日期: {}", today);
                            
                            // 检查当天是否有课程（过滤掉"言言"这节课）
                            List<WeeklyInstanceSchedule> todaySchedulesAll = instanceSchedules.stream()
                                    .filter(schedule -> schedule.getScheduleDate() != null && schedule.getScheduleDate().equals(today))
                                    .collect(Collectors.toList());

                            // 今日请假数
                            todayLeaves = (int) todaySchedulesAll.stream()
                                    .filter(s -> s.getIsOnLeave() != null && s.getIsOnLeave())
                                    .count();
                            
                            // 调试日志：检查上官教练的请假统计
                            if (coach.getNickname() != null && coach.getNickname().contains("上官")) {
                                logger.info("=== 上官教练调试信息 ===");
                                logger.info("今日总课程数: {}", todaySchedulesAll.size());
                                logger.info("今日请假数: {}", todayLeaves);
                                for (WeeklyInstanceSchedule s : todaySchedulesAll) {
                                    logger.info("课程: {} {}-{}, isOnLeave: {}, 日期: {}", 
                                        s.getStudentName(), s.getStartTime(), s.getEndTime(), 
                                        s.getIsOnLeave(), s.getScheduleDate());
                                }
                            }

                            // 今日课程数（不含请假，且过滤掉“言言”特殊课）
                            List<WeeklyInstanceSchedule> todaySchedules = todaySchedulesAll.stream()
                                    .filter(s -> s.getIsOnLeave() == null || !s.getIsOnLeave())
                                    .filter(schedule -> schedule.getStudentName() == null || !schedule.getStudentName().contains("言言"))
                                    .collect(Collectors.toList());

                            todayCourses = todaySchedules.size();
                            
                            // 收集今日课程详情
                            for (WeeklyInstanceSchedule schedule : todaySchedules) {
                                logger.info("今日课程: {} {}-{} (日期: {})", 
                                    schedule.getStudentName(), schedule.getStartTime(), 
                                    schedule.getEndTime(), schedule.getScheduleDate());
                                
                                // 特别标记"言言"这节课
                                if (schedule.getStudentName() != null && schedule.getStudentName().contains("言言")) {
                                    logger.warn("发现言言课程: {} {}-{} (日期: {})", 
                                        schedule.getStudentName(), schedule.getStartTime(), 
                                        schedule.getEndTime(), schedule.getScheduleDate());
                                }
                                
                                java.util.Map<String, Object> item = new java.util.HashMap<>();
                                item.put("studentName", schedule.getStudentName());
                                item.put("startTime", schedule.getStartTime() != null ? schedule.getStartTime().toString() : "");
                                item.put("endTime", schedule.getEndTime() != null ? schedule.getEndTime().toString() : "");
                                todayCourseDetails.add(item);
                            }
                            
                            logger.info("今日课程总数: {}", todayCourses);
                        } catch (Exception e) {
                            // 忽略错误
                        }
                    } else {
                        // 日期范围课表：查询并过滤为本周范围
                        try {
                            List<Schedules> schedules = scheduleService.getTimetableSchedules(activeTimetable.getId(), null);
                            // 课程当前实现为物理删除，仅统计本周的课程
                            List<Schedules> validSchedules = schedules.stream()
                                    .filter(s -> s.getScheduleDate() != null
                                            && ( !s.getScheduleDate().isBefore(startOfWeek) && !s.getScheduleDate().isAfter(endOfWeek) ))
                                    .collect(java.util.stream.Collectors.toList());
                            weeklyCourses = validSchedules.size();
                            
                            // 检查当天是否有课程
                            List<Schedules> todaySchedules = validSchedules.stream()
                                    .filter(schedule -> schedule.getScheduleDate() != null && schedule.getScheduleDate().equals(today))
                                    .collect(Collectors.toList());
                            
                            todayCourses = todaySchedules.size();
                            
                            // 收集今日课程详情
                            for (Schedules schedule : todaySchedules) {
                                java.util.Map<String, Object> item = new java.util.HashMap<>();
                                item.put("studentName", schedule.getStudentName());
                                item.put("startTime", schedule.getStartTime() != null ? schedule.getStartTime().toString() : "");
                                item.put("endTime", schedule.getEndTime() != null ? schedule.getEndTime().toString() : "");
                                todayCourseDetails.add(item);
                            }
                        } catch (Exception e) {
                            // 忽略错误
                        }
                    }
                }
                
                // 对今日课程详情按开始时间排序
                todayCourseDetails.sort((a, b) -> {
                    String timeA = (String) a.get("startTime");
                    String timeB = (String) b.get("startTime");
                    return timeA.compareTo(timeB);
                });
                
                coachStat.put("todayCourses", todayCourses);
                coachStat.put("todayLeaves", todayLeaves);
                coachStat.put("weeklyCourses", weeklyCourses);
                coachStat.put("todayCourseDetails", todayCourseDetails);

                // 新增：上月课程数（包含活动课表和已归档课表）
                int lastMonthCourses = 0;
                try {
                    lastMonthCourses = timetableService.getLastMonthCourseCountForCoach(coach.getId());
                } catch (Exception ignore) {}
                coachStat.put("lastMonthCourses", lastMonthCourses);
                
                return coachStat;
            }).collect(Collectors.toList());
            
            statistics.put("coaches", coachStats);
            statistics.put("totalCoaches", coaches.size());
            statistics.put("totalTodayCourses", coachStats.stream().mapToInt(c -> (Integer) c.get("todayCourses")).sum());
            // 使用第一次遍历时正确计算的周课程数进行累加
            statistics.put("totalWeeklyCourses", coachStats.stream().mapToInt(c -> (Integer) c.get("weeklyCourses")).sum());
            
            // 计算上周课时总数（只统计每个教练的第一个活动课表）
            int totalLastWeekCourses = 0;
            for (Users coach : coaches) {
                List<Timetables> coachTimetables = timetableService.getUserTimetables(coach.getId()).stream()
                        .filter(t -> t.getIsDeleted() == null || t.getIsDeleted() == 0)
                        .filter(t -> t.getIsArchived() == null || t.getIsArchived() == 0)
                        .filter(t -> t.getIsActive() != null && t.getIsActive() == 1)
                        .collect(Collectors.toList());
                
                // 只统计第一个活动课表
                if (!coachTimetables.isEmpty()) {
                    Timetables timetable = coachTimetables.get(0);
                    totalLastWeekCourses += timetableService.getLastWeekCourseCountForTimetable(timetable.getId());
                }
            }
            statistics.put("totalLastWeekCourses", totalLastWeekCourses);
            
            return ResponseEntity.ok(ApiResponse.success("获取教练统计信息成功", statistics));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("获取统计信息失败: " + e.getMessage()));
        }
    }

    /**
     * 获取指定教练的上月课程明细（分页）
     */
    @GetMapping("/coaches/{coachId}/last-month-records")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getCoachLastMonthRecords(
            @PathVariable Long coachId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            if (page <= 0 || size <= 0) {
                return ResponseEntity.badRequest().body(ApiResponse.error("page/size 必须为正整数"));
            }
            
            java.util.Map<String, Object> result = timetableService.getLastMonthCourseRecordsForCoachPaged(coachId, page, size);
            return ResponseEntity.ok(ApiResponse.success("获取上月课程记录成功", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("获取上月课程记录失败: " + e.getMessage()));
        }
    }

    /**
     * 紧急修复：批量生成所有缺失的当前周实例
     */
    @PostMapping("/emergency-fix/weekly-instances")
    public ResponseEntity<ApiResponse<Map<String, Object>>> emergencyFixWeeklyInstances() {
        try {
            Map<String, Object> result = weeklyInstanceService.generateCurrentWeekInstancesForAllActiveTimetables();
            return ResponseEntity.ok(ApiResponse.success("紧急修复完成", result));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("紧急修复失败: " + e.getMessage()));
        }
    }

    /**
     * 自动检查并修复缺失的当前周实例
     */
    @PostMapping("/auto-fix/weekly-instances")
    public ResponseEntity<ApiResponse<String>> autoFixWeeklyInstances() {
        try {
            weeklyInstanceService.ensureCurrentWeekInstancesExist();
            return ResponseEntity.ok(ApiResponse.success("自动修复完成", "已检查并生成缺失的当前周实例"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("自动修复失败: " + e.getMessage()));
        }
    }

    /**
     * 清理所有周实例中的重复课程数据
     */
    @PostMapping("/clean-duplicate-schedules")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cleanDuplicateSchedules() {
        try {
            Map<String, Object> result = weeklyInstanceService.cleanAllDuplicateSchedules();
            return ResponseEntity.ok(ApiResponse.success("清理重复数据完成", result));
        } catch (Exception e) {
            logger.error("清理重复数据失败", e);
            return ResponseEntity.status(500).body(ApiResponse.error("清理重复数据失败: " + e.getMessage()));
        }
    }

    /**
     * 清理指定课表的重复课程数据
     */
    @PostMapping("/clean-duplicate-schedules/{timetableId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cleanDuplicateSchedulesForTimetable(@PathVariable Long timetableId) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // 获取课表的所有周实例
            List<WeeklyInstance> instances = weeklyInstanceRepository.findByTemplateTimetableId(timetableId);
            int totalCleaned = 0;
            int instancesProcessed = 0;
            
            for (WeeklyInstance instance : instances) {
                try {
                    List<WeeklyInstanceSchedule> beforeSchedules = weeklyInstanceScheduleRepository.findByWeeklyInstanceId(instance.getId());
                    weeklyInstanceService.cleanDuplicateSchedules(instance.getId());
                    List<WeeklyInstanceSchedule> afterSchedules = weeklyInstanceScheduleRepository.findByWeeklyInstanceId(instance.getId());
                    
                    int cleaned = beforeSchedules.size() - afterSchedules.size();
                    totalCleaned += cleaned;
                    instancesProcessed++;
                    
                    if (cleaned > 0) {
                        logger.info("课表 {} 实例 {} 清理了 {} 个重复课程", timetableId, instance.getId(), cleaned);
                    }
                } catch (Exception e) {
                    logger.error("清理课表 {} 实例 {} 的重复课程失败: {}", timetableId, instance.getId(), e.getMessage());
                }
            }
            
            result.put("timetableId", timetableId);
            result.put("instancesProcessed", instancesProcessed);
            result.put("totalCleaned", totalCleaned);
            result.put("message", String.format("课表 %d 处理了 %d 个实例，清理了 %d 个重复课程", timetableId, instancesProcessed, totalCleaned));
            
            return ResponseEntity.ok(ApiResponse.success("清理指定课表重复数据完成", result));
        } catch (Exception e) {
            logger.error("清理指定课表重复数据失败", e);
            return ResponseEntity.status(500).body(ApiResponse.error("清理指定课表重复数据失败: " + e.getMessage()));
        }
    }

    /**
     * 调试接口：检查杨教练的课表和课程数据
     */
    @GetMapping("/debug/yang-coach")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> debugYangCoachData() {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // 查找杨教练（通过用户名或昵称）
            List<Users> allUsers = userService.getAllApprovedUsers();
            Users yangCoach = allUsers.stream()
                .filter(u -> (u.getNickname() != null && u.getNickname().contains("杨")) ||
                           (u.getUsername() != null && u.getUsername().contains("杨")))
                .findFirst()
                .orElse(null);
            
            if (yangCoach == null) {
                result.put("error", "找不到杨教练");
                return ResponseEntity.ok(result);
            }
            
            Map<String, Object> coachInfo = new HashMap<>();
            coachInfo.put("id", yangCoach.getId());
            coachInfo.put("username", yangCoach.getUsername());
            coachInfo.put("nickname", yangCoach.getNickname());
            result.put("coach", coachInfo);
            
            // 获取杨教练的所有课表
            List<Timetables> allTimetables = timetableService.getUserTimetables(yangCoach.getId());
            result.put("allTimetablesCount", allTimetables.size());
            result.put("allTimetables", allTimetables.stream().map(t -> {
                Map<String, Object> timetableInfo = new HashMap<>();
                timetableInfo.put("id", t.getId());
                timetableInfo.put("name", t.getName());
                timetableInfo.put("isActive", t.getIsActive());
                timetableInfo.put("isDeleted", t.getIsDeleted());
                timetableInfo.put("isArchived", t.getIsArchived());
                timetableInfo.put("isWeekly", t.getIsWeekly());
                return timetableInfo;
            }).collect(Collectors.toList()));
            
            // 获取活动课表
            List<Timetables> activeTimetables = allTimetables.stream()
                .filter(t -> t.getIsDeleted() == null || t.getIsDeleted() == 0)
                .filter(t -> t.getIsArchived() == null || t.getIsArchived() == 0)
                .filter(t -> t.getIsActive() != null && t.getIsActive() == 1)
                .collect(Collectors.toList());
            
            result.put("activeTimetablesCount", activeTimetables.size());
            
            // 检查今日课程
            LocalDate today = LocalDate.now();
            List<Map<String, Object>> todayCoursesDetails = new ArrayList<>();
            
            for (Timetables timetable : activeTimetables) {
                Map<String, Object> timetableInfo = new HashMap<>();
                timetableInfo.put("timetableId", timetable.getId());
                timetableInfo.put("timetableName", timetable.getName());
                
                if (timetable.getIsWeekly() != null && timetable.getIsWeekly() == 1) {
                    // 周固定课表
                    try {
                        List<WeeklyInstanceSchedule> instanceSchedules = weeklyInstanceService.getCurrentWeekInstanceSchedules(timetable.getId());
                        List<WeeklyInstanceSchedule> todaySchedules = instanceSchedules.stream()
                            .filter(s -> s.getScheduleDate() != null && s.getScheduleDate().equals(today))
                            .collect(Collectors.toList());
                        
                        timetableInfo.put("todayCoursesCount", todaySchedules.size());
                        timetableInfo.put("todayCourses", todaySchedules.stream().map(s -> {
                            Map<String, Object> scheduleInfo = new HashMap<>();
                            scheduleInfo.put("id", s.getId());
                            scheduleInfo.put("studentName", s.getStudentName());
                            scheduleInfo.put("startTime", s.getStartTime());
                            scheduleInfo.put("endTime", s.getEndTime());
                            scheduleInfo.put("scheduleDate", s.getScheduleDate());
                            return scheduleInfo;
                        }).collect(Collectors.toList()));
                    } catch (Exception e) {
                        timetableInfo.put("error", e.getMessage());
                    }
                } else {
                    // 日期范围课表
                    try {
                        List<Schedules> schedules = scheduleService.getTimetableSchedules(timetable.getId(), null);
                        List<Schedules> todaySchedules = schedules.stream()
                            .filter(s -> s.getScheduleDate() != null && s.getScheduleDate().equals(today))
                            .collect(Collectors.toList());
                        
                        timetableInfo.put("todayCoursesCount", todaySchedules.size());
                        timetableInfo.put("todayCourses", todaySchedules.stream().map(s -> {
                            Map<String, Object> scheduleInfo = new HashMap<>();
                            scheduleInfo.put("id", s.getId());
                            scheduleInfo.put("studentName", s.getStudentName());
                            scheduleInfo.put("startTime", s.getStartTime());
                            scheduleInfo.put("endTime", s.getEndTime());
                            scheduleInfo.put("scheduleDate", s.getScheduleDate());
                            return scheduleInfo;
                        }).collect(Collectors.toList()));
                    } catch (Exception e) {
                        timetableInfo.put("error", e.getMessage());
                    }
                }
                
                todayCoursesDetails.add(timetableInfo);
            }
            
            result.put("todayCoursesDetails", todayCoursesDetails);
            result.put("date", today);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            return ResponseEntity.status(500).body(errorResult);
        }
    }

    /**
     * 调试接口：检查指定课表的模板数据和实例数据
     */
    @GetMapping("/debug/timetable/{timetableId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> debugTimetableData(@PathVariable Long timetableId) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // 获取模板课程
            List<Schedules> templateSchedules = scheduleRepository.findTemplateSchedulesByTimetableId(timetableId);
            result.put("templateSchedules", templateSchedules);
            result.put("templateCount", templateSchedules.size());
            
            // 获取当前周实例
            WeeklyInstance currentInstance = weeklyInstanceService.getCurrentWeekInstance(timetableId);
            if (currentInstance != null) {
                result.put("currentInstance", currentInstance);
                
                // 获取实例课程
                List<WeeklyInstanceSchedule> instanceSchedules = weeklyInstanceScheduleRepository.findByWeeklyInstanceId(currentInstance.getId());
                result.put("instanceSchedules", instanceSchedules);
                result.put("instanceCount", instanceSchedules.size());
                
                // 获取今日课程
                LocalDate today = LocalDate.now();
                List<WeeklyInstanceSchedule> todaySchedules = instanceSchedules.stream()
                    .filter(schedule -> today.equals(schedule.getScheduleDate()))
                    .collect(Collectors.toList());
                result.put("todaySchedules", todaySchedules);
                result.put("todayCount", todaySchedules.size());
                
                // 检查今日课程中是否有重复数据
                Map<String, Integer> duplicateCheck = new HashMap<>();
                for (WeeklyInstanceSchedule schedule : todaySchedules) {
                    String key = schedule.getStudentName() + "_" + schedule.getStartTime() + "_" + schedule.getEndTime();
                    duplicateCheck.put(key, duplicateCheck.getOrDefault(key, 0) + 1);
                }
                result.put("duplicateCheck", duplicateCheck);
                
                // 通过ScheduleService获取今日课程（转换后的数据）
                List<Schedules> todaySchedulesConverted = scheduleService.getTodaySchedules(timetableId);
                result.put("todaySchedulesConverted", todaySchedulesConverted);
                result.put("todayConvertedCount", todaySchedulesConverted.size());
                
                // 检查转换后的数据是否有重复
                Map<String, Integer> duplicateCheckConverted = new HashMap<>();
                for (Schedules schedule : todaySchedulesConverted) {
                    String key = schedule.getStudentName() + "_" + schedule.getStartTime() + "_" + schedule.getEndTime();
                    duplicateCheckConverted.put(key, duplicateCheckConverted.getOrDefault(key, 0) + 1);
                }
                result.put("duplicateCheckConverted", duplicateCheckConverted);
            } else {
                result.put("currentInstance", null);
                result.put("instanceSchedules", new ArrayList<>());
                result.put("instanceCount", 0);
                result.put("todaySchedules", new ArrayList<>());
                result.put("todayCount", 0);
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("调试课表数据失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
} 