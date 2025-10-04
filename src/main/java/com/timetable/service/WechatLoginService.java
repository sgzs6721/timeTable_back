package com.timetable.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.timetable.dto.WechatAccessToken;
import com.timetable.dto.WechatUserInfo;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.repository.UserRepository;
import com.timetable.util.JwtUtil;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信登录服务
 */
@Service
public class WechatLoginService {
    
    private static final Logger logger = LoggerFactory.getLogger(WechatLoginService.class);
    
    @Value("${wechat.app-id}")
    private String appId;
    
    @Value("${wechat.app-secret}")
    private String appSecret;
    
    @Value("${wechat.redirect-uri}")
    private String redirectUri;
    
    @Value("${wechat.scope}")
    private String scope;
    
    @Value("${wechat.state}")
    private String state;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 微信API地址
    private static final String WECHAT_ACCESS_TOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/access_token";
    private static final String WECHAT_USER_INFO_URL = "https://api.weixin.qq.com/sns/userinfo";
    
    /**
     * 通过授权码获取微信访问令牌
     */
    public WechatAccessToken getAccessToken(String code) throws IOException {
        String url = String.format("%s?appid=%s&secret=%s&code=%s&grant_type=authorization_code",
                WECHAT_ACCESS_TOKEN_URL, appId, appSecret, code);
        
        Request request = new Request.Builder()
                .url(url)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("获取微信访问令牌失败: " + response);
            }
            
            String responseBody = response.body().string();
            logger.info("微信访问令牌响应: {}", responseBody);
            
            return objectMapper.readValue(responseBody, WechatAccessToken.class);
        }
    }
    
    /**
     * 通过访问令牌获取微信用户信息
     */
    public WechatUserInfo getUserInfo(String accessToken, String openid) throws IOException {
        String url = String.format("%s?access_token=%s&openid=%s",
                WECHAT_USER_INFO_URL, accessToken, openid);
        
        Request request = new Request.Builder()
                .url(url)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("获取微信用户信息失败: " + response);
            }
            
            String responseBody = response.body().string();
            logger.info("微信用户信息响应: {}", responseBody);
            
            return objectMapper.readValue(responseBody, WechatUserInfo.class);
        }
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
            
            // 3. 查找或创建用户
            Users user = findOrCreateUser(wechatUserInfo);
            
            // 4. 生成JWT Token
            String token = jwtUtil.generateToken(user.getUsername());
            
            // 5. 构建响应数据
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("user", convertUserToDTO(user));
            data.put("isNewUser", user.getCreatedAt().equals(user.getUpdatedAt()));
            
            return data;
            
        } catch (Exception e) {
            logger.error("微信登录处理失败", e);
            throw new RuntimeException("微信登录失败: " + e.getMessage());
        }
    }
    
    /**
     * 查找或创建用户
     */
    private Users findOrCreateUser(WechatUserInfo wechatUserInfo) {
        // 使用openid作为用户名查找用户
        Users existingUser = userRepository.findByUsername(wechatUserInfo.getOpenid());
        
        if (existingUser != null) {
            // 更新用户信息
            existingUser.setNickname(wechatUserInfo.getNickname());
            existingUser.setUpdatedAt(java.time.LocalDateTime.now());
            userRepository.update(existingUser);
            return existingUser;
        } else {
            // 创建新用户
            Users newUser = new Users();
            newUser.setUsername(wechatUserInfo.getOpenid());
            newUser.setPasswordHash(""); // 微信登录用户不需要密码
            newUser.setRole("USER");
            newUser.setNickname(wechatUserInfo.getNickname());
            newUser.setStatus("APPROVED"); // 微信登录用户直接批准
            newUser.setIsDeleted((byte) 0);
            newUser.setCreatedAt(java.time.LocalDateTime.now());
            newUser.setUpdatedAt(java.time.LocalDateTime.now());
            
            userRepository.save(newUser);
            return newUser;
        }
    }
    
    /**
     * 转换用户实体为DTO
     */
    private Map<String, Object> convertUserToDTO(Users user) {
        Map<String, Object> userDTO = new HashMap<>();
        userDTO.put("id", user.getId());
        userDTO.put("username", user.getUsername());
        userDTO.put("role", user.getRole());
        userDTO.put("nickname", user.getNickname());
        userDTO.put("status", user.getStatus());
        userDTO.put("createdAt", user.getCreatedAt());
        userDTO.put("updatedAt", user.getUpdatedAt());
        return userDTO;
    }
    
    /**
     * 生成微信登录授权URL
     */
    public String generateWechatAuthUrl() {
        return String.format("https://open.weixin.qq.com/connect/oauth2/authorize?appid=%s&redirect_uri=%s&response_type=code&scope=%s&state=%s#wechat_redirect",
                appId, redirectUri, scope, state);
    }
}
