package com.timetable.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 周实例课程实体类
 */
public class WeeklyInstanceSchedule {
    private Long id;
    private Long weeklyInstanceId;     // 所属周实例ID
    private Long templateScheduleId;   // 源固定课表课程ID（用于同步）
    private String studentName;
    private String subject;
    private String dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDate scheduleDate;    // 具体日期
    private String note;
    private Boolean isManualAdded;     // 是否为手动添加（非模板同步）
    private Boolean isModified;        // 是否被手动修改过
    private Boolean isOnLeave;         // 是否请假
    private String leaveReason;        // 请假原因
    private LocalDateTime leaveRequestedAt; // 请假申请时间
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 构造函数
    public WeeklyInstanceSchedule() {}

    public WeeklyInstanceSchedule(Long weeklyInstanceId, String studentName, String dayOfWeek, 
                                LocalTime startTime, LocalTime endTime, LocalDate scheduleDate) {
        this.weeklyInstanceId = weeklyInstanceId;
        this.studentName = studentName;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.scheduleDate = scheduleDate;
        this.isManualAdded = false;
        this.isModified = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 从模板课程创建实例课程的构造函数
    public WeeklyInstanceSchedule(Long weeklyInstanceId, Long templateScheduleId, String studentName, 
                                String subject, String dayOfWeek, LocalTime startTime, LocalTime endTime, 
                                LocalDate scheduleDate, String note) {
        this.weeklyInstanceId = weeklyInstanceId;
        this.templateScheduleId = templateScheduleId;
        this.studentName = studentName;
        this.subject = subject;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.scheduleDate = scheduleDate;
        this.note = note;
        this.isManualAdded = false;
        this.isModified = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getter 和 Setter 方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWeeklyInstanceId() {
        return weeklyInstanceId;
    }

    public void setWeeklyInstanceId(Long weeklyInstanceId) {
        this.weeklyInstanceId = weeklyInstanceId;
    }

    public Long getTemplateScheduleId() {
        return templateScheduleId;
    }

    public void setTemplateScheduleId(Long templateScheduleId) {
        this.templateScheduleId = templateScheduleId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public LocalDate getScheduleDate() {
        return scheduleDate;
    }

    public void setScheduleDate(LocalDate scheduleDate) {
        this.scheduleDate = scheduleDate;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Boolean getIsManualAdded() {
        return isManualAdded;
    }

    public void setIsManualAdded(Boolean isManualAdded) {
        this.isManualAdded = isManualAdded;
    }

    public Boolean getIsModified() {
        return isModified;
    }

    public void setIsModified(Boolean isModified) {
        this.isModified = isModified;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getIsOnLeave() {
        return isOnLeave;
    }

    public void setIsOnLeave(Boolean isOnLeave) {
        this.isOnLeave = isOnLeave;
    }

    public String getLeaveReason() {
        return leaveReason;
    }

    public void setLeaveReason(String leaveReason) {
        this.leaveReason = leaveReason;
    }

    public LocalDateTime getLeaveRequestedAt() {
        return leaveRequestedAt;
    }

    public void setLeaveRequestedAt(LocalDateTime leaveRequestedAt) {
        this.leaveRequestedAt = leaveRequestedAt;
    }
}
