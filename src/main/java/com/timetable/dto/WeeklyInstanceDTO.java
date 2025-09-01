package com.timetable.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 周实例DTO
 */
public class WeeklyInstanceDTO {
    private Long id;
    private Long templateTimetableId;
    private String templateTimetableName;
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private String yearWeek;
    private Boolean isCurrent;
    private LocalDateTime generatedAt;
    private LocalDateTime lastSyncedAt;
    private Integer scheduleCount;

    // 构造函数
    public WeeklyInstanceDTO() {}

    public WeeklyInstanceDTO(Long id, Long templateTimetableId, String templateTimetableName,
                           LocalDate weekStartDate, LocalDate weekEndDate, String yearWeek,
                           Boolean isCurrent, LocalDateTime generatedAt, LocalDateTime lastSyncedAt,
                           Integer scheduleCount) {
        this.id = id;
        this.templateTimetableId = templateTimetableId;
        this.templateTimetableName = templateTimetableName;
        this.weekStartDate = weekStartDate;
        this.weekEndDate = weekEndDate;
        this.yearWeek = yearWeek;
        this.isCurrent = isCurrent;
        this.generatedAt = generatedAt;
        this.lastSyncedAt = lastSyncedAt;
        this.scheduleCount = scheduleCount;
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

    public String getTemplateTimetableName() {
        return templateTimetableName;
    }

    public void setTemplateTimetableName(String templateTimetableName) {
        this.templateTimetableName = templateTimetableName;
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

    public Integer getScheduleCount() {
        return scheduleCount;
    }

    public void setScheduleCount(Integer scheduleCount) {
        this.scheduleCount = scheduleCount;
    }
}
