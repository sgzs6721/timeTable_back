package com.timetable.service;

import com.timetable.dto.TimetableRequest;
import com.timetable.model.Timetable;
import com.timetable.repository.TimetableRepository;
import com.timetable.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 课表服务
 */
@Service
public class TimetableService {
    
    @Autowired
    private TimetableRepository timetableRepository;
    
    @Autowired
    private ScheduleRepository scheduleRepository;
    
    /**
     * 获取用户的课表列表
     */
    public List<Timetable> getUserTimetables(Long userId) {
        return timetableRepository.findByUserId(userId);
    }
    
    /**
     * 创建新课表
     */
    public Timetable createTimetable(Long userId, TimetableRequest request) {
        Timetable timetable = new Timetable();
        timetable.setUserId(userId);
        timetable.setName(request.getName());
        timetable.setDescription(request.getDescription());
        timetable.setIsWeekly(request.getIsWeekly());
        timetable.setStartDate(request.getStartDate());
        timetable.setEndDate(request.getEndDate());
        
        return timetableRepository.save(timetable);
    }
    
    /**
     * 获取课表详情
     */
    public Timetable getTimetable(Long timetableId, Long userId) {
        return timetableRepository.findByIdAndUserId(timetableId, userId);
    }
    
    /**
     * 更新课表
     */
    public Timetable updateTimetable(Long timetableId, Long userId, TimetableRequest request) {
        Timetable timetable = timetableRepository.findByIdAndUserId(timetableId, userId);
        if (timetable == null) {
            return null;
        }
        
        timetable.setName(request.getName());
        timetable.setDescription(request.getDescription());
        timetable.setIsWeekly(request.getIsWeekly());
        timetable.setStartDate(request.getStartDate());
        timetable.setEndDate(request.getEndDate());
        timetable.setUpdatedAt(LocalDateTime.now());
        
        return timetableRepository.save(timetable);
    }
    
    /**
     * 删除课表
     */
    public boolean deleteTimetable(Long timetableId, Long userId) {
        if (!timetableRepository.existsByIdAndUserId(timetableId, userId)) {
            return false;
        }
        
        // 删除相关的排课
        scheduleRepository.deleteByTimetableId(timetableId);
        
        // 删除课表
        timetableRepository.deleteById(timetableId);
        
        return true;
    }
    
    /**
     * 检查课表是否存在且属于用户
     */
    public boolean isUserTimetable(Long timetableId, Long userId) {
        return timetableRepository.existsByIdAndUserId(timetableId, userId);
    }
    
    /**
     * 获取所有课表（管理员功能）
     */
    public List<Timetable> getAllTimetables() {
        return timetableRepository.findAll();
    }
    
    /**
     * 合并课表（管理员功能）
     */
    public Timetable mergeTimetables(List<Long> timetableIds, String mergedName, String description, Long adminUserId) {
        List<Timetable> timetablesToMerge = timetableRepository.findByIdIn(timetableIds);
        
        if (timetablesToMerge.isEmpty()) {
            return null;
        }
        
        // 创建新的合并课表
        Timetable mergedTimetable = new Timetable();
        mergedTimetable.setUserId(adminUserId);
        mergedTimetable.setName(mergedName);
        mergedTimetable.setDescription(description);
        mergedTimetable.setIsWeekly(true); // 合并后的课表默认为周固定
        
        mergedTimetable = timetableRepository.save(mergedTimetable);
        
        // 将所有相关的排课复制到新课表
        List<com.timetable.model.Schedule> allSchedules = scheduleRepository.findByTimetableIdIn(timetableIds);
        for (com.timetable.model.Schedule schedule : allSchedules) {
            com.timetable.model.Schedule newSchedule = new com.timetable.model.Schedule();
            newSchedule.setTimetableId(mergedTimetable.getId());
            newSchedule.setStudentName(schedule.getStudentName());
            newSchedule.setSubject(schedule.getSubject());
            newSchedule.setDayOfWeek(schedule.getDayOfWeek());
            newSchedule.setStartTime(schedule.getStartTime());
            newSchedule.setEndTime(schedule.getEndTime());
            newSchedule.setWeekNumber(schedule.getWeekNumber());
            newSchedule.setScheduleDate(schedule.getScheduleDate());
            newSchedule.setNote(schedule.getNote() + " [来自: " + 
                    timetablesToMerge.stream()
                            .filter(t -> t.getId().equals(schedule.getTimetableId()))
                            .findFirst()
                            .map(Timetable::getName)
                            .orElse("未知课表") + "]");
            
            scheduleRepository.save(newSchedule);
        }
        
        return mergedTimetable;
    }
} 