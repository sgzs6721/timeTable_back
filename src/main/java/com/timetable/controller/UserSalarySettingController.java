package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.UserSalarySettingDTO;
import com.timetable.entity.UserSalarySetting;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.service.UserSalarySettingService;
import com.timetable.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/salary-settings")
public class UserSalarySettingController {

    @Autowired
    private UserSalarySettingService salarySettingService;

    @Autowired
    private UserService userService;

    /**
     * 获取所有用户的工资设置（仅管理员）
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserSalarySettingDTO>>> getAllUserSalarySettings(
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }

            if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
                return ResponseEntity.status(403).body(ApiResponse.error("无权限访问"));
            }

            List<UserSalarySettingDTO> settings = salarySettingService.getAllUserSalarySettings();
            return ResponseEntity.ok(ApiResponse.success("获取工资设置成功", settings));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("获取工资设置失败: " + e.getMessage()));
        }
    }

    /**
     * 保存或更新用户工资设置（仅管理员）
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UserSalarySetting>> saveOrUpdateSalarySetting(
            @RequestBody UserSalarySetting setting,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }

            if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
                return ResponseEntity.status(403).body(ApiResponse.error("无权限操作"));
            }

            UserSalarySetting savedSetting = salarySettingService.saveOrUpdate(setting);
            return ResponseEntity.ok(ApiResponse.success("保存工资设置成功", savedSetting));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("保存工资设置失败: " + e.getMessage()));
        }
    }

    /**
     * 删除用户工资设置（仅管理员）
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<String>> deleteSalarySetting(
            @PathVariable Long userId,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }

            if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
                return ResponseEntity.status(403).body(ApiResponse.error("无权限操作"));
            }

            salarySettingService.deleteByUserId(userId);
            return ResponseEntity.ok(ApiResponse.success("删除工资设置成功", null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("删除工资设置失败: " + e.getMessage()));
        }
    }
}

