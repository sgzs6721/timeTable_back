package com.timetable.service;

import com.timetable.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.jooq.DSLContext;
import com.timetable.generated.tables.pojos.Users;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户服务
 */
@Service
public class UserService implements UserDetailsService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    
    @Autowired
    private DSLContext dsl;
    
    /**
     * Spring Security用户认证
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Users user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
        
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                authorities
        );
    }
    
    /**
     * 根据用户名查找用户
     */
    public Users findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    /**
     * 根据用户ID查找用户
     */
    public Users findById(Long id) {
        return userRepository.findById(id);
    }
    
    /**
     * 检查用户名是否存在
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    /**
     * 创建新用户
     */
    public Users createUser(String username, String password, String role) {
        String encodedPassword = passwordEncoder.encode(password);
        Users user = new Users();
        user.setUsername(username);
        user.setPasswordHash(encodedPassword);
        user.setRole(role);
        userRepository.save(user);
        return userRepository.findByUsername(username);
    }
    
    /**
     * 验证密码
     */
    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
    
    /**
     * 更新用户名
     */
    public Users updateUsername(String currentUsername, String newUsername) {
        Users user = userRepository.findByUsername(currentUsername);
        if (user == null) {
            return null;
        }
        
        user.setUsername(newUsername);
        userRepository.update(user);
        return userRepository.findByUsername(newUsername);
    }
    
    /**
     * 更新密码
     */
    public boolean updatePassword(String username, String newPassword) {
        Users user = userRepository.findByUsername(username);
        if (user == null) {
            return false;
        }
        
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPasswordHash(encodedPassword);
        userRepository.update(user);
        return true;
    }
} 