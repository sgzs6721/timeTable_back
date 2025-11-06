package com.timetable.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 排课更新 DTO，可部分更新。
 */
public class UpdateScheduleRequest {

    private String studentName;
    private String subject;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer weekNumber;
    private LocalDate scheduleDate;
    private String note;
    private Boolean isTrial;
    private Boolean isHalfHour;
    private Boolean isTimeBlock;

    public UpdateScheduleRequest() {
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

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
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

    public Integer getWeekNumber() {
        return weekNumber;
    }

    public void setWeekNumber(Integer weekNumber) {
        this.weekNumber = weekNumber;
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

    public Boolean getIsTimeBlock() {
        return isTimeBlock;
    }

    public void setIsTimeBlock(Boolean isTimeBlock) {
        this.isTimeBlock = isTimeBlock;
    }
} 