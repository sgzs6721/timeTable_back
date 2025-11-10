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
            
            // 获取用户所属机构和职位的权限配置
            Long organizationId = user.getOrganizationId();
            String position = user.getPosition(); // position 字段存储用户职位：COACH, SALES, RECEPTIONIST, MANAGER
            
            if (organizationId == null) {
                return ResponseEntity.ok(ApiResponse.error("用户未分配机构"));
            }
            
            if (position == null) {
                return ResponseEntity.ok(ApiResponse.error("用户未分配职位"));
            }
            
            // 所有用户都根据 position 查询权限配置（不管 role 是 ADMIN 还是 USER）
            RolePermissionDTO permission = rolePermissionService.getRolePermission(organizationId, position);
            
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
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and @userService.isOrganizationAdmin(authentication.name, #organizationId))")
    public ResponseEntity<ApiResponse<List<RolePermissionDTO>>> getOrganizationPermissions(
            @PathVariable Long organizationId, Authentication authentication) {
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
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and @userService.isOrganizationAdmin(authentication.name, #organizationId))")
    public ResponseEntity<ApiResponse<RolePermissionDTO>> getRolePermission(
            @PathVariable Long organizationId,
            @PathVariable String role,
            Authentication authentication) {
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
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and @userService.isOrganizationAdmin(authentication.name, #organizationId))")
    public ResponseEntity<ApiResponse<List<RolePermissionDTO>>> saveRolePermissions(
            @PathVariable Long organizationId,
            @RequestBody List<RolePermissionDTO> dtos,
            Authentication authentication) {
        try {
            List<RolePermissionDTO> saved = rolePermissionService.saveRolePermissions(organizationId, dtos);
            return ResponseEntity.ok(ApiResponse.success("批量保存权限成功", saved));
        } catch (Exception e) {
            logger.error("Error saving role permissions", e);
            return ResponseEntity.ok(ApiResponse.error("批量保存权限失败: " + e.getMessage()));
        }
    }
}

