package com.timetable.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 文本输入请求DTO
 */
public class TextInputRequest {
    
    @NotBlank(message = "输入文本不能为空")
    @Size(max = 1000, message = "输入文本长度不能超过1000字符")
    private String text;
    
    // 构造函数
    public TextInputRequest() {
    }
    
    public TextInputRequest(String text) {
        this.text = text;
    }
    
    // Getter和Setter方法
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    @Override
    public String toString() {
        return "TextInputRequest{" +
                "text='" + text + '\'' +
                '}';
    }
} 