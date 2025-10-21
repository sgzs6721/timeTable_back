package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.CustomerStatusChangeRequest;
import com.timetable.dto.CustomerStatusHistoryDTO;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.service.CustomerStatusHistoryService;
import com.timetable.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/customers/{customerId}/status-history")
@CrossOrigin(origins = "*")
public class CustomerStatusHistoryController {

    @Autowired
    private CustomerStatusHistoryService historyService;

    @Autowired
    private UserService userService;

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
}

