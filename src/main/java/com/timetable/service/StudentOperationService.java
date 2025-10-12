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
     * 重命名学员
     */
    public void renameStudent(Long coachId, StudentOperationRequest request) {
        logger.info("重命名学员: {} -> {}", request.getOldName(), request.getNewName());
        
        // 获取该教练的所有课表ID
        List<Long> timetableIds = timetableRepository.findTimetableIdsByCoachId(coachId);
        
        // 更新周实例课程记录中的学员姓名
        int updatedInstanceCount = 0;
        // 获取所有周实例ID
        List<Long> instanceIds = new ArrayList<>();
        for (Long timetableId : timetableIds) {
            List<Long> ids = weeklyInstanceRepository.findInstanceIdsByTemplateId(timetableId);
            instanceIds.addAll(ids);
        }
        
        for (Long instanceId : instanceIds) {
            List<WeeklyInstanceSchedule> schedules = weeklyInstanceScheduleRepository.findByInstanceId(instanceId);
            for (WeeklyInstanceSchedule schedule : schedules) {
                if (request.getOldName().equals(schedule.getStudentName())) {
                    schedule.setStudentName(request.getNewName());
                    weeklyInstanceScheduleRepository.update(schedule);
                    updatedInstanceCount++;
                }
            }
        }
        
        // 更新日期类课表记录中的学员姓名
        int updatedDateCount = 0;
        for (Long timetableId : timetableIds) {
            com.timetable.generated.tables.pojos.Timetables timetable = timetableRepository.findById(timetableId);
            if (timetable != null && (timetable.getIsWeekly() == null || timetable.getIsWeekly() == 0)) {
                // 只处理日期类课表
                List<com.timetable.generated.tables.pojos.Schedules> schedules = scheduleRepository.findByTimetableId(timetableId);
                for (com.timetable.generated.tables.pojos.Schedules schedule : schedules) {
                    if (request.getOldName().equals(schedule.getStudentName())) {
                        schedule.setStudentName(request.getNewName());
                        scheduleRepository.update(schedule);
                        updatedDateCount++;
                    }
                }
            }
        }
        
        logger.info("成功更新了 {} 条周实例课程记录和 {} 条日期类课程记录中的学员姓名", updatedInstanceCount, updatedDateCount);
        
        // 记录操作
        try {
            String details = objectMapper.writeValueAsString(java.util.Map.of(
                "updatedInstanceCount", updatedInstanceCount,
                "updatedDateCount", updatedDateCount,
                "totalUpdated", updatedInstanceCount + updatedDateCount
            ));
            
            StudentOperationRecord record = new StudentOperationRecord(
                coachId,
                "RENAME",
                request.getOldName(),
                request.getNewName(),
                details
            );
            operationRecordRepository.save(record);
            logger.info("成功记录重命名操作");
        } catch (Exception e) {
            logger.error("记录重命名操作失败", e);
        }
    }
    
    /**
     * 删除学员（软删除，不影响课表记录）
     */
    public void deleteStudent(Long coachId, String studentName) {
        // 这里可以实现删除逻辑
        // 由于不影响课表记录，主要是标记为不显示
        logger.info("删除学员: {}", studentName);
    }
    
    /**
     * 为学员分配别名
     */
    public StudentAliasDTO assignAlias(Long coachId, StudentOperationRequest request) {
        StudentAliasDTO alias = new StudentAliasDTO();
        alias.setCoachId(coachId);
        alias.setAliasName(request.getAliasName());
        alias.setStudentNames(Collections.singletonList(request.getOldName()));
        
        Long id = studentAliasRepository.save(alias);
        alias.setId(id);
        return alias;
    }
}
