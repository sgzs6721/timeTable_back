package com.timetable.dto;

import javax.validation.constraints.NotNull;

/**
 * 申请加入机构请求
 */
public class ApplyOrganizationRequest {
    
    @NotNull(message = "机构ID不能为空")
    private Long organizationId;
    
    private String applyReason;

    public ApplyOrganizationRequest() {
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public String getApplyReason() {
        return applyReason;
    }

    public void setApplyReason(String applyReason) {
        this.applyReason = applyReason;
    }
}

