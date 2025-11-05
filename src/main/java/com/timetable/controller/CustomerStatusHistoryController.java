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
     * 直接从历史记录读取课表ID，不需要通过学员名字查询
     */
    @PostMapping("/{historyId}/cancel-trial")
    public ResponseEntity<ApiResponse<String>> cancelTrial(
            @PathVariable Long customerId,
            @PathVariable Long historyId,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }
            
            // 调用事务方法：标记取消 + 删除课表
            // 课表ID直接从历史记录中读取
            boolean success = historyService.cancelTrialScheduleWithTransaction(historyId);
            
            if (success) {
                logger.info("体验课程取消成功: historyId={}", historyId);
                return ResponseEntity.ok(ApiResponse.success("体验课程已取消", "取消成功"));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error("取消失败"));
            }
            
        } catch (Exception e) {
            logger.error("取消体验课程失败: historyId={}", historyId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("取消体验课程失败: " + e.getMessage()));
        }
    }
    
    /**
     * 标记体验课程为已完成
     */
    @PostMapping("/{historyId}/complete-trial")
    public ResponseEntity<ApiResponse<String>> completeTrial(
            @PathVariable Long customerId,
            @PathVariable Long historyId,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }
            
            boolean success = historyService.markTrialAsCompleted(historyId);
            
            if (success) {
                logger.info("体验课程标记完成成功: historyId={}", historyId);
                return ResponseEntity.ok(ApiResponse.success("体验课程已标记完成", "标记成功"));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error("标记失败"));
            }
            
        } catch (Exception e) {
            logger.error("标记体验课程完成失败: historyId={}", historyId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("标记体验课程完成失败: " + e.getMessage()));
        }
    }
}

