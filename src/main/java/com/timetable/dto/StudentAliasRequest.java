package com.timetable.dto;

import java.util.List;

/**
 * 学员别名请求DTO
 */
public class StudentAliasRequest {
    private String aliasName;
    private List<String> studentNames;

    public StudentAliasRequest() {}

    public StudentAliasRequest(String aliasName, List<String> studentNames) {
        this.aliasName = aliasName;
        this.studentNames = studentNames;
    }

    public String getAliasName() { return aliasName; }
    public void setAliasName(String aliasName) { this.aliasName = aliasName; }
    public List<String> getStudentNames() { return studentNames; }
    public void setStudentNames(List<String> studentNames) { this.studentNames = studentNames; }
}



