package com.timetable.service;

import com.timetable.dto.UserSalarySettingDTO;
import com.timetable.entity.UserSalarySetting;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.repository.UserSalarySettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserSalarySettingService {

    @Autowired
    private UserSalarySettingRepository salarySettingRepository;

    @Autowired
    private UserService userService;

    public List<UserSalarySettingDTO> getAllUserSalarySettings() {
        List<UserSalarySettingDTO> dtoList = new ArrayList<>();
        
        // 获取所有已批准的用户
        List<Users> users = userService.getAllApprovedUsers();
        
        for (Users user : users) {
            UserSalarySettingDTO dto = new UserSalarySettingDTO();
            dto.setUserId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setNickname(user.getNickname());
            dto.setRole(user.getRole()); // 设置用户角色
            
            // 查找该用户的工资设置
            UserSalarySetting setting = salarySettingRepository.findByUserId(user.getId());
            if (setting != null) {
                dto.setId(setting.getId());
                dto.setBaseSalary(setting.getBaseSalary());
                dto.setSocialSecurity(setting.getSocialSecurity());
                dto.setHourlyRate(setting.getHourlyRate());
                dto.setCommissionRate(setting.getCommissionRate());
            } else {
                // 如果没有设置，使用默认值
                dto.setBaseSalary(BigDecimal.ZERO);
                dto.setSocialSecurity(BigDecimal.ZERO);
                dto.setHourlyRate(BigDecimal.ZERO);
                dto.setCommissionRate(BigDecimal.ZERO);
            }
            
            dtoList.add(dto);
        }
        
        return dtoList;
    }

    public List<UserSalarySettingDTO> getUserSalarySettingsByOrganizationId(Long organizationId) {
        List<UserSalarySettingDTO> dtoList = new ArrayList<>();
        
        // 获取该机构所有已批准的用户
        List<Users> users = userService.getUsersByOrganizationId(organizationId);
        
        for (Users user : users) {
            if (!"APPROVED".equalsIgnoreCase(user.getStatus())) {
                continue;
            }

            UserSalarySettingDTO dto = new UserSalarySettingDTO();
            dto.setUserId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setNickname(user.getNickname());
            dto.setRole(user.getRole());
            
            // 查找该用户在该机构的工资设置
            UserSalarySetting setting = salarySettingRepository.findByUserIdAndOrganizationId(user.getId(), organizationId);
            if (setting != null) {
                dto.setId(setting.getId());
                dto.setBaseSalary(setting.getBaseSalary());
                dto.setSocialSecurity(setting.getSocialSecurity());
                dto.setHourlyRate(setting.getHourlyRate());
                dto.setCommissionRate(setting.getCommissionRate());
            } else {
                // 如果没有设置，使用默认值
                dto.setBaseSalary(BigDecimal.ZERO);
                dto.setSocialSecurity(BigDecimal.ZERO);
                dto.setHourlyRate(BigDecimal.ZERO);
                dto.setCommissionRate(BigDecimal.ZERO);
            }
            
            dtoList.add(dto);
        }
        
        return dtoList;
    }

    public UserSalarySetting saveOrUpdate(UserSalarySetting setting) {
        if (setting == null) {
            throw new IllegalArgumentException("工资设置对象不能为空");
        }
        
        if (setting.getUserId() == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        if (setting.getOrganizationId() == null) {
            throw new IllegalArgumentException("机构ID不能为空");
        }
        
        try {
            UserSalarySetting existing = salarySettingRepository.findByUserIdAndOrganizationId(
                setting.getUserId(), setting.getOrganizationId());
            
            if (existing != null) {
                // 更新现有记录
                setting.setId(existing.getId());
                setting.setCreatedAt(existing.getCreatedAt()); // 保持原创建时间
                setting.setUpdatedAt(java.time.LocalDateTime.now());
                salarySettingRepository.update(setting);
                return salarySettingRepository.findById(existing.getId());
            } else {
                // 创建新记录
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                setting.setCreatedAt(now);
                setting.setUpdatedAt(now);
                Long id = salarySettingRepository.save(setting);
                if (id == null) {
                    throw new RuntimeException("保存工资设置失败，未获取到生成的ID");
                }
                return salarySettingRepository.findById(id);
            }
        } catch (Exception e) {
            throw new RuntimeException("保存工资设置时发生错误: " + e.getMessage(), e);
        }
    }

    public void deleteByUserId(Long userId) {
        salarySettingRepository.deleteByUserId(userId);
    }

    public void deleteByUserIdAndOrganizationId(Long userId, Long organizationId) {
        salarySettingRepository.deleteByUserIdAndOrganizationId(userId, organizationId);
    }
}

