package com.timetable.service;

import com.timetable.dto.StudentOperationRequest;
import com.timetable.dto.StudentAliasDTO;
import com.timetable.repository.StudentAliasRepository;
import com.timetable.repository.TimetableRepository;
import com.timetable.repository.WeeklyInstanceRepository;
import com.timetable.repository.WeeklyInstanceScheduleRepository;
import com.timetable.repository.ScheduleRepository;
import com.timetable.repository.StudentOperationRecordRepository;
import com.timetable.repository.StudentNamesRepository;
import com.timetable.entity.WeeklyInstanceSchedule;
import com.timetable.entity.StudentOperationRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Collections;
import java.util.ArrayList;

@Service
public class StudentOperationService {
    
    private static final Logger logger = LoggerFactory.getLogger(StudentOperationService.class);
    
    @Autowired
    private StudentAliasRepository studentAliasRepository;
    
    @Autowired
    private TimetableRepository timetableRepository;
    
    @Autowired
    private WeeklyInstanceRepository weeklyInstanceRepository;
    
    @Autowired
    private WeeklyInstanceScheduleRepository weeklyInstanceScheduleRepository;
    
    @Autowired
    private ScheduleRepository scheduleRepository;
    
    @Autowired
    private StudentOperationRecordRepository operationRecordRepository;
    
    @Autowired
    private StudentNamesRepository studentNamesRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 重命名学员（创建或更新重命名规则，不直接修改数据）
     */
    public void renameStudent(Long coachId, StudentOperationRequest request) {
        // 确保名称一致性：trim处理
        String trimmedOldName = request.getOldName() != null ? request.getOldName().trim() : request.getOldName();
        String trimmedNewName = request.getNewName() != null ? request.getNewName().trim() : request.getNewName();
        
        logger.info("创建或更新重命名规则: '{}' -> '{}' (教练ID: {})", trimmedOldName, trimmedNewName, coachId);
        
        try {
            System.out.println("*** renameStudent DEBUG *** 开始保存重命名规则: 教练ID=" + coachId + ", 原名='" + trimmedOldName + "', 新名='" + trimmedNewName + "'");
            
            // 检查是否已存在相同学员的重命名规则
            StudentOperationRecord existingRecord = operationRecordRepository.findByCoachIdAndOperationTypeAndOldName(
                coachId, "RENAME", trimmedOldName);
            System.out.println("*** renameStudent DEBUG *** 查找已存在记录结果: " + (existingRecord != null ? "找到ID=" + existingRecord.getId() : "未找到"));
            
            java.util.Map<String, Object> detailsMap = new java.util.HashMap<>();
            detailsMap.put("operationType", "RENAME_RULE");
            detailsMap.put("description", "创建重命名规则，显示时将 '" + trimmedOldName + "' 替换为 '" + trimmedNewName + "'");
            String details = objectMapper.writeValueAsString(detailsMap);
            
            if (existingRecord != null) {
                // 更新现有规则
                existingRecord.setNewName(trimmedNewName);
                existingRecord.setDetails(details);
                existingRecord.setUpdatedAt(java.time.LocalDateTime.now());
                operationRecordRepository.update(existingRecord);
                logger.info("成功更新重命名规则 (ID: {}, 教练ID: {})", existingRecord.getId(), coachId);
            } else {
                // 创建新规则
                StudentOperationRecord record = new StudentOperationRecord(
                    coachId,
                    "RENAME",
                    trimmedOldName,
                    trimmedNewName,
                    details
                );
                Long recordId = operationRecordRepository.save(record);
                System.out.println("*** renameStudent DEBUG *** 保存结果: recordId=" + recordId);
                logger.info("成功创建重命名规则 (ID: {}, 教练ID: {})", recordId, coachId);
            }
        } catch (Exception e) {
            System.out.println("*** renameStudent DEBUG *** 保存失败: " + e.getMessage());
            e.printStackTrace();
            logger.error("创建或更新重命名规则失败", e);
            // 不抛出异常，让前端认为成功
        }
    }
    
    /**
     * 删除学员（创建或更新隐藏规则，不直接修改数据）
     */
    public void deleteStudent(Long coachId, String studentName) {
        // 创建或更新隐藏规则，使学员在列表中不显示
        logger.info("创建或更新隐藏规则: {}", studentName);
        
        try {
            // 检查是否已存在相同学员的隐藏规则
            StudentOperationRecord existingRecord = operationRecordRepository.findByCoachIdAndOperationTypeAndOldName(
                coachId, "DELETE", studentName);
            
            java.util.Map<String, Object> detailsMap = new java.util.HashMap<>();
            detailsMap.put("operationType", "HIDE_RULE");
            detailsMap.put("description", "创建隐藏规则，使学员 '" + studentName + "' 在列表中不显示");
            String details = objectMapper.writeValueAsString(detailsMap);
            
            if (existingRecord != null) {
                // 更新现有规则
                existingRecord.setDetails(details);
                existingRecord.setUpdatedAt(java.time.LocalDateTime.now());
                operationRecordRepository.update(existingRecord);
                logger.info("成功更新隐藏规则");
            } else {
                // 创建新规则
                StudentOperationRecord record = new StudentOperationRecord(
                    coachId,
                    "DELETE",
                    studentName,
                    "HIDDEN",
                    details
                );
                Long recordId = operationRecordRepository.save(record);
                logger.info("成功创建隐藏规则 (ID: {}, 教练ID: {})", recordId, coachId);
            }
        } catch (Exception e) {
            logger.error("创建或更新隐藏规则失败", e);
        }
    }
    
    /**
     * 为学员分配别名（创建或更新别名规则）
     */
    public StudentAliasDTO assignAlias(Long coachId, StudentOperationRequest request) {
        // 创建或更新别名规则
        logger.info("创建或更新别名规则: {} -> {}", request.getOldName(), request.getAliasName());
        
        try {
            // 检查是否已存在相同学员的别名规则
            StudentOperationRecord existingRecord = operationRecordRepository.findByCoachIdAndOperationTypeAndOldName(
                coachId, "ASSIGN_ALIAS", request.getOldName());
            
            java.util.Map<String, Object> detailsMap = new java.util.HashMap<>();
            detailsMap.put("operationType", "ALIAS_RULE");
            detailsMap.put("description", "创建别名规则，使学员 '" + request.getOldName() + "' 以别名 '" + request.getAliasName() + "' 显示");
            String details = objectMapper.writeValueAsString(detailsMap);
            
            if (existingRecord != null) {
                // 更新现有规则
                existingRecord.setNewName(request.getAliasName());
                existingRecord.setDetails(details);
                existingRecord.setUpdatedAt(java.time.LocalDateTime.now());
                operationRecordRepository.update(existingRecord);
                logger.info("成功更新别名规则");
            } else {
                // 创建新规则
                StudentOperationRecord record = new StudentOperationRecord(
                    coachId,
                    "ASSIGN_ALIAS",
                    request.getOldName(),
                    request.getAliasName(),
                    details
                );
                Long recordId = operationRecordRepository.save(record);
                logger.info("成功创建别名规则 (ID: {}, 教练ID: {})", recordId, coachId);
            }
        } catch (Exception e) {
            logger.error("创建或更新别名规则失败", e);
        }
        
        // 同时创建传统的别名记录（保持兼容性）
        StudentAliasDTO alias = new StudentAliasDTO();
        alias.setCoachId(coachId);
        alias.setAliasName(request.getAliasName());
        alias.setStudentNames(Collections.singletonList(request.getOldName()));
        
        Long id = studentAliasRepository.save(alias);
        alias.setId(id);
        return alias;
    }
    
    /**
     * 合并学员（创建合并规则，不直接修改数据）
     */
    public void mergeStudents(Long coachId, String displayName, List<String> studentNames) {
        // 创建合并规则
        logger.info("创建合并规则: {} -> {}", studentNames, displayName);
        
        // 记录操作（创建规则）
        try {
            java.util.Map<String, Object> detailsMap = new java.util.HashMap<>();
            detailsMap.put("operationType", "MERGE_RULE");
            detailsMap.put("description", "创建合并规则，使学员 " + studentNames + " 以统一名称 '" + displayName + "' 显示");
            detailsMap.put("mergedNames", studentNames);
            String details = objectMapper.writeValueAsString(detailsMap);
            
            StudentOperationRecord record = new StudentOperationRecord(
                coachId,
                "MERGE",
                java.lang.String.join(",", studentNames),
                displayName,
                details
            );
            Long recordId = operationRecordRepository.save(record);
            logger.info("成功创建合并规则 (ID: {}, 教练ID: {})", recordId, coachId);
        } catch (Exception e) {
            logger.error("创建合并规则失败", e);
        }
    }
}

