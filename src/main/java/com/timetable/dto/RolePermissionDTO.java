package com.timetable.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * 角色权限DTO
 */
public class RolePermissionDTO {
    private Long id;
    
    @JsonProperty("organizationId")
    private Long organizationId;
    
    private String role;
    
    @JsonProperty("menuPermissions")
    private Map<String, Boolean> menuPermissions;
    
    @JsonProperty("actionPermissions")
    private Map<String, Boolean> actionPermissions;

    public RolePermissionDTO() {
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

    public Map<String, Boolean> getMenuPermissions() {
        return menuPermissions;
    }

    public void setMenuPermissions(Map<String, Boolean> menuPermissions) {
        this.menuPermissions = menuPermissions;
    }

    public Map<String, Boolean> getActionPermissions() {
        return actionPermissions;
    }

    public void setActionPermissions(Map<String, Boolean> actionPermissions) {
        this.actionPermissions = actionPermissions;
    }
}

