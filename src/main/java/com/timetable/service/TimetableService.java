package com.timetable.service;

import com.timetable.dto.TimetableRequest;
import com.timetable.generated.tables.pojos.Timetables;
import com.timetable.repository.TimetableRepository;
import com.timetable.repository.ScheduleRepository;
import com.timetable.generated.tables.pojos.Schedules;
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
    public List<Timetables> getUserTimetables(Long userId) {
        return timetableRepository.findByUserId(userId);
    }
    
    /**
     * 创建新课表
     */
    public Timetables createTimetable(Long userId, TimetableRequest request) {
        Timetables timetable = new Timetables();
        timetable.setUserId(userId);
        timetable.setName(request.getName());
        timetable.setDescription(request.getDescription());
        timetable.setIsWeekly((byte) (request.getType() == TimetableRequest.TimetableType.WEEKLY ? 1 : 0));
        timetable.setStartDate(request.getStartDate());
        timetable.setEndDate(request.getEndDate());
        
        return timetableRepository.save(timetable);
    }
    
    /**
     * 获取课表详情
     */
    public Timetables getTimetable(Long timetableId, Long userId) {
        return timetableRepository.findByIdAndUserId(timetableId, userId);
    }
    
    /**
     * 更新课表
     */
    public Timetables updateTimetable(Long timetableId, Long userId, TimetableRequest request) {
        Timetables timetable = timetableRepository.findByIdAndUserId(timetableId, userId);
        if (timetable == null) {
            return null;
        }
        
        timetable.setName(request.getName());
        timetable.setDescription(request.getDescription());
        timetable.setIsWeekly((byte) (request.getType() == TimetableRequest.TimetableType.WEEKLY ? 1 : 0));
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
    public List<Timetables> getAllTimetables() {
        return timetableRepository.findAll();
    }
    
    /**
     * 合并课表（管理员功能）
     */
    public Timetables mergeTimetables(List<Long> timetableIds, String mergedName, String description, Long adminUserId) {
        List<Timetables> timetablesToMerge = timetableRepository.findByIdIn(timetableIds);
        
        if (timetablesToMerge.isEmpty()) {
            return null;
        }
        
        // 创建新的合并课表
        Timetables mergedTimetable = new Timetables();
        mergedTimetable.setUserId(adminUserId);
        mergedTimetable.setName(mergedName);
        mergedTimetable.setDescription(description);
        mergedTimetable.setIsWeekly((byte) 1); // 合并后的课表默认为周固定
        
        mergedTimetable = timetableRepository.save(mergedTimetable);
        
        // 将所有相关的排课复制到新课表
        List<Schedules> allSchedules = scheduleRepository.findByTimetableIdIn(timetableIds);
        for (Schedules schedule : allSchedules) {
            Schedules newSchedule = new Schedules();
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
                            .map(Timetables::getName)
                            .orElse("未知课表") + "]");
            
            scheduleRepository.save(newSchedule);
        }
        
        return mergedTimetable;
    }
} 