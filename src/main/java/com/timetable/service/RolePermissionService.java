package com.timetable.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.timetable.dto.RolePermissionDTO;
import com.timetable.entity.RolePermission;
import com.timetable.repository.RolePermissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 角色权限Service
 */
@Service
public class RolePermissionService {
    
    private static final Logger logger = LoggerFactory.getLogger(RolePermissionService.class);
    
    @Autowired
    private RolePermissionRepository rolePermissionRepository;
    
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 获取机构的所有角色权限
     */
    public List<RolePermissionDTO> getOrganizationPermissions(Long organizationId) {
        List<RolePermission> permissions = rolePermissionRepository.findByOrganizationId(organizationId);
        return permissions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取指定角色的权限
     */
    public RolePermissionDTO getRolePermission(Long organizationId, String role) {
        return rolePermissionRepository.findByOrganizationIdAndRole(organizationId, role)
                .map(this::convertToDTO)
                .orElse(createDefaultPermission(organizationId, role));
    }

    /**
     * 保存或更新角色权限
     */
    @Transactional
    public RolePermissionDTO saveRolePermission(RolePermissionDTO dto) {
        try {
            RolePermission permission = rolePermissionRepository
                    .findByOrganizationIdAndRole(dto.getOrganizationId(), dto.getRole())
                    .orElse(new RolePermission());
            
            permission.setOrganizationId(dto.getOrganizationId());
            permission.setRole(dto.getRole());
            permission.setMenuPermissions(objectMapper.writeValueAsString(dto.getMenuPermissions()));
            permission.setActionPermissions(objectMapper.writeValueAsString(dto.getActionPermissions()));
            
            RolePermission saved = rolePermissionRepository.save(permission);
            return convertToDTO(saved);
        } catch (JsonProcessingException e) {
            logger.error("Error saving role permission", e);
            throw new RuntimeException("Failed to save role permission", e);
        }
    }

    /**
     * 批量保存角色权限
     */
    @Transactional
    public List<RolePermissionDTO> saveRolePermissions(Long organizationId, List<RolePermissionDTO> dtos) {
        return dtos.stream()
                .peek(dto -> dto.setOrganizationId(organizationId))
                .map(this::saveRolePermission)
                .collect(Collectors.toList());
    }

    /**
     * 转换为DTO
     */
    private RolePermissionDTO convertToDTO(RolePermission permission) {
        RolePermissionDTO dto = new RolePermissionDTO();
        dto.setId(permission.getId());
        dto.setOrganizationId(permission.getOrganizationId());
        dto.setRole(permission.getRole());
        
        try {
            if (permission.getMenuPermissions() != null) {
                dto.setMenuPermissions(objectMapper.readValue(
                    permission.getMenuPermissions(), 
                    new TypeReference<Map<String, Boolean>>() {}
                ));
            } else {
                dto.setMenuPermissions(getDefaultMenuPermissions());
            }
            
            if (permission.getActionPermissions() != null) {
                dto.setActionPermissions(objectMapper.readValue(
                    permission.getActionPermissions(), 
                    new TypeReference<Map<String, Boolean>>() {}
                ));
            } else {
                dto.setActionPermissions(getDefaultActionPermissions(permission.getRole()));
            }
        } catch (JsonProcessingException e) {
            logger.error("Error converting role permission to DTO", e);
            dto.setMenuPermissions(getDefaultMenuPermissions());
            dto.setActionPermissions(getDefaultActionPermissions(permission.getRole()));
        }
        
        return dto;
    }

    /**
     * 创建默认权限
     */
    private RolePermissionDTO createDefaultPermission(Long organizationId, String role) {
        RolePermissionDTO dto = new RolePermissionDTO();
        dto.setOrganizationId(organizationId);
        dto.setRole(role);
        dto.setMenuPermissions(getDefaultMenuPermissions());
        dto.setActionPermissions(getDefaultActionPermissions(role));
        return dto;
    }

    /**
     * 获取默认菜单权限
     */
    private Map<String, Boolean> getDefaultMenuPermissions() {
        Map<String, Boolean> permissions = new HashMap<>();
        permissions.put("dashboard", true);
        permissions.put("todo", true);
        permissions.put("customer", true);
        permissions.put("mySchedule", true);
        permissions.put("myStudents", true);
        permissions.put("myHours", true);
        permissions.put("mySalary", true);
        return permissions;
    }

    /**
     * 获取默认操作权限
     */
    private Map<String, Boolean> getDefaultActionPermissions(String role) {
        Map<String, Boolean> permissions = new HashMap<>();
        permissions.put("refresh", true);
        permissions.put("admin", "ADMIN".equalsIgnoreCase(role));
        permissions.put("archived", true);
        permissions.put("profile", true);
        permissions.put("guide", true);
        permissions.put("logout", true);
        return permissions;
    }
}

