package com.timetable.service;

import com.timetable.repository.UserRepository;
import com.timetable.repository.TimetableRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户服务
 */
@Service
public class UserService implements UserDetailsService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TimetableRepository timetableRepository;
    
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
        // 设置创建和更新时间
        user.setCreatedAt(java.time.LocalDateTime.now());
        user.setUpdatedAt(java.time.LocalDateTime.now());
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
        // 手动设置更新时间
        user.setUpdatedAt(java.time.LocalDateTime.now());
        userRepository.update(user);
        return userRepository.findByUsername(newUsername);
    }

    /**
     * 更新用户昵称
     */
    public Users updateNickname(String username, String nickname) {
        Users user = userRepository.findByUsername(username);
        if (user == null) {
            return null;
        }
        
        user.setNickname(nickname);
        // 手动设置更新时间
        user.setUpdatedAt(java.time.LocalDateTime.now());
        userRepository.update(user);
        return user;
    }

    /**
     * 管理员更新用户昵称
     */
    public Users updateUserNickname(Long userId, String nickname) {
        Users user = userRepository.findById(userId);
        if (user == null) {
            return null;
        }
        
        user.setNickname(nickname);
        user.setUpdatedAt(java.time.LocalDateTime.now());
        userRepository.update(user);
        return user;
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
        // 手动设置更新时间
        user.setUpdatedAt(java.time.LocalDateTime.now());
        userRepository.update(user);
        return true;
    }
    
    /**
     * 软删除用户账号（暂时注释，等数据库迁移完成后启用）
     */
    public boolean deactivateUser(String username) {
        Users user = userRepository.findByUsername(username);
        if (user == null) {
            return false;
        }
        
        // 检查是否已经软删除
        if (user.getIsDeleted() != null && user.getIsDeleted() == 1) {
            return false;  // 已经删除了
        }
        
        // 软删除：将is_deleted字段置为1
        user.setIsDeleted((byte) 1);
        user.setDeletedAt(java.time.LocalDateTime.now());
        user.setUpdatedAt(java.time.LocalDateTime.now());
        userRepository.update(user);
        
        return true;
    }
    
    /**
     * 获取所有用户列表（管理员用）
     */
    public List<Map<String, Object>> getAllUsersForAdmin() {
        List<Users> users = userRepository.findAllActiveUsers();
        List<Map<String, Object>> userDTOs = new ArrayList<>();
        
        for (Users user : users) {
            userDTOs.add(convertUserToDTO(user));
        }
        
        return userDTOs;
    }
    
    /**
     * 更新用户权限
     */
    public Map<String, Object> updateUserRole(Long userId, String newRole) {
        Users user = userRepository.findById(userId);
        if (user == null) {
            return null;
        }
        
        user.setRole(newRole);
        user.setUpdatedAt(java.time.LocalDateTime.now());
        userRepository.update(user);
        
        return convertUserToDTO(user);
    }
    
    /**
     * 重置用户密码
     */
    public boolean resetUserPassword(Long userId, String newPassword) {
        Users user = userRepository.findById(userId);
        if (user == null) {
            return false;
        }
        
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPasswordHash(encodedPassword);
        user.setUpdatedAt(java.time.LocalDateTime.now());
        userRepository.update(user);
        
        return true;
    }


    /**
     * 软删除用户
     */
    public void softDeleteUser(Long userId, String currentUsername) {
        Users userToDelete = userRepository.findById(userId);
        if (userToDelete == null) {
            throw new IllegalArgumentException("要删除的用户不存在");
        }

        Users currentUser = userRepository.findByUsername(currentUsername);
        if (currentUser == null) {
            // 正常情况下，执行此操作的用户肯定存在
            throw new IllegalArgumentException("操作用户不存在");
        }

        // 检查是否在尝试删除自己
        if (userToDelete.getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("不能删除自己的账户");
        }

        // 检查是否在尝试删除另一个ADMIN
        if ("ADMIN".equals(userToDelete.getRole())) {
            throw new IllegalArgumentException("不能删除管理员账户");
        }

        userToDelete.setIsDeleted((byte) 1);
        userToDelete.setDeletedAt(java.time.LocalDateTime.now());
        userToDelete.setUpdatedAt(java.time.LocalDateTime.now());
        userRepository.update(userToDelete);
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
        userDTO.put("createdAt", user.getCreatedAt());
        userDTO.put("updatedAt", user.getUpdatedAt());
        return userDTO;
    }
} 