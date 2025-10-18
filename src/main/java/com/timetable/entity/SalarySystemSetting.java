package com.timetable.entity;

import java.time.LocalDateTime;

public class SalarySystemSetting {
    private Long id;
    private Integer salaryStartDay; // 记薪开始日
    private Integer salaryEndDay;   // 记薪结束日
    private Integer salaryPayDay;   // 工资发放日
    private String description;     // 工资计算说明
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SalarySystemSetting() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getSalaryStartDay() {
        return salaryStartDay;
    }

    public void setSalaryStartDay(Integer salaryStartDay) {
        this.salaryStartDay = salaryStartDay;
    }

    public Integer getSalaryEndDay() {
        return salaryEndDay;
    }

    public void setSalaryEndDay(Integer salaryEndDay) {
        this.salaryEndDay = salaryEndDay;
    }

    public Integer getSalaryPayDay() {
        return salaryPayDay;
    }

    public void setSalaryPayDay(Integer salaryPayDay) {
        this.salaryPayDay = salaryPayDay;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
