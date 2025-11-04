package com.timetable.service;

import com.timetable.dto.TodoDTO;
import com.timetable.dto.TodoRequest;
import com.timetable.entity.Customer;
import com.timetable.entity.Todo;
import com.timetable.repository.CustomerRepository;
import com.timetable.repository.TodoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TodoService {

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerStatusHistoryService historyService;

    @Transactional
    public TodoDTO createTodo(TodoRequest request, Long userId, Long organizationId) {
        Todo todo = new Todo();
        todo.setCustomerId(request.getCustomerId());
        todo.setCustomerName(request.getCustomerName());
        todo.setContent(request.getContent());
        todo.setReminderDate(request.getReminderDate());
        todo.setReminderTime(request.getReminderTime());
        todo.setType(request.getType() != null ? request.getType() : "CUSTOMER_FOLLOW_UP");
        todo.setStatus(request.getStatus() != null ? request.getStatus() : "PENDING");
        todo.setCreatedBy(userId);
        todo.setOrganizationId(organizationId);

        Todo created = todoRepository.create(todo);
        return convertToDTO(created);
    }

    public List<TodoDTO> getTodosByUser(Long userId, Long organizationId) {
        List<Todo> todos = todoRepository.findByCreatedByAndOrganizationId(userId, organizationId);
        return todos.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<TodoDTO> getTodosByUserAndStatus(Long userId, String status, Long organizationId) {
        List<Todo> todos = todoRepository.findByCreatedByAndStatusAndOrganizationId(userId, status, organizationId);
        return todos.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public int getUnreadCount(Long userId, Long organizationId) {
        return todoRepository.countUnreadByCreatedByAndOrganizationId(userId, organizationId);
    }

    @Transactional
    public boolean markAsRead(Long todoId, Long userId) {
        Todo todo = todoRepository.findById(todoId);
        if (todo == null || !todo.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权限操作此待办");
        }
        return todoRepository.markAsRead(todoId) > 0;
    }

    @Transactional
    public boolean markAsCompleted(Long todoId, Long userId) {
        Todo todo = todoRepository.findById(todoId);
        if (todo == null || !todo.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权限操作此待办");
        }
        return todoRepository.markAsCompleted(todoId) > 0;
    }

    @Transactional
    public boolean markAsCancelled(Long todoId, Long userId) {
        Todo todo = todoRepository.findById(todoId);
        if (todo == null || !todo.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权限操作此待办");
        }
        return todoRepository.markAsCancelled(todoId) > 0;
    }

    @Transactional
    public boolean updateStatus(Long todoId, String status, Long userId) {
        Todo todo = todoRepository.findById(todoId);
        if (todo == null || !todo.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权限操作此待办");
        }
        return todoRepository.updateStatus(todoId, status) > 0;
    }

    @Transactional
    public boolean deleteTodo(Long todoId, Long userId) {
        Todo todo = todoRepository.findById(todoId);
        if (todo == null || !todo.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权限操作此待办");
        }
        return todoRepository.delete(todoId) > 0;
    }

    public boolean customerHasTodo(Long customerId, Long organizationId) {
        return todoRepository.existsByCustomerIdAndOrganizationId(customerId, organizationId);
    }

    public TodoDTO getLatestTodoForCustomer(Long customerId, Long organizationId) {
        Todo todo = todoRepository.findLatestTodoByCustomerIdAndOrganizationId(customerId, organizationId);
        return todo != null ? convertToDTO(todo) : null;
    }

    @Transactional
    public TodoDTO updateTodo(Long todoId, TodoRequest request, Long userId) {
        Todo todo = todoRepository.findById(todoId);
        if (todo == null) {
            throw new RuntimeException("待办不存在");
        }

        // 更新待办信息
        if (request.getCustomerId() != null) {
            todo.setCustomerId(request.getCustomerId());
        }
        if (request.getCustomerName() != null) {
            todo.setCustomerName(request.getCustomerName());
        }
        if (request.getContent() != null) {
            todo.setContent(request.getContent());
        }
        if (request.getReminderDate() != null) {
            todo.setReminderDate(request.getReminderDate());
        }
        if (request.getReminderTime() != null) {
            todo.setReminderTime(request.getReminderTime());
        }
        if (request.getType() != null) {
            todo.setType(request.getType());
        }
        if (request.getStatus() != null) {
            todo.setStatus(request.getStatus());
        }

        Todo updated = todoRepository.update(todo);
        return convertToDTO(updated);
    }

    @Transactional
    public boolean updateReminderTime(Long todoId, String reminderDateTime, Long userId) {
        Todo todo = todoRepository.findById(todoId);
        if (todo == null) {
            throw new RuntimeException("待办不存在");
        }
        
        if (!todo.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权限操作此待办");
        }

        // 解析日期时间字符串 "2025-10-27 11:30:00"
        try {
            String[] parts = reminderDateTime.split(" ");
            if (parts.length == 2) {
                todo.setReminderDate(java.time.LocalDate.parse(parts[0]));
                todo.setReminderTime(java.time.LocalTime.parse(parts[1]));
            } else {
                throw new RuntimeException("日期时间格式错误");
            }
            
            todoRepository.update(todo);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("更新提醒时间失败: " + e.getMessage());
        }
    }

    private String getStatusText(String status) {
        if (status == null) return "未知";
        switch (status) {
            case "NEW": return "新建";
            case "CONTACTED": return "已联系";
            case "SCHEDULED": return "待体验";
            case "PENDING_CONFIRM": return "待确认";
            case "VISITED": return "已体验";
            case "RE_EXPERIENCE": return "待再体验";
            case "PENDING_SOLD": return "待成交";
            case "SOLD": return "已成交";
            case "CLOSED": return "已结束";
            default: return status;
        }
    }

    private TodoDTO convertToDTO(Todo todo) {
        TodoDTO dto = new TodoDTO();
        dto.setId(todo.getId());
        dto.setCustomerId(todo.getCustomerId());
        dto.setCustomerName(todo.getCustomerName());
        
        // 查询客户信息（电话、状态、地点、详情）
        if (todo.getCustomerId() != null) {
            Customer customer = customerRepository.findById(todo.getCustomerId());
            if (customer != null) {
                dto.setCustomerPhone(customer.getParentPhone());
                dto.setCustomerStatus(customer.getStatus());
                dto.setCustomerStatusText(getStatusText(customer.getStatus()));
                dto.setCustomerSource(customer.getSource());
                dto.setCustomerNotes(customer.getNotes());
            }
        }
        
        dto.setContent(todo.getContent());
        dto.setReminderDate(todo.getReminderDate());
        dto.setReminderTime(todo.getReminderTime());
        dto.setType(todo.getType());
        dto.setStatus(todo.getStatus());
        dto.setIsRead(todo.getIsRead());
        dto.setCompletedAt(todo.getCompletedAt());
        dto.setCancelledAt(todo.getCancelledAt());
        dto.setCreatedBy(todo.getCreatedBy());
        dto.setOrganizationId(todo.getOrganizationId());
        dto.setCreatedAt(todo.getCreatedAt());
        dto.setUpdatedAt(todo.getUpdatedAt());
        
        // 查询流转记录
        if (todo.getCustomerId() != null && todo.getCreatedBy() != null) {
            try {
                dto.setStatusHistory(historyService.getHistoryByCustomerId(todo.getCustomerId(), todo.getCreatedBy()));
            } catch (Exception e) {
                // 如果查询失败（如权限不足），不影响待办显示
                dto.setStatusHistory(new java.util.ArrayList<>());
            }
        }
        
        return dto;
    }
}

