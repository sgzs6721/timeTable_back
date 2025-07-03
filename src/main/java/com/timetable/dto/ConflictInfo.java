package com.timetable.dto;

import com.timetable.generated.tables.pojos.Schedules;

/**
 * 冲突信息DTO
 */
public class ConflictInfo {
    
    /**
     * 新排课在请求列表中的索引
     */
    private Integer newScheduleIndex;
    
    /**
     * 新排课信息
     */
    private ScheduleRequest newSchedule;
    
    /**
     * 冲突的现有排课
     */
    private Schedules existingSchedule;
    
    /**
     * 另一个新排课的索引（当新排课之间冲突时）
     */
    private Integer otherNewScheduleIndex;
    
    /**
     * 另一个新排课信息（当新排课之间冲突时）
     */
    private ScheduleRequest otherNewSchedule;
    
    /**
     * 冲突类型
     * STUDENT_TIME_CONFLICT: 学生时间冲突
     * TIME_SLOT_CONFLICT: 时间段占用冲突
     * NEW_SCHEDULE_CONFLICT: 新排课之间的冲突
     */
    private String conflictType;
    
    /**
     * 冲突描述
     */
    private String conflictDescription;
    
    // Constructors
    public ConflictInfo() {}
    
    public ConflictInfo(Integer newScheduleIndex, ScheduleRequest newSchedule, 
                       Schedules existingSchedule, String conflictType, String conflictDescription) {
        this.newScheduleIndex = newScheduleIndex;
        this.newSchedule = newSchedule;
        this.existingSchedule = existingSchedule;
        this.conflictType = conflictType;
        this.conflictDescription = conflictDescription;
    }
    
    // Getters and Setters
    public Integer getNewScheduleIndex() {
        return newScheduleIndex;
    }
    
    public void setNewScheduleIndex(Integer newScheduleIndex) {
        this.newScheduleIndex = newScheduleIndex;
    }
    
    public ScheduleRequest getNewSchedule() {
        return newSchedule;
    }
    
    public void setNewSchedule(ScheduleRequest newSchedule) {
        this.newSchedule = newSchedule;
    }
    
    public Schedules getExistingSchedule() {
        return existingSchedule;
    }
    
    public void setExistingSchedule(Schedules existingSchedule) {
        this.existingSchedule = existingSchedule;
    }
    
    public Integer getOtherNewScheduleIndex() {
        return otherNewScheduleIndex;
    }
    
    public void setOtherNewScheduleIndex(Integer otherNewScheduleIndex) {
        this.otherNewScheduleIndex = otherNewScheduleIndex;
    }
    
    public ScheduleRequest getOtherNewSchedule() {
        return otherNewSchedule;
    }
    
    public void setOtherNewSchedule(ScheduleRequest otherNewSchedule) {
        this.otherNewSchedule = otherNewSchedule;
    }
    
    public String getConflictType() {
        return conflictType;
    }
    
    public void setConflictType(String conflictType) {
        this.conflictType = conflictType;
    }
    
    public String getConflictDescription() {
        return conflictDescription;
    }
    
    public void setConflictDescription(String conflictDescription) {
        this.conflictDescription = conflictDescription;
    }
    
    @Override
    public String toString() {
        return "ConflictInfo{" +
                "newScheduleIndex=" + newScheduleIndex +
                ", conflictType='" + conflictType + '\'' +
                ", conflictDescription='" + conflictDescription + '\'' +
                '}';
    }
}
