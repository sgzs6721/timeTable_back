package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.RolePermissionDTO;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.service.RolePermissionService;
import com.timetable.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色权限Controller
 */
@RestController
@RequestMapping("/api/role-permissions")
public class RolePermissionController {
    
    private static final Logger logger = LoggerFactory.getLogger(RolePermissionController.class);
    
    @Autowired
    private RolePermissionService rolePermissionService;
    
    @Autowired
    private UserService userService;

    /**
     * 获取当前用户的权限配置
     */
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<RolePermissionDTO>> getCurrentUserPermissions(Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.ok(ApiResponse.error("用户不存在"));
            }
            
            // 获取用户所属机构和职位/角色的权限配置
            Long organizationId = user.getOrganizationId();
            String permissionRole = "ADMIN".equalsIgnoreCase(user.getRole()) ? user.getRole() : user.getPosition();
            
            if (organizationId == null) {
                return ResponseEntity.ok(ApiResponse.error("用户未分配机构"));
            }
            
            if (permissionRole == null) {
                return ResponseEntity.ok(ApiResponse.error("用户未分配职位"));
            }
            
            RolePermissionDTO permission = rolePermissionService.getRolePermission(organizationId, permissionRole);
            
            return ResponseEntity.ok(ApiResponse.success("获取当前用户权限成功", permission));
        } catch (Exception e) {
            logger.error("Error getting current user permissions", e);
            return ResponseEntity.ok(ApiResponse.error("获取当前用户权限失败: " + e.getMessage()));
        }
    }

    /**
     * 获取机构的所有角色权限
     */
    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<ApiResponse<List<RolePermissionDTO>>> getOrganizationPermissions(
            @PathVariable Long organizationId) {
        try {
            List<RolePermissionDTO> permissions = rolePermissionService.getOrganizationPermissions(organizationId);
            return ResponseEntity.ok(ApiResponse.success("获取机构权限成功", permissions));
        } catch (Exception e) {
            logger.error("Error getting organization permissions", e);
            return ResponseEntity.ok(ApiResponse.error("获取机构权限失败: " + e.getMessage()));
        }
    }

    /**
     * 获取指定角色的权限
     */
    @GetMapping("/organization/{organizationId}/role/{role}")
    public ResponseEntity<ApiResponse<RolePermissionDTO>> getRolePermission(
            @PathVariable Long organizationId,
            @PathVariable String role) {
        try {
            RolePermissionDTO permission = rolePermissionService.getRolePermission(organizationId, role);
            return ResponseEntity.ok(ApiResponse.success("获取角色权限成功", permission));
        } catch (Exception e) {
            logger.error("Error getting role permission", e);
            return ResponseEntity.ok(ApiResponse.error("获取角色权限失败: " + e.getMessage()));
        }
    }

    /**
     * 保存或更新角色权限
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RolePermissionDTO>> saveRolePermission(
            @RequestBody RolePermissionDTO dto) {
        try {
            RolePermissionDTO saved = rolePermissionService.saveRolePermission(dto);
            return ResponseEntity.ok(ApiResponse.success("保存权限成功", saved));
        } catch (Exception e) {
            logger.error("Error saving role permission", e);
            return ResponseEntity.ok(ApiResponse.error("保存权限失败: " + e.getMessage()));
        }
    }

    /**
     * 批量保存机构的角色权限
     */
    @PostMapping("/organization/{organizationId}/batch")
    public ResponseEntity<ApiResponse<List<RolePermissionDTO>>> saveRolePermissions(
            @PathVariable Long organizationId,
            @RequestBody List<RolePermissionDTO> dtos) {
        try {
            List<RolePermissionDTO> saved = rolePermissionService.saveRolePermissions(organizationId, dtos);
            return ResponseEntity.ok(ApiResponse.success("批量保存权限成功", saved));
        } catch (Exception e) {
            logger.error("Error saving role permissions", e);
            return ResponseEntity.ok(ApiResponse.error("批量保存权限失败: " + e.getMessage()));
        }
    }
}

