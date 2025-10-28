package com.timetable.service;

import com.timetable.dto.OrganizationDTO;
import com.timetable.entity.Organization;
import com.timetable.repository.OrganizationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 机构服务层
 */
@Service
public class OrganizationService {

    @Autowired
    private OrganizationRepository organizationRepository;

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

