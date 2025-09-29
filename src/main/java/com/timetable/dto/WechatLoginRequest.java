package com.timetable.dto;

/**
 * 微信登录请求DTO
 */
public class WechatLoginRequest {
    
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
