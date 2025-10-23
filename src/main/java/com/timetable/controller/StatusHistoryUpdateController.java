package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.CustomerStatusHistoryDTO;
import com.timetable.dto.UpdateCustomerStatusHistoryRequest;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.service.CustomerStatusHistoryService;
import com.timetable.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/customers/status-history")
@CrossOrigin(origins = "*")
public class StatusHistoryUpdateController {

    @Autowired
    private CustomerStatusHistoryService historyService;

    @Autowired
    private UserService userService;

    @PutMapping("/{historyId}")
    public ResponseEntity<ApiResponse<CustomerStatusHistoryDTO>> updateHistory(
            @PathVariable Long historyId,
            @Valid @RequestBody UpdateCustomerStatusHistoryRequest request,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }

            CustomerStatusHistoryDTO history = historyService.updateHistory(historyId, request.getNotes(), user.getId());
            return ResponseEntity.ok(ApiResponse.success("更新成功", history));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("更新失败: " + e.getMessage()));
        }
    }
}

