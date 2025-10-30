package com.timetable.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class UserRegistrationRequest {
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度必须在6-100个字符之间")
    private String password;
    
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;
    
    @NotBlank(message = "机构代码不能为空")
    @Size(max = 50, message = "机构代码长度不能超过50个字符")
    private String organizationCode;

    // 构造函数
    public UserRegistrationRequest() {}

    public UserRegistrationRequest(String username, String password, String nickname) {
        this.username = username;
        this.password = password;
        this.nickname = nickname;
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

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getOrganizationCode() {
        return organizationCode;
    }

    public void setOrganizationCode(String organizationCode) {
        this.organizationCode = organizationCode;
    }
} 