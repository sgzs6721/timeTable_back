package com.timetable.service;

import com.timetable.dto.TimetableRequest;
import com.timetable.generated.tables.pojos.Timetables;
import com.timetable.repository.TimetableRepository;
import com.timetable.repository.ScheduleRepository;
import com.timetable.generated.tables.pojos.Schedules;
import com.timetable.dto.AdminTimetableDTO;
import com.timetable.service.UserService;
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
    
    @Autowired
    private UserService userService;
    
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
        timetable.setCreatedAt(LocalDateTime.now());
        timetable.setUpdatedAt(LocalDateTime.now());
        return timetableRepository.save(timetable);
    }
    
    /**
     * 获取课表详情
     */
    public Timetables getTimetable(Long timetableId, Long userId) {
        return timetableRepository.findByIdAndUserId(timetableId, userId);
    }
    
    /**
     * 管理员直接按ID获取课表，不校验用户
     */
    public Timetables getTimetableById(Long id) {
        return timetableRepository.findById(id);
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
    
    /**
     * 获取所有课表并附带用户名、课程数量（管理员功能）
     */
    public List<AdminTimetableDTO> getAllTimetablesWithUser() {
        List<Timetables> timetables = timetableRepository.findAll();
        return timetables.stream().map(t -> {
            String username = null;
            try {
                com.timetable.generated.tables.pojos.Users u = userService.findById(t.getUserId());
                if (u != null) {
                    username = u.getUsername();
                }
            } catch (Exception ignored) {}

            int scheduleCount = 0;
            try {
                scheduleCount = scheduleRepository.findByTimetableId(t.getId()).size();
            } catch (Exception ignored) {}

            return new AdminTimetableDTO(
                    t.getId(),
                    t.getUserId(),
                    username,
                    t.getName(),
                    t.getIsWeekly() != null && t.getIsWeekly() == 1,
                    t.getStartDate(),
                    t.getEndDate(),
                    scheduleCount,
                    t.getCreatedAt()
            );
        }).collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 根据ID列表批量获取课表信息（包含用户信息）
     */
    public List<AdminTimetableDTO> getTimetablesByIds(List<Long> timetableIds) {
        List<Timetables> timetables = timetableRepository.findByIdIn(timetableIds);
        return timetables.stream().map(t -> {
            String username = null;
            try {
                com.timetable.generated.tables.pojos.Users u = userService.findById(t.getUserId());
                if (u != null) {
                    username = u.getUsername();
                }
            } catch (Exception ignored) {}

            int scheduleCount = 0;
            try {
                scheduleCount = scheduleRepository.findByTimetableId(t.getId()).size();
            } catch (Exception ignored) {}

            return new AdminTimetableDTO(
                    t.getId(),
                    t.getUserId(),
                    username,
                    t.getName(),
                    t.getIsWeekly() != null && t.getIsWeekly() == 1,
                    t.getStartDate(),
                    t.getEndDate(),
                    scheduleCount,
                    t.getCreatedAt()
            );
        }).collect(java.util.stream.Collectors.toList());
    }
} 