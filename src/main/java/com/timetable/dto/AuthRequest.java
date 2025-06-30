package com.timetable.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 认证请求DTO
 */
public class AuthRequest {
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50字符之间")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度必须在6-100字符之间")
    private String password;
    
    @Email(message = "邮箱格式不正确")
    private String email; // 注册时使用
    
    // 构造函数
    public AuthRequest() {
    }
    
    public AuthRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
    
    public AuthRequest(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }
    
    // Getter和Setter方法
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    @Override
    public String toString() {
        return "AuthRequest{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
} 