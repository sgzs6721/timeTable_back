package com.timetable.dto;

/**
 * 学员操作请求DTO
 */
public class StudentOperationRequest {
    private String oldName;
    private String newName;
    private String aliasName;

    public StudentOperationRequest() {}

    public StudentOperationRequest(String oldName, String newName) {
        this.oldName = oldName;
        this.newName = newName;
    }

    public String getOldName() { return oldName; }
    public void setOldName(String oldName) { this.oldName = oldName; }
    public String getNewName() { return newName; }
    public void setNewName(String newName) { this.newName = newName; }
    public String getAliasName() { return aliasName; }
    public void setAliasName(String aliasName) { this.aliasName = aliasName; }
}





