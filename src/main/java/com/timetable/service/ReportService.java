package com.timetable.service;

import com.timetable.generated.tables.pojos.Schedules;
import com.timetable.dto.ScheduleWithCoachDTO;
import com.timetable.repository.ReportRepository;
import com.timetable.repository.StudentOperationRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    @Autowired
    private ReportRepository reportRepository;
    
    @Autowired
    private StudentOperationRecordRepository studentOperationRecordRepository;

    public Map<String, Object> queryHoursPaged(Long userId, LocalDate start, LocalDate end, int page, int size, String sortOrder) {
        List<ScheduleWithCoachDTO> list = reportRepository.querySchedulesByUserPaged(userId, start, end, page, size, sortOrder);
        long total = reportRepository.countSchedulesByUser(userId, start, end);
        
        // 应用学员操作规则
        list = applyStudentOperationRules(list, userId);
        
        // 计算总课时数（包括半小时课程按0.5计算）
        double totalHours = 0.0;
        for (ScheduleWithCoachDTO schedule : list) {
            if (schedule.getStartTime() != null && schedule.getEndTime() != null) {
                long durationMinutes = java.time.Duration.between(schedule.getStartTime(), schedule.getEndTime()).toMinutes();
                totalHours += durationMinutes / 60.0; // 转换为小时，支持小数
            } else {
                totalHours += 1.0; // 如果没有时间信息，默认1课时
            }
        }
        
        // 计算所有记录的课时总数（用于总计显示）
        List<ScheduleWithCoachDTO> allSchedules = reportRepository.querySchedulesByUserPaged(userId, start, end, 1, Integer.MAX_VALUE, sortOrder);
        // 对所有记录也应用规则
        allSchedules = applyStudentOperationRules(allSchedules, userId);
        
        double grandTotalHours = 0.0;
        for (ScheduleWithCoachDTO schedule : allSchedules) {
            if (schedule.getStartTime() != null && schedule.getEndTime() != null) {
                long durationMinutes = java.time.Duration.between(schedule.getStartTime(), schedule.getEndTime()).toMinutes();
                grandTotalHours += durationMinutes / 60.0;
            } else {
                grandTotalHours += 1.0;
            }
        }
        
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);
        data.put("totalHours", totalHours); // 当前页课时数
        data.put("grandTotalHours", grandTotalHours); // 总计课时数
        return data;
    }
    
    /**
     * 应用学员操作规则到课程列表
     */
    private List<ScheduleWithCoachDTO> applyStudentOperationRules(List<ScheduleWithCoachDTO> schedules, Long coachId) {
        if (schedules == null || schedules.isEmpty()) {
            return schedules;
        }
        
        // 获取该教练的学员操作规则
        List<com.timetable.entity.StudentOperationRecord> records = 
            studentOperationRecordRepository.findByCoachId(coachId);
        
        // 构建规则映射
        Map<String, String> renameRules = new HashMap<>(); // 原名称 -> 新名称
        Map<String, String> mergeRules = new HashMap<>(); // 原名称 -> 合并后的名称
        Map<String, String> assignHoursRules = new HashMap<>(); // 学员名称 -> 源课程名称
        List<String> hiddenStudents = new java.util.ArrayList<>(); // 隐藏的学员列表
        
        for (com.timetable.entity.StudentOperationRecord record : records) {
            String operationType = record.getOperationType();
            String oldName = record.getOldName();
            String newName = record.getNewName();
            
            switch (operationType) {
                case "RENAME":
                    if (oldName != null && newName != null) {
                        renameRules.put(oldName, newName);
                    }
                    break;
                case "MERGE":
                    if (oldName != null && newName != null) {
                        // 合并规则：将原学员重命名为新名字
                        mergeRules.put(oldName, newName);
                    }
                    break;
                case "HIDE":
                    if (oldName != null) {
                        hiddenStudents.add(oldName);
                    }
                    break;
                case "ASSIGN_HOURS":
                    if (oldName != null && newName != null) {
                        // oldName是被分配的学员，newName是源课程名称
                        assignHoursRules.put(oldName, newName);
                    }
                    break;
            }
        }
        
        // 应用规则：先处理名称转换，最后过滤隐藏记录
        return schedules.stream()
            .map(schedule -> {
                String studentName = schedule.getStudentName();
                ScheduleWithCoachDTO newSchedule = new ScheduleWithCoachDTO(schedule, schedule.getCoachName());
                
                // 1. 先应用重命名规则
                if (renameRules.containsKey(studentName)) {
                    newSchedule.setStudentName(renameRules.get(studentName));
                    studentName = newSchedule.getStudentName(); // 更新当前名称
                }
                
                // 2. 再应用合并规则
                if (mergeRules.containsKey(studentName)) {
                    newSchedule.setStudentName(mergeRules.get(studentName));
                    studentName = newSchedule.getStudentName(); // 更新当前名称
                }
                
                // 3. 最后应用分配课时规则
                if (assignHoursRules.containsKey(studentName)) {
                    newSchedule.setStudentName(assignHoursRules.get(studentName));
                }
                
                return newSchedule;
            })
            .filter(schedule -> {
                // 4. 最后过滤掉被隐藏的学员（基于最终转换后的名称）
                String finalStudentName = schedule.getStudentName();
                
                // 直接检查最终名称是否在隐藏列表中
                if (hiddenStudents.contains(finalStudentName)) {
                    return false;
                }
                
                // 还需要检查原始名称是否在隐藏列表中
                String originalStudentName = schedule.getStudentName();
                // 这里需要反向查找原始名称
                // 由于我们已经应用了所有转换规则，需要检查是否有原始名称被隐藏
                for (String hiddenName : hiddenStudents) {
                    // 检查是否通过任何规则转换到了隐藏的学员
                    if (renameRules.containsKey(hiddenName) && renameRules.get(hiddenName).equals(originalStudentName)) {
                        return false;
                    }
                    if (mergeRules.containsKey(hiddenName) && mergeRules.get(hiddenName).equals(originalStudentName)) {
                        return false;
                    }
                    if (assignHoursRules.containsKey(hiddenName) && assignHoursRules.get(hiddenName).equals(originalStudentName)) {
                        return false;
                    }
                }
                
                return true;
            })
            .collect(java.util.stream.Collectors.toList());
    }
}


