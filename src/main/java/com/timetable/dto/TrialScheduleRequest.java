package com.timetable.dto;

import javax.validation.constraints.NotNull;

/**
 * 体验课程请求
 */
public class TrialScheduleRequest {
    
    @NotNull(message = "教练ID不能为空")
    private Long coachId;
    
    @NotNull(message = "学员姓名不能为空")
    private String studentName;
    
    @NotNull(message = "课程日期不能为空")
    private String scheduleDate;
    
    @NotNull(message = "开始时间不能为空")
    private String startTime;
    
    @NotNull(message = "结束时间不能为空")
    private String endTime;
    
    private Boolean isTrial;
    
    private Boolean isHalfHour;
    
    private String customerPhone;

    // Getters and Setters
    public Long getCoachId() {
        return coachId;
    }

    public void setCoachId(Long coachId) {
        this.coachId = coachId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getScheduleDate() {
        return scheduleDate;
    }

    public void setScheduleDate(String scheduleDate) {
        this.scheduleDate = scheduleDate;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public Boolean getIsTrial() {
        return isTrial;
    }

    public void setIsTrial(Boolean isTrial) {
        this.isTrial = isTrial;
    }

    public Boolean getIsHalfHour() {
        return isHalfHour;
    }

    public void setIsHalfHour(Boolean isHalfHour) {
        this.isHalfHour = isHalfHour;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }
}

