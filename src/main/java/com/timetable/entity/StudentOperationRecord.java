package com.timetable.entity;

import java.time.LocalDateTime;

/**
 * 学员操作记录实体
 */
public class StudentOperationRecord {
    private Long id;
    private Long coachId;
    private Long organizationId;
    private String operationType; // RENAME, DELETE, ASSIGN_ALIAS, MERGE
    private Long studentId; // 学员ID，引用student_names表
    private String oldName;
    private String newName; // 对于重命名操作，存储新名称；对于其他操作，存储操作描述
    private String details; // 操作详情，JSON格式
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public StudentOperationRecord() {}
    
    public StudentOperationRecord(Long coachId, String operationType, String oldName, String newName, String details) {
        this.coachId = coachId;
        this.operationType = operationType;
        this.oldName = oldName;
        this.newName = newName;
        this.details = details;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public StudentOperationRecord(Long coachId, String operationType, Long studentId, String oldName, String newName, String details) {
        this.coachId = coachId;
        this.operationType = operationType;
        this.studentId = studentId;
        this.oldName = oldName;
        this.newName = newName;
        this.details = details;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getCoachId() {
        return coachId;
    }
    
    public void setCoachId(Long coachId) {
        this.coachId = coachId;
    }
    
    public Long getOrganizationId() {
        return organizationId;
    }
    
    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }
    
    public Long getStudentId() {
        return studentId;
    }
    
    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }
    
    public String getOperationType() {
        return operationType;
    }
    
    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }
    
    public String getOldName() {
        return oldName;
    }
    
    public void setOldName(String oldName) {
        this.oldName = oldName;
    }
    
    public String getNewName() {
        return newName;
    }
    
    public void setNewName(String newName) {
        this.newName = newName;
    }
    
    public String getDetails() {
        return details;
    }
    
    public void setDetails(String details) {
        this.details = details;
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
}