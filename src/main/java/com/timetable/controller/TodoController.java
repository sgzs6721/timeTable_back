package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.TodoDTO;
import com.timetable.dto.TodoRequest;
import com.timetable.service.TodoService;
import com.timetable.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/todos")
@CrossOrigin(origins = "*")
public class TodoController {

    @Autowired
    private TodoService todoService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping
    public ResponseEntity<ApiResponse<TodoDTO>> createTodo(
            @RequestBody TodoRequest request,
            @RequestHeader("Authorization") String token) {
        try {
            String jwt = token.substring(7);
            String username = jwtUtil.extractUsername(jwt);
            Long userId = jwtUtil.extractUserId(jwt);

            TodoDTO todo = todoService.createTodo(request, userId);
            return ResponseEntity.ok(ApiResponse.success(todo));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("创建待办失败: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TodoDTO>>> getTodos(
            @RequestParam(required = false) String status,
            @RequestHeader("Authorization") String token) {
        try {
            String jwt = token.substring(7);
            Long userId = jwtUtil.extractUserId(jwt);

            List<TodoDTO> todos;
            if (status != null && !status.isEmpty()) {
                todos = todoService.getTodosByUserAndStatus(userId, status);
            } else {
                todos = todoService.getTodosByUser(userId);
            }

            return ResponseEntity.ok(ApiResponse.success(todos));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取待办列表失败: " + e.getMessage()));
        }
    }

    @GetMapping("/unread/count")
    public ResponseEntity<ApiResponse<Integer>> getUnreadCount(
            @RequestHeader("Authorization") String token) {
        try {
            String jwt = token.substring(7);
            Long userId = jwtUtil.extractUserId(jwt);

            int count = todoService.getUnreadCount(userId);
            return ResponseEntity.ok(ApiResponse.success(count));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取未读数量失败: " + e.getMessage()));
        }
    }

    @PutMapping("/{todoId}/read")
    public ResponseEntity<ApiResponse<Boolean>> markAsRead(
            @PathVariable Long todoId,
            @RequestHeader("Authorization") String token) {
        try {
            boolean success = todoService.markAsRead(todoId);
            return ResponseEntity.ok(ApiResponse.success(success));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("标记已读失败: " + e.getMessage()));
        }
    }

    @PutMapping("/{todoId}/complete")
    public ResponseEntity<ApiResponse<Boolean>> markAsCompleted(
            @PathVariable Long todoId,
            @RequestHeader("Authorization") String token) {
        try {
            boolean success = todoService.markAsCompleted(todoId);
            return ResponseEntity.ok(ApiResponse.success(success));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("标记完成失败: " + e.getMessage()));
        }
    }

    @PutMapping("/{todoId}/status")
    public ResponseEntity<ApiResponse<Boolean>> updateStatus(
            @PathVariable Long todoId,
            @RequestBody Map<String, String> request,
            @RequestHeader("Authorization") String token) {
        try {
            String status = request.get("status");
            boolean success = todoService.updateStatus(todoId, status);
            return ResponseEntity.ok(ApiResponse.success(success));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("更新状态失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{todoId}")
    public ResponseEntity<ApiResponse<Boolean>> deleteTodo(
            @PathVariable Long todoId,
            @RequestHeader("Authorization") String token) {
        try {
            boolean success = todoService.deleteTodo(todoId);
            return ResponseEntity.ok(ApiResponse.success(success));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("删除待办失败: " + e.getMessage()));
        }
    }
}

