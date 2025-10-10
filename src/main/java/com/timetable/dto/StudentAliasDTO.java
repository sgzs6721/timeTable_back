package com.timetable.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 学员别名DTO
 */
public class StudentAliasDTO {
    private Long id;
    private String aliasName;
    private List<String> studentNames;
    private Long coachId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public StudentAliasDTO() {}

    public StudentAliasDTO(Long id, String aliasName, List<String> studentNames, Long coachId) {
        this.id = id;
        this.aliasName = aliasName;
        this.studentNames = studentNames;
        this.coachId = coachId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAliasName() { return aliasName; }
    public void setAliasName(String aliasName) { this.aliasName = aliasName; }
    public List<String> getStudentNames() { return studentNames; }
    public void setStudentNames(List<String> studentNames) { this.studentNames = studentNames; }
    public Long getCoachId() { return coachId; }
    public void setCoachId(Long coachId) { this.coachId = coachId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
