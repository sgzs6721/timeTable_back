package com.timetable.service;

import com.timetable.dto.OrganizationRoleDTO;
import com.timetable.entity.OrganizationRole;
import com.timetable.repository.OrganizationRoleRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrganizationRoleService {

    private final OrganizationRoleRepository roleRepository;

    public OrganizationRoleService(OrganizationRoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public List<OrganizationRoleDTO> getOrganizationRoles(Long organizationId) {
        List<OrganizationRole> roles = roleRepository.findByOrganizationId(organizationId);
        return roles.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public OrganizationRoleDTO getRoleById(Long id) {
        OrganizationRole role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("角色不存在"));
        return convertToDTO(role);
    }

    @Transactional
    public OrganizationRoleDTO createRole(OrganizationRoleDTO dto) {
        // 检查角色代码是否已存在
        if (roleRepository.existsByOrganizationIdAndRoleCode(dto.getOrganizationId(), dto.getRoleCode())) {
            throw new RuntimeException("角色代码已存在");
        }

        OrganizationRole role = new OrganizationRole();
        BeanUtils.copyProperties(dto, role);
        role.setIsSystem(false); // 用户创建的角色都是非系统角色
        
        OrganizationRole savedRole = roleRepository.save(role);
        return convertToDTO(savedRole);
    }

    @Transactional
    public OrganizationRoleDTO updateRole(Long id, OrganizationRoleDTO dto) {
        OrganizationRole existing = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("角色不存在"));

        existing.setRoleName(dto.getRoleName());
        existing.setRoleCode(dto.getRoleCode());
        existing.setDescription(dto.getDescription());
        existing.setIcon(dto.getIcon());
        existing.setColor(dto.getColor());

        OrganizationRole updatedRole = roleRepository.save(existing);
        return convertToDTO(updatedRole);
    }

    @Transactional
    public void deleteRole(Long id) {
        OrganizationRole role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("角色不存在"));

        roleRepository.deleteById(id);
    }

    private OrganizationRoleDTO convertToDTO(OrganizationRole role) {
        OrganizationRoleDTO dto = new OrganizationRoleDTO();
        BeanUtils.copyProperties(role, dto);
        return dto;
    }
}

