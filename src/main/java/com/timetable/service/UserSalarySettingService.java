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

    public UserSalarySetting saveOrUpdate(UserSalarySetting setting) {
        UserSalarySetting existing = salarySettingRepository.findByUserId(setting.getUserId());
        
        if (existing != null) {
            // 更新现有记录
            setting.setId(existing.getId());
            salarySettingRepository.update(setting);
            return salarySettingRepository.findById(existing.getId());
        } else {
            // 创建新记录
            Long id = salarySettingRepository.save(setting);
            return salarySettingRepository.findById(id);
        }
    }

    public void deleteByUserId(Long userId) {
        salarySettingRepository.deleteByUserId(userId);
    }
}

