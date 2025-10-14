package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.StudentAliasDTO;
import com.timetable.dto.StudentAliasRequest;
import com.timetable.service.StudentAliasService;
import com.timetable.service.UserService;
import com.timetable.generated.tables.pojos.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController
@RequestMapping("/api/student-alias")
public class StudentAliasController {
    
    private static final Logger logger = LoggerFactory.getLogger(StudentAliasController.class);
    
    @Autowired
    private StudentAliasService studentAliasService;
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<StudentAliasDTO>>> getAliases(Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }
            
            List<StudentAliasDTO> aliases = studentAliasService.getAliasesByCoach(user.getId());
            return ResponseEntity.ok(ApiResponse.success("获取学员别名列表成功", aliases));
        } catch (Exception e) {
            logger.error("获取学员别名列表失败", e);
            return ResponseEntity.status(500).body(ApiResponse.error("获取学员别名列表失败: " + e.getMessage()));
        }
    }
    
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<StudentAliasDTO>> createAlias(
            @RequestBody StudentAliasRequest request,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }
            
            StudentAliasDTO alias = studentAliasService.createAlias(user.getId(), request);
            return ResponseEntity.ok(ApiResponse.success("创建学员别名成功", alias));
        } catch (Exception e) {
            logger.error("创建学员别名失败", e);
            return ResponseEntity.status(500).body(ApiResponse.error("创建学员别名失败: " + e.getMessage()));
        }
    }
    
    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<StudentAliasDTO>> updateAlias(
            @PathVariable Long id,
            @RequestBody StudentAliasRequest request,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }
            
            StudentAliasDTO alias = studentAliasService.updateAlias(id, request);
            return ResponseEntity.ok(ApiResponse.success("更新学员别名成功", alias));
        } catch (Exception e) {
            logger.error("更新学员别名失败", e);
            return ResponseEntity.status(500).body(ApiResponse.error("更新学员别名失败: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAlias(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }
            
            studentAliasService.deleteAlias(id);
            return ResponseEntity.ok(ApiResponse.success("删除学员别名成功", null));
        } catch (Exception e) {
            logger.error("删除学员别名失败", e);
            return ResponseEntity.status(500).body(ApiResponse.error("删除学员别名失败: " + e.getMessage()));
        }
    }
}

