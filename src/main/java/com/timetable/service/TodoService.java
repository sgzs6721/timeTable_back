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
    public TodoDTO createTodo(TodoRequest request, Long userId) {
        Todo todo = new Todo();
        todo.setCustomerId(request.getCustomerId());
        todo.setCustomerName(request.getCustomerName());
        todo.setContent(request.getContent());
        todo.setReminderDate(request.getReminderDate());
        todo.setReminderTime(request.getReminderTime());
        todo.setType(request.getType() != null ? request.getType() : "CUSTOMER_FOLLOW_UP");
        todo.setStatus(request.getStatus() != null ? request.getStatus() : "PENDING");
        todo.setCreatedBy(userId);

        Todo created = todoRepository.create(todo);
        return convertToDTO(created);
    }

    public List<TodoDTO> getTodosByUser(Long userId) {
        List<Todo> todos = todoRepository.findByCreatedBy(userId);
        return todos.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<TodoDTO> getTodosByUserAndStatus(Long userId, String status) {
        List<Todo> todos = todoRepository.findByCreatedByAndStatus(userId, status);
        return todos.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public int getUnreadCount(Long userId) {
        return todoRepository.countUnreadByCreatedBy(userId);
    }

    @Transactional
    public boolean markAsRead(Long todoId) {
        return todoRepository.markAsRead(todoId) > 0;
    }

    @Transactional
    public boolean markAsCompleted(Long todoId) {
        return todoRepository.markAsCompleted(todoId) > 0;
    }

    @Transactional
    public boolean updateStatus(Long todoId, String status) {
        return todoRepository.updateStatus(todoId, status) > 0;
    }

    @Transactional
    public boolean deleteTodo(Long todoId) {
        return todoRepository.delete(todoId) > 0;
    }

    public boolean customerHasTodo(Long customerId) {
        return todoRepository.existsByCustomerId(customerId);
    }

    public TodoDTO getLatestTodoForCustomer(Long customerId) {
        Todo todo = todoRepository.findLatestTodoByCustomerId(customerId);
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

    private TodoDTO convertToDTO(Todo todo) {
        TodoDTO dto = new TodoDTO();
        dto.setId(todo.getId());
        dto.setCustomerId(todo.getCustomerId());
        dto.setCustomerName(todo.getCustomerName());
        
        // 查询客户电话
        if (todo.getCustomerId() != null) {
            Customer customer = customerRepository.findById(todo.getCustomerId());
            if (customer != null) {
                dto.setCustomerPhone(customer.getParentPhone());
            }
        }
        
        dto.setContent(todo.getContent());
        dto.setReminderDate(todo.getReminderDate());
        dto.setReminderTime(todo.getReminderTime());
        dto.setType(todo.getType());
        dto.setStatus(todo.getStatus());
        dto.setIsRead(todo.getIsRead());
        dto.setCompletedAt(todo.getCompletedAt());
        dto.setCreatedBy(todo.getCreatedBy());
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

