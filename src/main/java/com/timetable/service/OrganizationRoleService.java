package com.timetable.service;

import com.timetable.dto.OrganizationRoleDTO;
import com.timetable.entity.OrganizationRole;
import com.timetable.repository.OrganizationRoleRepository;
import com.timetable.repository.UserRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrganizationRoleService {

    private final OrganizationRoleRepository roleRepository;
    private final UserRepository userRepository;

    public OrganizationRoleService(OrganizationRoleRepository roleRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
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

        // 检查角色是否有成员（根据 position 字段统计）
        int memberCount = userRepository.countByPositionAndOrganizationId(role.getRoleCode(), role.getOrganizationId());
        if (memberCount > 0) {
            throw new RuntimeException("该角色下有 " + memberCount + " 个成员，无法删除");
        }

        roleRepository.deleteById(id);
    }

    /**
     * 获取角色成员数量
     */
    public int getRoleMemberCount(Long roleId) {
        return userRepository.countByOrganizationRoleId(roleId);
    }

    /**
     * 为用户分配角色
     */
    @Transactional
    public void assignRoleToUser(Long roleId, Long userId) {
        // 验证角色存在
        OrganizationRole role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("角色不存在"));
        
        // 更新用户角色
        userRepository.updateOrganizationRoleId(userId, roleId);
    }

    /**
     * 从角色中移除用户
     */
    @Transactional
    public void removeUserFromRole(Long userId) {
        userRepository.updateOrganizationRoleId(userId, null);
    }

    /**
     * 批量为用户分配角色
     */
    @Transactional
    public void assignRoleToUsers(Long roleId, List<Long> userIds) {
        // 验证角色存在
        OrganizationRole role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("角色不存在"));
        
        for (Long userId : userIds) {
            userRepository.updateOrganizationRoleId(userId, roleId);
        }
    }

    private OrganizationRoleDTO convertToDTO(OrganizationRole role) {
        OrganizationRoleDTO dto = new OrganizationRoleDTO();
        BeanUtils.copyProperties(role, dto);
        // 添加成员数量（根据 position 字段统计）
        dto.setMemberCount(userRepository.countByPositionAndOrganizationId(role.getRoleCode(), role.getOrganizationId()));
        return dto;
    }
}

