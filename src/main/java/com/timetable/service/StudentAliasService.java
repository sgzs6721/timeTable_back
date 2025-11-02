package com.timetable.service;

import com.timetable.dto.StudentAliasDTO;
import com.timetable.dto.StudentAliasRequest;
import com.timetable.entity.StudentOperationRecord;
import com.timetable.repository.StudentOperationRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

@Service
public class StudentAliasService {
    
    private static final Logger logger = LoggerFactory.getLogger(StudentAliasService.class);
    
    @Autowired
    private StudentOperationRecordRepository operationRecordRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public List<StudentAliasDTO> getAliasesByCoach(Long coachId) {
        // 从student_operation_records表查询ASSIGN_ALIAS类型的记录
        List<StudentOperationRecord> records = operationRecordRepository.findByCoachId(coachId);
        return records.stream()
                .filter(r -> "ASSIGN_ALIAS".equals(r.getOperationType()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public StudentAliasDTO createAlias(Long coachId, StudentAliasRequest request) {
        // 创建ASSIGN_ALIAS类型的操作记录
        StudentOperationRecord record = new StudentOperationRecord();
        record.setCoachId(coachId);
        record.setOperationType("ASSIGN_ALIAS");
        record.setOldName(String.join(",", request.getStudentNames()));
        record.setNewName(request.getAliasName());
        
        try {
            Map<String, Object> detailsMap = new HashMap<>();
            detailsMap.put("studentNames", request.getStudentNames());
            record.setDetails(objectMapper.writeValueAsString(detailsMap));
        } catch (Exception e) {
            logger.error("序列化details失败", e);
        }
        
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        
        Long id = operationRecordRepository.save(record);
        
        StudentAliasDTO dto = new StudentAliasDTO();
        dto.setId(id);
        dto.setCoachId(coachId);
        dto.setAliasName(request.getAliasName());
        dto.setStudentNames(request.getStudentNames());
        dto.setCreatedAt(record.getCreatedAt());
        dto.setUpdatedAt(record.getUpdatedAt());
        return dto;
    }
    
    public StudentAliasDTO updateAlias(Long id, StudentAliasRequest request) {
        StudentOperationRecord record = operationRecordRepository.findById(id);
        if (record == null || !"ASSIGN_ALIAS".equals(record.getOperationType())) {
            throw new IllegalArgumentException("别名记录不存在");
        }
        
        record.setOldName(String.join(",", request.getStudentNames()));
        record.setNewName(request.getAliasName());
        
        try {
            Map<String, Object> detailsMap = new HashMap<>();
            detailsMap.put("studentNames", request.getStudentNames());
            record.setDetails(objectMapper.writeValueAsString(detailsMap));
        } catch (Exception e) {
            logger.error("序列化details失败", e);
        }
        
        operationRecordRepository.update(record);
        return convertToDTO(record);
    }
    
    public void deleteAlias(Long id) {
        StudentOperationRecord record = operationRecordRepository.findById(id);
        if (record == null || !"ASSIGN_ALIAS".equals(record.getOperationType())) {
            throw new IllegalArgumentException("别名记录不存在");
        }
        operationRecordRepository.deleteById(id);
    }
    
    private StudentAliasDTO convertToDTO(StudentOperationRecord record) {
        StudentAliasDTO dto = new StudentAliasDTO();
        dto.setId(record.getId());
        dto.setCoachId(record.getCoachId());
        dto.setAliasName(record.getNewName());
        dto.setCreatedAt(record.getCreatedAt());
        dto.setUpdatedAt(record.getUpdatedAt());
        
        // 从details中解析studentNames
        try {
            if (record.getDetails() != null) {
                Map<String, Object> detailsMap = objectMapper.readValue(record.getDetails(), new TypeReference<Map<String, Object>>() {});
                if (detailsMap.containsKey("studentNames")) {
                    List<String> studentNames = objectMapper.convertValue(detailsMap.get("studentNames"), new TypeReference<List<String>>() {});
                    dto.setStudentNames(studentNames);
                }
            }
        } catch (Exception e) {
            logger.error("解析details失败", e);
            // 如果解析失败，从old_name分割
            if (record.getOldName() != null) {
                dto.setStudentNames(java.util.Arrays.asList(record.getOldName().split(",")));
            }
        }
        
        return dto;
    }
}





