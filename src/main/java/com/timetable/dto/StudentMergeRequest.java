package com.timetable.dto;

import java.util.List;

/**
 * 学员合并请求DTO
 */
public class StudentMergeRequest {
    private String displayName;
    private List<String> studentNames;

    public StudentMergeRequest() {}

    public StudentMergeRequest(String displayName, List<String> studentNames) {
        this.displayName = displayName;
        this.studentNames = studentNames;
    }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public List<String> getStudentNames() { return studentNames; }
    public void setStudentNames(List<String> studentNames) { this.studentNames = studentNames; }
}
