package com.timetable.dto;

import javax.validation.constraints.NotNull;

/**
 * 审批机构申请请求
 */
public class ApproveOrganizationRequestDTO {
    
    @NotNull(message = "申请ID不能为空")
    private Long requestId;
    
    @NotNull(message = "审批结果不能为空")
    private Boolean approved; // true: 同意, false: 拒绝
    
    private String rejectReason; // 拒绝理由（拒绝时必填）
    
    private String defaultRole; // 默认角色（同意时可选，默认为USER）
    
    private String defaultPosition; // 默认职位（同意时可选，默认为COACH）

    public ApproveOrganizationRequestDTO() {
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public Boolean getApproved() {
        return approved;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public String getDefaultRole() {
        return defaultRole;
    }

    public void setDefaultRole(String defaultRole) {
        this.defaultRole = defaultRole;
    }

    public String getDefaultPosition() {
        return defaultPosition;
    }

    public void setDefaultPosition(String defaultPosition) {
        this.defaultPosition = defaultPosition;
    }
}

