package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.AuthRequest;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.service.UserService;
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
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@Valid @RequestBody AuthRequest authRequest) {
        try {
            logger.info("用户尝试登录: {}", authRequest.getUsername());
            
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
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@Valid @RequestBody AuthRequest authRequest) {
        try {
            logger.info("用户尝试注册: {}", authRequest.getUsername());
            
            // 检查用户名是否已存在
            if (userService.existsByUsername(authRequest.getUsername())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("用户名已存在"));
            }
            
            // 创建新用户（不再传email）
            Users newUser = userService.createUser(
                    authRequest.getUsername(),
                    authRequest.getPassword(),
                    "USER"
            );
            
            // 生成JWT Token
            String token = jwtUtil.generateToken(newUser.getUsername());
            
            // 构建响应数据
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("user", convertUserToDTO(newUser));
            
            logger.info("用户注册成功: {}", newUser.getUsername());
            return ResponseEntity.ok(ApiResponse.success("注册成功", data));
            
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
            
            if (newUsername == null || newUsername.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("用户名不能为空"));
            }
            
            // 检查新用户名是否已存在（除了当前用户）
            if (!newUsername.equals(currentUsername) && userService.existsByUsername(newUsername)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("用户名已存在"));
            }
            
            // 更新用户名
            Users updatedUser = userService.updateUsername(currentUsername, newUsername);
            
            if (updatedUser == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("更新失败"));
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
     * 转换用户对象为DTO（不包含敏感信息）
     */
    private Map<String, Object> convertUserToDTO(Users user) {
        Map<String, Object> userDTO = new HashMap<>();
        userDTO.put("id", user.getId());
        userDTO.put("username", user.getUsername());
        userDTO.put("role", user.getRole());
        userDTO.put("createdAt", user.getCreatedAt());
        userDTO.put("updatedAt", user.getUpdatedAt());
        return userDTO;
    }
}