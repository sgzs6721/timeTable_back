package com.timetable.entity;

import java.time.LocalDateTime;

/**
 * 角色权限实体类
 */
public class RolePermission {
    private Long id;
    private Long organizationId;
    private String role;
    private String menuPermissions;
    private String actionPermissions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RolePermission() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getMenuPermissions() {
        return menuPermissions;
    }

    public void setMenuPermissions(String menuPermissions) {
        this.menuPermissions = menuPermissions;
    }

    public String getActionPermissions() {
        return actionPermissions;
    }

    public void setActionPermissions(String actionPermissions) {
        this.actionPermissions = actionPermissions;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

