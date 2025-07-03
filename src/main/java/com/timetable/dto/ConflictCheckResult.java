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
        
        long studentConflicts = conflicts.stream()
            .filter(c -> "STUDENT_TIME_CONFLICT".equals(c.getConflictType()))
            .count();
            
        long timeSlotConflicts = conflicts.stream()
            .filter(c -> "TIME_SLOT_CONFLICT".equals(c.getConflictType()))
            .count();
            
        long newScheduleConflicts = conflicts.stream()
            .filter(c -> "NEW_SCHEDULE_CONFLICT".equals(c.getConflictType()))
            .count();
        
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("发现 %d 个冲突：", totalConflicts));
        
        if (studentConflicts > 0) {
            summary.append(String.format(" 学生时间冲突 %d 个", studentConflicts));
        }
        
        if (timeSlotConflicts > 0) {
            if (studentConflicts > 0) summary.append("，");
            summary.append(String.format(" 时间段占用冲突 %d 个", timeSlotConflicts));
        }
        
        if (newScheduleConflicts > 0) {
            if (studentConflicts > 0 || timeSlotConflicts > 0) summary.append("，");
            summary.append(String.format(" 新排课冲突 %d 个", newScheduleConflicts));
        }
        
        return summary.toString();
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
