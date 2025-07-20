package com.timetable.entity;

import java.time.LocalDateTime;

public class StudentNames {
    private Long id;
    private String name;
    private Long userId;
    private Integer usageCount;
    private LocalDateTime firstUsedAt;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 构造函数
    public StudentNames() {}

    public StudentNames(String name, Long userId) {
        this.name = name;
        this.userId = userId;
        this.usageCount = 1;
        this.firstUsedAt = LocalDateTime.now();
        this.lastUsedAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getter和Setter方法
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }

    public LocalDateTime getFirstUsedAt() {
        return firstUsedAt;
    }

    public void setFirstUsedAt(LocalDateTime firstUsedAt) {
        this.firstUsedAt = firstUsedAt;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
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