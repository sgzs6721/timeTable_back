package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.WeeklyInstanceDTO;
import com.timetable.entity.WeeklyInstance;
import com.timetable.entity.WeeklyInstanceSchedule;
import com.timetable.service.WeeklyInstanceService;
import com.timetable.service.TimetableService;
import com.timetable.service.UserService;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.generated.tables.pojos.Timetables;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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

    @Autowired
    private WeeklyInstanceService weeklyInstanceService;

    @Autowired
    private TimetableService timetableService;

    @Autowired
    private UserService userService;

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
        WeeklyInstance instance = weeklyInstanceService.switchToWeekInstance(instanceId);
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

        List<WeeklyInstanceSchedule> schedules = weeklyInstanceService.getCurrentWeekInstanceSchedules(instance.getTemplateTimetableId());
        return ResponseEntity.ok(ApiResponse.success("获取实例课程成功", schedules));
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
}
