package com.timetable.repository;

import com.timetable.model.User;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用户Repository - 内存实现（用于测试）
 */
@Repository
public class UserRepository {
    
    private final Map<String, User> usersByUsername = new HashMap<>();
    private final Map<String, User> usersByEmail = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    /**
     * 根据用户名查找用户
     */
    public User findByUsername(String username) {
        return usersByUsername.get(username);
    }
    
    /**
     * 根据邮箱查找用户
     */
    public User findByEmail(String email) {
        return usersByEmail.get(email);
    }
    
    /**
     * 检查用户名是否存在
     */
    public boolean existsByUsername(String username) {
        return usersByUsername.containsKey(username);
    }
    
    /**
     * 检查邮箱是否存在
     */
    public boolean existsByEmail(String email) {
        return usersByEmail.containsKey(email);
    }
    
    /**
     * 保存用户
     */
    public User save(User user) {
        if (user.getId() == null) {
            user.setId(idGenerator.getAndIncrement());
        }
        
        usersByUsername.put(user.getUsername(), user);
        if (user.getEmail() != null) {
            usersByEmail.put(user.getEmail(), user);
        }
        
        return user;
    }
} 