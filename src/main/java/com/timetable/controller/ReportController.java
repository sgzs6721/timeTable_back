package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.service.ReportService;
import com.timetable.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/reports")
@Validated
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private UserService userService;

    @GetMapping("/my-hours")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyHours(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long coachId,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam int page,
            @RequestParam int size,
            org.springframework.security.core.Authentication authentication) {

        if (page <= 0 || size <= 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("page/size 必须为正整数"));
        }

        Users current = userService.findByUsername(authentication.getName());
        if (current == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }

        // 非管理员强制使用自己的ID
        Long targetUserId;
        if (current.getRole() != null && current.getRole().equalsIgnoreCase("ADMIN")) {
            targetUserId = coachId != null ? coachId : current.getId();
        } else {
            targetUserId = current.getId();
        }

        LocalDate start = startDate != null && !startDate.isEmpty() ? LocalDate.parse(startDate) : null;
        LocalDate end = endDate != null && !endDate.isEmpty() ? LocalDate.parse(endDate) : null;

        // 获取当前用户的机构ID，用于过滤课时记录
        Long organizationId = current.getOrganizationId();

        Map<String, Object> data = reportService.queryHoursPaged(targetUserId, organizationId, start, end, page, size, sortOrder);
        return ResponseEntity.ok(ApiResponse.success("获取课时记录成功", data));
    }
}


