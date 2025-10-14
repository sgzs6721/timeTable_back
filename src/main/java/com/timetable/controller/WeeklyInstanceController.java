package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.WeeklyInstanceDTO;
import com.timetable.dto.LeaveRequest;
import com.timetable.entity.WeeklyInstance;
import com.timetable.entity.WeeklyInstanceSchedule;
import com.timetable.entity.StudentOperationRecord;
import com.timetable.repository.StudentOperationRecordRepository;
import com.timetable.service.WeeklyInstanceService;
import com.timetable.service.TimetableService;
import com.timetable.service.UserService;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.generated.tables.pojos.Timetables;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * 周实例控制器
 */
@RestController
@RequestMapping("/weekly-instances")
public class WeeklyInstanceController {

    private static final Logger logger = LoggerFactory.getLogger(WeeklyInstanceController.class);

    @Autowired
    private WeeklyInstanceService weeklyInstanceService;

    @Autowired
    private TimetableService timetableService;

    @Autowired
    private UserService userService;
    
    @Autowired
    private StudentOperationRecordRepository studentOperationRecordRepository;

    /**
     * 为指定课表生成当前周实例
     */
    @PostMapping("/generate/{timetableId}")
    public ResponseEntity<ApiResponse<WeeklyInstance>> generateCurrentWeekInstance(
            @PathVariable Long timetableId,
            Authentication authentication) {
        
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }

        // 检查课表是否属于当前用户或用户是否为管理员
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            Timetables timetable = timetableService.getTimetable(timetableId, user.getId());
            if (timetable == null) {
                return ResponseEntity.notFound().build();
            }
        }

        try {
            WeeklyInstance instance = weeklyInstanceService.generateCurrentWeekInstance(timetableId);
            return ResponseEntity.ok(ApiResponse.success("当前周实例生成成功", instance));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            // 添加详细的错误日志
            System.err.println("生成周实例失败，课表ID: " + timetableId);
            System.err.println("错误类型: " + e.getClass().getName());
            System.err.println("错误详情: " + e.getMessage());
            e.printStackTrace();
            
            // 检查是否是数据库表不存在的问题
            String errorMsg = e.getMessage();
            if (errorMsg != null && (errorMsg.contains("weekly_instances") || errorMsg.contains("Table") || errorMsg.contains("doesn't exist"))) {
                return ResponseEntity.status(500).body(ApiResponse.error("数据库表不存在，请执行数据库迁移: " + e.getMessage()));
            }
            
            String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.status(500).body(ApiResponse.error("服务器内部错误: " + errorMessage));
        }
    }

    /**
     * 获取指定课表的当前周实例
     */
    @GetMapping("/current/{timetableId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentWeekInstance(
            @PathVariable Long timetableId,
            Authentication authentication) {
        
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }

        // 检查课表是否属于当前用户或用户是否为管理员
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            Timetables timetable = timetableService.getTimetable(timetableId, user.getId());
            if (timetable == null) {
                return ResponseEntity.notFound().build();
            }
        }

        WeeklyInstance instance = weeklyInstanceService.getCurrentWeekInstance(timetableId);
        List<WeeklyInstanceSchedule> schedules = new ArrayList<>();
        
        if (instance != null) {
            schedules = weeklyInstanceService.getCurrentWeekInstanceSchedules(timetableId);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("instance", instance);
        result.put("schedules", schedules);
        result.put("hasInstance", instance != null);

        return ResponseEntity.ok(ApiResponse.success("获取当前周实例成功", result));
    }

    /**
     * 获取指定课表的当前周实例（包含请假课程）
     */
    @GetMapping("/current/{timetableId}/including-leaves")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentWeekInstanceIncludingLeaves(
            @PathVariable Long timetableId,
            Authentication authentication) {
        
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }

        // 检查课表是否属于当前用户或用户是否为管理员
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            Timetables timetable = timetableService.getTimetable(timetableId, user.getId());
            if (timetable == null) {
                return ResponseEntity.notFound().build();
            }
        }

        WeeklyInstance instance = weeklyInstanceService.getCurrentWeekInstance(timetableId);
        List<WeeklyInstanceSchedule> schedules = new ArrayList<>();
        
        if (instance != null) {
            schedules = weeklyInstanceService.getCurrentWeekInstanceSchedulesIncludingLeaves(timetableId);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("instance", instance);
        result.put("schedules", schedules);
        result.put("hasInstance", instance != null);

        return ResponseEntity.ok(ApiResponse.success("获取当前周实例成功（包含请假）", result));
    }

    /**
     * 获取指定课表的所有周实例
     */
    @GetMapping("/list/{timetableId}")
    public ResponseEntity<ApiResponse<List<WeeklyInstanceDTO>>> getWeeklyInstances(
            @PathVariable Long timetableId,
            Authentication authentication) {
        
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }

        // 检查课表是否属于当前用户或用户是否为管理员
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            Timetables timetable = timetableService.getTimetable(timetableId, user.getId());
            if (timetable == null) {
                return ResponseEntity.notFound().build();
            }
        }

        List<WeeklyInstanceDTO> instances = weeklyInstanceService.getWeeklyInstancesByTemplateId(timetableId);
        return ResponseEntity.ok(ApiResponse.success("获取周实例列表成功", instances));
    }

    /**
     * 切换到指定的周实例
     */
    @PutMapping("/switch/{instanceId}")
    public ResponseEntity<ApiResponse<WeeklyInstance>> switchToWeekInstance(
            @PathVariable Long instanceId,
            Authentication authentication) {
        
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }

        try {
            WeeklyInstance instance = weeklyInstanceService.switchToWeekInstance(instanceId);
            
            // 验证用户权限
            if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
                Timetables timetable = timetableService.getTimetable(instance.getTemplateTimetableId(), user.getId());
                if (timetable == null) {
                    return ResponseEntity.notFound().build();
                }
            }

            return ResponseEntity.ok(ApiResponse.success("切换周实例成功", instance));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 获取周实例的课程安排
     */
    @GetMapping("/{instanceId}/schedules")
    public ResponseEntity<ApiResponse<List<WeeklyInstanceSchedule>>> getInstanceSchedules(
            @PathVariable Long instanceId,
            Authentication authentication) {
        
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }

        // 获取实例信息
        WeeklyInstance instance = weeklyInstanceService.findById(instanceId);
        if (instance == null) {
            return ResponseEntity.notFound().build();
        }

        // 验证用户权限
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            Timetables timetable = timetableService.getTimetable(instance.getTemplateTimetableId(), user.getId());
            if (timetable == null) {
                return ResponseEntity.notFound().build();
            }
        }

        // 获取指定实例的课程，而不是当前周实例的课程
        List<WeeklyInstanceSchedule> schedules = weeklyInstanceService.getInstanceSchedules(instanceId);
        return ResponseEntity.ok(ApiResponse.success("获取实例课程成功", schedules));
    }

    /**
     * 根据日期返回“实例逻辑”的活动课表课程（今日从本周实例；明日如果跨周则用下周实例）
     */
    @GetMapping("/by-date")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInstanceSchedulesByDate(
            @RequestParam String date,
            Authentication authentication) {
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }
        try {
            Map<String, Object> map = weeklyInstanceService.getActiveInstanceSchedulesByDate(date);
            return ResponseEntity.ok(ApiResponse.success("获取实例课程成功", map));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("获取失败: " + e.getMessage()));
        }
    }

    /**
     * 批量为所有活动的周固定课表生成当前周实例
     */
    @PostMapping("/batch-generate/current-week")
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchGenerateCurrentWeekInstances(
            Authentication authentication) {
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }

        // 只有管理员可以执行批量操作
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            return ResponseEntity.status(403).body(ApiResponse.error("权限不足，只有管理员可以执行批量操作"));
        }

        try {
            Map<String, Object> result = weeklyInstanceService.generateCurrentWeekInstancesForAllActiveTimetables();
            return ResponseEntity.ok(ApiResponse.success("批量生成当前周实例完成", result));
        } catch (Exception e) {
            logger.error("批量生成当前周实例失败", e);
            return ResponseEntity.status(500).body(ApiResponse.error("批量生成失败: " + e.getMessage()));
        }
    }

    /**
     * 自动修复缺失的当前周实例
     */
    @PostMapping("/auto-fix/current-week")
    public ResponseEntity<ApiResponse<String>> autoFixCurrentWeekInstances(
            Authentication authentication) {
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }

        // 只有管理员可以执行修复操作
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            return ResponseEntity.status(403).body(ApiResponse.error("权限不足，只有管理员可以执行修复操作"));
        }

        try {
            weeklyInstanceService.ensureCurrentWeekInstancesExist();
            return ResponseEntity.ok(ApiResponse.success("自动修复完成", "已检查并生成缺失的当前周实例"));
        } catch (Exception e) {
            logger.error("自动修复当前周实例失败", e);
            return ResponseEntity.status(500).body(ApiResponse.error("自动修复失败: " + e.getMessage()));
        }
    }

    /**
     * 生成下周实例（手动）
     */
    @PostMapping("/next-week/generate/{timetableId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateNextWeekInstanceManual(
            @PathVariable Long timetableId,
            Authentication authentication) {
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }
        // 非管理员需要验证课表归属
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            Timetables timetable = timetableService.getTimetable(timetableId, user.getId());
            if (timetable == null) {
                return ResponseEntity.notFound().build();
            }
        }
        try {
            WeeklyInstance instance = weeklyInstanceService.generateNextWeekInstance(timetableId);
            Map<String, Object> data = new HashMap<>();
            data.put("id", instance.getId());
            data.put("weekStartDate", instance.getWeekStartDate());
            data.put("weekEndDate", instance.getWeekEndDate());
            return ResponseEntity.ok(ApiResponse.success("生成下周实例成功", data));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("生成失败: " + e.getMessage()));
        }
    }

    /**
     * 删除下周实例（如果存在）
     */
    @DeleteMapping("/next-week/{timetableId}")
    public ResponseEntity<ApiResponse<String>> deleteNextWeekInstance(
            @PathVariable Long timetableId,
            Authentication authentication) {
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }
        // 非管理员需要验证课表归属
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            Timetables timetable = timetableService.getTimetable(timetableId, user.getId());
            if (timetable == null) {
                return ResponseEntity.notFound().build();
            }
        }
        try {
            boolean deleted = weeklyInstanceService.deleteNextWeekInstance(timetableId);
            if (deleted) {
                return ResponseEntity.ok(ApiResponse.success("已删除下周实例"));
            } else {
                return ResponseEntity.ok(ApiResponse.success("未找到下周实例，无需删除"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("删除失败: " + e.getMessage()));
        }
    }

    /**
     * 在周实例中创建课程
     */
    @PostMapping("/{instanceId}/schedules")
    public ResponseEntity<ApiResponse<WeeklyInstanceSchedule>> createInstanceSchedule(
            @PathVariable Long instanceId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }

        try {
            // 解析请求参数
            String studentName = (String) request.get("studentName");
            String subject = (String) request.get("subject");
            String dayOfWeek = (String) request.get("dayOfWeek");
            String startTimeStr = (String) request.get("startTime");
            String endTimeStr = (String) request.get("endTime");
            String scheduleDateStr = (String) request.get("scheduleDate");
            String note = (String) request.get("note");

            if (studentName == null || dayOfWeek == null || startTimeStr == null || endTimeStr == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("缺少必要参数"));
            }

            LocalTime startTime = LocalTime.parse(startTimeStr);
            LocalTime endTime = LocalTime.parse(endTimeStr);
            LocalDate scheduleDate = null;
            
            // 如果提供了scheduleDate，则解析它
            if (scheduleDateStr != null && !scheduleDateStr.trim().isEmpty()) {
                scheduleDate = LocalDate.parse(scheduleDateStr);
            }

            WeeklyInstanceSchedule schedule = new WeeklyInstanceSchedule();
            schedule.setStudentName(studentName);
            schedule.setDayOfWeek(dayOfWeek);
            schedule.setStartTime(startTime);
            schedule.setEndTime(endTime);
            schedule.setScheduleDate(scheduleDate);
            schedule.setSubject(subject);
            schedule.setNote(note);

            WeeklyInstanceSchedule createdSchedule = weeklyInstanceService.createInstanceSchedule(instanceId, schedule);
            return ResponseEntity.ok(ApiResponse.success("创建实例课程成功", createdSchedule));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("创建课程失败: " + e.getMessage()));
        }
    }

    /**
     * 在周实例中批量创建课程
     */
    @PostMapping("/{instanceId}/schedules/batch")
    public ResponseEntity<ApiResponse<String>> createInstanceSchedulesBatch(
            @PathVariable Long instanceId,
            @RequestBody List<Map<String, Object>> schedulesList,
            Authentication authentication) {
        
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }

        try {
            List<WeeklyInstanceSchedule> schedulesToCreate = new ArrayList<>();
            
            for (Map<String, Object> request : schedulesList) {
                // 解析请求参数
                String studentName = (String) request.get("studentName");
                String subject = (String) request.get("subject");
                String dayOfWeek = (String) request.get("dayOfWeek");
                String startTimeStr = (String) request.get("startTime");
                String endTimeStr = (String) request.get("endTime");
                String scheduleDateStr = (String) request.get("scheduleDate");
                String note = (String) request.get("note");

                WeeklyInstanceSchedule schedule = new WeeklyInstanceSchedule();
                schedule.setStudentName(studentName);
                schedule.setSubject(subject != null ? subject : "");
                schedule.setDayOfWeek(dayOfWeek);
                schedule.setStartTime(LocalTime.parse(startTimeStr));
                schedule.setEndTime(LocalTime.parse(endTimeStr));
                if (scheduleDateStr != null) {
                    schedule.setScheduleDate(LocalDate.parse(scheduleDateStr));
                }
                schedule.setNote(note != null ? note : "批量添加");

                schedulesToCreate.add(schedule);
            }

            weeklyInstanceService.createInstanceSchedulesBatch(instanceId, schedulesToCreate);
            return ResponseEntity.ok(ApiResponse.success("批量创建实例课程成功，共创建 " + schedulesToCreate.size() + " 个课程"));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("批量创建课程失败: " + e.getMessage()));
        }
    }

    /**
     * 更新周实例中的课程
     */
    @PutMapping("/schedules/{scheduleId}")
    public ResponseEntity<ApiResponse<WeeklyInstanceSchedule>> updateInstanceSchedule(
            @PathVariable Long scheduleId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }

        try {
            // 解析请求参数
            String studentName = (String) request.get("studentName");
            String subject = (String) request.get("subject");
            String dayOfWeek = (String) request.get("dayOfWeek");
            String startTimeStr = (String) request.get("startTime");
            String endTimeStr = (String) request.get("endTime");
            String scheduleDateStr = (String) request.get("scheduleDate");
            String note = (String) request.get("note");

            WeeklyInstanceSchedule updatedSchedule = new WeeklyInstanceSchedule();
            // 只设置非null的字段，让服务层处理部分更新
            if (studentName != null) updatedSchedule.setStudentName(studentName);
            if (subject != null) updatedSchedule.setSubject(subject);
            if (dayOfWeek != null) updatedSchedule.setDayOfWeek(dayOfWeek);
            if (startTimeStr != null) updatedSchedule.setStartTime(LocalTime.parse(startTimeStr));
            if (endTimeStr != null) updatedSchedule.setEndTime(LocalTime.parse(endTimeStr));
            if (scheduleDateStr != null && !scheduleDateStr.trim().isEmpty()) {
                updatedSchedule.setScheduleDate(LocalDate.parse(scheduleDateStr));
            }
            if (note != null) updatedSchedule.setNote(note);

            WeeklyInstanceSchedule result = weeklyInstanceService.updateInstanceSchedule(scheduleId, updatedSchedule);
            return ResponseEntity.ok(ApiResponse.success("更新实例课程成功", result));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("更新课程失败: " + e.getMessage()));
        }
    }

    /**
     * 删除周实例中的课程
     */
    @DeleteMapping("/schedules/{scheduleId}")
    public ResponseEntity<ApiResponse<String>> deleteInstanceSchedule(
            @PathVariable Long scheduleId,
            Authentication authentication) {
        
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }

        try {
            weeklyInstanceService.deleteInstanceSchedule(scheduleId);
            return ResponseEntity.ok(ApiResponse.success("删除实例课程成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("删除课程失败: " + e.getMessage()));
        }
    }

    /**
     * 批量删除周实例中的课程
     */
    @DeleteMapping("/schedules/batch")
    public ResponseEntity<ApiResponse<Integer>> deleteInstanceSchedulesBatch(
            @RequestBody List<Long> scheduleIds,
            Authentication authentication) {
        
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }

        try {
            int deletedCount = weeklyInstanceService.deleteInstanceSchedulesBatch(scheduleIds);
            return ResponseEntity.ok(ApiResponse.success("批量删除实例课程成功", deletedCount));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("批量删除课程失败: " + e.getMessage()));
        }
    }

    /**
     * 同步模板课表到周实例
     */
    @PostMapping("/sync/{timetableId}")
    public ResponseEntity<ApiResponse<String>> syncTemplateToInstances(
            @PathVariable Long timetableId,
            Authentication authentication) {
        
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }

        // 检查课表是否属于当前用户或用户是否为管理员
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            Timetables timetable = timetableService.getTimetable(timetableId, user.getId());
            if (timetable == null) {
                return ResponseEntity.notFound().build();
            }
        }

        try {
            weeklyInstanceService.syncTemplateChangesToInstances(timetableId);
            return ResponseEntity.ok(ApiResponse.success("同步模板课表到实例成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("同步失败: " + e.getMessage()));
        }
    }

    /**
     * 完全恢复当前周实例为固定课表状态
     */
    @PostMapping("/restore/{timetableId}")
    public ResponseEntity<ApiResponse<String>> restoreCurrentWeekInstanceToTemplate(
            @PathVariable Long timetableId,
            Authentication authentication) {
        
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }

        // 检查课表是否属于当前用户或用户是否为管理员
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            Timetables timetable = timetableService.getTimetable(timetableId, user.getId());
            if (timetable == null) {
                return ResponseEntity.notFound().build();
            }
        }

        try {
            weeklyInstanceService.restoreCurrentWeekInstanceToTemplate(timetableId);
            return ResponseEntity.ok(ApiResponse.success("已完全恢复为固定课表"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("恢复失败: " + e.getMessage()));
        }
    }

    /**
     * 清空指定课表当前周实例中的所有课程
     */
    @DeleteMapping("/current/{timetableId}/schedules")
    public ResponseEntity<ApiResponse<Integer>> clearCurrentWeekInstanceSchedules(
            @PathVariable Long timetableId,
            Authentication authentication) {
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }
        // 权限校验：非管理员需是自己课表
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            Timetables timetable = timetableService.getTimetable(timetableId, user.getId());
            if (timetable == null) {
                return ResponseEntity.notFound().build();
            }
        }
        try {
            int deleted = weeklyInstanceService.clearCurrentWeekInstanceSchedules(timetableId);
            return ResponseEntity.ok(ApiResponse.success("清空本周实例课程成功", deleted));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("清空失败: " + e.getMessage()));
        }
    }

    /**
     * 检查课表是否有当前周实例
     */
    @GetMapping("/check/{timetableId}")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkCurrentWeekInstance(
            @PathVariable Long timetableId,
            Authentication authentication) {
        
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }

        // 检查课表是否属于当前用户或用户是否为管理员
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            Timetables timetable = timetableService.getTimetable(timetableId, user.getId());
            if (timetable == null) {
                return ResponseEntity.notFound().build();
            }
        }

        boolean hasInstance = weeklyInstanceService.hasCurrentWeekInstance(timetableId);
        Map<String, Boolean> result = new HashMap<>();
        result.put("hasCurrentWeekInstance", hasInstance);

        return ResponseEntity.ok(ApiResponse.success("检查完成", result));
    }

    /**
     * 学生请假
     */
    @PostMapping("/schedules/leave")
    public ResponseEntity<ApiResponse<WeeklyInstanceSchedule>> requestLeave(
            @RequestBody LeaveRequest leaveRequest,
            Authentication authentication) {
        
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }

        try {
            WeeklyInstanceSchedule schedule = weeklyInstanceService.requestLeave(
                leaveRequest.getScheduleId(), 
                leaveRequest.getLeaveReason()
            );
            return ResponseEntity.ok(ApiResponse.success("请假申请成功", schedule));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("请假申请失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("请假申请失败: " + e.getMessage()));
        }
    }

    /**
     * 取消请假
     */
    @PostMapping("/schedules/cancel-leave/{scheduleId}")
    public ResponseEntity<ApiResponse<WeeklyInstanceSchedule>> cancelLeave(
            @PathVariable Long scheduleId,
            Authentication authentication) {
        
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }

        try {
            WeeklyInstanceSchedule schedule = weeklyInstanceService.cancelLeave(scheduleId);
            return ResponseEntity.ok(ApiResponse.success("取消请假成功", schedule));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("取消请假失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("取消请假失败: " + e.getMessage()));
        }
    }

    /**
     * 获取所有请假记录
     */
    @GetMapping("/leave-records")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getLeaveRecords(
            Authentication authentication) {
        
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }

        // 只有管理员可以查看所有请假记录
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            return ResponseEntity.status(403).body(ApiResponse.error("权限不足"));
        }

        try {
            List<Map<String, Object>> leaveRecords = weeklyInstanceService.getAllLeaveRecords();
            return ResponseEntity.ok(ApiResponse.success("获取请假记录成功", leaveRecords));
        } catch (Exception e) {
            logger.error("获取请假记录失败", e);
            return ResponseEntity.status(500).body(ApiResponse.error("获取请假记录失败: " + e.getMessage()));
        }
    }

    /**
     * 删除请假记录
     */
    @DeleteMapping("/leave-records/{recordId}")
    public ResponseEntity<ApiResponse<String>> deleteLeaveRecord(
            @PathVariable Long recordId,
            Authentication authentication) {
        
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }

        // 只有管理员可以删除请假记录
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            return ResponseEntity.status(403).body(ApiResponse.error("权限不足"));
        }

        try {
            boolean success = weeklyInstanceService.deleteLeaveRecord(recordId);
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("删除请假记录成功", "删除成功"));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error("删除请假记录失败"));
            }
        } catch (Exception e) {
            logger.error("删除请假记录失败", e);
            return ResponseEntity.status(500).body(ApiResponse.error("删除请假记录失败: " + e.getMessage()));
        }
    }

    /**
     * 批量删除请假记录
     */
    @DeleteMapping("/leave-records/batch")
    public ResponseEntity<ApiResponse<String>> deleteLeaveRecordsBatch(
            @RequestBody List<Long> recordIds,
            Authentication authentication) {
        
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }

        // 只有管理员可以批量删除请假记录
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            return ResponseEntity.status(403).body(ApiResponse.error("权限不足"));
        }

        try {
            int deletedCount = weeklyInstanceService.deleteLeaveRecordsBatch(recordIds);
            return ResponseEntity.ok(ApiResponse.success("批量删除请假记录成功", "成功删除 " + deletedCount + " 条记录"));
        } catch (Exception e) {
            logger.error("批量删除请假记录失败", e);
            return ResponseEntity.status(500).body(ApiResponse.error("批量删除请假记录失败: " + e.getMessage()));
        }
    }

    /**
     * 获取学员记录
     */
    @GetMapping("/student-records")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStudentRecords(
            @RequestParam String studentName,
            @RequestParam(required = false) String coachName,
            Authentication authentication) {
        
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }

        try {
            Map<String, Object> studentRecords;
            if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                // 管理员可以查看所有学员记录
                studentRecords = weeklyInstanceService.getStudentRecords(studentName, coachName);
            } else {
                // 普通用户只能查看自己的学员记录
                String currentCoachName = user.getNickname() != null ? user.getNickname() : user.getUsername();
                studentRecords = weeklyInstanceService.getStudentRecords(studentName, currentCoachName);
            }
            return ResponseEntity.ok(ApiResponse.success("获取学员记录成功", studentRecords));
        } catch (Exception e) {
            logger.error("获取学员记录失败", e);
            return ResponseEntity.status(500).body(ApiResponse.error("获取学员记录失败: " + e.getMessage()));
        }
    }

    /**
     * 获取学员列表，支持分组（全部）/原单列表
     */
    @GetMapping("/students")
    public ResponseEntity<ApiResponse<?>> getAllStudents(
            @RequestParam(defaultValue = "false") Boolean showAll,
            Authentication authentication) {
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }
        try {
            // 获取被隐藏的学员列表、合并规则和分配课时规则
            List<String> hiddenStudents = getHiddenStudents();
            java.util.Map<String, String> mergeRules = getMergeRules();
            java.util.Map<String, Integer> assignHoursRules = getAssignHoursRules();
            
            if ("ADMIN".equalsIgnoreCase(user.getRole()) && showAll) {
                // 分组返回教练列表
                List<com.timetable.dto.CoachStudentSummaryDTO> grouped = weeklyInstanceService.getStudentGroupByCoachSummaryAll();
                // 应用合并规则
                grouped = applyMergeRulesToGrouped(grouped, mergeRules);
                // 应用分配课时规则
                grouped = applyAssignHoursToGrouped(grouped, assignHoursRules);
                // 过滤掉被隐藏的学员
                grouped = filterHiddenStudentsFromGrouped(grouped, hiddenStudents);
                return ResponseEntity.ok(ApiResponse.success("获取学员列表成功", grouped));
            } else {
                // 保持原有教练/普通模式单纯列表
                List<com.timetable.dto.StudentSummaryDTO> students =
                        weeklyInstanceService.getStudentSummariesByCoach(user.getId());
                // 应用合并规则
                students = applyMergeRulesToList(students, mergeRules);
                // 应用分配课时规则
                students = applyAssignHoursToList(students, assignHoursRules);
                // 过滤掉被隐藏的学员
                students = filterHiddenStudentsFromList(students, hiddenStudents);
                return ResponseEntity.ok(ApiResponse.success("获取学员列表成功", students));
            }
        } catch (Exception e) {
            logger.error("获取学员列表失败", e);
            return ResponseEntity.status(500).body(ApiResponse.error("获取学员列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取操作记录（管理员可以查看所有记录，普通用户只能查看自己的记录）
     */
    @GetMapping("/operation-records")
    public ResponseEntity<ApiResponse<List<StudentOperationRecord>>> getOperationRecords(
            @RequestParam(defaultValue = "false") Boolean showAll,
            @RequestParam(required = false) String studentName,
            @RequestParam(required = false) Long coachId,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }
            
            List<StudentOperationRecord> records;
            
            // 首先根据用户权限和参数获取基础记录集
            List<StudentOperationRecord> baseRecords;
            if ("ADMIN".equalsIgnoreCase(user.getRole()) && showAll && coachId == null) {
                // 管理员查看全部记录且未指定教练ID
                baseRecords = studentOperationRecordRepository.findAll();
            } else if (coachId != null) {
                // 指定了教练ID，查询该教练的记录
                baseRecords = studentOperationRecordRepository.findByCoachId(coachId);
            } else {
                // 默认查询当前用户的记录
                baseRecords = studentOperationRecordRepository.findByCoachId(user.getId());
            }
            
            // 如果指定了学员名称，需要过滤特定学员的记录
            if (studentName != null && !studentName.trim().isEmpty()) {
                final String trimmedStudentName = studentName.trim();
                
                // 过滤出与该学员相关的记录（考虑重命名转换）
                records = baseRecords.stream()
                    .filter(record -> {
                        // 直接匹配：学员名称匹配oldName或newName
                        if (trimmedStudentName.equals(record.getOldName()) || 
                            trimmedStudentName.equals(record.getNewName())) {
                            return true;
                        }
                        
                        // 反向查找：如果studentName是重命名后的名字，找到原名相关的记录
                        // 例如：studentName="跃跃2"，需要找到"跃跃"->"跃跃2"的记录
                        return false;
                    })
                    .collect(java.util.stream.Collectors.toList());
                    
            } else {
                // 没有指定学员名称，返回基础记录集（已经根据权限和教练ID过滤过了）
                records = baseRecords;
            }
            
            return ResponseEntity.ok(ApiResponse.success("获取操作记录成功", records));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("获取操作记录失败: " + e.getMessage()));
        }
    }

    /**
     * 重命名学员 (临时API)
     */
    /**
     * 合并学员 (临时API)
     */
    @PostMapping("/merge-students")
    public ResponseEntity<ApiResponse<String>> mergeStudents(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }

            String displayName = (String) request.get("displayName");
            @SuppressWarnings("unchecked")
            List<String> studentNames = (List<String>) request.get("studentNames");
            
            if (displayName == null || displayName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("合并后名称不能为空"));
            }
            
            if (studentNames == null || studentNames.size() < 2) {
                return ResponseEntity.badRequest().body(ApiResponse.error("至少需要选择两个学员进行合并"));
            }

            displayName = displayName.trim();
            
            // 查找第一个学员所属的教练ID作为合并记录的教练ID
            Long coachId = null;
            for (String studentName : studentNames) {
                if (studentName != null && !studentName.trim().isEmpty()) {
                    coachId = weeklyInstanceService.findCoachIdByStudentName(studentName.trim());
                    if (coachId != null) {
                        break;
                    }
                }
            }
            if (coachId == null) {
                coachId = user.getId(); // 如果找不到，使用当前用户ID
            }

            // 创建一条合并操作记录
            StudentOperationRecord record = new StudentOperationRecord();
            record.setCoachId(coachId);
            record.setOperationType("MERGE");
            record.setOldName(String.join(",", studentNames)); // 将所有学员名称用逗号连接
            record.setNewName(displayName); // 合并后的显示名称
            record.setDetails("{\"operationType\":\"MERGE_STUDENT\",\"description\":\"合并学员\",\"displayName\":\"" + displayName + "\",\"originalStudents\":" + studentNames.toString() + "}");
            record.setCreatedAt(java.time.LocalDateTime.now());
            record.setUpdatedAt(java.time.LocalDateTime.now());

            // 保存操作记录
            weeklyInstanceService.saveOrUpdateMergeRule(record);

            logger.info("合并学员操作: displayName={}, studentNames={}, userId={}", 
                       displayName, studentNames, user.getId());

            return ResponseEntity.ok(ApiResponse.success("学员合并成功", ""));
        } catch (Exception e) {
            logger.error("合并学员失败", e);
            return ResponseEntity.status(500).body(ApiResponse.error("合并学员失败: " + e.getMessage()));
        }
    }

    /**
     * 隐藏学员 (临时API)
     */
    @PostMapping("/hide-student")
    public ResponseEntity<ApiResponse<String>> hideStudent(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }

            String studentName = (String) request.get("studentName");
            if (studentName == null || studentName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("学员名称不能为空"));
            }

            studentName = studentName.trim();
            
            // 获取教练ID，优先使用前端传递的coachId
            Long coachId = null;
            Object coachIdObj = request.get("coachId");
            if (coachIdObj != null) {
                if (coachIdObj instanceof Number) {
                    coachId = ((Number) coachIdObj).longValue();
                } else if (coachIdObj instanceof String) {
                    try {
                        coachId = Long.parseLong((String) coachIdObj);
                    } catch (NumberFormatException e) {
                        // 忽略解析错误，使用默认逻辑
                    }
                }
            }
            
            // 如果前端没有传递coachId，则查找学员所属的教练ID
            if (coachId == null) {
                coachId = weeklyInstanceService.findCoachIdByStudentName(studentName);
                if (coachId == null) {
                    coachId = user.getId(); // 如果找不到，使用当前用户ID
                }
            }

            // 创建隐藏操作记录
            StudentOperationRecord record = new StudentOperationRecord();
            record.setCoachId(coachId);
            record.setOperationType("HIDE");
            record.setOldName(studentName);
            record.setNewName(null); // 隐藏操作没有新名称
            record.setDetails("{\"operationType\":\"HIDE_STUDENT\",\"description\":\"隐藏学员\"}");
            record.setCreatedAt(java.time.LocalDateTime.now());
            record.setUpdatedAt(java.time.LocalDateTime.now());

            // 保存操作记录
            weeklyInstanceService.saveOrUpdateHideRule(record);

            logger.info("隐藏学员操作: studentName={}, coachId={}, userId={}", 
                       studentName, coachId, user.getId());

            return ResponseEntity.ok(ApiResponse.success("学员隐藏成功", ""));
        } catch (Exception e) {
            logger.error("隐藏学员失败", e);
            return ResponseEntity.status(500).body(ApiResponse.error("隐藏学员失败: " + e.getMessage()));
        }
    }

    /**
     * 分配课时（大课功能）- 创建分配课时规则
     */
    @PostMapping("/assign-hours")
    public ResponseEntity<ApiResponse<String>> assignHours(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }

            String className = (String) request.get("className");
            List<String> studentNames = (List<String>) request.get("studentNames");
            String date = (String) request.get("date");
            String timeRange = (String) request.get("timeRange");

            if (className == null || className.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("课程名称不能为空"));
            }

            if (studentNames == null || studentNames.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("学员列表不能为空"));
            }

            if (timeRange == null || timeRange.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("上课时间不能为空"));
            }

            // 为每个学员创建一条分配课时规则记录
            int successCount = 0;
            for (String studentName : studentNames) {
                try {
                    StudentOperationRecord record = new StudentOperationRecord();
                    record.setCoachId(user.getId());
                    record.setOperationType("ASSIGN_HOURS");
                    record.setOldName(studentName); // 学员名称
                    record.setNewName(className); // 大课名称
                    record.setDetails(String.format("{\"date\":\"%s\",\"timeRange\":\"%s\"}", 
                        date != null ? date : "", timeRange));
                    record.setCreatedAt(java.time.LocalDateTime.now());
                    record.setUpdatedAt(java.time.LocalDateTime.now());
                    
                    studentOperationRecordRepository.save(record);
                    successCount++;
                } catch (Exception e) {
                    logger.error("为学员 {} 创建分配课时记录失败: {}", studentName, e.getMessage());
                }
            }

            logger.info("分配课时操作: className={}, studentCount={}, successCount={}, userId={}", 
                       className, studentNames.size(), successCount, user.getId());

            return ResponseEntity.ok(ApiResponse.success(
                String.format("成功为 %d/%d 个学员分配课时", successCount, studentNames.size()), ""));
        } catch (Exception e) {
            logger.error("分配课时失败", e);
            return ResponseEntity.status(500).body(ApiResponse.error("分配课时失败: " + e.getMessage()));
        }
    }

    @PostMapping("/rename-student")
    public ResponseEntity<ApiResponse<String>> renameStudent(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }
            
            String oldName = (String) request.get("oldName");
            String newName = (String) request.get("newName");
            
            if (oldName == null || newName == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("学员名称不能为空"));
            }
            
            // 查找该学员所属的教练ID
            Long targetCoachId = null;
            
            // 从请求中获取coachId（如果前端传了）
            if (request.containsKey("coachId")) {
                targetCoachId = Long.valueOf(request.get("coachId").toString());
                logger.info("从请求中获取教练ID: {}", targetCoachId);
            } else {
                // 如果前端没传coachId，则查找学员所属的教练
                targetCoachId = weeklyInstanceService.findCoachIdByStudentName(oldName.trim());
                logger.info("通过学员名称查找教练ID: {} -> {}", oldName.trim(), targetCoachId);
            }
            
            // 如果找不到教练ID，使用当前登录用户的ID
            if (targetCoachId == null) {
                targetCoachId = user.getId();
                logger.warn("未找到学员所属教练，使用当前用户ID: {}", targetCoachId);
            }
            
            logger.info("保存重命名规则 - 用户名: {}, 教练ID: {}, 原名: '{}', 新名: '{}'", 
                authentication.getName(), targetCoachId, oldName.trim(), newName.trim());
            
            com.timetable.entity.StudentOperationRecord record = new com.timetable.entity.StudentOperationRecord();
            record.setCoachId(targetCoachId);
            record.setOperationType("RENAME");
            record.setOldName(oldName.trim());
            record.setNewName(newName.trim());
            record.setDetails("{\"operationType\":\"RENAME_RULE\",\"description\":\"重命名规则\"}");
            record.setCreatedAt(java.time.LocalDateTime.now());
            record.setUpdatedAt(java.time.LocalDateTime.now());
            
            logger.info("准备保存重命名规则记录: coachId={}, operationType={}, oldName='{}', newName='{}'",
                record.getCoachId(), record.getOperationType(), record.getOldName(), record.getNewName());
            
            weeklyInstanceService.saveOrUpdateRenameRule(record);
            
            logger.info("重命名规则保存完成");
            
            return ResponseEntity.ok(ApiResponse.success("重命名规则创建成功", "OK"));
        } catch (Exception e) {
            logger.error("创建重命名规则失败", e);
            return ResponseEntity.status(500).body(ApiResponse.error("创建重命名规则失败: " + e.getMessage()));
        }
    }
    
    /**
     * 检查数据库中的重命名规则
     */
    @GetMapping("/check-rename-rules/{coachId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkRenameRules(
            @PathVariable Long coachId) {
        try {
            List<com.timetable.entity.StudentOperationRecord> records = 
                weeklyInstanceService.getOperationRecordsByCoachId(coachId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("coachId", coachId);
            result.put("totalRecords", records.size());
            
            List<Map<String, Object>> renameRules = new ArrayList<>();
            for (com.timetable.entity.StudentOperationRecord record : records) {
                if ("RENAME".equals(record.getOperationType())) {
                    Map<String, Object> rule = new HashMap<>();
                    rule.put("id", record.getId());
                    rule.put("coachId", record.getCoachId());  // 添加教练ID
                    rule.put("oldName", record.getOldName());
                    rule.put("newName", record.getNewName());
                    rule.put("createdAt", record.getCreatedAt());
                    renameRules.add(rule);
                }
            }
            result.put("renameRules", renameRules);
            
            return ResponseEntity.ok(ApiResponse.success("获取重命名规则成功", result));
        } catch (Exception e) {
            logger.error("检查重命名规则失败", e);
            return ResponseEntity.status(500).body(ApiResponse.error("检查重命名规则失败: " + e.getMessage()));
        }
    }
    
    /**
     * 测试重命名规则
     */
    @GetMapping("/test-rename-rules/{coachId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testRenameRules(
            @PathVariable Long coachId,
            Authentication authentication) {
        
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }

        // 只有管理员可以执行测试操作
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            return ResponseEntity.status(403).body(ApiResponse.error("权限不足，只有管理员可以执行测试操作"));
        }

        try {
            Map<String, Object> result = weeklyInstanceService.testRenameRules(coachId);
            return ResponseEntity.ok(ApiResponse.success("测试重命名规则完成", result));
        } catch (Exception e) {
            logger.error("测试重命名规则失败", e);
            return ResponseEntity.status(500).body(ApiResponse.error("测试失败: " + e.getMessage()));
        }
    }

    /**
     * 公开的测试重命名规则（不需要管理员权限）
     */
    @GetMapping("/debug-rename-rules/{coachId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> debugRenameRules(
            @PathVariable Long coachId,
            Authentication authentication) {
        
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }

        try {
            Map<String, Object> result = weeklyInstanceService.testRenameRules(coachId);
            return ResponseEntity.ok(ApiResponse.success("调试重命名规则完成", result));
        } catch (Exception e) {
            logger.error("调试重命名规则失败", e);
            return ResponseEntity.status(500).body(ApiResponse.error("调试失败: " + e.getMessage()));
        }
    }

    /**
     * 强制清除缓存并重新获取学员列表
     */
    @GetMapping("/students/force-refresh")
    public ResponseEntity<ApiResponse<?>> forceRefreshStudents(
            @RequestParam(defaultValue = "false") Boolean showAll,
            Authentication authentication) {
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }
        try {
            // 强制刷新，不使用任何缓存
            if ("ADMIN".equalsIgnoreCase(user.getRole()) && showAll) {
                List<com.timetable.dto.CoachStudentSummaryDTO> grouped = weeklyInstanceService.getStudentGroupByCoachSummaryAll();
                return ResponseEntity.ok(ApiResponse.success("强制刷新学员列表成功", grouped));
            } else {
                List<com.timetable.dto.StudentSummaryDTO> students =
                        weeklyInstanceService.getStudentSummariesByCoach(user.getId());
                return ResponseEntity.ok(ApiResponse.success("强制刷新学员列表成功", students));
            }
        } catch (Exception e) {
            logger.error("强制刷新学员列表失败", e);
            return ResponseEntity.status(500).body(ApiResponse.error("强制刷新学员列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取被隐藏的学员列表
     */
    private List<String> getHiddenStudents() {
        try {
            List<StudentOperationRecord> hideRecords = studentOperationRecordRepository.findAll()
                .stream()
                .filter(record -> "HIDE".equals(record.getOperationType()))
                .collect(java.util.stream.Collectors.toList());
            
            return hideRecords.stream()
                .map(StudentOperationRecord::getOldName)
                .filter(name -> name != null && !name.trim().isEmpty())
                .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            logger.error("获取隐藏学员列表失败", e);
            return new java.util.ArrayList<>();
        }
    }
    
    /**
     * 获取合并规则映射
     */
    private java.util.Map<String, String> getMergeRules() {
        try {
            List<StudentOperationRecord> mergeRecords = studentOperationRecordRepository.findAll()
                .stream()
                .filter(record -> "MERGE".equals(record.getOperationType()))
                .collect(java.util.stream.Collectors.toList());
            
            java.util.Map<String, String> mergeMap = new java.util.HashMap<>();
            for (StudentOperationRecord record : mergeRecords) {
                String oldNames = record.getOldName();
                String newName = record.getNewName();
                if (oldNames != null && newName != null) {
                    // 处理逗号分隔的多个学员名称
                    String[] names = oldNames.split(",");
                    for (String name : names) {
                        if (name != null && !name.trim().isEmpty()) {
                            mergeMap.put(name.trim(), newName.trim());
                        }
                    }
                }
            }
            return mergeMap;
        } catch (Exception e) {
            logger.error("获取合并规则失败", e);
            return new java.util.HashMap<>();
        }
    }
    
    /**
     * 获取分配课时规则
     * @return Map<学员名称, 分配的课时数>
     */
    private java.util.Map<String, Integer> getAssignHoursRules() {
        try {
            java.util.Map<String, Integer> hoursMap = new java.util.HashMap<>();
            List<StudentOperationRecord> records = studentOperationRecordRepository
                .findByOperationType("ASSIGN_HOURS");
            
            for (StudentOperationRecord record : records) {
                String studentName = record.getOldName(); // 学员名称存在oldName
                if (studentName != null && !studentName.trim().isEmpty()) {
                    hoursMap.put(studentName.trim(), hoursMap.getOrDefault(studentName.trim(), 0) + 1);
                }
            }
            return hoursMap;
        } catch (Exception e) {
            logger.error("获取分配课时规则失败", e);
            return new java.util.HashMap<>();
        }
    }
    
    /**
     * 应用分配课时规则到分组列表
     */
    private List<com.timetable.dto.CoachStudentSummaryDTO> applyAssignHoursToGrouped(
            List<com.timetable.dto.CoachStudentSummaryDTO> grouped,
            java.util.Map<String, Integer> hoursMap) {
        
        return grouped.stream()
            .map(coach -> {
                List<com.timetable.dto.StudentSummaryDTO> updatedStudents = coach.getStudents()
                    .stream()
                    .map(student -> {
                        String name = student.getStudentName();
                        if (hoursMap.containsKey(name)) {
                            com.timetable.dto.StudentSummaryDTO updated = new com.timetable.dto.StudentSummaryDTO();
                            updated.setStudentName(name);
                            updated.setAttendedCount(student.getAttendedCount() + hoursMap.get(name));
                            return updated;
                        }
                        return student;
                    })
                    .collect(java.util.stream.Collectors.toList());
                
                // 重新计算总课时
                int newTotalCount = updatedStudents.stream()
                    .mapToInt(com.timetable.dto.StudentSummaryDTO::getAttendedCount)
                    .sum();
                
                com.timetable.dto.CoachStudentSummaryDTO newCoach = new com.timetable.dto.CoachStudentSummaryDTO();
                newCoach.setCoachId(coach.getCoachId());
                newCoach.setCoachName(coach.getCoachName());
                newCoach.setTotalCount(newTotalCount);
                newCoach.setStudents(updatedStudents);
                
                return newCoach;
            })
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 应用分配课时规则到学员列表
     */
    private List<com.timetable.dto.StudentSummaryDTO> applyAssignHoursToList(
            List<com.timetable.dto.StudentSummaryDTO> students,
            java.util.Map<String, Integer> hoursMap) {
        
        return students.stream()
            .map(student -> {
                String name = student.getStudentName();
                if (hoursMap.containsKey(name)) {
                    com.timetable.dto.StudentSummaryDTO updated = new com.timetable.dto.StudentSummaryDTO();
                    updated.setStudentName(name);
                    updated.setAttendedCount(student.getAttendedCount() + hoursMap.get(name));
                    return updated;
                }
                return student;
            })
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 从分组列表中过滤掉被隐藏的学员
     */
    private List<com.timetable.dto.CoachStudentSummaryDTO> filterHiddenStudentsFromGrouped(
            List<com.timetable.dto.CoachStudentSummaryDTO> grouped, 
            List<String> hiddenStudents) {
        
        return grouped.stream()
            .map(coach -> {
                // 过滤掉被隐藏的学员
                List<com.timetable.dto.StudentSummaryDTO> filteredStudents = coach.getStudents()
                    .stream()
                    .filter(student -> !hiddenStudents.contains(student.getStudentName()))
                    .collect(java.util.stream.Collectors.toList());
                
                // 重新计算总课时
                int newTotalCount = filteredStudents.stream()
                    .mapToInt(com.timetable.dto.StudentSummaryDTO::getAttendedCount)
                    .sum();
                
                // 创建新的教练对象
                com.timetable.dto.CoachStudentSummaryDTO newCoach = new com.timetable.dto.CoachStudentSummaryDTO();
                newCoach.setCoachId(coach.getCoachId());
                newCoach.setCoachName(coach.getCoachName());
                newCoach.setTotalCount(newTotalCount);
                newCoach.setStudents(filteredStudents);
                
                return newCoach;
            })
            .filter(coach -> coach.getStudents() != null && !coach.getStudents().isEmpty())
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 从学员列表中过滤掉被隐藏的学员
     */
    private List<com.timetable.dto.StudentSummaryDTO> filterHiddenStudentsFromList(
            List<com.timetable.dto.StudentSummaryDTO> students, 
            List<String> hiddenStudents) {
        
        return students.stream()
            .filter(student -> !hiddenStudents.contains(student.getStudentName()))
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 应用合并规则到分组学员列表
     */
    private List<com.timetable.dto.CoachStudentSummaryDTO> applyMergeRulesToGrouped(
            List<com.timetable.dto.CoachStudentSummaryDTO> grouped, 
            java.util.Map<String, String> mergeRules) {
        
        return grouped.stream()
            .map(coach -> {
                // 应用合并规则到学员列表
                java.util.Map<String, com.timetable.dto.StudentSummaryDTO> mergedStudents = new java.util.HashMap<>();
                
                for (com.timetable.dto.StudentSummaryDTO student : coach.getStudents()) {
                    String originalName = student.getStudentName();
                    String mergedName = mergeRules.getOrDefault(originalName, originalName);
                    
                    if (mergedStudents.containsKey(mergedName)) {
                        // 如果已存在合并后的学员，累加课时
                        com.timetable.dto.StudentSummaryDTO existing = mergedStudents.get(mergedName);
                        existing.setAttendedCount(existing.getAttendedCount() + student.getAttendedCount());
                    } else {
                        // 创建新的学员记录
                        com.timetable.dto.StudentSummaryDTO mergedStudent = new com.timetable.dto.StudentSummaryDTO();
                        mergedStudent.setStudentName(mergedName);
                        mergedStudent.setAttendedCount(student.getAttendedCount());
                        mergedStudent.setCoachId(student.getCoachId());
                        mergedStudents.put(mergedName, mergedStudent);
                    }
                }
                
                // 转换为列表并重新计算总课时
                List<com.timetable.dto.StudentSummaryDTO> studentList = new java.util.ArrayList<>(mergedStudents.values());
                int newTotalCount = studentList.stream()
                    .mapToInt(com.timetable.dto.StudentSummaryDTO::getAttendedCount)
                    .sum();
                
                // 创建新的教练对象
                com.timetable.dto.CoachStudentSummaryDTO newCoach = new com.timetable.dto.CoachStudentSummaryDTO();
                newCoach.setCoachId(coach.getCoachId());
                newCoach.setCoachName(coach.getCoachName());
                newCoach.setTotalCount(newTotalCount);
                newCoach.setStudents(studentList);
                
                return newCoach;
            })
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 应用合并规则到学员列表
     */
    private List<com.timetable.dto.StudentSummaryDTO> applyMergeRulesToList(
            List<com.timetable.dto.StudentSummaryDTO> students, 
            java.util.Map<String, String> mergeRules) {
        
        java.util.Map<String, com.timetable.dto.StudentSummaryDTO> mergedStudents = new java.util.HashMap<>();
        
        for (com.timetable.dto.StudentSummaryDTO student : students) {
            String originalName = student.getStudentName();
            String mergedName = mergeRules.getOrDefault(originalName, originalName);
            
            if (mergedStudents.containsKey(mergedName)) {
                // 如果已存在合并后的学员，累加课时
                com.timetable.dto.StudentSummaryDTO existing = mergedStudents.get(mergedName);
                existing.setAttendedCount(existing.getAttendedCount() + student.getAttendedCount());
            } else {
                // 创建新的学员记录
                com.timetable.dto.StudentSummaryDTO mergedStudent = new com.timetable.dto.StudentSummaryDTO();
                mergedStudent.setStudentName(mergedName);
                mergedStudent.setAttendedCount(student.getAttendedCount());
                mergedStudent.setCoachId(student.getCoachId());
                mergedStudents.put(mergedName, mergedStudent);
            }
        }
        
        return new java.util.ArrayList<>(mergedStudents.values());
    }
}
