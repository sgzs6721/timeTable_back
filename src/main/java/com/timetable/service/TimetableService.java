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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

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
        // 判断是否已有活动课表
        List<Timetables> userTables = timetableRepository.findByUserId(userId)
            .stream().filter(t -> t.getIsActive() != null && t.getIsActive() == 1).collect(Collectors.toList());
        if (userTables.isEmpty()) {
            timetable.setIsActive((byte) 1);
        } else {
            timetable.setIsActive((byte) 0);
        }
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
     * 软删除课表
     */
    public boolean deleteTimetable(Long timetableId, Long userId) {
        Timetables timetable = timetableRepository.findByIdAndUserId(timetableId, userId);
        if (timetable == null) {
            return false;
        }
        
        // 检查是否已经软删除
        if (timetable.getIsDeleted() != null && timetable.getIsDeleted() == 1) {
            return false;  // 已经删除了
        }
        
        // 软删除：将is_deleted字段置为1
        timetable.setIsDeleted((byte) 1);
        timetable.setDeletedAt(LocalDateTime.now());
        timetable.setUpdatedAt(LocalDateTime.now());
        
        // 保存修改
        timetableRepository.save(timetable);
        
        return true;
    }
    
    /**
     * 检查课表是否存在且属于用户
     */
    public boolean isUserTimetable(Long timetableId, Long userId) {
        return timetableRepository.existsByIdAndUserId(timetableId, userId);
    }
    
    /**
     * 获取所有课表（管理员功能）- 包括已软删除的课表，便于管理员查看
     */
    public List<Timetables> getAllTimetables() {
        return timetableRepository.findAll();
    }
    
    /**
     * 设为活动课表（每个用户只能有一个活动课表）
     */
    @Transactional
    public boolean setActiveTimetable(Long timetableId, Long userId) {
        Timetables t = timetableRepository.findByIdAndUserId(timetableId, userId);
        if (t == null || Boolean.TRUE.equals(t.getIsDeleted())) return false;
        timetableRepository.clearActiveForUser(userId);
        t.setIsActive((byte) 1);
        t.setUpdatedAt(java.time.LocalDateTime.now());
        timetableRepository.save(t);
        return true;
    }

    /**
     * 归档课表
     */
    public boolean archiveTimetable(Long timetableId, Long userId) {
        Timetables t = timetableRepository.findByIdAndUserId(timetableId, userId);
        if (t == null || Boolean.TRUE.equals(t.getIsDeleted())) return false;
        t.setIsArchived((byte) 1);
        t.setUpdatedAt(java.time.LocalDateTime.now());
        timetableRepository.save(t);
        return true;
    }

    /**
     * 恢复归档课表
     */
    public boolean restoreTimetable(Long timetableId, Long userId) {
        Timetables t = timetableRepository.findByIdAndUserId(timetableId, userId);
        if (t == null || Boolean.TRUE.equals(t.getIsDeleted())) return false;
        t.setIsArchived((byte) 0);
        t.setUpdatedAt(java.time.LocalDateTime.now());
        timetableRepository.save(t);
        return true;
    }
    
    /**
     * 获取所有课表并附带用户名、课程数量（管理员功能）
     */
    public List<AdminTimetableDTO> getAllTimetablesWithUser() {
        List<Timetables> timetables = getAllTimetables(); // 获取所有有效课表
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
                    t.getCreatedAt(),
                    t.getIsActive(),
                    t.getIsArchived()
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
                    t.getCreatedAt(),
                    t.getIsActive(),
                    t.getIsArchived()
            );
        }).collect(java.util.stream.Collectors.toList());
    }

    public List<AdminTimetableDTO> findArchivedByUserId(Long userId) {
        List<Timetables> timetables = timetableRepository.findArchivedByUserId(userId);
        String username = null;
        try {
            com.timetable.generated.tables.pojos.Users u = userService.findById(userId);
            if (u != null) {
                username = u.getUsername();
            }
        } catch (Exception ignored) {}

        String finalUsername = username;
        return timetables.stream().map(t -> new AdminTimetableDTO(
                t.getId(),
                t.getUserId(),
                finalUsername,
                t.getName(),
                t.getIsWeekly() != null && t.getIsWeekly() == 1,
                t.getStartDate(),
                t.getEndDate(),
                0, // scheduleCount not needed for this view
                t.getCreatedAt(),
                t.getIsActive(),
                t.getIsArchived()
        )).collect(Collectors.toList());
    }

    /**
     * 批量删除课表
     */
    public int batchDeleteTimetables(List<Long> ids, Long userId) {
        return timetableRepository.batchDeleteByIdsAndUserId(ids, userId);
    }

    /**
     * 批量恢复课表
     */
    public int batchRestoreTimetables(List<Long> ids, Long userId) {
        return timetableRepository.batchRestoreByIdsAndUserId(ids, userId);
    }
}