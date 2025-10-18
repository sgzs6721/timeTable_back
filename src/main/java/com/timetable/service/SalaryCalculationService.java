package com.timetable.service;

import com.timetable.dto.SalaryCalculationDTO;
import com.timetable.entity.UserSalarySetting;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.repository.UserSalarySettingRepository;
import com.timetable.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class SalaryCalculationService {

    @Autowired
    private UserSalarySettingRepository salarySettingRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ScheduleRepository scheduleRepository;

    /**
     * 计算指定月份的工资
     * @param month 月份，格式：YYYY-MM
     * @return 工资计算结果列表
     */
    public List<SalaryCalculationDTO> calculateSalary(String month) {
        List<SalaryCalculationDTO> result = new ArrayList<>();
        
        // 解析月份
        YearMonth yearMonth = YearMonth.parse(month);
        
        // 计算记薪周期（假设从1号到月末）
        LocalDate periodStart = yearMonth.atDay(1);
        LocalDate periodEnd = yearMonth.atEndOfMonth();
        
        // 获取所有用户
        List<Users> users = userService.getAllApprovedUsers();
        
        for (Users user : users) {
            if (!"USER".equals(user.getRole())) {
                continue; // 只计算普通用户（教练）的工资
            }
            
            SalaryCalculationDTO dto = calculateUserSalary(user, month, periodStart, periodEnd);
            if (dto != null) {
                result.add(dto);
            }
        }
        
        return result;
    }

    /**
     * 计算单个用户的工资
     */
    private SalaryCalculationDTO calculateUserSalary(Users user, String month, LocalDate periodStart, LocalDate periodEnd) {
        // 获取用户工资设置
        UserSalarySetting setting = salarySettingRepository.findByUserId(user.getId());
        if (setting == null) {
            return null; // 没有工资设置的用户不计算工资
        }

        SalaryCalculationDTO dto = new SalaryCalculationDTO();
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setMonth(month);
        dto.setSalaryPeriodStart(periodStart);
        dto.setSalaryPeriodEnd(periodEnd);
        dto.setSalaryPeriod(periodStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + 
                           " ~ " + periodEnd.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

        // 设置基础工资信息
        dto.setBaseSalary(setting.getBaseSalary() != null ? setting.getBaseSalary() : BigDecimal.ZERO);
        dto.setHourlyRate(setting.getHourlyRate() != null ? setting.getHourlyRate() : BigDecimal.ZERO);
        dto.setSocialSecurity(setting.getSocialSecurity() != null ? setting.getSocialSecurity() : BigDecimal.ZERO);
        dto.setCommissionRate(setting.getCommissionRate() != null ? setting.getCommissionRate() : BigDecimal.ZERO);

        // 计算该时间段内的课时数
        Integer totalHours = calculateTotalHours(user.getId(), periodStart, periodEnd);
        dto.setTotalHours(totalHours);

        // 计算课时费
        BigDecimal hourlyPay = dto.getHourlyRate().multiply(new BigDecimal(totalHours));
        dto.setHourlyPay(hourlyPay);

        // 计算提成（这里简化为基于课时费的提成）
        BigDecimal commission = hourlyPay.multiply(dto.getCommissionRate()).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
        dto.setCommission(commission);

        // 计算总工资：底薪 + 课时费 + 提成 - 社保
        BigDecimal totalSalary = dto.getBaseSalary().add(hourlyPay).add(commission).subtract(dto.getSocialSecurity());
        dto.setTotalSalary(totalSalary);

        // 设置默认发放状态
        dto.setPayStatus("unpaid");

        return dto;
    }

    /**
     * 计算用户在指定时间段内的总课时数
     */
    private Integer calculateTotalHours(Long userId, LocalDate startDate, LocalDate endDate) {
        try {
            // TODO: 这里需要根据实际的数据库结构来计算课时
            // 现在返回模拟数据
            if (userId == 1L) {
                return 45;
            } else if (userId == 2L) {
                return 38;
            } else {
                return 30;
            }
        } catch (Exception e) {
            // 发生错误时返回0
            return 0;
        }
    }

    /**
     * 获取最近N个月的工资计算结果
     */
    public List<SalaryCalculationDTO> getRecentSalaryCalculations(int months) {
        List<SalaryCalculationDTO> result = new ArrayList<>();
        
        LocalDate now = LocalDate.now();
        for (int i = 0; i < months; i++) {
            YearMonth targetMonth = YearMonth.from(now.minusMonths(i));
            List<SalaryCalculationDTO> monthlyResult = calculateSalary(targetMonth.toString());
            result.addAll(monthlyResult);
        }
        
        return result;
    }
}
