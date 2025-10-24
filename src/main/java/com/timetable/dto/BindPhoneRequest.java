package com.timetable.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 绑定手机号请求DTO
 */
public class BindPhoneRequest {
    
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    
    // 可选：验证码（如果需要短信验证）
    private String verificationCode;
    
    public BindPhoneRequest() {}
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getVerificationCode() {
        return verificationCode;
    }
    
    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }
    
    @Override
    public String toString() {
        return "BindPhoneRequest{" +
                "phone='" + phone + '\'' +
                ", verificationCode='" + (verificationCode != null ? "***" : null) + '\'' +
                '}';
    }
}





