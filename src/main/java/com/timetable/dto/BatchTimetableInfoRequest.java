package com.timetable.dto;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量获取课表信息请求DTO
 */
public class BatchTimetableInfoRequest {
    
    @NotEmpty(message = "课表ID列表不能为空")
    private List<Long> timetableIds;
    
    public List<Long> getTimetableIds() {
        return timetableIds;
    }
    
    public void setTimetableIds(List<Long> timetableIds) {
        this.timetableIds = timetableIds;
    }
} 