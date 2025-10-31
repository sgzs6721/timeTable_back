package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.CustomerStatusChangeRequest;
import com.timetable.dto.CustomerStatusHistoryDTO;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.service.CustomerStatusHistoryService;
import com.timetable.service.UserService;
import com.timetable.service.ScheduleService;
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
     * 取消体验课程（事务：标记取消 + 删除课表）
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
            
            // 如果提供了学员姓名，查询课表中的体验课程信息
            Long trialScheduleId = null;
            Long trialTimetableId = null;
            String sourceType = null;
            
            if (studentName != null && !studentName.isEmpty()) {
                try {
                    Map<String, Object> trialSchedule = scheduleService.findTrialScheduleByStudentName(studentName);
                    if (trialSchedule != null) {
                        sourceType = (String) trialSchedule.get("sourceType");
                        Object scheduleIdObj = trialSchedule.get("id");
                        Object timetableIdObj = trialSchedule.get("timetableId");
                        
                        if (scheduleIdObj != null) {
                            trialScheduleId = ((Number) scheduleIdObj).longValue();
                        }
                        if (timetableIdObj != null) {
                            trialTimetableId = ((Number) timetableIdObj).longValue();
                        }
                        
                        logger.info("查询到体验课程: scheduleId={}, timetableId={}, sourceType={}", 
                                   trialScheduleId, trialTimetableId, sourceType);
                    }
                } catch (Exception e) {
                    logger.warn("查询课表中的体验课程失败: {}", e.getMessage());
                    // 查询失败，只标记取消，不删除课表
                }
            }
            
            // 调用事务方法：标记取消 + 删除课表（如果有权限）
            boolean success = historyService.cancelTrialScheduleWithTransaction(
                historyId, 
                trialScheduleId, 
                trialTimetableId, 
                sourceType
            );
            
            if (success) {
                logger.info("体验课程取消成功: historyId={}, scheduleId={}", historyId, trialScheduleId);
                return ResponseEntity.ok(ApiResponse.success("体验课程已取消", "取消成功"));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error("取消失败"));
            }
            
        } catch (Exception e) {
            logger.error("取消体验课程失败: historyId={}", historyId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("取消体验课程失败: " + e.getMessage()));
        }
    }
}

