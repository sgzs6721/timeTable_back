package com.timetable.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public class TrialCustomerDTO {
    private Long customerId;
    private String childName;
    private String parentPhone;
    private String status;
    private String statusText;
    private LocalDate trialScheduleDate;
    private LocalTime trialStartTime;
    private LocalTime trialEndTime;
    private Long trialCoachId;
    private String trialCoachName;
    private String trialStudentName;
    private Integer trialCount; // 第几次体验

    public TrialCustomerDTO() {
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getChildName() {
        return childName;
    }

    public void setChildName(String childName) {
        this.childName = childName;
    }

    public String getParentPhone() {
        return parentPhone;
    }

    public void setParentPhone(String parentPhone) {
        this.parentPhone = parentPhone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
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

    public String getTrialCoachName() {
        return trialCoachName;
    }

    public void setTrialCoachName(String trialCoachName) {
        this.trialCoachName = trialCoachName;
    }

    public String getTrialStudentName() {
        return trialStudentName;
    }

    public void setTrialStudentName(String trialStudentName) {
        this.trialStudentName = trialStudentName;
    }

    public Integer getTrialCount() {
        return trialCount;
    }

    public void setTrialCount(Integer trialCount) {
        this.trialCount = trialCount;
    }
}

