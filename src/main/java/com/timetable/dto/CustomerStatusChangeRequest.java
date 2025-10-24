package com.timetable.dto;

import javax.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalTime;

public class CustomerStatusChangeRequest {
    @NotBlank(message = "新状态不能为空")
    private String toStatus;
    
    private String notes;
    
    // 体验课程相关字段（可选）
    private String trialScheduleDate;
    private String trialStartTime;
    private String trialEndTime;
    private Long trialCoachId;

    public CustomerStatusChangeRequest() {
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

    public String getTrialScheduleDate() {
        return trialScheduleDate;
    }

    public void setTrialScheduleDate(String trialScheduleDate) {
        this.trialScheduleDate = trialScheduleDate;
    }

    public String getTrialStartTime() {
        return trialStartTime;
    }

    public void setTrialStartTime(String trialStartTime) {
        this.trialStartTime = trialStartTime;
    }

    public String getTrialEndTime() {
        return trialEndTime;
    }

    public void setTrialEndTime(String trialEndTime) {
        this.trialEndTime = trialEndTime;
    }

    public Long getTrialCoachId() {
        return trialCoachId;
    }

    public void setTrialCoachId(Long trialCoachId) {
        this.trialCoachId = trialCoachId;
    }
}

