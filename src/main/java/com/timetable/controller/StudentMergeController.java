package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.StudentMergeDTO;
import com.timetable.dto.StudentMergeRequest;
import com.timetable.service.StudentMergeService;
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
@RequestMapping("/api/student-merge")
public class StudentMergeController {
    
    private static final Logger logger = LoggerFactory.getLogger(StudentMergeController.class);
    
    @Autowired
    private StudentMergeService studentMergeService;
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<StudentMergeDTO>>> getMerges(Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }
            
            List<StudentMergeDTO> merges = studentMergeService.getMergesByCoach(user.getId());
            return ResponseEntity.ok(ApiResponse.success("获取学员合并列表成功", merges));
        } catch (Exception e) {
            logger.error("获取学员合并列表失败", e);
            return ResponseEntity.status(500).body(ApiResponse.error("获取学员合并列表失败: " + e.getMessage()));
        }
    }
    
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<StudentMergeDTO>> createMerge(
            @RequestBody StudentMergeRequest request,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }
            
            StudentMergeDTO merge = studentMergeService.createMerge(user.getId(), request);
            return ResponseEntity.ok(ApiResponse.success("创建学员合并成功", merge));
        } catch (Exception e) {
            logger.error("创建学员合并失败", e);
            return ResponseEntity.status(500).body(ApiResponse.error("创建学员合并失败: " + e.getMessage()));
        }
    }
    
    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<StudentMergeDTO>> updateMerge(
            @PathVariable Long id,
            @RequestBody StudentMergeRequest request,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }
            
            StudentMergeDTO merge = studentMergeService.updateMerge(id, request);
            return ResponseEntity.ok(ApiResponse.success("更新学员合并成功", merge));
        } catch (Exception e) {
            logger.error("更新学员合并失败", e);
            return ResponseEntity.status(500).body(ApiResponse.error("更新学员合并失败: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMerge(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }
            
            studentMergeService.deleteMerge(id);
            return ResponseEntity.ok(ApiResponse.success("删除学员合并成功", null));
        } catch (Exception e) {
            logger.error("删除学员合并失败", e);
            return ResponseEntity.status(500).body(ApiResponse.error("删除学员合并失败: " + e.getMessage()));
        }
    }
}

