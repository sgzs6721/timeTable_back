package com.timetable.dto;

import java.time.LocalDateTime;

public class CustomerDTO {
    private Long id;
    private String childName;
    private String childGender;
    private Integer childAge;
    private String grade;
    private String parentPhone;
    private String wechat;
    private String parentRelation;
    private String availableTime;
    private String source;
    private String status;
    private String statusText; // 状态的中文显示
    private String notes;
    private LocalDateTime nextContactTime;
    private LocalDateTime visitTime;
    private Long assignedSalesId;
    private String assignedSalesName;
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String lastStatusChangeNote; // 最后一次状态流转备注
    private LocalDateTime lastStatusChangeTime; // 最后一次状态流转时间

    public CustomerDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChildName() {
        return childName;
    }

    public void setChildName(String childName) {
        this.childName = childName;
    }

    public String getChildGender() {
        return childGender;
    }

    public void setChildGender(String childGender) {
        this.childGender = childGender;
    }

    public Integer getChildAge() {
        return childAge;
    }

    public void setChildAge(Integer childAge) {
        this.childAge = childAge;
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

    public String getWechat() {
        return wechat;
    }

    public void setWechat(String wechat) {
        this.wechat = wechat;
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

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
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

    public String getAssignedSalesName() {
        return assignedSalesName;
    }

    public void setAssignedSalesName(String assignedSalesName) {
        this.assignedSalesName = assignedSalesName;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
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

    public String getLastStatusChangeNote() {
        return lastStatusChangeNote;
    }

    public void setLastStatusChangeNote(String lastStatusChangeNote) {
        this.lastStatusChangeNote = lastStatusChangeNote;
    }

    public LocalDateTime getLastStatusChangeTime() {
        return lastStatusChangeTime;
    }

    public void setLastStatusChangeTime(LocalDateTime lastStatusChangeTime) {
        this.lastStatusChangeTime = lastStatusChangeTime;
    }
}
