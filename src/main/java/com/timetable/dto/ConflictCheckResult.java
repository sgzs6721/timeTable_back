package com.timetable.dto;

import java.util.List;

/**
 * 冲突检测结果DTO
 */
public class ConflictCheckResult {

    /**
     * 是否有冲突
     */
    private boolean hasConflicts;

    /**
     * 冲突列表
     */
    private List<ConflictInfo> conflicts;

    /**
     * 冲突总数
     */
    private int totalConflicts;

    /**
     * 检测摘要信息
     */
    private String summary;

    // Constructors
    public ConflictCheckResult() {}

    public ConflictCheckResult(boolean hasConflicts, List<ConflictInfo> conflicts, int totalConflicts) {
        this.hasConflicts = hasConflicts;
        this.conflicts = conflicts;
        this.totalConflicts = totalConflicts;
        this.summary = generateSummary();
    }

    // Getters and Setters
    public boolean isHasConflicts() {
        return hasConflicts;
    }

    public void setHasConflicts(boolean hasConflicts) {
        this.hasConflicts = hasConflicts;
    }

    public List<ConflictInfo> getConflicts() {
        return conflicts;
    }

    public void setConflicts(List<ConflictInfo> conflicts) {
        this.conflicts = conflicts;
        this.summary = generateSummary();
    }

    public int getTotalConflicts() {
        return totalConflicts;
    }

    public void setTotalConflicts(int totalConflicts) {
        this.totalConflicts = totalConflicts;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    /**
     * 生成冲突摘要
     */
    private String generateSummary() {
        if (!hasConflicts || conflicts == null || conflicts.isEmpty()) {
            return "无冲突";
        }

        return "发现冲突";
    }

    @Override
    public String toString() {
        return "ConflictCheckResult{" +
                "hasConflicts=" + hasConflicts +
                ", totalConflicts=" + totalConflicts +
                ", summary='" + summary + '\'' +
                '}';
    }
}
