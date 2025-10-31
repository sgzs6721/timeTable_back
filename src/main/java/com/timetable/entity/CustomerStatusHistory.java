package com.timetable.entity;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;

public class CustomerStatusHistory {
    private Long id;
    private Long customerId;
    private String fromStatus;
    private String toStatus;
    private String notes;
    private Long createdBy;
    private LocalDateTime createdAt;
    
    // 体验课程相关字段
    private LocalDate trialScheduleDate;
    private LocalTime trialStartTime;
    private LocalTime trialEndTime;
    private Long trialCoachId;
    private String trialStudentName;
    private Boolean trialCancelled;  // 体验课程是否已取消
    private Long trialScheduleId;    // 体验课程ID
    private Long trialTimetableId;   // 体验课程所属课表ID
    private String trialSourceType;  // 课程来源类型

    public CustomerStatusHistory() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDate getTrialScheduleDate() {
        return trialScheduleDate;
    }

    public void setTrialScheduleDate(LocalDate trialScheduleDate) {
        this.trialScheduleDate = trialScheduleDate;
    }

    public LocalTime getTrialStartTime() {
        return trialStartTime;
    }

    public void setTrialStartTime(LocalTime trialStartTime) {
        this.trialStartTime = trialStartTime;
    }

    public LocalTime getTrialEndTime() {
        return trialEndTime;
    }

    public void setTrialEndTime(LocalTime trialEndTime) {
        this.trialEndTime = trialEndTime;
    }

    public Long getTrialCoachId() {
        return trialCoachId;
    }

    public void setTrialCoachId(Long trialCoachId) {
        this.trialCoachId = trialCoachId;
    }

    public String getTrialStudentName() {
        return trialStudentName;
    }

    public void setTrialStudentName(String trialStudentName) {
        this.trialStudentName = trialStudentName;
    }

    public Boolean getTrialCancelled() {
        return trialCancelled;
    }

    public void setTrialCancelled(Boolean trialCancelled) {
        this.trialCancelled = trialCancelled;
    }

    public Long getTrialScheduleId() {
        return trialScheduleId;
    }

    public void setTrialScheduleId(Long trialScheduleId) {
        this.trialScheduleId = trialScheduleId;
    }

    public Long getTrialTimetableId() {
        return trialTimetableId;
    }

    public void setTrialTimetableId(Long trialTimetableId) {
        this.trialTimetableId = trialTimetableId;
    }

    public String getTrialSourceType() {
        return trialSourceType;
    }

    public void setTrialSourceType(String trialSourceType) {
        this.trialSourceType = trialSourceType;
    }
}

