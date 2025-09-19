package com.timetable.dto;

import javax.validation.constraints.NotNull;

/**
 * 请假请求DTO
 */
public class LeaveRequest {
    @NotNull(message = "课程ID不能为空")
    private Long scheduleId;
    
    private String leaveReason; // 请假原因，非必填

    public LeaveRequest() {}

    public LeaveRequest(Long scheduleId, String leaveReason) {
        this.scheduleId = scheduleId;
        this.leaveReason = leaveReason;
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public String getLeaveReason() {
        return leaveReason;
    }

    public void setLeaveReason(String leaveReason) {
        this.leaveReason = leaveReason;
    }
}
