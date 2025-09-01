package com.timetable.dto;

import javax.validation.constraints.NotNull;

/**
 * 复制课表请求DTO
 */
public class CopyTimetableRequest {
    
    @NotNull(message = "源课表ID不能为空")
    private Long sourceTimetableId;
    
    @NotNull(message = "目标用户ID不能为空")
    private Long targetUserId;
    
    private String newTimetableName; // 可选，新课表名称
    
    public CopyTimetableRequest() {}
    
    public CopyTimetableRequest(Long sourceTimetableId, Long targetUserId, String newTimetableName) {
        this.sourceTimetableId = sourceTimetableId;
        this.targetUserId = targetUserId;
        this.newTimetableName = newTimetableName;
    }
    
    public Long getSourceTimetableId() {
        return sourceTimetableId;
    }
    
    public void setSourceTimetableId(Long sourceTimetableId) {
        this.sourceTimetableId = sourceTimetableId;
    }
    
    public Long getTargetUserId() {
        return targetUserId;
    }
    
    public void setTargetUserId(Long targetUserId) {
        this.targetUserId = targetUserId;
    }
    
    public String getNewTimetableName() {
        return newTimetableName;
    }
    
    public void setNewTimetableName(String newTimetableName) {
        this.newTimetableName = newTimetableName;
    }
}
