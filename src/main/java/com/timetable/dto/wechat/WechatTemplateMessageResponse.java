package com.timetable.dto.wechat;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WechatTemplateMessageResponse {
    
    @JsonProperty("errcode")
    private Integer errcode;
    
    @JsonProperty("errmsg")
    private String errmsg;
    
    @JsonProperty("msgid")
    private Long msgid;

    public Integer getErrcode() {
        return errcode;
    }

    public void setErrcode(Integer errcode) {
        this.errcode = errcode;
    }

    public String getErrmsg() {
        return errmsg;
    }

    public void setErrmsg(String errmsg) {
        this.errmsg = errmsg;
    }

    public Long getMsgid() {
        return msgid;
    }

    public void setMsgid(Long msgid) {
        this.msgid = msgid;
    }

    public boolean isSuccess() {
        return errcode != null && errcode == 0;
    }
}

