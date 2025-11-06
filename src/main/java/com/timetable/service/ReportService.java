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

    public Map<String, Object> queryHoursPaged(Long userId, Long organizationId, LocalDate start, LocalDate end, int page, int size, String sortOrder) {
        // 先获取所有记录
        List<ScheduleWithCoachDTO> allSchedules = reportRepository.querySchedulesByUserPaged(userId, organizationId, start, end, 1, Integer.MAX_VALUE, sortOrder);
        
        // 应用学员操作规则（过滤隐藏的学员等）
        allSchedules = applyStudentOperationRules(allSchedules, userId);
        
        // 过滤掉占用时间段（studentName为"【占用】"的记录，待数据库迁移完成后使用isTimeBlock字段）
        allSchedules = allSchedules.stream()
            .filter(schedule -> !"【占用】".equals(schedule.getStudentName()))
            .collect(java.util.stream.Collectors.toList());
        
        // 计算应用规则后的总记录数
        long filteredTotal = allSchedules.size();
        
        // 计算所有记录的课时总数（用于总计显示）
        double grandTotalHours = 0.0;
        for (ScheduleWithCoachDTO schedule : allSchedules) {
            if (schedule.getStartTime() != null && schedule.getEndTime() != null) {
                long durationMinutes = java.time.Duration.between(schedule.getStartTime(), schedule.getEndTime()).toMinutes();
                grandTotalHours += durationMinutes / 60.0;
            } else {
                grandTotalHours += 1.0;
            }
        }
        
        // 手动分页：计算起始和结束索引
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, allSchedules.size());
        
        // 获取当前页的数据
        List<ScheduleWithCoachDTO> list;
        if (startIndex >= allSchedules.size()) {
            list = new java.util.ArrayList<>();
        } else {
            list = allSchedules.subList(startIndex, endIndex);
        }
        
        // 计算当前页的课时数
        double totalHours = 0.0;
        for (ScheduleWithCoachDTO schedule : list) {
            if (schedule.getStartTime() != null && schedule.getEndTime() != null) {
                long durationMinutes = java.time.Duration.between(schedule.getStartTime(), schedule.getEndTime()).toMinutes();
                totalHours += durationMinutes / 60.0;
            } else {
                totalHours += 1.0;
            }
        }
        
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", filteredTotal); // 使用过滤后的总数
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
                
                // 3. 分配课时规则不应该改变学员名称显示，只影响课时计算
                // 这里暂时注释掉，因为分配课时规则应该只影响课时统计，不应该改变学员名称
                // if (assignHoursRules.containsKey(studentName)) {
                //     newSchedule.setStudentName(assignHoursRules.get(studentName));
                // }
                
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


