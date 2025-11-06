package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.SalaryCalculationDTO;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.service.SalaryCalculationService;
import com.timetable.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/salary-calculations")
public class SalaryCalculationController {

    @Autowired
    private SalaryCalculationService salaryCalculationService;

    @Autowired
    private UserService userService;

    /**
     * 判断用户是否有管理权限（ADMIN角色或MANAGER职位）
     */
    private boolean isManager(Users user) {
        if (user == null) {
            return false;
        }
        return "ADMIN".equalsIgnoreCase(user.getRole()) || "MANAGER".equalsIgnoreCase(user.getPosition());
    }

    /**
     * 获取指定月份的工资计算结果（仅管理员）- 只返回当前机构的数据
     */
    @GetMapping("/{month}")
    public ResponseEntity<ApiResponse<List<SalaryCalculationDTO>>> getSalaryCalculations(
            @PathVariable String month,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (!isManager(user)) {
                return ResponseEntity.status(403).body(ApiResponse.error("无权限访问"));
            }
            
            if (user.getOrganizationId() == null) {
                return ResponseEntity.status(400).body(ApiResponse.error("用户未关联机构"));
            }

            List<SalaryCalculationDTO> allResults = salaryCalculationService.calculateSalary(month);
            // 过滤出当前机构的数据
            List<SalaryCalculationDTO> result = allResults.stream()
                    .filter(dto -> {
                        Users targetUser = userService.findById(dto.getUserId());
                        return targetUser != null && user.getOrganizationId().equals(targetUser.getOrganizationId());
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success("获取工资计算结果成功", result));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("获取工资计算结果失败: " + e.getMessage()));
        }
    }

    /**
     * 获取工资计算结果（管理员获取所有，普通用户获取自己的）- 只返回当前机构的数据
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SalaryCalculationDTO>>> getAllSalaryCalculations(Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.status(403).body(ApiResponse.error("用户未登录"));
            }
            
            if (user.getOrganizationId() == null) {
                return ResponseEntity.status(400).body(ApiResponse.error("用户未关联机构"));
            }

            List<SalaryCalculationDTO> result;
            if (isManager(user)) {
                // 管理员获取所有教练的工资数据（最近6个月），但只返回当前机构的
                List<SalaryCalculationDTO> allResults = salaryCalculationService.getRecentSalaryCalculations(6);
                result = allResults.stream()
                        .filter(dto -> {
                            Users targetUser = userService.findById(dto.getUserId());
                            return targetUser != null && user.getOrganizationId().equals(targetUser.getOrganizationId());
                        })
                        .collect(java.util.stream.Collectors.toList());
            } else {
                // 普通用户只获取自己的工资数据（最近12个月，包含当年所有数据）
                result = salaryCalculationService.getUserSalaryCalculations(user.getId(), 12);
            }
            
            return ResponseEntity.ok(ApiResponse.success("获取工资计算结果成功", result));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("获取工资计算结果失败: " + e.getMessage()));
        }
    }

    /**
     * 获取最近N个月的工资计算结果（仅管理员）- 只返回当前机构的数据
     */
    @GetMapping("/recent/{months}")
    public ResponseEntity<ApiResponse<List<SalaryCalculationDTO>>> getRecentSalaryCalculations(
            @PathVariable int months,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (!isManager(user)) {
                return ResponseEntity.status(403).body(ApiResponse.error("无权限访问"));
            }
            
            if (user.getOrganizationId() == null) {
                return ResponseEntity.status(400).body(ApiResponse.error("用户未关联机构"));
            }

            List<SalaryCalculationDTO> allResults = salaryCalculationService.getRecentSalaryCalculations(months);
            // 过滤出当前机构的数据
            List<SalaryCalculationDTO> result = allResults.stream()
                    .filter(dto -> {
                        Users targetUser = userService.findById(dto.getUserId());
                        return targetUser != null && user.getOrganizationId().equals(targetUser.getOrganizationId());
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success("获取最近工资计算结果成功", result));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("获取最近工资计算结果失败: " + e.getMessage()));
        }
    }

    /**
     * 获取有课时记录的月份列表（管理员获取所有，普通用户获取自己的）
     * 注意：此接口返回的是当前机构所有用户的月份列表，需要按机构过滤
     */
    @GetMapping("/available-months")
    public ResponseEntity<ApiResponse<List<String>>> getAvailableMonths(Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.status(403).body(ApiResponse.error("用户未登录"));
            }
            
            if (user.getOrganizationId() == null) {
                return ResponseEntity.status(400).body(ApiResponse.error("用户未关联机构"));
            }

            List<String> months;
            if (isManager(user)) {
                // 管理员获取当前机构的所有月份
                months = salaryCalculationService.getAvailableMonths(user.getOrganizationId());
            } else {
                // 普通用户只获取自己有工资记录的月份
                months = salaryCalculationService.getUserAvailableMonths(user.getId());
            }
            
            return ResponseEntity.ok(ApiResponse.success("获取可用月份列表成功", months));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("获取可用月份列表失败: " + e.getMessage()));
        }
    }
}