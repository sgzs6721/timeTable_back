package com.timetable.service;

import com.timetable.dto.WechatAccessToken;
import com.timetable.dto.WechatUserInfo;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 简化的微信登录服务（用于编译测试）
 */
public class SimpleWechatLoginService {
    
    private String appId;
    private String appSecret;
    
    // 微信API地址
    private static final String WECHAT_ACCESS_TOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/access_token";
    private static final String WECHAT_USER_INFO_URL = "https://api.weixin.qq.com/sns/userinfo";
    
    public SimpleWechatLoginService(String appId, String appSecret) {
        this.appId = appId;
        this.appSecret = appSecret;
    }
    
    /**
     * 通过授权码获取微信访问令牌
     */
    public WechatAccessToken getAccessToken(String code) throws IOException {
        // 这里只是示例，实际需要HTTP请求
        WechatAccessToken token = new WechatAccessToken();
        token.setAccessToken("test_access_token");
        token.setOpenid("test_openid");
        return token;
    }
    
    /**
     * 通过访问令牌获取微信用户信息
     */
    public WechatUserInfo getUserInfo(String accessToken, String openid) throws IOException {
        // 这里只是示例，实际需要HTTP请求
        WechatUserInfo userInfo = new WechatUserInfo();
        userInfo.setOpenid(openid);
        userInfo.setNickname("测试用户");
        return userInfo;
    }
    
    /**
     * 处理微信登录
     */
    public Map<String, Object> processWechatLogin(String code) {
        try {
            // 1. 获取微信访问令牌
            WechatAccessToken accessToken = getAccessToken(code);
            if (accessToken.getAccessToken() == null) {
                throw new RuntimeException("获取微信访问令牌失败");
            }
            
            // 2. 获取微信用户信息
            WechatUserInfo wechatUserInfo = getUserInfo(accessToken.getAccessToken(), accessToken.getOpenid());
            if (wechatUserInfo.getOpenid() == null) {
                throw new RuntimeException("获取微信用户信息失败");
            }
            
            // 3. 构建响应数据
            Map<String, Object> data = new HashMap<>();
            data.put("token", "test_jwt_token");
            data.put("user", createUserDTO(wechatUserInfo));
            data.put("isNewUser", true);
            
            return data;
            
        } catch (Exception e) {
            throw new RuntimeException("微信登录失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成微信登录授权URL
     */
    public String generateWechatAuthUrl() {
        String redirectUri = "http://121.36.91.199:8080/timetable/api/auth/wechat/callback";
        String scope = "snsapi_userinfo";
        String state = "timetable_wechat_login";
        
        return String.format("https://open.weixin.qq.com/connect/oauth2/authorize?appid=%s&redirect_uri=%s&response_type=code&scope=%s&state=%s#wechat_redirect",
                appId, redirectUri, scope, state);
    }
    
    /**
     * 创建用户DTO
     */
    private Map<String, Object> createUserDTO(WechatUserInfo wechatUserInfo) {
        Map<String, Object> userDTO = new HashMap<>();
        userDTO.put("id", 1L);
        userDTO.put("username", wechatUserInfo.getOpenid());
        userDTO.put("nickname", wechatUserInfo.getNickname());
        userDTO.put("role", "USER");
        userDTO.put("status", "APPROVED");
        return userDTO;
    }
}
