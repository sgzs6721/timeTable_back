package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.CustomerStatusChangeRequest;
import com.timetable.dto.CustomerStatusHistoryDTO;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.service.CustomerStatusHistoryService;
import com.timetable.service.UserService;
import com.timetable.service.ScheduleService;
import com.timetable.service.WeeklyInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/customers/{customerId}/status-history")
@CrossOrigin(origins = "*")
public class CustomerStatusHistoryController {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomerStatusHistoryController.class);

    @Autowired
    private CustomerStatusHistoryService historyService;

    @Autowired
    private UserService userService;
    
    @Autowired
    private ScheduleService scheduleService;
    
    @Autowired
    private WeeklyInstanceService weeklyInstanceService;

    @PostMapping("/change")
    public ResponseEntity<ApiResponse<CustomerStatusHistoryDTO>> changeStatus(
            @PathVariable Long customerId,
            @Valid @RequestBody CustomerStatusChangeRequest request,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }

            CustomerStatusHistoryDTO history = historyService.changeStatus(customerId, request, user.getId());
            return ResponseEntity.ok(ApiResponse.success("状态变更成功", history));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("状态变更失败: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerStatusHistoryDTO>>> getHistory(
            @PathVariable Long customerId,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }

            List<CustomerStatusHistoryDTO> histories = historyService.getHistoryByCustomerId(customerId, user.getId());
            return ResponseEntity.ok(ApiResponse.success("获取成功", histories));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("获取历史记录失败: " + e.getMessage()));
        }
    }
    
    /**
     * 取消体验课程
     */
    @PostMapping("/{historyId}/cancel-trial")
    public ResponseEntity<ApiResponse<String>> cancelTrial(
            @PathVariable Long customerId,
            @PathVariable Long historyId,
            @RequestParam(required = false) String studentName,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }
            
            // 1. 标记历史记录为已取消
            boolean marked = historyService.markTrialAsCancelled(historyId);
            if (!marked) {
                return ResponseEntity.badRequest().body(ApiResponse.error("标记取消失败，未找到该历史记录"));
            }
            
            // 2. 如果提供了学员姓名，尝试删除课表中的体验课程（需要有课表权限的用户才能成功删除）
            if (studentName != null && !studentName.isEmpty()) {
                try {
                    // 查询体验课程
                    Map<String, Object> trialSchedule = scheduleService.findTrialScheduleByStudentName(studentName);
                    if (trialSchedule != null) {
                        Object sourceType = trialSchedule.get("sourceType");
                        Object scheduleId = trialSchedule.get("id");
                        Object timetableId = trialSchedule.get("timetableId");
                        
                        if ("weekly_instance".equals(sourceType) && scheduleId != null) {
                            // 删除周实例课程
                            weeklyInstanceService.deleteInstanceSchedule(((Number) scheduleId).longValue());
                            logger.info("已删除周实例体验课程: scheduleId={}", scheduleId);
                        } else if ("schedule".equals(sourceType) && scheduleId != null && timetableId != null) {
                            // 删除普通课程
                            scheduleService.deleteSchedule(((Number) timetableId).longValue(), ((Number) scheduleId).longValue());
                            logger.info("已删除体验课程: timetableId={}, scheduleId={}", timetableId, scheduleId);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("删除课表中的体验课程失败（可能无权限或课程不存在）: {}", e.getMessage());
                    // 删除课表失败不影响标记取消的操作
                }
            }
            
            return ResponseEntity.ok(ApiResponse.success("体验课程已取消", "取消成功"));
        } catch (Exception e) {
            logger.error("取消体验课程失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("取消体验课程失败: " + e.getMessage()));
        }
    }
}

