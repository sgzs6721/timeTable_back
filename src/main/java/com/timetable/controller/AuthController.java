package com.timetable.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.timetable.dto.ApiResponse;
import com.timetable.dto.ApplyOrganizationRequest;
import com.timetable.dto.AuthRequest;
import com.timetable.dto.BindPhoneRequest;
import com.timetable.dto.UserOrganizationRequestDTO;
import com.timetable.dto.UserRegistrationRequest;
import com.timetable.dto.WechatLoginRequest;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.repository.UserRepository;
import com.timetable.service.UserService;
import com.timetable.service.WechatLoginService;
import com.timetable.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/auth")
@Validated
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private WechatLoginService wechatLoginService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private com.timetable.service.UserOrganizationRequestService requestService;
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@Valid @RequestBody AuthRequest authRequest) {
        try {
            logger.info("用户尝试登录: {}", authRequest.getUsername());
            
            // 检查用户状态
            Map<String, Object> loginStatus = userService.getUserLoginStatus(authRequest.getUsername());
            boolean canLogin = (Boolean) loginStatus.get("canLogin");
            String statusMessage = (String) loginStatus.get("message");
            String userStatus = (String) loginStatus.get("status");
            
            if (!canLogin) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(statusMessage));
            }
            
            // 认证用户
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequest.getUsername(),
                            authRequest.getPassword()
                    )
            );
            
            // 获取用户信息
            Users user = userService.findByUsername(authRequest.getUsername());
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("用户不存在"));
            }
            
            // 生成JWT Token
            String token = jwtUtil.generateToken(user.getUsername());
            
            // 构建响应数据
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("user", convertUserToDTO(user));
            
            logger.info("用户登录成功: {}", user.getUsername());
            return ResponseEntity.ok(ApiResponse.success("登录成功", data));
            
        } catch (BadCredentialsException e) {
            logger.warn("用户登录失败，密码错误: {}", authRequest.getUsername());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户名或密码错误"));
        } catch (Exception e) {
            logger.error("登录过程中发生错误", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("登录失败，请稍后重试"));
        }
    }
    
    /**
     * 用户注册申请
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@Valid @RequestBody UserRegistrationRequest registrationRequest) {
        try {
            logger.info("用户尝试注册: {}", registrationRequest.getUsername());
            
            // 创建新用户（注册申请）
            Users newUser = userService.createUserRegistration(registrationRequest);
            
            // 构建响应数据（不返回token，因为需要等待审批）
            Map<String, Object> data = new HashMap<>();
            data.put("user", convertUserToDTO(newUser));
            data.put("message", "注册申请已提交，请等待管理员确认");
            
            logger.info("用户注册申请提交成功: {}", newUser.getUsername());
            return ResponseEntity.ok(ApiResponse.success("注册申请已提交，请等待管理员确认", data));
            
        } catch (IllegalArgumentException e) {
            logger.warn("注册申请失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("注册过程中发生错误", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("注册失败，请稍后重试"));
        }
    }
    
    /**
     * 验证Token
     */
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateToken(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Token格式错误"));
            }
            
            String token = authHeader.substring(7);
            String username = jwtUtil.extractUsername(token);
            
            if (username != null && jwtUtil.validateToken(token, username)) {
                Users user = userService.findByUsername(username);
                if (user != null) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("user", convertUserToDTO(user));
                    data.put("valid", true);
                    
                    return ResponseEntity.ok(ApiResponse.success("Token验证成功", data));
                }
            }
            
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Token无效"));
            
        } catch (Exception e) {
            logger.error("Token验证过程中发生错误", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Token验证失败"));
        }
    }
    
    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("用户未登录"));
            }
            
            String username = authentication.getName();
            Users user = userService.findByUsername(username);
            
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("用户不存在"));
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("user", convertUserToDTO(user));
            
            return ResponseEntity.ok(ApiResponse.success("获取用户信息成功", data));
            
        } catch (Exception e) {
            logger.error("获取用户信息过程中发生错误", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取用户信息失败"));
        }
    }
    
    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        try {
            // 清除安全上下文
            SecurityContextHolder.clearContext();
            
            return ResponseEntity.ok(ApiResponse.success("登出成功"));
            
        } catch (Exception e) {
            logger.error("登出过程中发生错误", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("登出失败"));
        }
    }
    
    /**
     * 更新用户资料
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProfile(@Valid @RequestBody Map<String, String> profileData) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("用户未登录"));
            }
            
            String currentUsername = authentication.getName();
            String newUsername = profileData.get("username");
            String nickname = profileData.get("nickname");
            
            if (newUsername == null || newUsername.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("用户名不能为空"));
            }
            
            // 更新用户名
            Users updatedUser;
            try {
                updatedUser = userService.updateUsername(currentUsername, newUsername);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(e.getMessage()));
            }
            
            if (updatedUser == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("更新失败"));
            }
            
            // 如果提供了昵称，也更新昵称
            if (nickname != null) {
                updatedUser = userService.updateNickname(updatedUser.getUsername(), nickname.trim());
            }
            
            // 如果用户名发生了变化，需要生成新的token
            String newToken = null;
            if (!newUsername.equals(currentUsername)) {
                newToken = jwtUtil.generateToken(newUsername);
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("user", convertUserToDTO(updatedUser));
            if (newToken != null) {
                data.put("token", newToken);
            }
            
            logger.info("用户资料更新成功: {} -> {}", currentUsername, newUsername);
            return ResponseEntity.ok(ApiResponse.success("用户资料更新成功", data));
            
        } catch (Exception e) {
            logger.error("更新用户资料过程中发生错误", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("更新失败，请稍后重试"));
        }
    }
    
    /**
     * 更新密码
     */
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(@Valid @RequestBody Map<String, String> passwordData) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("用户未登录"));
            }
            
            String username = authentication.getName();
            String oldPassword = passwordData.get("oldPassword");
            String newPassword = passwordData.get("newPassword");
            
            if (oldPassword == null || oldPassword.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("请输入当前密码"));
            }
            
            if (newPassword == null || newPassword.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("请输入新密码"));
            }
            
            if (newPassword.length() < 6) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("新密码至少6个字符"));
            }
            
            // 验证当前密码
            Users user = userService.findByUsername(username);
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("用户不存在"));
            }
            
            if (!userService.validatePassword(oldPassword, user.getPasswordHash())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("当前密码不正确"));
            }
            
            // 更新密码
            boolean updated = userService.updatePassword(username, newPassword);
            
            if (updated) {
                logger.info("用户密码更新成功: {}", username);
                return ResponseEntity.ok(ApiResponse.success("密码更新成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("密码更新失败"));
            }
            
        } catch (Exception e) {
            logger.error("更新密码过程中发生错误", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("密码更新失败，请稍后重试"));
        }
    }
    
    /**
     * 注销账号（软删除）
     */
    @DeleteMapping("/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("用户未登录"));
            }
            
            String username = authentication.getName();
            
            // 注销账号（软删除）
            boolean deactivated = userService.deactivateUser(username);
            if (deactivated) {
                // 清除安全上下文
                SecurityContextHolder.clearContext();
                logger.info("用户账号已注销: {}", username);
                return ResponseEntity.ok(ApiResponse.success("账号已成功注销"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("账号注销失败"));
            }
            
        } catch (Exception e) {
            logger.error("注销账号过程中发生错误", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("账号注销失败"));
        }
    }
    
    /**
     * 微信登录
     */
    @PostMapping("/wechat/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> wechatLogin(@Valid @RequestBody WechatLoginRequest wechatLoginRequest) {
        try {
            logger.info("用户尝试微信登录，授权码: {}", wechatLoginRequest.getCode());
            
            // 处理微信登录
            Map<String, Object> data = wechatLoginService.processWechatLogin(wechatLoginRequest.getCode());
            
            logger.info("微信登录成功");
            return ResponseEntity.ok(ApiResponse.success("微信登录成功", data));
            
        } catch (Exception e) {
            logger.error("微信登录过程中发生错误", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("微信登录失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取微信登录授权URL
     */
    @GetMapping("/wechat/auth-url")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWechatAuthUrl() {
        try {
            String authUrl = wechatLoginService.generateWechatAuthUrl();
            
            Map<String, Object> data = new HashMap<>();
            data.put("authUrl", authUrl);
            
            return ResponseEntity.ok(ApiResponse.success("获取微信授权URL成功", data));
            
        } catch (Exception e) {
            logger.error("获取微信授权URL过程中发生错误", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取微信授权URL失败"));
        }
    }
    
    /**
     * 微信授权回调接口
     */
    @GetMapping("/wechat/callback")
    public ResponseEntity<String> wechatCallback(@RequestParam String code, @RequestParam String state) {
        try {
            logger.info("收到微信授权回调，code: {}, state: {}", code, state);
            
            // 验证state参数
            if (!"timetable_wechat_login".equals(state)) {
                return ResponseEntity.badRequest()
                        .body("<html><body><h1>授权失败</h1><p>无效的state参数</p></body></html>");
            }
            
            // 处理微信登录
            Map<String, Object> loginResult = wechatLoginService.processWechatLogin(code);
            
            // 前端页面URL
            String frontendUrl = "https://timetable.devtesting.top";
            
            // 检查是否需要选择机构
            if (loginResult.containsKey("needSelectOrganization") && 
                (Boolean) loginResult.get("needSelectOrganization")) {
                // 需要选择机构，跳转到机构选择页面
                Map<String, Object> wechatUserInfo = (Map<String, Object>) loginResult.get("wechatUserInfo");
                String wechatUserInfoJson = objectMapper.writeValueAsString(wechatUserInfo);
                
                String htmlTemplate = "<!DOCTYPE html>" +
                    "<html>" +
                    "<head>" +
                        "<meta charset=\"UTF-8\">" +
                        "<title>选择机构</title>" +
                    "</head>" +
                    "<body>" +
                        "<script>" +
                            "const wechatUserInfo = %s;" +
                            "const frontendUrl = '%s';" +
                            "sessionStorage.setItem('wechatUserInfo', JSON.stringify(wechatUserInfo));" +
                            "window.location.href = frontendUrl + '/select-organization';" +
                        "</script>" +
                    "</body>" +
                    "</html>";
                
                String html = String.format(htmlTemplate, wechatUserInfoJson, frontendUrl);
                return ResponseEntity.ok(html);
            }
            
            // 检查是否有申请记录
            if (loginResult.containsKey("hasRequest") && 
                (Boolean) loginResult.get("hasRequest")) {
                String requestStatus = (String) loginResult.get("requestStatus");
                Map<String, Object> requestInfo = (Map<String, Object>) loginResult.get("requestInfo");
                Map<String, Object> wechatUserInfo = (Map<String, Object>) loginResult.get("wechatUserInfo");
                
                String requestInfoJson = objectMapper.writeValueAsString(requestInfo);
                String wechatUserInfoJson = objectMapper.writeValueAsString(wechatUserInfo);
                
                String htmlTemplate = "<!DOCTYPE html>" +
                    "<html>" +
                    "<head>" +
                        "<meta charset=\"UTF-8\">" +
                        "<title>申请状态</title>" +
                    "</head>" +
                    "<body>" +
                        "<script>" +
                            "const requestInfo = %s;" +
                            "const wechatUserInfo = %s;" +
                            "sessionStorage.setItem('requestInfo', JSON.stringify(requestInfo));" +
                            "sessionStorage.setItem('wechatUserInfo', JSON.stringify(wechatUserInfo));" +
                            "const frontendUrl = '%s';" +
                            "window.location.href = frontendUrl + '/application-status';" +
                        "</script>" +
                    "</body>" +
                    "</html>";
                
                String html = String.format(htmlTemplate, requestInfoJson, wechatUserInfoJson, frontendUrl);
                return ResponseEntity.ok(html);
            }
            
            // 已有机构，正常登录，直接跳转到Dashboard
            if (loginResult != null && loginResult.containsKey("token")) {
                String token = (String) loginResult.get("token");
                Map<String, Object> user = (Map<String, Object>) loginResult.get("user");
                
                String htmlTemplate = "<!DOCTYPE html>" +
                    "<html>" +
                    "<head>" +
                        "<meta charset=\"UTF-8\">" +
                        "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                        "<title>微信登录成功</title>" +
                        "<style>" +
                            "body { font-family: 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif; " +
                                "text-align: center; padding: 50px 20px; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); " +
                                "min-height: 100vh; margin: 0; }" +
                            ".container { background: white; border-radius: 10px; padding: 40px; max-width: 400px; " +
                                "margin: 0 auto; box-shadow: 0 10px 40px rgba(0,0,0,0.2); }" +
                            ".success { color: #52c41a; margin-bottom: 20px; }" +
                            ".info { margin: 20px 0; color: #666; }" +
                            ".info p { margin: 10px 0; }" +
                            ".avatar { width: 80px; height: 80px; border-radius: 50%%; margin: 0 auto 20px; }" +
                            ".loading { margin-top: 20px; color: #888; }" +
                        "</style>" +
                    "</head>" +
                    "<body>" +
                        "<div class=\"container\">" +
                            "<h1 class=\"success\">✓ 微信登录成功！</h1>" +
                            "%s" + // avatar
                            "<div class=\"info\">" +
                                "<p><strong>昵称：</strong>%s</p>" +
                            "</div>" +
                            "<div class=\"loading\">正在跳转...</div>" +
                        "</div>" +
                        "<script>" +
                            "const token = '%s';" +
                            "const frontendUrl = '%s';" +
                            "const user = %s;" +
                            "localStorage.setItem('token', token);" +
                            "localStorage.setItem('user', JSON.stringify(user));" +
                            "setTimeout(() => {" +
                                "window.location.href = frontendUrl + '/dashboard';" +
                            "}, 1000);" +
                        "</script>" +
                    "</body>" +
                    "</html>";
                
                // 构建头像HTML
                String avatarHtml = "";
                if (user.get("wechatAvatar") != null && !user.get("wechatAvatar").toString().isEmpty()) {
                    avatarHtml = String.format("<img src=\"%s\" class=\"avatar\" alt=\"头像\" />", 
                        user.get("wechatAvatar"));
                }
                
                logger.info("微信登录成功，跳转到Dashboard - Token: {}, FrontendUrl: {}", token, frontendUrl);
                
                String userNickname = user.get("nickname") != null ? user.get("nickname").toString() : "";
                
                // 将 user Map 转换为 JSON 字符串
                String userJson;
                try {
                    userJson = objectMapper.writeValueAsString(user);
                } catch (Exception e) {
                    logger.error("转换用户信息为JSON失败", e);
                    userJson = "{}";
                }
                
                String html = String.format(htmlTemplate,
                    avatarHtml,           // %s - avatar (HTML)
                    userNickname,         // %s - nickname (页面显示)
                    token,                // %s - token (JavaScript)
                    frontendUrl,          // %s - frontendUrl (JavaScript)
                    userJson              // %s - user JSON (JavaScript)
                );
                
                return ResponseEntity.ok(html);
            } else {
                return ResponseEntity.badRequest()
                        .body("<html><body><h1>登录失败</h1><p>微信登录处理失败</p></body></html>");
            }
            
        } catch (Exception e) {
            logger.error("微信授权回调处理失败", e);
            return ResponseEntity.internalServerError()
                    .body("<html><body><h1>登录失败</h1><p>系统错误：" + e.getMessage() + "</p></body></html>");
        }
    }
    
    /**
     * 绑定手机号
     */
    @PostMapping("/bind-phone")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bindPhone(
            @Valid @RequestBody BindPhoneRequest request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // 从token中获取用户名
            String token = authHeader.substring(7); // 去掉 "Bearer " 前缀
            String username = jwtUtil.extractUsername(token);
            
            logger.info("用户 {} 尝试绑定手机号: {}", username, request.getPhone());
            
            // 查找用户
            Users user = userRepository.findByUsername(username);
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("用户不存在"));
            }
            
            // 检查当前用户是否已绑定该手机号
            if (request.getPhone().equals(user.getPhone())) {
                logger.info("用户 {} 已绑定该手机号，无需重复绑定", username);
                Map<String, Object> data = new HashMap<>();
                data.put("user", convertUserToDTO(user));
                data.put("message", "手机号已绑定");
                return ResponseEntity.ok(ApiResponse.success("手机号已绑定", data));
            }
            
            // 检查手机号是否已被其他用户使用
            Users existingUser = userRepository.findByPhone(request.getPhone());
            if (existingUser != null && !existingUser.getId().equals(user.getId())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("该手机号已被其他用户使用"));
            }
            
            // 更新用户手机号
            userRepository.updatePhone(user.getId(), request.getPhone());
            
            // 重新查询用户信息
            user = userRepository.findById(user.getId());
            
            logger.info("用户 {} 成功绑定手机号", username);
            
            Map<String, Object> data = new HashMap<>();
            data.put("user", convertUserToDTO(user));
            data.put("message", "手机号绑定成功");
            
            return ResponseEntity.ok(ApiResponse.success("手机号绑定成功", data));
            
        } catch (Exception e) {
            logger.error("绑定手机号失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("绑定手机号失败: " + e.getMessage()));
        }
    }
    
    /**
     * 绑定微信到已有账号
     */
    @PostMapping("/wechat/bind-account")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bindWechatToAccount(
            @RequestBody Map<String, String> request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // 从token中获取当前微信用户信息
            String token = authHeader.substring(7);
            String currentUsername = jwtUtil.extractUsername(token);
            
            String targetUsername = request.get("username");
            String targetPassword = request.get("password");
            
            logger.info("微信用户 {} 尝试绑定到账号: {}", currentUsername, targetUsername);
            
            // 验证目标账号的用户名和密码
            try {
                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(targetUsername, targetPassword)
                );
            } catch (BadCredentialsException e) {
                logger.warn("绑定失败，目标账号密码错误: {}", targetUsername);
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("用户名或密码错误"));
            }
            
            // 获取当前微信用户
            Users wechatUser = userRepository.findByUsername(currentUsername);
            if (wechatUser == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("当前用户不存在"));
            }
            
            // 获取目标账号
            Users targetUser = userRepository.findByUsername(targetUsername);
            if (targetUser == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("目标账号不存在"));
            }
            
            // 检查目标账号是否已经绑定了微信
            if (targetUser.getWechatOpenid() != null && !targetUser.getWechatOpenid().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("该账号已绑定其他微信"));
            }
            
            // 检查当前微信是否已经绑定了其他账号（不是临时账号）
            if (!currentUsername.startsWith("wx_")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("当前微信已绑定其他账号"));
            }
            
            // 保存微信信息到变量中
            String wechatOpenid = wechatUser.getWechatOpenid();
            String wechatUnionid = wechatUser.getWechatUnionid();
            String wechatAvatar = wechatUser.getWechatAvatar();
            Byte wechatSex = wechatUser.getWechatSex();
            String wechatProvince = wechatUser.getWechatProvince();
            String wechatCity = wechatUser.getWechatCity();
            String wechatCountry = wechatUser.getWechatCountry();
            String wechatNickname = wechatUser.getNickname();
            
            // 先删除临时微信账号（避免 wechat_openid 唯一索引冲突）
            if (currentUsername.startsWith("wx_")) {
                logger.info("删除临时微信账号: {}", currentUsername);
                userRepository.deleteById(wechatUser.getId());
            }
            
            // 将微信信息绑定到目标账号
            targetUser.setWechatOpenid(wechatOpenid);
            targetUser.setWechatUnionid(wechatUnionid);
            targetUser.setWechatAvatar(wechatAvatar);
            targetUser.setWechatSex(wechatSex);
            targetUser.setWechatProvince(wechatProvince);
            targetUser.setWechatCity(wechatCity);
            targetUser.setWechatCountry(wechatCountry);
            
            // 如果目标账号没有昵称，使用微信昵称
            if (targetUser.getNickname() == null || targetUser.getNickname().isEmpty()) {
                targetUser.setNickname(wechatNickname);
            }
            
            targetUser.setUpdatedAt(java.time.LocalDateTime.now());
            userRepository.update(targetUser);
            
            // 生成新的token（使用目标账号）
            String newToken = jwtUtil.generateToken(targetUser.getUsername());
            
            logger.info("微信绑定成功，微信用户 {} 已绑定到账号 {}", currentUsername, targetUsername);
            
            Map<String, Object> data = new HashMap<>();
            data.put("token", newToken);
            data.put("user", convertUserToDTO(targetUser));
            data.put("message", "账号绑定成功");
            
            return ResponseEntity.ok(ApiResponse.success("账号绑定成功", data));
            
        } catch (BadCredentialsException e) {
            logger.warn("绑定失败，密码错误");
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户名或密码错误"));
        } catch (Exception e) {
            logger.error("绑定微信到账号失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("绑定失败: " + e.getMessage()));
        }
    }
    
    /**
     * 微信登录选择机构（直接加入机构，不需要审批）
     */
    @PostMapping("/wechat/select-organization")
    public ResponseEntity<ApiResponse<Map<String, Object>>> selectOrganization(
            @Valid @RequestBody Map<String, Object> requestBody) {
        try {
            Long organizationId = Long.valueOf(requestBody.get("organizationId").toString());
            
            @SuppressWarnings("unchecked")
            Map<String, Object> wechatUserInfo = (Map<String, Object>) requestBody.get("wechatUserInfo");
            
            if (wechatUserInfo == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("缺少微信用户信息"));
            }
            
            String wechatOpenid = wechatUserInfo.get("openid").toString();
            String wechatUnionid = wechatUserInfo.get("unionid") != null ? 
                wechatUserInfo.get("unionid").toString() : null;
            String wechatNickname = wechatUserInfo.get("nickname") != null ? 
                wechatUserInfo.get("nickname").toString() : null;
            String wechatAvatar = wechatUserInfo.get("headimgurl") != null ? 
                wechatUserInfo.get("headimgurl").toString() : null;
            Byte wechatSex = wechatUserInfo.get("sex") != null ? 
                Byte.valueOf(wechatUserInfo.get("sex").toString()) : null;
            String wechatProvince = wechatUserInfo.get("province") != null ? 
                wechatUserInfo.get("province").toString() : null;
            String wechatCity = wechatUserInfo.get("city") != null ? 
                wechatUserInfo.get("city").toString() : null;
            String wechatCountry = wechatUserInfo.get("country") != null ? 
                wechatUserInfo.get("country").toString() : null;
            
            // 检查用户是否已存在
            Users existingUser = userRepository.findByWechatOpenid(wechatOpenid);
            Users user;
            
            if (existingUser != null) {
                // 更新用户的机构信息
                existingUser.setOrganizationId(organizationId);
                existingUser.setUpdatedAt(java.time.LocalDateTime.now());
                userRepository.update(existingUser);
                user = existingUser;
                logger.info("更新已存在用户的机构信息: userId={}, organizationId={}", user.getId(), organizationId);
            } else {
                // 创建新用户
                Users newUser = new Users();
                newUser.setUsername("wx_" + wechatOpenid);
                newUser.setPasswordHash("");
                newUser.setRole("USER");
                newUser.setNickname(wechatNickname);
                newUser.setStatus("APPROVED");
                newUser.setOrganizationId(organizationId);
                
                newUser.setWechatOpenid(wechatOpenid);
                newUser.setWechatUnionid(wechatUnionid);
                newUser.setWechatAvatar(wechatAvatar);
                newUser.setWechatSex(wechatSex);
                newUser.setWechatProvince(wechatProvince);
                newUser.setWechatCity(wechatCity);
                newUser.setWechatCountry(wechatCountry);
                
                newUser.setIsDeleted((byte) 0);
                newUser.setCreatedAt(java.time.LocalDateTime.now());
                newUser.setUpdatedAt(java.time.LocalDateTime.now());
                
                userRepository.save(newUser);
                user = userRepository.findByWechatOpenid(wechatOpenid);
                logger.info("创建新用户并关联机构: userId={}, organizationId={}", user.getId(), organizationId);
            }
            
            // 生成JWT Token
            String token = jwtUtil.generateToken(user.getUsername());
            
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("user", convertUserToDTO(user));
            
            logger.info("微信用户选择机构成功: openid={}, organizationId={}", wechatOpenid, organizationId);
            return ResponseEntity.ok(ApiResponse.success("加入机构成功", data));
            
        } catch (Exception e) {
            logger.error("选择机构失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("选择机构失败: " + e.getMessage()));
        }
    }
    
    /**
     * 提交机构申请
     */
    @PostMapping("/apply-organization")
    public ResponseEntity<ApiResponse<UserOrganizationRequestDTO>> applyOrganization(
            @Valid @RequestBody Map<String, Object> requestBody) {
        try {
            Long organizationId = Long.valueOf(requestBody.get("organizationId").toString());
            String applyReason = requestBody.get("applyReason") != null ? 
                requestBody.get("applyReason").toString() : null;
            
            @SuppressWarnings("unchecked")
            Map<String, Object> wechatUserInfo = (Map<String, Object>) requestBody.get("wechatUserInfo");
            
            if (wechatUserInfo == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("缺少微信用户信息"));
            }
            
            String wechatOpenid = wechatUserInfo.get("openid").toString();
            String wechatUnionid = wechatUserInfo.get("unionid") != null ? 
                wechatUserInfo.get("unionid").toString() : null;
            String wechatNickname = wechatUserInfo.get("nickname") != null ? 
                wechatUserInfo.get("nickname").toString() : null;
            String wechatAvatar = wechatUserInfo.get("headimgurl") != null ? 
                wechatUserInfo.get("headimgurl").toString() : null;
            Byte wechatSex = wechatUserInfo.get("sex") != null ? 
                Byte.valueOf(wechatUserInfo.get("sex").toString()) : null;
            
            UserOrganizationRequestDTO result = requestService.createRequest(
                wechatOpenid, wechatUnionid, wechatNickname, wechatAvatar, 
                wechatSex, organizationId, applyReason
            );
            
            logger.info("用户提交机构申请成功：openid={}, organizationId={}", wechatOpenid, organizationId);
            return ResponseEntity.ok(ApiResponse.success("申请已提交，请等待管理员审批", result));
            
        } catch (RuntimeException e) {
            logger.warn("提交机构申请失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("提交机构申请失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("提交申请失败，请稍后重试"));
        }
    }
    
    /**
     * 转换用户对象为DTO（不包含敏感信息）
     */
    private Map<String, Object> convertUserToDTO(Users user) {
        Map<String, Object> userDTO = new HashMap<>();
        userDTO.put("id", user.getId());
        userDTO.put("username", user.getUsername());
        userDTO.put("nickname", user.getNickname());
        userDTO.put("role", user.getRole());
        userDTO.put("position", user.getPosition());
        userDTO.put("phone", user.getPhone());
        userDTO.put("hasPhone", user.getPhone() != null && !user.getPhone().isEmpty());
        userDTO.put("wechatAvatar", user.getWechatAvatar());
        userDTO.put("createdAt", user.getCreatedAt());
        userDTO.put("updatedAt", user.getUpdatedAt());
        return userDTO;
    }
}