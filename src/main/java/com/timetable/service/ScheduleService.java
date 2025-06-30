package com.timetable.service;

import com.timetable.model.Schedule;
import com.timetable.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 课程安排服务
 */
@Service
public class ScheduleService {
    
    @Autowired
    private ScheduleRepository scheduleRepository;
    
    /**
     * 保存课程安排列表
     */
    public List<Schedule> saveSchedules(List<Schedule> schedules) {
        return scheduleRepository.saveAll(schedules);
    }
    
    /**
     * 根据课程表ID获取课程安排
     */
    public List<Schedule> getSchedulesByTimetableId(Long timetableId, Integer week) {
        if (week != null) {
            return scheduleRepository.findByTimetableIdAndWeekNumber(timetableId, week);
        } else {
            return scheduleRepository.findByTimetableId(timetableId);
        }
    }
    
    /**
     * 删除课程安排
     */
    public boolean deleteSchedule(Long scheduleId, Long timetableId) {
        Schedule schedule = scheduleRepository.findById(scheduleId);
        if (schedule != null && schedule.getTimetableId().equals(timetableId)) {
            scheduleRepository.delete(scheduleId);
            return true;
        }
        return false;
    }
    
    /**
     * 更新课程安排
     */
    public Schedule updateSchedule(Long scheduleId, Long timetableId, Map<String, Object> scheduleData) {
        Schedule schedule = scheduleRepository.findById(scheduleId);
        if (schedule != null && schedule.getTimetableId().equals(timetableId)) {
            // 更新字段
            if (scheduleData.containsKey("studentName")) {
                schedule.setStudentName((String) scheduleData.get("studentName"));
            }
            if (scheduleData.containsKey("subject")) {
                schedule.setSubject((String) scheduleData.get("subject"));
            }
            if (scheduleData.containsKey("note")) {
                schedule.setNote((String) scheduleData.get("note"));
            }
            // 可以添加更多字段的更新逻辑
            
            return scheduleRepository.save(schedule);
        }
        return null;
    }
} 