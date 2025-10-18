package com.timetable.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SalaryCalculationDTO {
    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private String month; // 工资月份，格式：YYYY-MM
    private LocalDate salaryPeriodStart; // 记薪周期开始日期
    private LocalDate salaryPeriodEnd; // 记薪周期结束日期
    private BigDecimal baseSalary; // 底薪
    private BigDecimal hourlyRate; // 课时费单价
    private Double totalHours; // 总课时数（支持小数）
    private BigDecimal hourlyPay; // 课时费总额
    private BigDecimal socialSecurity; // 社保扣款
    private BigDecimal commissionRate; // 提成比例
    private BigDecimal commission; // 提成金额
    private BigDecimal totalSalary; // 应发工资总额
    private String payStatus; // 发放状态：unpaid, paid
    private String salaryPeriod; // 记薪周期显示文本

    public SalaryCalculationDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public LocalDate getSalaryPeriodStart() {
        return salaryPeriodStart;
    }

    public void setSalaryPeriodStart(LocalDate salaryPeriodStart) {
        this.salaryPeriodStart = salaryPeriodStart;
    }

    public LocalDate getSalaryPeriodEnd() {
        return salaryPeriodEnd;
    }

    public void setSalaryPeriodEnd(LocalDate salaryPeriodEnd) {
        this.salaryPeriodEnd = salaryPeriodEnd;
    }

    public BigDecimal getBaseSalary() {
        return baseSalary;
    }

    public void setBaseSalary(BigDecimal baseSalary) {
        this.baseSalary = baseSalary;
    }

    public BigDecimal getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(BigDecimal hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public Double getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(Double totalHours) {
        this.totalHours = totalHours;
    }

    public BigDecimal getHourlyPay() {
        return hourlyPay;
    }

    public void setHourlyPay(BigDecimal hourlyPay) {
        this.hourlyPay = hourlyPay;
    }

    public BigDecimal getSocialSecurity() {
        return socialSecurity;
    }

    public void setSocialSecurity(BigDecimal socialSecurity) {
        this.socialSecurity = socialSecurity;
    }

    public BigDecimal getCommissionRate() {
        return commissionRate;
    }

    public void setCommissionRate(BigDecimal commissionRate) {
        this.commissionRate = commissionRate;
    }

    public BigDecimal getCommission() {
        return commission;
    }

    public void setCommission(BigDecimal commission) {
        this.commission = commission;
    }

    public BigDecimal getTotalSalary() {
        return totalSalary;
    }

    public void setTotalSalary(BigDecimal totalSalary) {
        this.totalSalary = totalSalary;
    }

    public String getPayStatus() {
        return payStatus;
    }

    public void setPayStatus(String payStatus) {
        this.payStatus = payStatus;
    }

    public String getSalaryPeriod() {
        return salaryPeriod;
    }

    public void setSalaryPeriod(String salaryPeriod) {
        this.salaryPeriod = salaryPeriod;
    }
}
