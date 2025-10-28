package com.timetable.service;

import com.timetable.dto.OrganizationDTO;
import com.timetable.entity.Organization;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.repository.OrganizationRepository;
import com.timetable.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 机构服务层
 */
@Service
public class OrganizationService {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 获取所有活跃机构
     */
    public List<OrganizationDTO> getActiveOrganizations() {
        List<Organization> organizations = organizationRepository.findActiveOrganizations();
        return organizations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有机构
     */
    public List<OrganizationDTO> getAllOrganizations() {
        List<Organization> organizations = organizationRepository.findAll();
        return organizations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取机构
     */
    public OrganizationDTO getOrganizationById(Long id) {
        Organization organization = organizationRepository.findById(id);
        if (organization == null) {
            throw new RuntimeException("机构不存在");
        }
        return convertToDTO(organization);
    }

    /**
     * 根据Code获取机构
     */
    public Organization getOrganizationByCode(String code) {
        return organizationRepository.findByCode(code);
    }

    /**
     * 创建机构
     */
    @Transactional
    public OrganizationDTO createOrganization(Organization organization) {
        // 检查机构代码是否已存在
        if (organizationRepository.existsByCode(organization.getCode())) {
            throw new RuntimeException("机构代码已存在");
        }
        
        Organization savedOrganization = organizationRepository.save(organization);
        return convertToDTO(savedOrganization);
    }

    /**
     * 更新机构
     */
    @Transactional
    public OrganizationDTO updateOrganization(Long id, Organization organization) {
        Organization existingOrganization = organizationRepository.findById(id);
        if (existingOrganization == null) {
            throw new RuntimeException("机构不存在");
        }

        // 检查机构代码是否被其他机构使用
        if (organizationRepository.existsByCodeAndNotId(organization.getCode(), id)) {
            throw new RuntimeException("机构代码已被其他机构使用");
        }

        organization.setId(id);
        Organization updatedOrganization = organizationRepository.update(organization);
        return convertToDTO(updatedOrganization);
    }

    /**
     * 删除机构
     */
    @Transactional
    public void deleteOrganization(Long id) {
        Organization organization = organizationRepository.findById(id);
        if (organization == null) {
            throw new RuntimeException("机构不存在");
        }
        organizationRepository.deleteById(id);
    }

    /**
     * 获取机构的管理员列表
     */
    public List<Map<String, Object>> getOrganizationAdmins(Long organizationId) {
        Organization organization = organizationRepository.findById(organizationId);
        if (organization == null) {
            throw new RuntimeException("机构不存在");
        }

        // 查找该机构的所有管理员
        List<Users> users = userRepository.findByOrganizationId(organizationId);
        List<Map<String, Object>> admins = new ArrayList<>();

        for (Users user : users) {
            if ("ADMIN".equals(user.getRole())) {
                Map<String, Object> adminInfo = new HashMap<>();
                adminInfo.put("id", user.getId());
                adminInfo.put("username", user.getUsername());
                adminInfo.put("nickname", user.getNickname());
                adminInfo.put("phone", user.getPhone());
                adminInfo.put("status", user.getStatus());
                adminInfo.put("createdAt", user.getCreatedAt());
                admins.add(adminInfo);
            }
        }

        return admins;
    }

    /**
     * 设置用户为机构管理员
     */
    @Transactional
    public void setOrganizationAdmin(Long organizationId, Long userId) {
        // 验证机构存在
        Organization organization = organizationRepository.findById(organizationId);
        if (organization == null) {
            throw new RuntimeException("机构不存在");
        }

        // 验证用户存在
        Users user = userRepository.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 检查用户是否属于该机构
        if (!organizationId.equals(user.getOrganizationId())) {
            throw new RuntimeException("用户不属于该机构");
        }

        // 设置为管理员
        user.setRole("ADMIN");
        userRepository.update(user);
    }

    /**
     * 移除机构管理员（降为普通用户）
     */
    @Transactional
    public void removeOrganizationAdmin(Long organizationId, Long userId) {
        // 验证机构存在
        Organization organization = organizationRepository.findById(organizationId);
        if (organization == null) {
            throw new RuntimeException("机构不存在");
        }

        // 验证用户存在
        Users user = userRepository.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 检查用户是否属于该机构
        if (!organizationId.equals(user.getOrganizationId())) {
            throw new RuntimeException("用户不属于该机构");
        }

        // 降为普通用户
        user.setRole("USER");
        userRepository.update(user);
    }

    /**
     * 转换为DTO
     */
    private OrganizationDTO convertToDTO(Organization organization) {
        OrganizationDTO dto = new OrganizationDTO();
        dto.setId(organization.getId());
        dto.setName(organization.getName());
        dto.setCode(organization.getCode());
        dto.setAddress(organization.getAddress());
        dto.setContactPhone(organization.getContactPhone());
        dto.setContactPerson(organization.getContactPerson());
        dto.setStatus(organization.getStatus());
        dto.setCreatedAt(organization.getCreatedAt());
        return dto;
    }
}

