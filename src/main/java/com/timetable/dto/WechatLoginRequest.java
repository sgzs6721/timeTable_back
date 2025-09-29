package com.timetable.dto;

import javax.validation.constraints.NotBlank;

/**
 * 微信登录请求DTO
 */
public class WechatLoginRequest {
    
    @NotBlank(message = "授权码不能为空")
    private String code;
    
    private String state;
    
    public WechatLoginRequest() {}
    
    public WechatLoginRequest(String code, String state) {
        this.code = code;
        this.state = state;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
    }
    
    @Override
    public String toString() {
        return "WechatLoginRequest{" +
                "code='" + code + '\'' +
                ", state='" + state + '\'' +
                '}';
    }
}
