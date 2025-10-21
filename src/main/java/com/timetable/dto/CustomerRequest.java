package com.timetable.dto;

import java.time.LocalDateTime;

public class CustomerRequest {
    private String childName;
    private String grade;
    private String parentPhone;
    private String parentRelation;
    private String availableTime;
    private String source;
    private String status;
    private String notes;
    private LocalDateTime nextContactTime;
    private LocalDateTime visitTime;
    private Long assignedSalesId;

    public CustomerRequest() {
    }

    public String getChildName() {
        return childName;
    }

    public void setChildName(String childName) {
        this.childName = childName;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getParentPhone() {
        return parentPhone;
    }

    public void setParentPhone(String parentPhone) {
        this.parentPhone = parentPhone;
    }

    public String getParentRelation() {
        return parentRelation;
    }

    public void setParentRelation(String parentRelation) {
        this.parentRelation = parentRelation;
    }

    public String getAvailableTime() {
        return availableTime;
    }

    public void setAvailableTime(String availableTime) {
        this.availableTime = availableTime;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getNextContactTime() {
        return nextContactTime;
    }

    public void setNextContactTime(LocalDateTime nextContactTime) {
        this.nextContactTime = nextContactTime;
    }

    public LocalDateTime getVisitTime() {
        return visitTime;
    }

    public void setVisitTime(LocalDateTime visitTime) {
        this.visitTime = visitTime;
    }

    public Long getAssignedSalesId() {
        return assignedSalesId;
    }

    public void setAssignedSalesId(Long assignedSalesId) {
        this.assignedSalesId = assignedSalesId;
    }
}
