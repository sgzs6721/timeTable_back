package com.timetable.dto;

import javax.validation.constraints.NotNull;

/**
 * 调换课程请求DTO
 */
public class SwapSchedulesRequest {
    
    @NotNull(message = "第一个课程ID不能为空")
    private Long scheduleId1;
    
    @NotNull(message = "第二个课程ID不能为空")
    private Long scheduleId2;
    
    public SwapSchedulesRequest() {}
    
    public SwapSchedulesRequest(Long scheduleId1, Long scheduleId2) {
        this.scheduleId1 = scheduleId1;
        this.scheduleId2 = scheduleId2;
    }
    
    public Long getScheduleId1() {
        return scheduleId1;
    }
    
    public void setScheduleId1(Long scheduleId1) {
        this.scheduleId1 = scheduleId1;
    }
    
    public Long getScheduleId2() {
        return scheduleId2;
    }
    
    public void setScheduleId2(Long scheduleId2) {
        this.scheduleId2 = scheduleId2;
    }
}
