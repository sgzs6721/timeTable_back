package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.task.TodoPushScheduledTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 微信推送测试控制器
 * 仅用于测试环境，生产环境请删除或禁用
 */
@RestController
@RequestMapping("/test/wechat-push")
@CrossOrigin(origins = "*")
public class WechatPushTestController {

    @Autowired
    private TodoPushScheduledTask todoPushScheduledTask;

    /**
     * 手动触发待办推送任务
     * 访问地址：GET /timetable/api/test/wechat-push/trigger
     */
    @GetMapping("/trigger")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerPush() {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("message", "开始执行待办推送任务");
            result.put("timestamp", System.currentTimeMillis());
            
            // 手动触发推送任务
            todoPushScheduledTask.manualPushTodoReminders();
            
            result.put("status", "执行完成");
            result.put("tip", "请查看日志了解推送结果");
            
            return ResponseEntity.ok(ApiResponse.success("推送任务已触发", result));
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("触发推送任务失败: " + e.getMessage()));
        }
    }

    /**
     * 获取推送任务状态信息
     * 访问地址：GET /timetable/api/test/wechat-push/status
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", true);
        status.put("cron", "每5分钟执行一次");
        status.put("manual_trigger_url", "/timetable/api/test/wechat-push/trigger");
        status.put("tip", "访问 /trigger 端点可手动触发推送任务");
        
        return ResponseEntity.ok(ApiResponse.success("获取状态成功", status));
    }
}

