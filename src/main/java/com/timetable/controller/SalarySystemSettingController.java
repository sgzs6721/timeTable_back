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
            @RequestParam(required = false) Long organizationId,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }


            // 确定要使用的机构ID
            Long targetOrganizationId = organizationId != null ? organizationId : user.getOrganizationId();
            if (targetOrganizationId == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("机构ID不能为空"));
            }

            SalarySystemSetting setting = salarySystemSettingService.getSettingByOrganizationId(targetOrganizationId);
            return ResponseEntity.ok(ApiResponse.success("获取工资系统设置成功", setting));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("获取工资系统设置失败: " + e.getMessage()));
        }
    }

    /**
     * 获取当前工资系统设置（所有用户可访问，用于查询记薪周期）
     */
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<SalarySystemSetting>> getCurrentSettingForAll(
            @RequestParam(required = false) Long organizationId,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }

            // 确定要使用的机构ID
            Long targetOrganizationId = organizationId != null ? organizationId : user.getOrganizationId();
            if (targetOrganizationId == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("机构ID不能为空"));
            }

            SalarySystemSetting setting = salarySystemSettingService.getSettingByOrganizationId(targetOrganizationId);
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
            @RequestParam(required = false) Long organizationId,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }


            // 确定要使用的机构ID
            Long targetOrganizationId = organizationId != null ? organizationId : user.getOrganizationId();
            if (targetOrganizationId == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("机构ID不能为空"));
            }

            setting.setOrganizationId(targetOrganizationId);
            SalarySystemSetting savedSetting = salarySystemSettingService.saveOrUpdateByOrganizationId(setting);
            return ResponseEntity.ok(ApiResponse.success("保存工资系统设置成功", savedSetting));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("保存工资系统设置失败: " + e.getMessage()));
        }
    }
}
