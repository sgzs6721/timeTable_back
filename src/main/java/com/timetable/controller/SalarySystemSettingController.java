package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.entity.SalarySystemSetting;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.service.SalarySystemSettingService;
import com.timetable.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/salary-system-settings")
public class SalarySystemSettingController {

    @Autowired
    private SalarySystemSettingService salarySystemSettingService;

    @Autowired
    private UserService userService;

    /**
     * 获取当前工资系统设置（仅管理员）
     */
    @GetMapping
    public ResponseEntity<ApiResponse<SalarySystemSetting>> getCurrentSetting(
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }

            if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
                return ResponseEntity.status(403).body(ApiResponse.error("无权限访问"));
            }

            SalarySystemSetting setting = salarySystemSettingService.getCurrentSetting();
            return ResponseEntity.ok(ApiResponse.success("获取工资系统设置成功", setting));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("获取工资系统设置失败: " + e.getMessage()));
        }
    }

    /**
     * 保存或更新工资系统设置（仅管理员）
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SalarySystemSetting>> saveOrUpdateSetting(
            @RequestBody SalarySystemSetting setting,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }

            if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
                return ResponseEntity.status(403).body(ApiResponse.error("无权限操作"));
            }

            SalarySystemSetting savedSetting = salarySystemSettingService.saveOrUpdate(setting);
            return ResponseEntity.ok(ApiResponse.success("保存工资系统设置成功", savedSetting));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("保存工资系统设置失败: " + e.getMessage()));
        }
    }
}
