package com.timetable.service;

import com.timetable.dto.StudentOperationRequest;
import com.timetable.dto.StudentAliasDTO;
import com.timetable.repository.StudentAliasRepository;
import com.timetable.repository.TimetableRepository;
import com.timetable.repository.WeeklyInstanceRepository;
import com.timetable.repository.WeeklyInstanceScheduleRepository;
import com.timetable.repository.ScheduleRepository;
import com.timetable.repository.StudentOperationRecordRepository;
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
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 重命名学员（创建重命名规则，不直接修改数据）
     */
    public void renameStudent(Long coachId, StudentOperationRequest request) {
        logger.info("创建重命名规则: {} -> {}", request.getOldName(), request.getNewName());
        
        // 记录操作（创建规则）
        try {
            java.util.Map<String, Object> detailsMap = new java.util.HashMap<>();
            detailsMap.put("operationType", "RENAME_RULE");
            detailsMap.put("description", "创建重命名规则，显示时将 '" + request.getOldName() + "' 替换为 '" + request.getNewName() + "'");
            String details = objectMapper.writeValueAsString(detailsMap);
            
            StudentOperationRecord record = new StudentOperationRecord(
                coachId,
                "RENAME",
                request.getOldName(),
                request.getNewName(),
                details
            );
            operationRecordRepository.save(record);
            logger.info("成功创建重命名规则");
        } catch (Exception e) {
            logger.error("创建重命名规则失败", e);
        }
    }
    
    /**
     * 删除学员（创建隐藏规则，不直接修改数据）
     */
    public void deleteStudent(Long coachId, String studentName) {
        // 创建隐藏规则，使学员在列表中不显示
        logger.info("创建隐藏规则: {}", studentName);
        
        // 记录操作（创建规则）
        try {
            java.util.Map<String, Object> detailsMap = new java.util.HashMap<>();
            detailsMap.put("operationType", "HIDE_RULE");
            detailsMap.put("description", "创建隐藏规则，使学员 '" + studentName + "' 在列表中不显示");
            String details = objectMapper.writeValueAsString(detailsMap);
            
            StudentOperationRecord record = new StudentOperationRecord(
                coachId,
                "DELETE",
                studentName,
                "HIDDEN",
                details
            );
            operationRecordRepository.save(record);
            logger.info("成功创建隐藏规则");
        } catch (Exception e) {
            logger.error("创建隐藏规则失败", e);
        }
    }
    
    /**
     * 为学员分配别名（创建别名规则）
     */
    public StudentAliasDTO assignAlias(Long coachId, StudentOperationRequest request) {
        // 创建别名规则
        logger.info("创建别名规则: {} -> {}", request.getOldName(), request.getAliasName());
        
        // 记录操作（创建规则）
        try {
            java.util.Map<String, Object> detailsMap = new java.util.HashMap<>();
            detailsMap.put("operationType", "ALIAS_RULE");
            detailsMap.put("description", "创建别名规则，使学员 '" + request.getOldName() + "' 以别名 '" + request.getAliasName() + "' 显示");
            String details = objectMapper.writeValueAsString(detailsMap);
            
            StudentOperationRecord record = new StudentOperationRecord(
                coachId,
                "ASSIGN_ALIAS",
                request.getOldName(),
                request.getAliasName(),
                details
            );
            operationRecordRepository.save(record);
            logger.info("成功创建别名规则");
        } catch (Exception e) {
            logger.error("创建别名规则失败", e);
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
            operationRecordRepository.save(record);
            logger.info("成功创建合并规则");
        } catch (Exception e) {
            logger.error("创建合并规则失败", e);
        }
    }
}
