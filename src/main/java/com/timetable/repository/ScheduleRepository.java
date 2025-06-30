package com.timetable.repository;

import com.timetable.model.Schedule;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 排课Repository - 内存实现（用于测试）
 */
@Repository
public class ScheduleRepository {
    
    private final Map<Long, Schedule> schedules = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    /**
     * 根据课表ID查找所有排课
     */
    public List<Schedule> findByTimetableId(Long timetableId) {
        return schedules.values().stream()
                .filter(schedule -> timetableId.equals(schedule.getTimetableId()))
                .collect(Collectors.toList());
    }
    
    /**
     * 根据课表ID和周数查找排课
     */
    public List<Schedule> findByTimetableIdAndWeekNumber(Long timetableId, Integer weekNumber) {
        return schedules.values().stream()
                .filter(schedule -> timetableId.equals(schedule.getTimetableId()) 
                        && (weekNumber == null || weekNumber.equals(schedule.getWeekNumber())))
                .collect(Collectors.toList());
    }
    
    /**
     * 根据ID查找排课
     */
    public Schedule findById(Long id) {
        return schedules.get(id);
    }
    
    /**
     * 根据ID和课表ID查找排课
     */
    public Schedule findByIdAndTimetableId(Long id, Long timetableId) {
        Schedule schedule = schedules.get(id);
        if (schedule != null && timetableId.equals(schedule.getTimetableId())) {
            return schedule;
        }
        return null;
    }
    
    /**
     * 保存排课
     */
    public Schedule save(Schedule schedule) {
        if (schedule.getId() == null) {
            schedule.setId(idGenerator.getAndIncrement());
            schedule.setCreatedAt(LocalDateTime.now());
        }
        schedule.setUpdatedAt(LocalDateTime.now());
        
        schedules.put(schedule.getId(), schedule);
        return schedule;
    }
    
    /**
     * 删除排课
     */
    public void deleteById(Long id) {
        schedules.remove(id);
    }
    
    /**
     * 根据课表ID删除所有排课
     */
    public void deleteByTimetableId(Long timetableId) {
        schedules.entrySet().removeIf(entry -> 
                timetableId.equals(entry.getValue().getTimetableId()));
    }
    
    /**
     * 检查排课是否存在
     */
    public boolean existsById(Long id) {
        return schedules.containsKey(id);
    }
    
    /**
     * 检查排课是否属于指定课表
     */
    public boolean existsByIdAndTimetableId(Long id, Long timetableId) {
        Schedule schedule = schedules.get(id);
        return schedule != null && timetableId.equals(schedule.getTimetableId());
    }
    
    /**
     * 根据课表ID列表查找所有排课
     */
    public List<Schedule> findByTimetableIdIn(List<Long> timetableIds) {
        return schedules.values().stream()
                .filter(schedule -> timetableIds.contains(schedule.getTimetableId()))
                .collect(Collectors.toList());
    }
} 