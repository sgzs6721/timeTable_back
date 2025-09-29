package com.timetable.dto;

/**
 * 微信访问令牌DTO
 */
public class WechatAccessToken {
    
    private String accessToken;
    
    private Integer expiresIn;
    
    private String refreshToken;
    
    private String openid;
    
    private String scope;
    
    private String unionid;
    
    public WechatAccessToken() {}
    
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public Integer getExpiresIn() {
        return expiresIn;
    }
    
    public void setExpiresIn(Integer expiresIn) {
        this.expiresIn = expiresIn;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    public String getOpenid() {
        return openid;
    }
    
    public void setOpenid(String openid) {
        this.openid = openid;
    }
    
    public String getScope() {
        return scope;
    }
    
    public void setScope(String scope) {
        this.scope = scope;
    }
    
    public String getUnionid() {
        return unionid;
    }
    
    public void setUnionid(String unionid) {
        this.unionid = unionid;
    }
    
    @Override
    public String toString() {
        return "WechatAccessToken{" +
                "accessToken='" + accessToken + '\'' +
                ", expiresIn=" + expiresIn +
                ", refreshToken='" + refreshToken + '\'' +
                ", openid='" + openid + '\'' +
                ", scope='" + scope + '\'' +
                ", unionid='" + unionid + '\'' +
                '}';
    }
}
