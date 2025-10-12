package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.entity.StudentOperationRecord;
import com.timetable.service.StudentOperationRecordService;
import com.timetable.service.UserService;
import com.timetable.generated.tables.pojos.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student-operation-records")
public class StudentOperationRecordController {
    
    @Autowired
    private StudentOperationRecordService operationRecordService;
    
    @Autowired
    private UserService userService;
    
    /**
     * 获取当前用户的所有学员操作记录
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<StudentOperationRecord>>> getOperationRecords(
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }
            
            List<StudentOperationRecord> records = operationRecordService.getRecordsByCoachId(user.getId());
            return ResponseEntity.ok(ApiResponse.success("获取操作记录成功", records));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("获取操作记录失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新操作记录
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentOperationRecord>> updateOperationRecord(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }
            
            StudentOperationRecord record = operationRecordService.getRecordById(id);
            if (record == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 检查是否是当前用户的记录
            if (!record.getCoachId().equals(user.getId())) {
                return ResponseEntity.status(403).body(ApiResponse.error("无权限修改此记录"));
            }
            
            String newName = request.get("newName");
            String details = request.get("details");
            
            if (newName != null) {
                record.setNewName(newName);
            }
            if (details != null) {
                record.setDetails(details);
            }
            
            StudentOperationRecord updatedRecord = operationRecordService.updateRecord(record);
            return ResponseEntity.ok(ApiResponse.success("更新操作记录成功", updatedRecord));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("更新操作记录失败: " + e.getMessage()));
        }
    }
    
    /**
     * 删除操作记录
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOperationRecord(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }
            
            StudentOperationRecord record = operationRecordService.getRecordById(id);
            if (record == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 检查是否是当前用户的记录
            if (!record.getCoachId().equals(user.getId())) {
                return ResponseEntity.status(403).body(ApiResponse.error("无权限删除此记录"));
            }
            
            operationRecordService.deleteRecord(id);
            return ResponseEntity.ok(ApiResponse.success("删除操作记录成功", null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("删除操作记录失败: " + e.getMessage()));
        }
    }
}