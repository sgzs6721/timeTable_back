package com.timetable.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 学员合并DTO
 */
public class StudentMergeDTO {
    private Long id;
    private String displayName;
    private List<String> studentNames;
    private Long coachId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public StudentMergeDTO() {}

    public StudentMergeDTO(Long id, String displayName, List<String> studentNames, Long coachId) {
        this.id = id;
        this.displayName = displayName;
        this.studentNames = studentNames;
        this.coachId = coachId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public List<String> getStudentNames() { return studentNames; }
    public void setStudentNames(List<String> studentNames) { this.studentNames = studentNames; }
    public Long getCoachId() { return coachId; }
    public void setCoachId(Long coachId) { this.coachId = coachId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
