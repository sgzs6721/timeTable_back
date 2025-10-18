package com.timetable.dto;

import java.time.LocalDateTime;

public class PendingUserDTO {
    private Long id;
    private String username;
    private String nickname;
    private String position;
    private String status;
    private LocalDateTime createdAt;

    public PendingUserDTO() {}

    public PendingUserDTO(Long id, String username, String nickname, String position, String status, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.position = position;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getter和Setter方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
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
} 