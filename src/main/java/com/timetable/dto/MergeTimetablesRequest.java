package com.timetable.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 合并课表请求DTO
 */
public class MergeTimetablesRequest {
    
    @NotEmpty(message = "课表ID列表不能为空")
    private List<Long> timetableIds;
    
    @NotBlank(message = "合并后的课表名称不能为空")
    private String mergedName;
    
    private String description;
    
    // 构造函数
    public MergeTimetablesRequest() {
    }
    
    public MergeTimetablesRequest(List<Long> timetableIds, String mergedName) {
        this.timetableIds = timetableIds;
        this.mergedName = mergedName;
    }
    
    // Getter和Setter方法
    public List<Long> getTimetableIds() {
        return timetableIds;
    }
    
    public void setTimetableIds(List<Long> timetableIds) {
        this.timetableIds = timetableIds;
    }
    
    public String getMergedName() {
        return mergedName;
    }
    
    public void setMergedName(String mergedName) {
        this.mergedName = mergedName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
} 