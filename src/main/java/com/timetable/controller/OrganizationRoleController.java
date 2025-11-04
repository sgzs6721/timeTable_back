package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.OrganizationRoleDTO;
import com.timetable.service.OrganizationRoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/organization-roles")
public class OrganizationRoleController {

    private final OrganizationRoleService roleService;

    public OrganizationRoleController(OrganizationRoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<ApiResponse<List<OrganizationRoleDTO>>> getOrganizationRoles(@PathVariable Long organizationId) {
        try {
            List<OrganizationRoleDTO> roles = roleService.getOrganizationRoles(organizationId);
            return ResponseEntity.ok(ApiResponse.success("获取角色列表成功", roles));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationRoleDTO>> getRoleById(@PathVariable Long id) {
        try {
            OrganizationRoleDTO role = roleService.getRoleById(id);
            return ResponseEntity.ok(ApiResponse.success("获取角色成功", role));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrganizationRoleDTO>> createRole(@RequestBody OrganizationRoleDTO dto) {
        try {
            OrganizationRoleDTO createdRole = roleService.createRole(dto);
            return ResponseEntity.ok(ApiResponse.success("创建角色成功", createdRole));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationRoleDTO>> updateRole(@PathVariable Long id, @RequestBody OrganizationRoleDTO dto) {
        try {
            OrganizationRoleDTO updatedRole = roleService.updateRole(id, dto);
            return ResponseEntity.ok(ApiResponse.success("更新角色成功", updatedRole));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable Long id) {
        try {
            roleService.deleteRole(id);
            return ResponseEntity.ok(ApiResponse.success("删除角色成功"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{roleId}/member-count")
    public ResponseEntity<ApiResponse<Integer>> getRoleMemberCount(@PathVariable Long roleId) {
        try {
            int count = roleService.getRoleMemberCount(roleId);
            return ResponseEntity.ok(ApiResponse.success("获取成员数量成功", count));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{roleId}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> assignRoleToUser(@PathVariable Long roleId, @PathVariable Long userId) {
        try {
            roleService.assignRoleToUser(roleId, userId);
            return ResponseEntity.ok(ApiResponse.success("分配角色成功"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{roleId}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeUserFromRole(@PathVariable Long roleId, @PathVariable Long userId) {
        try {
            roleService.removeUserFromRole(userId);
            return ResponseEntity.ok(ApiResponse.success("移除成员成功"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{roleId}/members/batch")
    public ResponseEntity<ApiResponse<Void>> assignRoleToUsers(@PathVariable Long roleId, @RequestBody List<Long> userIds) {
        try {
            roleService.assignRoleToUsers(roleId, userIds);
            return ResponseEntity.ok(ApiResponse.success("批量分配角色成功"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }
}

