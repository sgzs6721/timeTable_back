package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.TextInputRequest;
import com.timetable.model.Schedule;
import com.timetable.model.User;
import com.timetable.service.ScheduleService;
import com.timetable.service.UserService;
import com.timetable.service.VoiceProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 课程表控制器
 */
@RestController
@RequestMapping("/timetables")
@Validated
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class TimetableController {
    
    private static final Logger logger = LoggerFactory.getLogger(TimetableController.class);
    
    @Autowired
    private ScheduleService scheduleService;
    
    @Autowired
    private VoiceProcessingService voiceProcessingService;
    
    @Autowired
    private UserService userService;
    
    /**
     * 通过语音输入添加课程安排
     */
    @PostMapping("/{timetableId}/schedules/voice")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addScheduleByVoice(
            @PathVariable Long timetableId,
            @RequestParam("audio") MultipartFile audioFile) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("用户未登录"));
            }
            
            // 验证音频文件
            if (audioFile == null || audioFile.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("音频文件不能为空"));
            }
            
            logger.info("用户 {} 开始处理语音输入，课程表ID: {}", currentUser.getUsername(), timetableId);
            
            // 处理语音输入
            List<Schedule> newSchedules = voiceProcessingService.processVoiceInput(timetableId, audioFile);
            
            // 保存课程安排到数据库
            List<Schedule> savedSchedules = scheduleService.saveSchedules(newSchedules);
            
            // 构建响应数据
            Map<String, Object> data = new HashMap<>();
            data.put("schedules", savedSchedules);
            data.put("count", savedSchedules.size());
            
            logger.info("语音输入处理成功，添加了 {} 个课程安排", savedSchedules.size());
            return ResponseEntity.ok(ApiResponse.success("语音录入成功！已添加 " + savedSchedules.size() + " 个课程安排", data));
            
        } catch (Exception e) {
            logger.error("语音输入处理失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("语音录入失败：" + e.getMessage()));
        }
    }
    
    /**
     * 通过文本输入添加课程安排
     */
    @PostMapping("/{timetableId}/schedules/text")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addScheduleByText(
            @PathVariable Long timetableId,
            @Valid @RequestBody TextInputRequest textRequest) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("用户未登录"));
            }
            
            logger.info("用户 {} 开始处理文本输入，课程表ID: {}", currentUser.getUsername(), timetableId);
            
            // 处理文本输入
            List<Schedule> newSchedules = voiceProcessingService.processTextInput(timetableId, textRequest.getText());
            
            // 保存课程安排到数据库
            List<Schedule> savedSchedules = scheduleService.saveSchedules(newSchedules);
            
            // 构建响应数据
            Map<String, Object> data = new HashMap<>();
            data.put("schedules", savedSchedules);
            data.put("count", savedSchedules.size());
            
            logger.info("文本输入处理成功，添加了 {} 个课程安排", savedSchedules.size());
            return ResponseEntity.ok(ApiResponse.success("文字录入成功！已添加 " + savedSchedules.size() + " 个课程安排", data));
            
        } catch (Exception e) {
            logger.error("文本输入处理失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("文字录入失败：" + e.getMessage()));
        }
    }
    
    /**
     * 获取当前登录用户
     */
    private User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }
            
            String username = authentication.getName();
            return userService.findByUsername(username);
        } catch (Exception e) {
            logger.error("获取当前用户失败", e);
            return null;
        }
    }
} 