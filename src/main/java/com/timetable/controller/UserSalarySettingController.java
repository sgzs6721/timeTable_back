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
            @RequestParam(required = false) Long organizationId,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }

            if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
                return ResponseEntity.status(403).body(ApiResponse.error("无权限访问"));
            }

            // 确定要使用的机构ID
            Long targetOrganizationId = organizationId != null ? organizationId : user.getOrganizationId();
            if (targetOrganizationId == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("机构ID不能为空"));
            }

            List<UserSalarySettingDTO> settings = salarySettingService.getUserSalarySettingsByOrganizationId(targetOrganizationId);
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
            @RequestBody java.util.Map<String, Object> requestData,
            @RequestParam(required = false) Long organizationId,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }

            if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
                return ResponseEntity.status(403).body(ApiResponse.error("无权限操作"));
            }

            // 确定要使用的机构ID
            Long targetOrganizationId = organizationId != null ? organizationId : user.getOrganizationId();
            if (targetOrganizationId == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("机构ID不能为空"));
            }

            // 手动构建UserSalarySetting对象，确保数据类型转换正确
            UserSalarySetting setting = new UserSalarySetting();
            
            // 处理userId
            Object userIdObj = requestData.get("userId");
            if (userIdObj == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户ID不能为空"));
            }
            setting.setUserId(Long.valueOf(userIdObj.toString()));
            setting.setOrganizationId(targetOrganizationId);
            
            // 处理工资字段，确保BigDecimal转换正确
            setting.setBaseSalary(convertToBigDecimal(requestData.get("baseSalary")));
            setting.setSocialSecurity(convertToBigDecimal(requestData.get("socialSecurity")));
            setting.setHourlyRate(convertToBigDecimal(requestData.get("hourlyRate")));
            setting.setCommissionRate(convertToBigDecimal(requestData.get("commissionRate")));

            UserSalarySetting savedSetting = salarySettingService.saveOrUpdate(setting);
            return ResponseEntity.ok(ApiResponse.success("保存工资设置成功", savedSetting));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("保存工资设置失败: " + e.getMessage()));
        }
    }
    
    private java.math.BigDecimal convertToBigDecimal(Object value) {
        if (value == null) {
            return java.math.BigDecimal.ZERO;
        }
        try {
            return new java.math.BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return java.math.BigDecimal.ZERO;
        }
    }

    /**
     * 删除用户工资设置（仅管理员）
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<String>> deleteSalarySetting(
            @PathVariable Long userId,
            @RequestParam(required = false) Long organizationId,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }

            if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
                return ResponseEntity.status(403).body(ApiResponse.error("无权限操作"));
            }

            // 确定要使用的机构ID
            Long targetOrganizationId = organizationId != null ? organizationId : user.getOrganizationId();
            if (targetOrganizationId == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("机构ID不能为空"));
            }

            salarySettingService.deleteByUserIdAndOrganizationId(userId, targetOrganizationId);
            return ResponseEntity.ok(ApiResponse.success("删除工资设置成功", null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("删除工资设置失败: " + e.getMessage()));
        }
    }
}

