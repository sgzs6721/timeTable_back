package com.timetable.dto.wechat;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;

public class WechatTemplateMessage {
    
    @JsonProperty("touser")
    private String toUser;
    
    @JsonProperty("template_id")
    private String templateId;
    
    @JsonProperty("url")
    private String url;
    
    @JsonProperty("data")
    private Map<String, TemplateDataItem> data;

    public WechatTemplateMessage() {
        this.data = new HashMap<>();
    }

    public String getToUser() {
        return toUser;
    }

    public void setToUser(String toUser) {
        this.toUser = toUser;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, TemplateDataItem> getData() {
        return data;
    }

    public void setData(Map<String, TemplateDataItem> data) {
        this.data = data;
    }

    public void addData(String key, String value, String color) {
        this.data.put(key, new TemplateDataItem(value, color));
    }

    public void addData(String key, String value) {
        this.data.put(key, new TemplateDataItem(value, "#173177"));
    }

    public static class TemplateDataItem {
        private String value;
        private String color;

        public TemplateDataItem() {
        }

        public TemplateDataItem(String value, String color) {
            this.value = value;
            this.color = color;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }
    }
}

