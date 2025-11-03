package com.timetable.dto;

/**
 * 体验课程信息
 */
public class TrialScheduleInfo {
    private Long scheduleId;      // 课程ID
    private Long timetableId;     // 课表ID（如果是周实例，则是实例ID）
    private String sourceType;    // 来源类型：weekly_instance 或 schedule
    
    public TrialScheduleInfo() {
    }
    
    public TrialScheduleInfo(Long scheduleId, Long timetableId, String sourceType) {
        this.scheduleId = scheduleId;
        this.timetableId = timetableId;
        this.sourceType = sourceType;
    }
    
    public Long getScheduleId() {
        return scheduleId;
    }
    
    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }
    
    public Long getTimetableId() {
        return timetableId;
    }
    
    public void setTimetableId(Long timetableId) {
        this.timetableId = timetableId;
    }
    
    public String getSourceType() {
        return sourceType;
    }
    
    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }
}

