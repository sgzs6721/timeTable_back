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
            logger.info("开始处理微信登录，授权码: {}", code);
            
            // 检查依赖注入是否成功
            if (jwtUtil == null) {
                logger.error("jwtUtil 未注入");
                throw new RuntimeException("系统配置错误: JWT工具未初始化");
            }
            if (userRepository == null) {
                logger.error("userRepository 未注入");
                throw new RuntimeException("系统配置错误: 用户仓库未初始化");
            }
            if (appId == null || appId.isEmpty()) {
                logger.error("appId 配置为空");
                throw new RuntimeException("系统配置错误: 微信AppID未配置");
            }
            if (appSecret == null || appSecret.isEmpty()) {
                logger.error("appSecret 配置为空");
                throw new RuntimeException("系统配置错误: 微信AppSecret未配置");
            }
            
            logger.info("依赖检查通过，AppID: {}", appId);
            
            // 1. 获取微信访问令牌
            logger.info("步骤1: 获取微信访问令牌");
            WechatAccessToken accessToken = getAccessToken(code);
            if (accessToken == null) {
                logger.error("获取微信访问令牌失败: accessToken 为 null");
                throw new RuntimeException("获取微信访问令牌失败: 返回结果为空");
            }
            if (accessToken.getAccessToken() == null) {
                logger.error("获取微信访问令牌失败: accessToken.getAccessToken() 为 null");
                throw new RuntimeException("获取微信访问令牌失败: access_token 字段为空");
            }
            logger.info("成功获取访问令牌，openid: {}", accessToken.getOpenid());
            
            // 2. 获取微信用户信息
            logger.info("步骤2: 获取微信用户信息");
            WechatUserInfo wechatUserInfo = getUserInfo(accessToken.getAccessToken(), accessToken.getOpenid());
            if (wechatUserInfo == null) {
                logger.error("获取微信用户信息失败: wechatUserInfo 为 null");
                throw new RuntimeException("获取微信用户信息失败: 返回结果为空");
            }
            if (wechatUserInfo.getOpenid() == null) {
                logger.error("获取微信用户信息失败: openid 为 null");
                throw new RuntimeException("获取微信用户信息失败: openid 字段为空");
            }
            logger.info("成功获取用户信息，昵称: {}", wechatUserInfo.getNickname());
            
            // 3. 查找或创建用户
            logger.info("步骤3: 查找或创建用户");
            Users user = findOrCreateUser(wechatUserInfo);
            if (user == null) {
                logger.error("查找或创建用户失败: user 为 null");
                throw new RuntimeException("查找或创建用户失败");
            }
            logger.info("用户处理完成，用户名: {}", user.getUsername());
            
            // 4. 生成JWT Token
            logger.info("步骤4: 生成JWT Token");
            String token = jwtUtil.generateToken(user.getUsername());
            logger.info("JWT Token 生成成功");
            
            // 5. 判断是否需要绑定手机号
            boolean needBindPhone = (user.getPhone() == null || user.getPhone().isEmpty());
            logger.info("是否需要绑定手机号: {}", needBindPhone);
            
            // 6. 构建响应数据
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("user", convertUserToDTO(user));
            
            // 安全地判断是否是新用户
            boolean isNewUser = false;
            if (user.getCreatedAt() != null && user.getUpdatedAt() != null) {
                isNewUser = user.getCreatedAt().equals(user.getUpdatedAt());
            }
            data.put("isNewUser", isNewUser);
            data.put("needBindPhone", needBindPhone);
            
            logger.info("微信登录处理成功");
            return data;
            
        } catch (Exception e) {
            logger.error("微信登录处理失败", e);
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = e.getClass().getSimpleName() + ": " + e.toString();
            }
            throw new RuntimeException("微信登录失败: " + errorMsg, e);
        }
    }
    
    /**
     * 查找或创建用户
     */
    private Users findOrCreateUser(WechatUserInfo wechatUserInfo) {
        if (wechatUserInfo == null) {
            logger.error("findOrCreateUser: wechatUserInfo 为 null");
            throw new RuntimeException("微信用户信息为空");
        }
        if (wechatUserInfo.getOpenid() == null || wechatUserInfo.getOpenid().isEmpty()) {
            logger.error("findOrCreateUser: openid 为空");
            throw new RuntimeException("微信OpenID为空");
        }
        
        logger.info("查找或创建用户，OpenID: {}", wechatUserInfo.getOpenid());
        
        // 使用openid查找用户（通过wechat_openid字段）
        Users existingUser = userRepository.findByWechatOpenid(wechatUserInfo.getOpenid());
        
        if (existingUser != null) {
            // 更新用户微信信息
            logger.info("更新已存在用户的微信信息，用户ID: {}", existingUser.getId());
            // 只在临时微信账号或没有昵称时才更新昵称
            if (existingUser.getUsername().startsWith("wx_") || existingUser.getNickname() == null || existingUser.getNickname().isEmpty()) {
                existingUser.setNickname(wechatUserInfo.getNickname());
            }
            existingUser.setWechatAvatar(wechatUserInfo.getHeadimgurl());
            existingUser.setWechatSex(wechatUserInfo.getSex() != null ? wechatUserInfo.getSex().byteValue() : null);
            existingUser.setWechatProvince(wechatUserInfo.getProvince());
            existingUser.setWechatCity(wechatUserInfo.getCity());
            existingUser.setWechatCountry(wechatUserInfo.getCountry());
            existingUser.setWechatUnionid(wechatUserInfo.getUnionid());
            existingUser.setUpdatedAt(java.time.LocalDateTime.now());
            
            logger.info("更新用户信息到数据库");
            userRepository.update(existingUser);
            logger.info("用户信息更新成功");
            return existingUser;
        } else {
            // 创建新用户
            logger.info("创建新微信用户，OpenID: {}", wechatUserInfo.getOpenid());
            Users newUser = new Users();
            // 使用 "wx_" + openid 作为用户名，避免与普通用户冲突
            newUser.setUsername("wx_" + wechatUserInfo.getOpenid());
            newUser.setPasswordHash(""); // 微信登录用户不需要密码
            newUser.setRole("USER");
            newUser.setNickname(wechatUserInfo.getNickname());
            newUser.setStatus("APPROVED"); // 微信登录用户直接批准
            
            // 保存微信相关信息
            newUser.setWechatOpenid(wechatUserInfo.getOpenid());
            newUser.setWechatUnionid(wechatUserInfo.getUnionid());
            newUser.setWechatAvatar(wechatUserInfo.getHeadimgurl());
            newUser.setWechatSex(wechatUserInfo.getSex() != null ? wechatUserInfo.getSex().byteValue() : null);
            newUser.setWechatProvince(wechatUserInfo.getProvince());
            newUser.setWechatCity(wechatUserInfo.getCity());
            newUser.setWechatCountry(wechatUserInfo.getCountry());
            
            newUser.setIsDeleted((byte) 0);
            newUser.setCreatedAt(java.time.LocalDateTime.now());
            newUser.setUpdatedAt(java.time.LocalDateTime.now());
            
            logger.info("保存新用户到数据库");
            userRepository.save(newUser);
            
            // 重新查询保存后的用户以获取生成的ID
            Users savedUser = userRepository.findByWechatOpenid(wechatUserInfo.getOpenid());
            if (savedUser == null) {
                logger.error("保存用户后查询失败");
                throw new RuntimeException("保存用户失败");
            }
            logger.info("新用户保存成功，ID: {}, 用户名: {}", savedUser.getId(), savedUser.getUsername());
            return savedUser;
        }
    }
    
    /**
     * 转换用户实体为DTO
     */
    private Map<String, Object> convertUserToDTO(Users user) {
        if (user == null) {
            logger.error("convertUserToDTO: user 为 null");
            throw new RuntimeException("用户对象为空");
        }
        
        Map<String, Object> userDTO = new HashMap<>();
        userDTO.put("id", user.getId());
        userDTO.put("username", user.getUsername());
        userDTO.put("role", user.getRole());
        userDTO.put("nickname", user.getNickname());
        userDTO.put("status", user.getStatus());
        userDTO.put("phone", user.getPhone());
        userDTO.put("hasPhone", user.getPhone() != null && !user.getPhone().isEmpty());
        
        // 微信相关信息
        userDTO.put("wechatAvatar", user.getWechatAvatar());
        userDTO.put("wechatOpenid", user.getWechatOpenid());
        userDTO.put("wechatProvince", user.getWechatProvince());
        userDTO.put("wechatCity", user.getWechatCity());
        
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
