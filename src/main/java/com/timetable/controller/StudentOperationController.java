package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.StudentOperationRequest;
import com.timetable.dto.StudentAliasDTO;
import com.timetable.service.StudentOperationService;
import com.timetable.service.UserService;
import com.timetable.generated.tables.pojos.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/student-operation")
public class StudentOperationController {
    
    private static final Logger logger = LoggerFactory.getLogger(StudentOperationController.class);
    
    @Autowired
    private StudentOperationService studentOperationService;
    
    @Autowired
    private UserService userService;
    
    @PostMapping("/rename")
    public ResponseEntity<ApiResponse<Void>> renameStudent(
            @RequestBody StudentOperationRequest request,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }
            
            studentOperationService.renameStudent(user.getId(), request);
            return ResponseEntity.ok(ApiResponse.success("重命名学员成功", null));
        } catch (Exception e) {
            logger.error("重命名学员失败", e);
            return ResponseEntity.status(500).body(ApiResponse.error("重命名学员失败: " + e.getMessage()));
        }
    }
    
    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<Void>> deleteStudent(
            @RequestBody StudentOperationRequest request,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }
            
            studentOperationService.deleteStudent(user.getId(), request.getOldName());
            return ResponseEntity.ok(ApiResponse.success("创建隐藏规则成功", null));
        } catch (Exception e) {
            logger.error("创建隐藏规则失败", e);
            return ResponseEntity.status(500).body(ApiResponse.error("创建隐藏规则失败: " + e.getMessage()));
        }
    }
    
    @PostMapping("/assign-alias")
    public ResponseEntity<ApiResponse<StudentAliasDTO>> assignAlias(
            @RequestBody StudentOperationRequest request,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }
            
            StudentAliasDTO alias = studentOperationService.assignAlias(user.getId(), request);
            return ResponseEntity.ok(ApiResponse.success("分配别名成功", alias));
        } catch (Exception e) {
            logger.error("分配别名失败", e);
            return ResponseEntity.status(500).body(ApiResponse.error("分配别名失败: " + e.getMessage()));
        }
    }
    
    @PostMapping("/merge")
    public ResponseEntity<ApiResponse<Void>> mergeStudents(
            @RequestBody com.timetable.dto.StudentMergeRequest request,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }
            
            studentOperationService.mergeStudents(user.getId(), request.getDisplayName(), request.getStudentNames());
            return ResponseEntity.ok(ApiResponse.success("创建合并规则成功", null));
        } catch (Exception e) {
            logger.error("创建合并规则失败", e);
            return ResponseEntity.status(500).body(ApiResponse.error("创建合并规则失败: " + e.getMessage()));
        }
    }
}

