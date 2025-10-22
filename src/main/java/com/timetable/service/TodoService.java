package com.timetable.service;

import com.timetable.dto.TodoDTO;
import com.timetable.dto.TodoRequest;
import com.timetable.entity.Todo;
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

    @Transactional
    public TodoDTO createTodo(TodoRequest request, Long userId) {
        Todo todo = new Todo();
        todo.setCustomerId(request.getCustomerId());
        todo.setCustomerName(request.getCustomerName());
        todo.setContent(request.getContent());
        todo.setReminderDate(request.getReminderDate());
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

    private TodoDTO convertToDTO(Todo todo) {
        TodoDTO dto = new TodoDTO();
        dto.setId(todo.getId());
        dto.setCustomerId(todo.getCustomerId());
        dto.setCustomerName(todo.getCustomerName());
        dto.setContent(todo.getContent());
        dto.setReminderDate(todo.getReminderDate());
        dto.setType(todo.getType());
        dto.setStatus(todo.getStatus());
        dto.setIsRead(todo.getIsRead());
        dto.setCompletedAt(todo.getCompletedAt());
        dto.setCreatedBy(todo.getCreatedBy());
        dto.setCreatedAt(todo.getCreatedAt());
        dto.setUpdatedAt(todo.getUpdatedAt());
        return dto;
    }
}

