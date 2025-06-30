package com.timetable.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 课表请求DTO
 */
public class TimetableRequest {
    
    @NotBlank(message = "课表名称不能为空")
    @Size(min = 2, max = 100, message = "课表名称长度必须在2-100字符之间")
    private String name;
    
    private String description;
    
    private Boolean isWeekly = false;
    
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    // 构造函数
    public TimetableRequest() {
    }
    
    public TimetableRequest(String name, String description, Boolean isWeekly) {
        this.name = name;
        this.description = description;
        this.isWeekly = isWeekly;
    }
    
    // Getter和Setter方法
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Boolean getIsWeekly() {
        return isWeekly;
    }
    
    public void setIsWeekly(Boolean isWeekly) {
        this.isWeekly = isWeekly;
    }
    
    public LocalDate getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
    
    public LocalDate getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
} 