package com.timetable.controller;

import com.timetable.repository.StudentNamesRepository;
import com.timetable.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student-names")
@CrossOrigin(origins = "*")
public class StudentNamesController {
    
    @Autowired
    private StudentNamesRepository studentNamesRepository;
    
    @Autowired
    private UserService userService;
    
    /**
     * 记录学生姓名使用
     */
    @PostMapping("/record")
    public ResponseEntity<Map<String, Object>> recordStudentName(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String username = authentication.getName();
            String studentName = request.get("name");
            
            if (studentName == null || studentName.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "学生姓名不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取用户ID
            Long userId = userService.findByUsername(username).getId();
            
            // 记录学生姓名
            studentNamesRepository.saveOrUpdate(studentName.trim(), userId);
            
            response.put("success", true);
            response.put("message", "学生姓名记录成功");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "记录失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 根据前缀搜索学生姓名
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchStudentNames(
            @RequestParam String prefix,
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String username = authentication.getName();
            Long userId = userService.findByUsername(username).getId();
            
            List<String> suggestions = studentNamesRepository.searchNamesByPrefix(prefix, userId, limit);
            
            response.put("success", true);
            response.put("data", suggestions);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "搜索失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取用户的所有学生姓名（按使用频率排序）
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllStudentNames(
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String username = authentication.getName();
            Long userId = userService.findByUsername(username).getId();
            
            List<String> names = studentNamesRepository.getAllNamesByUserId(userId, limit);
            
            response.put("success", true);
            response.put("data", names);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
} 