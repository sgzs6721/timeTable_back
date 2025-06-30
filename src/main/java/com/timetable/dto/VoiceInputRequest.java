package com.timetable.dto;

import javax.validation.constraints.NotBlank;

/**
 * 语音输入请求DTO
 */
public class VoiceInputRequest {
    
    @NotBlank(message = "课表ID不能为空")
    private String timetableId;
    
    // 注意：音频文件通过MultipartFile传递，不在此DTO中定义
    
    // 构造函数
    public VoiceInputRequest() {
    }
    
    public VoiceInputRequest(String timetableId) {
        this.timetableId = timetableId;
    }
    
    // Getter和Setter方法
    public String getTimetableId() {
        return timetableId;
    }
    
    public void setTimetableId(String timetableId) {
        this.timetableId = timetableId;
    }
    
    @Override
    public String toString() {
        return "VoiceInputRequest{" +
                "timetableId='" + timetableId + '\'' +
                '}';
    }
} 