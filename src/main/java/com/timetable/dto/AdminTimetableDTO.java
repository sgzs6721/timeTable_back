package com.timetable.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AdminTimetableDTO {
    private Long id;
    private Long userId;
    private String username;
    private String name;
    private Boolean isWeekly;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer scheduleCount;
    private LocalDateTime createdAt;
    private Byte isActive;
    private Byte isArchived;

    public AdminTimetableDTO() { }

    public AdminTimetableDTO(Long id, Long userId, String username, String name, Boolean isWeekly,
                              LocalDate startDate, LocalDate endDate, Integer scheduleCount, LocalDateTime createdAt,
                              Byte isActive, Byte isArchived) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.name = name;
        this.isWeekly = isWeekly;
        this.startDate = startDate;
        this.endDate = endDate;
        this.scheduleCount = scheduleCount;
        this.createdAt = createdAt;
        this.isActive = isActive;
        this.isArchived = isArchived;
    }

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Boolean getIsWeekly() { return isWeekly; }
    public void setIsWeekly(Boolean isWeekly) { this.isWeekly = isWeekly; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Integer getScheduleCount() { return scheduleCount; }
    public void setScheduleCount(Integer scheduleCount) { this.scheduleCount = scheduleCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Byte getIsActive() { return isActive; }
    public void setIsActive(Byte isActive) { this.isActive = isActive; }
    public Byte getIsArchived() { return isArchived; }
    public void setIsArchived(Byte isArchived) { this.isArchived = isArchived; }
} 