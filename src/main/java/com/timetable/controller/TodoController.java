package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.TodoDTO;
import com.timetable.dto.TodoRequest;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.service.TodoService;
import com.timetable.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/todos")
@CrossOrigin(origins = "*")
public class TodoController {

    @Autowired
    private TodoService todoService;

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<TodoDTO>> createTodo(
            @RequestBody TodoRequest request,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }

            TodoDTO todo = todoService.createTodo(request, user.getId());
            return ResponseEntity.ok(ApiResponse.success("创建成功", todo));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("创建待办失败: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TodoDTO>>> getTodos(
            @RequestParam(required = false) String status,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }

            List<TodoDTO> todos;
            if (status != null && !status.isEmpty()) {
                todos = todoService.getTodosByUserAndStatus(user.getId(), status);
            } else {
                todos = todoService.getTodosByUser(user.getId());
            }

            return ResponseEntity.ok(ApiResponse.success("获取成功", todos));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("获取待办列表失败: " + e.getMessage()));
        }
    }

    @GetMapping("/unread/count")
    public ResponseEntity<ApiResponse<Integer>> getUnreadCount(
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }

            int count = todoService.getUnreadCount(user.getId());
            return ResponseEntity.ok(ApiResponse.success("获取成功", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("获取未读数量失败: " + e.getMessage()));
        }
    }

    @PutMapping("/{todoId}/read")
    public ResponseEntity<ApiResponse<Boolean>> markAsRead(
            @PathVariable Long todoId) {
        try {
            boolean success = todoService.markAsRead(todoId);
            return ResponseEntity.ok(ApiResponse.success("标记成功", success));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("标记已读失败: " + e.getMessage()));
        }
    }

    @PutMapping("/{todoId}/complete")
    public ResponseEntity<ApiResponse<Boolean>> markAsCompleted(
            @PathVariable Long todoId) {
        try {
            boolean success = todoService.markAsCompleted(todoId);
            return ResponseEntity.ok(ApiResponse.success("标记成功", success));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("标记完成失败: " + e.getMessage()));
        }
    }

    @PutMapping("/{todoId}/status")
    public ResponseEntity<ApiResponse<Boolean>> updateStatus(
            @PathVariable Long todoId,
            @RequestBody Map<String, String> request) {
        try {
            String status = request.get("status");
            boolean success = todoService.updateStatus(todoId, status);
            return ResponseEntity.ok(ApiResponse.success("更新成功", success));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("更新状态失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{todoId}")
    public ResponseEntity<ApiResponse<Boolean>> deleteTodo(
            @PathVariable Long todoId) {
        try {
            boolean success = todoService.deleteTodo(todoId);
            return ResponseEntity.ok(ApiResponse.success("删除成功", success));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("删除待办失败: " + e.getMessage()));
        }
    }

    @GetMapping("/customer/{customerId}/exists")
    public ResponseEntity<ApiResponse<Boolean>> checkCustomerHasTodo(
            @PathVariable Long customerId) {
        try {
            boolean hasTodo = todoService.customerHasTodo(customerId);
            return ResponseEntity.ok(ApiResponse.success("查询成功", hasTodo));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }

    @GetMapping("/customer/{customerId}/latest")
    public ResponseEntity<ApiResponse<TodoDTO>> getLatestTodoForCustomer(
            @PathVariable Long customerId) {
        try {
            TodoDTO todo = todoService.getLatestTodoForCustomer(customerId);
            return ResponseEntity.ok(ApiResponse.success("获取成功", todo));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("获取失败: " + e.getMessage()));
        }
    }

    @PutMapping("/{todoId}")
    public ResponseEntity<ApiResponse<TodoDTO>> updateTodo(
            @PathVariable Long todoId,
            @RequestBody TodoRequest request,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }

            TodoDTO todo = todoService.updateTodo(todoId, request, user.getId());
            return ResponseEntity.ok(ApiResponse.success("更新成功", todo));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("更新待办失败: " + e.getMessage()));
        }
    }

    @PutMapping("/{todoId}/reminder-time")
    public ResponseEntity<ApiResponse<Boolean>> updateReminderTime(
            @PathVariable Long todoId,
            @RequestBody Map<String, String> request) {
        try {
            String reminderDateTime = request.get("reminderDateTime");
            boolean success = todoService.updateReminderTime(todoId, reminderDateTime);
            return ResponseEntity.ok(ApiResponse.success("更新提醒时间成功", success));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("更新提醒时间失败: " + e.getMessage()));
        }
    }
}

