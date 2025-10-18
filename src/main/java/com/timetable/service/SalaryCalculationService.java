package com.timetable.service;

import com.timetable.dto.SalaryCalculationDTO;
import com.timetable.entity.SalarySystemSetting;
import com.timetable.entity.UserSalarySetting;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.repository.UserSalarySettingRepository;
import com.timetable.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SalaryCalculationService {

    @Autowired
    private UserSalarySettingRepository salarySettingRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ScheduleRepository scheduleRepository;
    
    @Autowired
    private SalarySystemSettingService salarySystemSettingService;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private ReportService reportService;

    /**
     * 计算指定月份的工资
     * @param month 月份，格式：YYYY-MM
     * @return 工资计算结果列表
     */
    public List<SalaryCalculationDTO> calculateSalary(String month) {
        List<SalaryCalculationDTO> result = new ArrayList<>();
        
        // 解析月份
        YearMonth yearMonth = YearMonth.parse(month);
        
        // 获取工资系统设置
        SalarySystemSetting systemSetting = salarySystemSettingService.getCurrentSetting();
        
        // 计算记薪周期
        LocalDate[] periodRange = calculateSalaryPeriod(yearMonth, systemSetting);
        LocalDate periodStart = periodRange[0];
        LocalDate periodEnd = periodRange[1];
        
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
     * 根据月份和工资系统设置计算记薪周期
     */
    private LocalDate[] calculateSalaryPeriod(YearMonth yearMonth, SalarySystemSetting systemSetting) {
        Integer startDay = systemSetting.getSalaryStartDay();
        Integer endDay = systemSetting.getSalaryEndDay();
        
        if (startDay == null) startDay = 1;
        if (endDay == null) endDay = 31;
        
        // 如果endDay为0，表示月末（最后一天）
        if (endDay == 0) {
            endDay = yearMonth.lengthOfMonth();
        }
        
        LocalDate periodStart;
        LocalDate periodEnd;
        
        // 如果开始日小于等于结束日，表示在同一个月内
        if (startDay <= endDay) {
            periodStart = yearMonth.atDay(Math.min(startDay, yearMonth.lengthOfMonth()));
            periodEnd = yearMonth.atDay(Math.min(endDay, yearMonth.lengthOfMonth()));
        } else {
            // 如果开始日大于结束日，表示跨月，从上月开始日到本月结束日
            YearMonth previousMonth = yearMonth.minusMonths(1);
            periodStart = previousMonth.atDay(Math.min(startDay, previousMonth.lengthOfMonth()));
            // 处理跨月情况下的月末
            int actualEndDay = endDay;
            if (systemSetting.getSalaryEndDay() == 0) {
                actualEndDay = yearMonth.lengthOfMonth();
            }
            periodEnd = yearMonth.atDay(Math.min(actualEndDay, yearMonth.lengthOfMonth()));
        }
        
        return new LocalDate[]{periodStart, periodEnd};
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
        Double totalHours = calculateTotalHours(user.getId(), periodStart, periodEnd);
        
        // 如果课时数为0且没有底薪，则不生成工资记录
        if (totalHours == 0.0 && dto.getBaseSalary().compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        
        dto.setTotalHours(totalHours);

        // 计算课时费
        BigDecimal hourlyPay = dto.getHourlyRate().multiply(BigDecimal.valueOf(totalHours));
        dto.setHourlyPay(hourlyPay);

        // 计算提成（这里简化为基于课时费的提成）
        BigDecimal commission = hourlyPay.multiply(dto.getCommissionRate()).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
        dto.setCommission(commission);

        // 计算总工资：底薪 + 课时费 + 提成 + 社保
        BigDecimal totalSalary = dto.getBaseSalary().add(hourlyPay).add(commission).add(dto.getSocialSecurity());
        dto.setTotalSalary(totalSalary);

        // 设置默认发放状态
        dto.setPayStatus("unpaid");

        return dto;
    }

    /**
     * 计算用户在指定时间段内的总课时数
     * 使用与"我的课时"页面相同的逻辑，包括学员操作规则过滤
     */
    private Double calculateTotalHours(Long userId, LocalDate startDate, LocalDate endDate) {
        try {
            // 使用ReportService的相同逻辑来计算课时，确保与"我的课时"页面保持一致
            Map<String, Object> hoursData = reportService.queryHoursPaged(userId, startDate, endDate, 1, Integer.MAX_VALUE, "desc");
            
            // 从结果中获取总课时数
            Object grandTotalHours = hoursData.get("grandTotalHours");
            if (grandTotalHours instanceof Double) {
                return Math.round(((Double) grandTotalHours) * 10.0) / 10.0;
            } else if (grandTotalHours instanceof Number) {
                return Math.round(((Number) grandTotalHours).doubleValue() * 10.0) / 10.0;
            }
            
            return 0.0;
        } catch (Exception e) {
            System.err.println("计算课时数时发生错误: " + e.getMessage());
            e.printStackTrace();
            return 0.0;
        }
    }

    /**
     * 获取最近N个月的工资计算结果
     */
    public List<SalaryCalculationDTO> getRecentSalaryCalculations(int months) {
        List<SalaryCalculationDTO> result = new ArrayList<>();
        
        LocalDate now = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(now);
        
        for (int i = 0; i < months; i++) {
            YearMonth targetMonth = currentMonth.minusMonths(i);
            
            // 只计算当前月份及之前的月份，不计算未来月份
            if (targetMonth.isAfter(currentMonth)) {
                continue;
            }
            
            List<SalaryCalculationDTO> monthlyResult = calculateSalary(targetMonth.toString());
            result.addAll(monthlyResult);
        }
        
        return result;
    }
}