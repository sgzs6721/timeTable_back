package com.timetable.dto;

import javax.validation.constraints.NotBlank;

public class CustomerStatusChangeRequest {
    @NotBlank(message = "新状态不能为空")
    private String toStatus;
    
    private String notes;

    public CustomerStatusChangeRequest() {
    }

    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

