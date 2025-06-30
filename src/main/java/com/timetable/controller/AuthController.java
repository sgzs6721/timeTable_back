package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.AuthRequest;
import com.timetable.model.User;
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
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
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
            User user = userService.findByUsername(authRequest.getUsername());
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
            
            // 检查邮箱是否已存在
            if (authRequest.getEmail() != null && userService.existsByEmail(authRequest.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("邮箱已被注册"));
            }
            
            // 创建新用户
            User newUser = userService.createUser(
                    authRequest.getUsername(),
                    authRequest.getPassword(),
                    authRequest.getEmail(),
                    User.UserRole.USER
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
                User user = userService.findByUsername(username);
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
            User user = userService.findByUsername(username);
            
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
     * 转换用户对象为DTO（不包含敏感信息）
     */
    private Map<String, Object> convertUserToDTO(User user) {
        Map<String, Object> userDTO = new HashMap<>();
        userDTO.put("id", user.getId());
        userDTO.put("username", user.getUsername());
        userDTO.put("email", user.getEmail());
        userDTO.put("role", user.getRole().toString().toLowerCase());
        userDTO.put("createdAt", user.getCreatedAt());
        return userDTO;
    }
} 