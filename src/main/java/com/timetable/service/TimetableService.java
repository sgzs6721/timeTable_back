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
import java.time.format.DateTimeFormatter;
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
     * 获取用户的课表列表（过滤已软删除的课表）
     */
    public List<Timetables> getUserTimetables(Long userId) {
        List<Timetables> allTimetables = timetableRepository.findByUserId(userId);
        // 过滤掉已软删除的课表（名称以[DELETED_开头的）
        return allTimetables.stream()
                .filter(t -> t.getName() != null && !t.getName().startsWith("[DELETED_"))
                .collect(java.util.stream.Collectors.toList());
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
        Timetables timetable = timetableRepository.findByIdAndUserId(timetableId, userId);
        // 检查是否已软删除
        if (timetable != null && timetable.getName() != null && timetable.getName().startsWith("[DELETED_")) {
            return null;  // 软删除的课表不返回
        }
        return timetable;
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
        
        // 检查是否已软删除
        if (timetable.getName() != null && timetable.getName().startsWith("[DELETED_")) {
            return null;  // 软删除的课表不能修改
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
     * 软删除课表（临时实现，使用name字段标记）
     */
    public boolean deleteTimetable(Long timetableId, Long userId) {
        Timetables timetable = timetableRepository.findByIdAndUserId(timetableId, userId);
        if (timetable == null) {
            return false;
        }
        
        // 检查是否已经软删除
        if (timetable.getName() != null && timetable.getName().startsWith("[DELETED_")) {
            return false;  // 已经删除了
        }
        
        // 使用课表名称前缀标记为已删除，同时记录删除时间
        String deletedName = "[DELETED_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + "] " + timetable.getName();
        timetable.setName(deletedName);
        timetable.setUpdatedAt(LocalDateTime.now());
        
        // 软删除相关的排课（也使用类似的标记方式）
        scheduleRepository.softDeleteByTimetableId(timetableId);
        
        // 保存修改后的课表
        timetableRepository.save(timetable);
        
        return true;
    }
    
    /**
     * 检查课表是否存在且属于用户（不包括已软删除的）
     */
    public boolean isUserTimetable(Long timetableId, Long userId) {
        Timetables timetable = timetableRepository.findByIdAndUserId(timetableId, userId);
        if (timetable == null) {
            return false;
        }
        // 检查是否已软删除
        return timetable.getName() == null || !timetable.getName().startsWith("[DELETED_");
    }
    
    /**
     * 获取所有课表（管理员功能）- 包括已软删除的课表，便于管理员查看
     */
    public List<Timetables> getAllTimetables() {
        return timetableRepository.findAll();
    }
    
    /**
     * 获取有效课表（管理员功能）- 不包括已软删除的课表
     */
    public List<Timetables> getActiveTimetables() {
        List<Timetables> allTimetables = timetableRepository.findAll();
        return allTimetables.stream()
                .filter(t -> t.getName() == null || !t.getName().startsWith("[DELETED_"))
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 合并课表（管理员功能）
     */
    public Timetables mergeTimetables(List<Long> timetableIds, String mergedName, String description, Long adminUserId) {
        List<Timetables> timetablesToMerge = timetableRepository.findByIdIn(timetableIds);
        
        if (timetablesToMerge.isEmpty()) {
            return null;
        }
        
        // 过滤掉已软删除的课表
        timetablesToMerge = timetablesToMerge.stream()
                .filter(t -> t.getName() == null || !t.getName().startsWith("[DELETED_"))
                .collect(java.util.stream.Collectors.toList());
        
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
            // 跳过已软删除的排课
            if (schedule.getNote() != null && schedule.getNote().contains("[DELETED_")) {
                continue;
            }
            
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
        List<Timetables> timetables = getActiveTimetables(); // 只显示有效课表
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
                // 统计有效的排课数量（不包括软删除的）
                List<Schedules> schedules = scheduleRepository.findByTimetableId(t.getId());
                scheduleCount = (int) schedules.stream()
                        .filter(s -> s.getNote() == null || !s.getNote().contains("[DELETED_"))
                        .count();
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
        // 过滤掉已软删除的课表
        timetables = timetables.stream()
                .filter(t -> t.getName() == null || !t.getName().startsWith("[DELETED_"))
                .collect(java.util.stream.Collectors.toList());
                
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
                // 统计有效的排课数量（不包括软删除的）
                List<Schedules> schedules = scheduleRepository.findByTimetableId(t.getId());
                scheduleCount = (int) schedules.stream()
                        .filter(s -> s.getNote() == null || !s.getNote().contains("[DELETED_"))
                        .count();
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