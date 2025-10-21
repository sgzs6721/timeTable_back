package com.timetable.dto;

import java.time.LocalDateTime;

public class CustomerStatusHistoryDTO {
    private Long id;
    private Long customerId;
    private String fromStatus;
    private String fromStatusText;
    private String toStatus;
    private String toStatusText;
    private String notes;
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdAt;

    public CustomerStatusHistoryDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }

    public String getFromStatusText() {
        return fromStatusText;
    }

    public void setFromStatusText(String fromStatusText) {
        this.fromStatusText = fromStatusText;
    }

    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }

    public String getToStatusText() {
        return toStatusText;
    }

    public void setToStatusText(String toStatusText) {
        this.toStatusText = toStatusText;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
}

