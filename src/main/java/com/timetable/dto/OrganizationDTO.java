package com.timetable.dto;

import java.time.LocalDateTime;

/**
 * 机构DTO
 */
public class OrganizationDTO {
    private Long id;
    private String name;
    private String code;
    private String address;
    private String contactPhone;
    private String contactPerson;
    private String status;
    private LocalDateTime createdAt;
    private NotificationSettingsDTO notificationSettings;

    public OrganizationDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public NotificationSettingsDTO getNotificationSettings() {
        return notificationSettings;
    }

    public void setNotificationSettings(NotificationSettingsDTO notificationSettings) {
        this.notificationSettings = notificationSettings;
    }
}

