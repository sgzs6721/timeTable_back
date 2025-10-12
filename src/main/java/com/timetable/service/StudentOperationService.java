package com.timetable.service;

import com.timetable.dto.StudentOperationRequest;
import com.timetable.dto.StudentAliasDTO;
import com.timetable.repository.StudentAliasRepository;
import com.timetable.repository.TimetableRepository;
import com.timetable.repository.WeeklyInstanceRepository;
import com.timetable.repository.WeeklyInstanceScheduleRepository;
import com.timetable.entity.WeeklyInstanceSchedule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    
    /**
     * 重命名学员
     */
    public void renameStudent(Long coachId, StudentOperationRequest request) {
        logger.info("重命名学员: {} -> {}", request.getOldName(), request.getNewName());
        
        // 获取该教练的所有课表ID
        List<Long> timetableIds = timetableRepository.findTimetableIdsByCoachId(coachId);
        
        // 获取所有周实例ID
        List<Long> instanceIds = new ArrayList<>();
        for (Long timetableId : timetableIds) {
            List<Long> ids = weeklyInstanceRepository.findInstanceIdsByTemplateId(timetableId);
            instanceIds.addAll(ids);
        }
        
        // 更新所有相关的周实例课程记录中的学员姓名
        int updatedCount = 0;
        for (Long instanceId : instanceIds) {
            List<WeeklyInstanceSchedule> schedules = weeklyInstanceScheduleRepository.findByInstanceId(instanceId);
            for (WeeklyInstanceSchedule schedule : schedules) {
                if (request.getOldName().equals(schedule.getStudentName())) {
                    schedule.setStudentName(request.getNewName());
                    weeklyInstanceScheduleRepository.update(schedule);
                    updatedCount++;
                }
            }
        }
        
        logger.info("成功更新了 {} 条课程记录中的学员姓名", updatedCount);
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
