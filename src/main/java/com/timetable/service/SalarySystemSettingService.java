package com.timetable.service;

import com.timetable.entity.SalarySystemSetting;
import com.timetable.repository.SalarySystemSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SalarySystemSettingService {

    @Autowired
    private SalarySystemSettingRepository salarySystemSettingRepository;

    /**
     * 获取当前工资系统设置
     */
    public SalarySystemSetting getCurrentSetting() {
        SalarySystemSetting setting = salarySystemSettingRepository.getCurrentSetting();
        if (setting == null) {
            // 如果没有设置，返回默认值
            setting = new SalarySystemSetting();
            setting.setSalaryStartDay(1);
            setting.setSalaryEndDay(31);
            setting.setSalaryPayDay(5);
            setting.setDescription("默认记薪周期：每月1号到31号，工资在次月5号发放");
        }
        return setting;
    }

    /**
     * 保存或更新工资系统设置
     */
    public SalarySystemSetting saveOrUpdate(SalarySystemSetting setting) {
        // 验证数据
        validateSetting(setting);
        
        return salarySystemSettingRepository.saveOrUpdate(setting);
    }

    /**
     * 验证工资系统设置数据
     */
    private void validateSetting(SalarySystemSetting setting) {
        if (setting == null) {
            throw new IllegalArgumentException("工资系统设置不能为空");
        }
        
        if (setting.getSalaryStartDay() == null || setting.getSalaryStartDay() < 1 || setting.getSalaryStartDay() > 31) {
            throw new IllegalArgumentException("记薪开始日必须在1-31之间");
        }
        
        if (setting.getSalaryEndDay() == null || setting.getSalaryEndDay() < 1 || setting.getSalaryEndDay() > 31) {
            throw new IllegalArgumentException("记薪结束日必须在1-31之间");
        }
        
        if (setting.getSalaryPayDay() == null || setting.getSalaryPayDay() < 1 || setting.getSalaryPayDay() > 31) {
            throw new IllegalArgumentException("工资发放日必须在1-31之间");
        }
    }
}
