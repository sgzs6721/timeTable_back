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
     * 获取指定月份的工资计算结果（仅管理员）
     */
    @GetMapping("/{month}")
    public ResponseEntity<ApiResponse<List<SalaryCalculationDTO>>> getSalaryCalculations(
            @PathVariable String month,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }

            if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
                return ResponseEntity.status(403).body(ApiResponse.error("无权限访问"));
            }

            List<SalaryCalculationDTO> calculations = salaryCalculationService.calculateSalary(month);
            return ResponseEntity.ok(ApiResponse.success("获取工资计算结果成功", calculations));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("获取工资计算结果失败: " + e.getMessage()));
        }
    }

    /**
     * 获取最近几个月的工资计算结果（仅管理员）
     */
    @GetMapping("/recent/{months}")
    public ResponseEntity<ApiResponse<List<SalaryCalculationDTO>>> getRecentSalaryCalculations(
            @PathVariable int months,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }

            if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
                return ResponseEntity.status(403).body(ApiResponse.error("无权限访问"));
            }

            List<SalaryCalculationDTO> calculations = salaryCalculationService.getRecentSalaryCalculations(months);
            return ResponseEntity.ok(ApiResponse.success("获取最近工资计算结果成功", calculations));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("获取最近工资计算结果失败: " + e.getMessage()));
        }
    }

    /**
     * 获取所有工资计算结果（仅管理员）
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SalaryCalculationDTO>>> getAllSalaryCalculations(
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }

            if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
                return ResponseEntity.status(403).body(ApiResponse.error("无权限访问"));
            }

            // 获取最近3个月的数据
            List<SalaryCalculationDTO> calculations = salaryCalculationService.getRecentSalaryCalculations(3);
            return ResponseEntity.ok(ApiResponse.success("获取所有工资计算结果成功", calculations));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("获取所有工资计算结果失败: " + e.getMessage()));
        }
    }
}
