package com.timetable.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "wechat")
public class WechatMpConfig {
    
    private String appId;
    private String appSecret;
    private Mp mp;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public Mp getMp() {
        return mp;
    }

    public void setMp(Mp mp) {
        this.mp = mp;
    }

    public static class Mp {
        private Boolean enabled;
        private String templateId;
        private Boolean tokenCacheEnabled;
        private Integer tokenCacheTime;
        private Integer pushRetryMax;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getTemplateId() {
            return templateId;
        }

        public void setTemplateId(String templateId) {
            this.templateId = templateId;
        }

        public Boolean getTokenCacheEnabled() {
            return tokenCacheEnabled;
        }

        public void setTokenCacheEnabled(Boolean tokenCacheEnabled) {
            this.tokenCacheEnabled = tokenCacheEnabled;
        }

        public Integer getTokenCacheTime() {
            return tokenCacheTime;
        }

        public void setTokenCacheTime(Integer tokenCacheTime) {
            this.tokenCacheTime = tokenCacheTime;
        }

        public Integer getPushRetryMax() {
            return pushRetryMax;
        }

        public void setPushRetryMax(Integer pushRetryMax) {
            this.pushRetryMax = pushRetryMax;
        }
    }
}

