package com.timetable.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 周实例实体类
 */
public class WeeklyInstance {
    private Long id;
    private Long templateTimetableId;  // 关联的固定课表ID
    private LocalDate weekStartDate;   // 周开始日期（周一）
    private LocalDate weekEndDate;     // 周结束日期（周日）
    private String yearWeek;           // 年-周格式：2025-03
    private Boolean isCurrent;         // 是否为当前周实例
    private LocalDateTime generatedAt; // 生成时间
    private LocalDateTime lastSyncedAt; // 最后同步时间
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long organizationId;       // 所属机构ID

    // 构造函数
    public WeeklyInstance() {}

    public WeeklyInstance(Long templateTimetableId, LocalDate weekStartDate, LocalDate weekEndDate, String yearWeek) {
        this.templateTimetableId = templateTimetableId;
        this.weekStartDate = weekStartDate;
        this.weekEndDate = weekEndDate;
        this.yearWeek = yearWeek;
        this.isCurrent = false;
        this.generatedAt = LocalDateTime.now();
        this.lastSyncedAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getter 和 Setter 方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTemplateTimetableId() {
        return templateTimetableId;
    }

    public void setTemplateTimetableId(Long templateTimetableId) {
        this.templateTimetableId = templateTimetableId;
    }

    public LocalDate getWeekStartDate() {
        return weekStartDate;
    }

    public void setWeekStartDate(LocalDate weekStartDate) {
        this.weekStartDate = weekStartDate;
    }

    public LocalDate getWeekEndDate() {
        return weekEndDate;
    }

    public void setWeekEndDate(LocalDate weekEndDate) {
        this.weekEndDate = weekEndDate;
    }

    public String getYearWeek() {
        return yearWeek;
    }

    public void setYearWeek(String yearWeek) {
        this.yearWeek = yearWeek;
    }

    public Boolean getIsCurrent() {
        return isCurrent;
    }

    public void setIsCurrent(Boolean isCurrent) {
        this.isCurrent = isCurrent;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public LocalDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(LocalDateTime lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
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

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }
}
