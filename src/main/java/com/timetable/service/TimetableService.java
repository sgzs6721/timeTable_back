package com.timetable.service;

import com.timetable.dto.TimetableRequest;
import com.timetable.generated.tables.pojos.Timetables;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.repository.TimetableRepository;
import com.timetable.repository.ScheduleRepository;
import com.timetable.repository.WeeklyInstanceScheduleRepository;
import com.timetable.repository.WeeklyInstanceRepository;
import com.timetable.generated.tables.pojos.Schedules;
import com.timetable.entity.WeeklyInstance;
import com.timetable.entity.WeeklyInstanceSchedule;
import com.timetable.dto.AdminTimetableDTO;
import com.timetable.service.UserService;
import com.timetable.service.WeeklyInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private WeeklyInstanceScheduleRepository weeklyInstanceScheduleRepository;

    @Autowired
    private WeeklyInstanceRepository weeklyInstanceRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private WeeklyInstanceService weeklyInstanceService;

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
        // 检查非归档课表数量
        long nonArchivedCount = timetableRepository.findByUserId(userId).stream()
                .filter(t -> t.getIsArchived() == null || t.getIsArchived() == 0)
                .count();

        if (nonArchivedCount >= 5) {
            throw new IllegalStateException("每个用户最多只能创建5个非归档课表");
        }

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
     * 管理员更新课表
     */
    public Timetables updateTimetableByAdmin(Long timetableId, TimetableRequest request) {
        Timetables timetable = timetableRepository.findById(timetableId);
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
    @Transactional
    public boolean deleteTimetable(Long timetableId, Long userId) {
        Timetables timetable = timetableRepository.findByIdAndUserId(timetableId, userId);
        if (timetable == null) {
            return false;
        }

        // 检查是否已经软删除
        if (timetable.getIsDeleted() != null && timetable.getIsDeleted() == 1) {
            return false;  // 已经删除了
        }

        // 检查是否为活动课表
        boolean isActiveTimetable = timetable.getIsActive() != null && timetable.getIsActive() == 1;

        // 软删除：将is_deleted字段置为1
        timetable.setIsDeleted((byte) 1);
        timetable.setDeletedAt(LocalDateTime.now());
        timetable.setUpdatedAt(LocalDateTime.now());
        
        // 如果删除的是活动课表，需要清除活动状态
        if (isActiveTimetable) {
            timetable.setIsActive((byte) 0);
        }

        // 保存修改
        timetableRepository.save(timetable);

        // 如果删除的是活动课表，需要设置另一个课表为活动状态
        if (isActiveTimetable) {
            // 获取该用户的所有非归档、非删除的课表
            List<Timetables> availableTimetables = timetableRepository.findByUserId(userId)
                    .stream()
                    .filter(t -> !t.getId().equals(timetableId)) // 排除刚删除的课表
                    .filter(t -> t.getIsArchived() == null || t.getIsArchived() == 0) // 非归档
                    .filter(t -> t.getIsDeleted() == null || t.getIsDeleted() == 0) // 非删除
                    .collect(Collectors.toList());

            // 如果还有其他课表，设置第一个为活动状态
            if (!availableTimetables.isEmpty()) {
                Timetables newActiveTimetable = availableTimetables.get(0);
                newActiveTimetable.setIsActive((byte) 1);
                newActiveTimetable.setUpdatedAt(LocalDateTime.now());
                timetableRepository.save(newActiveTimetable);
            }
        }

        return true;
    }

    /**
     * 检查课表是否存在且属于用户
     */
    public boolean isUserTimetable(Long timetableId, Long userId) {
        return timetableRepository.existsByIdAndUserId(timetableId, userId);
    }

    /**
     * 设为活动课表（每个用户只能有一个活动课表）
     */
    @Transactional
    public Timetables setTimetableActive(Long timetableId) {
        Timetables targetTimetable = timetableRepository.findById(timetableId);
        if (targetTimetable == null) {
            throw new IllegalArgumentException("课表不存在");
        }

        // 找到该用户的所有其他活动课表并设为非活动
        List<Timetables> activeTimetables = timetableRepository.findByUserId(targetTimetable.getUserId())
                .stream()
                .filter(t -> t.getIsActive() != null && t.getIsActive() == 1 && !t.getId().equals(timetableId))
                .collect(Collectors.toList());

        for (Timetables t : activeTimetables) {
            t.setIsActive((byte) 0);
            timetableRepository.save(t);
        }

        // 将目标课表设为活动
        targetTimetable.setIsActive((byte) 1);
        return timetableRepository.save(targetTimetable);
    }

    /**
     * 获取所有课表（管理员功能）- 包括已软删除的课表，便于管理员查看
     */
    public List<Timetables> getAllTimetables() {
        return timetableRepository.findAll();
    }

    /**
     * 获取所有活动课表（管理员功能）
     */
    public List<AdminTimetableDTO> getActiveTimetables() {
        List<Timetables> activeTimetables = timetableRepository.findAll()
                .stream()
                .filter(t -> t.getIsActive() != null && t.getIsActive() == 1)
                .filter(t -> t.getIsDeleted() == null || t.getIsDeleted() == 0)
                .filter(t -> t.getIsArchived() == null || t.getIsArchived() == 0)
                .collect(Collectors.toList());

        return activeTimetables.stream()
                .map(timetable -> {
                    Users user = userService.findById(timetable.getUserId());
                    AdminTimetableDTO dto = new AdminTimetableDTO();
                    dto.setId(timetable.getId());
                    dto.setName(timetable.getName());
                    dto.setIsWeekly(timetable.getIsWeekly() != null && timetable.getIsWeekly() == 1);
                    dto.setStartDate(timetable.getStartDate());
                    dto.setEndDate(timetable.getEndDate());
                    dto.setIsActive(timetable.getIsActive());
                    dto.setIsArchived(timetable.getIsArchived());
                    dto.setCreatedAt(timetable.getCreatedAt());
                    if (user != null) {
                        dto.setUsername(user.getUsername());
                        dto.setNickname(user.getNickname());
                    }
                    return dto;
                })
                .collect(Collectors.toList());
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
    @Transactional
    public boolean archiveTimetable(Long timetableId, Long userId) {
        Timetables t = timetableRepository.findByIdAndUserId(timetableId, userId);
        if (t == null || Boolean.TRUE.equals(t.getIsDeleted())) return false;
        
        // 检查是否为活动课表
        boolean isActiveTimetable = t.getIsActive() != null && t.getIsActive() == 1;
        
        // 归档操作
        t.setIsArchived((byte) 1);
        
        // 如果归档的是活动课表，需要清除活动状态
        if (isActiveTimetable) {
            t.setIsActive((byte) 0);
        }
        
        t.setUpdatedAt(java.time.LocalDateTime.now());
        timetableRepository.save(t);
        
        // 如果归档的是活动课表，需要设置另一个课表为活动状态
        if (isActiveTimetable) {
            // 获取该用户的所有非归档、非删除的课表
            List<Timetables> availableTimetables = timetableRepository.findByUserId(userId)
                    .stream()
                    .filter(t2 -> !t2.getId().equals(timetableId)) // 排除刚归档的课表
                    .filter(t2 -> t2.getIsArchived() == null || t2.getIsArchived() == 0) // 非归档
                    .filter(t2 -> t2.getIsDeleted() == null || t2.getIsDeleted() == 0) // 非删除
                    .collect(Collectors.toList());

            // 如果还有其他课表，设置第一个为活动状态
            if (!availableTimetables.isEmpty()) {
                Timetables newActiveTimetable = availableTimetables.get(0);
                newActiveTimetable.setIsActive((byte) 1);
                newActiveTimetable.setUpdatedAt(java.time.LocalDateTime.now());
                timetableRepository.save(newActiveTimetable);
            }
        }
        
        return true;
    }

    /**
     * 恢复归档课表
     */
    @Transactional
    public boolean restoreTimetable(Long timetableId, Long userId) {
        long nonArchivedCount = timetableRepository.findByUserId(userId).stream()
                .filter(t -> t.getIsArchived() == null || t.getIsArchived() == 0)
                .count();
        if (nonArchivedCount >= 5) {
            throw new IllegalStateException("无法恢复，非归档课表数量已达上限 (5个)");
        }

        Timetables t = timetableRepository.findByIdAndUserId(timetableId, userId);
        if (t == null || Boolean.TRUE.equals(t.getIsDeleted())) return false;
        
        // 检查用户是否有活动课表
        boolean hasActiveTimetable = timetableRepository.findByUserId(userId).stream()
                .anyMatch(timetable -> timetable.getIsActive() != null && timetable.getIsActive() == 1);
        
        t.setIsArchived((byte) 0);
        
        // 如果用户没有活动课表，将恢复的课表设为活动状态
        if (!hasActiveTimetable) {
            t.setIsActive((byte) 1);
        }
        
        t.setUpdatedAt(java.time.LocalDateTime.now());
        timetableRepository.save(t);
        return true;
    }

    /**
     * 获取所有课表并附带用户名、课程数量（管理员功能）
     */
    public List<AdminTimetableDTO> getAllTimetablesWithUser() {
        return getAllTimetablesWithUser(false);
    }
    
    /**
     * 获取活动课表原始数据（管理员功能）
     */
    public List<Timetables> getActiveTimetablesRaw() {
        return timetableRepository.findAll()
                .stream()
                .filter(t -> t.getIsActive() != null && t.getIsActive() == 1)
                .filter(t -> t.getIsDeleted() == null || t.getIsDeleted() == 0)
                .filter(t -> t.getIsArchived() == null || t.getIsArchived() == 0)
                .collect(Collectors.toList());
    }

    /**
     * 获取课表并附带用户名、课程数量（管理员功能）
     * @param activeOnly 是否只返回活动课表
     */
    public List<AdminTimetableDTO> getAllTimetablesWithUser(boolean activeOnly) {
        List<Timetables> timetables = activeOnly ? getActiveTimetablesRaw() : getAllTimetables();
        return timetables.stream()
            .map(t -> {
                String username = null;
                String nickname = null;
                try {
                    com.timetable.generated.tables.pojos.Users u = userService.findById(t.getUserId());
                    if (u != null && (u.getIsDeleted() == null || u.getIsDeleted() == 0)) {
                        username = u.getUsername();
                        nickname = u.getNickname();
                    } else {
                        // 如果用户已被软删除，跳过这个课表
                        return null;
                    }
                } catch (Exception ignored) {
                    // 如果获取用户信息失败，跳过这个课表
                    return null;
                }

                int scheduleCount = 0;
                try {
                    scheduleCount = scheduleRepository.findByTimetableId(t.getId()).size();
                } catch (Exception ignored) {}

                return new AdminTimetableDTO(
                        t.getId(),
                        t.getUserId(),
                        username,
                        nickname,
                        t.getName(),
                        t.getIsWeekly() != null && t.getIsWeekly() == 1,
                        t.getStartDate(),
                        t.getEndDate(),
                        scheduleCount,
                        t.getCreatedAt(),
                        t.getIsActive(),
                        t.getIsArchived()
                );
            })
            .filter(dto -> dto != null) // 过滤掉null值（被软删除用户的课表）
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 根据ID列表批量获取课表信息（包含用户信息）
     */
    public List<AdminTimetableDTO> getTimetablesByIds(List<Long> timetableIds) {
        List<Timetables> timetables = timetableRepository.findByIdIn(timetableIds);

        return timetables.stream()
            .map(t -> {
                String username = null;
                String nickname = null;
                try {
                    com.timetable.generated.tables.pojos.Users u = userService.findById(t.getUserId());
                    if (u != null && (u.getIsDeleted() == null || u.getIsDeleted() == 0)) {
                        username = u.getUsername();
                        nickname = u.getNickname();
                    } else {
                        // 如果用户已被软删除，跳过这个课表
                        return null;
                    }
                } catch (Exception ignored) {
                    // 如果获取用户信息失败，跳过这个课表
                    return null;
                }

                int scheduleCount = 0;
                try {
                    scheduleCount = scheduleRepository.findByTimetableId(t.getId()).size();
                } catch (Exception ignored) {}

                return new AdminTimetableDTO(
                        t.getId(),
                        t.getUserId(),
                        username,
                        nickname,
                        t.getName(),
                        t.getIsWeekly() != null && t.getIsWeekly() == 1,
                        t.getStartDate(),
                        t.getEndDate(),
                        scheduleCount,
                        t.getCreatedAt(),
                        t.getIsActive(),
                        t.getIsArchived()
                );
            })
            .filter(dto -> dto != null) // 过滤掉null值（被软删除用户的课表）
            .collect(java.util.stream.Collectors.toList());
    }

    public List<AdminTimetableDTO> findArchivedByUserId(Long userId) {
        List<Timetables> timetables = timetableRepository.findArchivedByUserId(userId);
        String username = null;
        String nickname = null;
        try {
            com.timetable.generated.tables.pojos.Users u = userService.findById(userId);
            if (u != null && (u.getIsDeleted() == null || u.getIsDeleted() == 0)) {
                username = u.getUsername();
                nickname = u.getNickname();
            } else {
                // 如果用户已被软删除，返回空列表
                return new ArrayList<>();
            }
        } catch (Exception ignored) {
            // 如果获取用户信息失败，返回空列表
            return new ArrayList<>();
        }

        String finalUsername = username;
        String finalNickname = nickname;
        return timetables.stream().map(t -> new AdminTimetableDTO(
                t.getId(),
                t.getUserId(),
                finalUsername,
                finalNickname,
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
     * 查找所有用户的归档课表（管理员专用）
     */
    public List<AdminTimetableDTO> findAllArchivedTimetables() {
        List<Timetables> timetables = timetableRepository.findAllArchivedTimetables();
        
        // 按用户ID分组，批量获取用户信息
        Map<Long, List<Timetables>> timetablesByUser = timetables.stream()
                .collect(Collectors.groupingBy(Timetables::getUserId));
        
        Map<Long, com.timetable.generated.tables.pojos.Users> userMap = new HashMap<>();
        for (Long userId : timetablesByUser.keySet()) {
            try {
                com.timetable.generated.tables.pojos.Users user = userService.findById(userId);
                if (user != null && (user.getIsDeleted() == null || user.getIsDeleted() == 0)) {
                    userMap.put(userId, user);
                }
            } catch (Exception ignored) {
                // 忽略获取用户信息失败的情况
            }
        }
        
        return timetables.stream()
                .filter(t -> userMap.containsKey(t.getUserId())) // 只包含有效用户的课表
                .map(t -> {
                    com.timetable.generated.tables.pojos.Users user = userMap.get(t.getUserId());
                    return new AdminTimetableDTO(
                            t.getId(),
                            t.getUserId(),
                            user.getUsername(),
                            user.getNickname(),
                            t.getName(),
                            t.getIsWeekly() != null && t.getIsWeekly() == 1,
                            t.getStartDate(),
                            t.getEndDate(),
                            0, // scheduleCount not needed for this view
                            t.getCreatedAt(),
                            t.getIsActive(),
                            t.getIsArchived()
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * 批量删除课表
     */
    @Transactional
    public int batchDeleteTimetables(List<Long> ids, Long userId) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        
        // 检查是否有活动课表要被删除
        List<Timetables> timetablesToDelete = timetableRepository.findByIdIn(ids)
                .stream()
                .filter(t -> t.getUserId().equals(userId))
                .collect(Collectors.toList());
        
        boolean hasActiveTimetable = timetablesToDelete.stream()
                .anyMatch(t -> t.getIsActive() != null && t.getIsActive() == 1);
        
        // 执行批量删除
        int deletedCount = timetableRepository.batchDeleteByIdsAndUserId(ids, userId);
        
        // 如果删除了活动课表，需要设置另一个课表为活动状态
        if (hasActiveTimetable && deletedCount > 0) {
            // 获取该用户的所有非归档、非删除的课表
            List<Timetables> availableTimetables = timetableRepository.findByUserId(userId)
                    .stream()
                    .filter(t -> !ids.contains(t.getId())) // 排除刚删除的课表
                    .filter(t -> t.getIsArchived() == null || t.getIsArchived() == 0) // 非归档
                    .filter(t -> t.getIsDeleted() == null || t.getIsDeleted() == 0) // 非删除
                    .collect(Collectors.toList());

            // 如果还有其他课表，设置第一个为活动状态
            if (!availableTimetables.isEmpty()) {
                Timetables newActiveTimetable = availableTimetables.get(0);
                newActiveTimetable.setIsActive((byte) 1);
                newActiveTimetable.setUpdatedAt(LocalDateTime.now());
                timetableRepository.save(newActiveTimetable);
            }
        }
        
        return deletedCount;
    }

    /**
     * 管理员归档课表（不需要用户ID验证）
     */
    @Transactional
    public boolean archiveTimetableByAdmin(Long timetableId) {
        Timetables timetable = timetableRepository.findById(timetableId);
        if (timetable == null) {
            return false;
        }
        if (timetable.getIsDeleted() != null && timetable.getIsDeleted() == 1) {
            return false;
        }
        if (timetable.getIsArchived() != null && timetable.getIsArchived() == 1) {
            return false; // 已经归档了
        }
        
        boolean isActiveTimetable = timetable.getIsActive() != null && timetable.getIsActive() == 1;
        
        // 归档操作
        timetable.setIsArchived((byte) 1);
        
        // 如果归档的是活动课表，需要清除活动状态
        if (isActiveTimetable) {
            timetable.setIsActive((byte) 0);
        }
        
        timetable.setUpdatedAt(LocalDateTime.now());
        timetableRepository.save(timetable);
        
        // 如果归档的是活动课表，需要设置另一个课表为活动状态
        if (isActiveTimetable) {
            List<Timetables> availableTimetables = timetableRepository.findByUserId(timetable.getUserId())
                    .stream()
                    .filter(t -> !t.getId().equals(timetableId))
                    .filter(t -> t.getIsArchived() == null || t.getIsArchived() == 0)
                    .filter(t -> t.getIsDeleted() == null || t.getIsDeleted() == 0)
                    .collect(Collectors.toList());
            if (!availableTimetables.isEmpty()) {
                Timetables newActiveTimetable = availableTimetables.get(0);
                newActiveTimetable.setIsActive((byte) 1);
                newActiveTimetable.setUpdatedAt(LocalDateTime.now());
                timetableRepository.save(newActiveTimetable);
            }
        }
        
        return true;
    }

    /**
     * 管理员删除课表（不需要用户ID验证）
     */
    @Transactional
    public boolean deleteTimetableByAdmin(Long timetableId) {
        Timetables timetable = timetableRepository.findById(timetableId);
        if (timetable == null) {
            return false;
        }

        // 检查是否已经软删除
        if (timetable.getIsDeleted() != null && timetable.getIsDeleted() == 1) {
            return false;  // 已经删除了
        }

        // 检查是否为活动课表
        boolean isActiveTimetable = timetable.getIsActive() != null && timetable.getIsActive() == 1;

        // 软删除：将is_deleted字段置为1
        timetable.setIsDeleted((byte) 1);
        timetable.setDeletedAt(LocalDateTime.now());
        timetable.setUpdatedAt(LocalDateTime.now());
        
        // 如果删除的是活动课表，需要清除活动状态
        if (isActiveTimetable) {
            timetable.setIsActive((byte) 0);
        }

        // 保存修改
        timetableRepository.save(timetable);

        // 如果删除的是活动课表，需要设置另一个课表为活动状态
        if (isActiveTimetable) {
            // 获取该用户的所有非归档、非删除的课表
            List<Timetables> availableTimetables = timetableRepository.findByUserId(timetable.getUserId())
                    .stream()
                    .filter(t -> !t.getId().equals(timetableId)) // 排除刚删除的课表
                    .filter(t -> t.getIsArchived() == null || t.getIsArchived() == 0) // 非归档
                    .filter(t -> t.getIsDeleted() == null || t.getIsDeleted() == 0) // 非删除
                    .collect(Collectors.toList());

            // 如果还有其他课表，设置第一个为活动状态
            if (!availableTimetables.isEmpty()) {
                Timetables newActiveTimetable = availableTimetables.get(0);
                newActiveTimetable.setIsActive((byte) 1);
                newActiveTimetable.setUpdatedAt(LocalDateTime.now());
                timetableRepository.save(newActiveTimetable);
            }
        }

        return true;
    }

    /**
     * 批量恢复课表
     */
    @Transactional
    public int batchRestoreTimetables(List<Long> ids, Long userId) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        long nonArchivedCount = timetableRepository.findByUserId(userId).stream()
                .filter(t -> t.getIsArchived() == null || t.getIsArchived() == 0)
                .count();
        if (nonArchivedCount + ids.size() > 5) {
            throw new IllegalStateException("无法恢复，操作后非归档课表将超过数量上限 (5个)");
        }
        
        // 检查用户是否有活动课表
        boolean hasActiveTimetable = timetableRepository.findByUserId(userId).stream()
                .anyMatch(timetable -> timetable.getIsActive() != null && timetable.getIsActive() == 1);
        
        // 执行批量恢复
        int restoredCount = timetableRepository.batchRestoreByIdsAndUserId(ids, userId);
        
        // 如果用户没有活动课表且恢复成功，设置第一个恢复的课表为活动状态
        if (!hasActiveTimetable && restoredCount > 0) {
            Timetables firstRestored = timetableRepository.findById(ids.get(0));
            if (firstRestored != null && firstRestored.getUserId().equals(userId)) {
                firstRestored.setIsActive((byte) 1);
                firstRestored.setUpdatedAt(LocalDateTime.now());
                timetableRepository.save(firstRestored);
            }
        }
        
        return restoredCount;
    }

    /**
     * 获取所有活动课表的指定日期课程信息
     */
    public Map<String, Object> getActiveSchedulesByDate(String dateStr) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> timetableSchedules = new ArrayList<>();

        try {
            // 解析日期
            LocalDate targetDate = LocalDate.parse(dateStr);

            // 获取所有活动课表
            List<Timetables> activeTimetables = timetableRepository.findAll()
                    .stream()
                    .filter(t -> t.getIsActive() != null && t.getIsActive() == 1)
                    .filter(t -> t.getIsDeleted() == null || t.getIsDeleted() == 0)
                    .filter(t -> t.getIsArchived() == null || t.getIsArchived() == 0)
                    .collect(Collectors.toList());

            for (Timetables timetable : activeTimetables) {
                // 获取课表所属用户信息
                com.timetable.generated.tables.pojos.Users user = userService.findById(timetable.getUserId());
                if (user == null || (user.getIsDeleted() != null && user.getIsDeleted() == 1)) {
                    continue; // 跳过已删除的用户
                }

                List<com.timetable.generated.tables.pojos.Schedules> schedules = new ArrayList<>();

                if (timetable.getIsWeekly() != null && timetable.getIsWeekly() == 1) {
                    // 周课表：优先获取本周实例的课程，如果没有实例则获取固定课表模板的课程
                    DayOfWeek dayOfWeek = targetDate.getDayOfWeek();
                    String dayOfWeekStr = dayOfWeek.toString();
                    
                    // 先尝试获取当前周实例的课程
                    WeeklyInstance currentInstance = weeklyInstanceService.getCurrentWeekInstance(timetable.getId());
                    if (currentInstance != null) {
                        // 有当前周实例，从实例中获取课程
                        List<WeeklyInstanceSchedule> instanceSchedules = weeklyInstanceScheduleRepository.findByWeeklyInstanceIdAndDate(currentInstance.getId(), targetDate);
                        // 转换为Schedules格式
                        schedules = instanceSchedules.stream()
                            .map(instanceSchedule -> {
                                Schedules schedule = new Schedules();
                                schedule.setId(instanceSchedule.getId());
                                schedule.setTimetableId(timetable.getId());
                                schedule.setStudentName(instanceSchedule.getStudentName());
                                schedule.setSubject(instanceSchedule.getSubject());
                                schedule.setDayOfWeek(instanceSchedule.getDayOfWeek());
                                schedule.setStartTime(instanceSchedule.getStartTime());
                                schedule.setEndTime(instanceSchedule.getEndTime());
                                schedule.setScheduleDate(instanceSchedule.getScheduleDate());
                                schedule.setNote(instanceSchedule.getNote());
                                return schedule;
                            })
                            .collect(Collectors.toList());
                    } else {
                        // 没有当前周实例，从固定课表模板获取课程
                        List<Schedules> templateSchedules = scheduleRepository.findTemplateSchedulesByTimetableIdAndDayOfWeek(timetable.getId(), dayOfWeekStr);
                        // 为模板课程设置具体的日期
                        schedules = templateSchedules.stream()
                            .map(templateSchedule -> {
                                Schedules schedule = new Schedules();
                                schedule.setId(templateSchedule.getId());
                                schedule.setTimetableId(templateSchedule.getTimetableId());
                                schedule.setStudentName(templateSchedule.getStudentName());
                                schedule.setSubject(templateSchedule.getSubject());
                                schedule.setDayOfWeek(templateSchedule.getDayOfWeek());
                                schedule.setStartTime(templateSchedule.getStartTime());
                                schedule.setEndTime(templateSchedule.getEndTime());
                                schedule.setScheduleDate(targetDate); // 设置具体日期
                                schedule.setNote(templateSchedule.getNote());
                                return schedule;
                            })
                            .collect(Collectors.toList());
                    }
                } else {
                    // 日期范围课表：根据具体日期获取课程
                    schedules = scheduleRepository.findByTimetableIdAndScheduleDate(timetable.getId(), targetDate);
                }

                if (!schedules.isEmpty()) {
                    Map<String, Object> timetableInfo = new HashMap<>();
                    timetableInfo.put("timetableId", timetable.getId());
                    timetableInfo.put("timetableName", timetable.getName());
                    timetableInfo.put("ownerName", user.getNickname() != null ? user.getNickname() : user.getUsername());
                    // 标记是否为周固定课表
                    timetableInfo.put("isWeekly", timetable.getIsWeekly());
                    timetableInfo.put("schedules", schedules);
                    timetableSchedules.add(timetableInfo);
                }
            }

            result.put("date", dateStr);
            result.put("timetables", timetableSchedules);
            result.put("totalTimetables", timetableSchedules.size());

        } catch (Exception e) {
            throw new RuntimeException("获取活动课表课程失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取所有活动课表的本周课程信息（优化版，一次性返回所有数据）
     */
    public List<Map<String, Object>> getActiveTimetablesThisWeekSchedules() {
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            // 获取本周的周一和周日
            LocalDate today = LocalDate.now();
            LocalDate monday = today.with(DayOfWeek.MONDAY);
            LocalDate sunday = today.with(DayOfWeek.SUNDAY);

            // 获取所有活动课表
            List<Timetables> activeTimetables = timetableRepository.findAll()
                    .stream()
                    .filter(t -> t.getIsActive() != null && t.getIsActive() == 1)
                    .filter(t -> t.getIsDeleted() == null || t.getIsDeleted() == 0)
                    .filter(t -> t.getIsArchived() == null || t.getIsArchived() == 0)
                    .collect(Collectors.toList());

            for (Timetables timetable : activeTimetables) {
                // 获取课表所属用户信息
                com.timetable.generated.tables.pojos.Users user = userService.findById(timetable.getUserId());
                if (user == null || (user.getIsDeleted() != null && user.getIsDeleted() == 1)) {
                    continue; // 跳过已删除的用户
                }

                List<com.timetable.generated.tables.pojos.Schedules> weekSchedules = new ArrayList<>();

                if (timetable.getIsWeekly() != null && timetable.getIsWeekly() == 1) {
                    // 周固定课表：从周实例数据获取本周课程
                    try {
                        // 检查是否存在当前周实例，如果不存在则生成
                        WeeklyInstance currentInstance = weeklyInstanceService.getCurrentWeekInstance(timetable.getId());
                        if (currentInstance == null) {
                            try {
                                currentInstance = weeklyInstanceService.generateCurrentWeekInstance(timetable.getId());
                            } catch (Exception genEx) {
                                // 生成失败，继续处理下一个课表
                                continue;
                            }
                        }
                        
                        List<WeeklyInstanceSchedule> instanceSchedules = 
                            weeklyInstanceService.getCurrentWeekInstanceSchedules(timetable.getId());
                        
                        // 转换为 Schedules 格式，并过滤掉请假的课程
                        weekSchedules = instanceSchedules.stream()
                            .filter(instanceSchedule -> instanceSchedule.getIsOnLeave() == null || !instanceSchedule.getIsOnLeave()) // 过滤掉请假的课程
                            .map(instanceSchedule -> {
                                Schedules schedule = new Schedules();
                                schedule.setId(instanceSchedule.getId());
                                schedule.setTimetableId(timetable.getId());
                                schedule.setStudentName(instanceSchedule.getStudentName());
                                schedule.setSubject(instanceSchedule.getSubject());
                                schedule.setDayOfWeek(instanceSchedule.getDayOfWeek());
                                schedule.setStartTime(instanceSchedule.getStartTime());
                                schedule.setEndTime(instanceSchedule.getEndTime());
                                schedule.setScheduleDate(instanceSchedule.getScheduleDate());
                                schedule.setNote(instanceSchedule.getNote());
                                return schedule;
                            })
                            .collect(Collectors.toList());
                    } catch (Exception e) {
                        // 如果获取周实例失败，继续处理下一个课表
                        continue;
                    }
                } else {
                    // 日期范围课表：获取本周课程，并过滤掉请假的课程
                    weekSchedules = scheduleRepository.findByTimetableIdAndScheduleDateBetween(
                        timetable.getId(), monday, sunday)
                        .stream()
                        .filter(schedule -> schedule.getIsOnLeave() == null || !schedule.getIsOnLeave()) // 过滤掉请假的课程
                        .collect(Collectors.toList());
                }

                // 将课程数据添加到结果中
                for (Schedules schedule : weekSchedules) {
                    Map<String, Object> scheduleInfo = new HashMap<>();
                    scheduleInfo.put("id", schedule.getId());
                    scheduleInfo.put("timetableId", timetable.getId());
                    scheduleInfo.put("timetableName", timetable.getName());
                    scheduleInfo.put("ownerNickname", user.getNickname());
                    scheduleInfo.put("ownerUsername", user.getUsername());
                    scheduleInfo.put("isWeekly", timetable.getIsWeekly());
                    scheduleInfo.put("studentName", schedule.getStudentName());
                    scheduleInfo.put("subject", schedule.getSubject());
                    scheduleInfo.put("dayOfWeek", schedule.getDayOfWeek());
                    scheduleInfo.put("startTime", schedule.getStartTime());
                    scheduleInfo.put("endTime", schedule.getEndTime());
                    scheduleInfo.put("scheduleDate", schedule.getScheduleDate());
                    scheduleInfo.put("note", schedule.getNote());
                    result.add(scheduleInfo);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("获取活动课表本周课程失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取所有活动课表的模板课程信息（优化版，一次性返回所有数据）
     */
    public List<Map<String, Object>> getActiveTimetablesTemplateSchedules() {
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            // 获取所有活动课表
            List<Timetables> activeTimetables = timetableRepository.findAll()
                    .stream()
                    .filter(t -> t.getIsActive() != null && t.getIsActive() == 1)
                    .filter(t -> t.getIsDeleted() == null || t.getIsDeleted() == 0)
                    .filter(t -> t.getIsArchived() == null || t.getIsArchived() == 0)
                    .collect(Collectors.toList());

            for (Timetables timetable : activeTimetables) {
                // 获取课表所属用户信息
                com.timetable.generated.tables.pojos.Users user = userService.findById(timetable.getUserId());
                if (user == null || (user.getIsDeleted() != null && user.getIsDeleted() == 1)) {
                    continue; // 跳过已删除的用户
                }

                List<com.timetable.generated.tables.pojos.Schedules> templateSchedules = new ArrayList<>();

                if (timetable.getIsWeekly() != null && timetable.getIsWeekly() == 1) {
                    // 周固定课表：获取模板数据（只获取模板数据）
                    templateSchedules = scheduleRepository.findTemplateSchedulesByTimetableId(timetable.getId());
                } else {
                    // 日期范围课表：按当前周获取（这种情况下模板概念不太适用，但为了一致性还是处理）
                    LocalDate startDate = timetable.getStartDate();
                    if (startDate != null) {
                        LocalDate currentDate = LocalDate.now();
                        long weeks = java.time.temporal.ChronoUnit.WEEKS.between(startDate, currentDate);
                        int currentWeek = (int) weeks + 1;
                        templateSchedules = scheduleRepository.findByTimetableIdAndWeekNumber(timetable.getId(), currentWeek);
                    }
                }

                // 将课程数据添加到结果中
                for (Schedules schedule : templateSchedules) {
                    Map<String, Object> scheduleInfo = new HashMap<>();
                    scheduleInfo.put("id", schedule.getId());
                    scheduleInfo.put("timetableId", timetable.getId());
                    scheduleInfo.put("timetableName", timetable.getName());
                    scheduleInfo.put("ownerNickname", user.getNickname());
                    scheduleInfo.put("ownerUsername", user.getUsername());
                    scheduleInfo.put("isWeekly", timetable.getIsWeekly());
                    scheduleInfo.put("studentName", schedule.getStudentName());
                    scheduleInfo.put("subject", schedule.getSubject());
                    scheduleInfo.put("dayOfWeek", schedule.getDayOfWeek());
                    scheduleInfo.put("startTime", schedule.getStartTime() != null ? schedule.getStartTime().toString() : "");
                    scheduleInfo.put("endTime", schedule.getEndTime() != null ? schedule.getEndTime().toString() : "");
                    scheduleInfo.put("scheduleDate", schedule.getScheduleDate());
                    scheduleInfo.put("weekNumber", schedule.getWeekNumber());
                    scheduleInfo.put("note", schedule.getNote());
                    result.add(scheduleInfo);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("获取活动课表模板课程失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 复制课表到指定用户
     */
    @Transactional
    public Timetables copyTimetableToUser(Long sourceTimetableId, Long targetUserId, String newTimetableName) {
        // 1. 获取源课表
        Timetables sourceTimetable = timetableRepository.findById(sourceTimetableId);
        if (sourceTimetable == null) {
            throw new IllegalArgumentException("源课表不存在");
        }

        // 2. 检查目标用户是否存在
        com.timetable.generated.tables.pojos.Users targetUser = userService.findById(targetUserId);
        if (targetUser == null || (targetUser.getIsDeleted() != null && targetUser.getIsDeleted() == 1)) {
            throw new IllegalArgumentException("目标用户不存在或已被删除");
        }

        // 3. 检查目标用户的非归档课表数量
        long nonArchivedCount = timetableRepository.findByUserId(targetUserId).stream()
                .filter(t -> t.getIsArchived() == null || t.getIsArchived() == 0)
                .count();

        if (nonArchivedCount >= 5) {
            throw new IllegalStateException("目标用户的非归档课表数量已达上限 (5个)");
        }

        // 4. 创建新课表
        Timetables newTimetable = new Timetables();
        newTimetable.setUserId(targetUserId);
        
        // 设置课表名称：直接使用提供的名称
        if (newTimetableName == null || newTimetableName.trim().isEmpty()) {
            throw new IllegalArgumentException("新课表名称不能为空");
        }
        newTimetable.setName(newTimetableName.trim());
        
        newTimetable.setDescription(sourceTimetable.getDescription());
        newTimetable.setIsWeekly(sourceTimetable.getIsWeekly());
        newTimetable.setStartDate(sourceTimetable.getStartDate());
        newTimetable.setEndDate(sourceTimetable.getEndDate());
        newTimetable.setCreatedAt(LocalDateTime.now());
        newTimetable.setUpdatedAt(LocalDateTime.now());

        // 判断是否已有活动课表
        List<Timetables> userTables = timetableRepository.findByUserId(targetUserId)
            .stream().filter(t -> t.getIsActive() != null && t.getIsActive() == 1).collect(Collectors.toList());
        if (userTables.isEmpty()) {
            newTimetable.setIsActive((byte) 1);
        } else {
            newTimetable.setIsActive((byte) 0);
        }

        // 保存新课表
        Timetables savedTimetable = timetableRepository.save(newTimetable);

        // 5. 复制所有课程
        List<Schedules> sourceSchedules = scheduleRepository.findByTimetableId(sourceTimetableId);
        for (Schedules sourceSchedule : sourceSchedules) {
            Schedules newSchedule = new Schedules();
            newSchedule.setTimetableId(savedTimetable.getId());
            newSchedule.setStudentName(sourceSchedule.getStudentName());
            newSchedule.setSubject(sourceSchedule.getSubject());
            newSchedule.setDayOfWeek(sourceSchedule.getDayOfWeek());
            newSchedule.setStartTime(sourceSchedule.getStartTime());
            newSchedule.setEndTime(sourceSchedule.getEndTime());
            newSchedule.setScheduleDate(sourceSchedule.getScheduleDate());
            newSchedule.setWeekNumber(sourceSchedule.getWeekNumber());
            newSchedule.setNote(sourceSchedule.getNote());
            newSchedule.setCreatedAt(LocalDateTime.now());
            newSchedule.setUpdatedAt(LocalDateTime.now());
            
            scheduleRepository.save(newSchedule);
        }

        return savedTimetable;
    }

    /**
     * 列出日期范围课表内，按周（周一至周日）的课程数量
     */
    public List<Map<String, Object>> getWeeksWithCounts(Long timetableId) {
        Timetables t = timetableRepository.findById(timetableId);
        if (t == null) throw new IllegalArgumentException("课表不存在");
        if (t.getIsWeekly() != null && t.getIsWeekly() == 1) {
            throw new IllegalArgumentException("周固定课表不支持该查询");
        }
        LocalDate start = t.getStartDate();
        LocalDate end = t.getEndDate();
        if (start == null || end == null) return new ArrayList<>();
        // 规范到周一开始
        LocalDate cursor = start.with(DayOfWeek.MONDAY);
        if (cursor.isAfter(start)) {
            cursor = cursor.minusDays(7);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        while (!cursor.isAfter(end)) {
            LocalDate weekStart = cursor;
            LocalDate weekEnd = cursor.with(DayOfWeek.SUNDAY);
            if (weekEnd.isBefore(start)) {
                cursor = cursor.plusWeeks(1);
                continue;
            }
            LocalDate rangeStart = weekStart.isBefore(start) ? start : weekStart;
            LocalDate rangeEnd = weekEnd.isAfter(end) ? end : weekEnd;
            if (!rangeStart.isAfter(rangeEnd)) {
                List<Schedules> list = scheduleRepository.findByTimetableIdAndScheduleDateBetween(timetableId, rangeStart, rangeEnd);
                Map<String, Object> row = new HashMap<>();
                row.put("weekStart", weekStart.toString());
                row.put("weekEnd", weekEnd.toString());
                row.put("count", list.size());
                result.add(row);
            }
            cursor = cursor.plusWeeks(1);
        }
        return result;
    }

    /**
     * 将日期范围课表转换为周固定课表，使用某一周（周一至周日）作为模板
     */
    @Transactional
    public void convertDateRangeToWeekly(Long timetableId, LocalDate weekStart) {
        Timetables t = timetableRepository.findById(timetableId);
        if (t == null) throw new IllegalArgumentException("课表不存在");
        if (t.getIsWeekly() != null && t.getIsWeekly() == 1) {
            throw new IllegalArgumentException("该课表已经是周固定课表");
        }
        LocalDate ws = weekStart.with(DayOfWeek.MONDAY);
        LocalDate we = ws.with(DayOfWeek.SUNDAY);
        List<Schedules> weekSchedules = scheduleRepository.findByTimetableIdAndScheduleDateBetween(timetableId, ws, we);
        if (weekSchedules.isEmpty()) {
            throw new IllegalArgumentException("所选周内没有课程，无法转换");
        }
        // 清空原课程
        scheduleRepository.deleteByTimetableId(timetableId);
        // 基于该周生成“周固定”课程
        for (Schedules s : weekSchedules) {
            Schedules ns = new Schedules();
            ns.setTimetableId(timetableId);
            ns.setStudentName(s.getStudentName());
            ns.setSubject(s.getSubject());
            ns.setDayOfWeek(s.getScheduleDate().getDayOfWeek().toString());
            ns.setStartTime(s.getStartTime());
            ns.setEndTime(s.getEndTime());
            ns.setScheduleDate(null);
            ns.setWeekNumber(null);
            ns.setNote(s.getNote());
            ns.setCreatedAt(LocalDateTime.now());
            ns.setUpdatedAt(LocalDateTime.now());
            scheduleRepository.save(ns);
        }
        // 更新课表为周固定
        t.setIsWeekly((byte)1);
        t.setStartDate(null);
        t.setEndDate(null);
        t.setUpdatedAt(LocalDateTime.now());
        timetableRepository.save(t);
    }

    /**
     * 将周固定课表按日期范围展开为日期类课表
     */
    @Transactional
    public void convertWeeklyToDateRange(Long timetableId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("日期范围不合法");
        }
        Timetables t = timetableRepository.findById(timetableId);
        if (t == null) throw new IllegalArgumentException("课表不存在");
        if (t.getIsWeekly() == null || t.getIsWeekly() != 1) {
            throw new IllegalArgumentException("该课表不是周固定课表");
        }
        List<Schedules> weekly = scheduleRepository.findByTimetableId(timetableId)
                .stream().filter(sc -> sc.getDayOfWeek() != null).collect(Collectors.toList());
        if (weekly.isEmpty()) throw new IllegalArgumentException("周固定课表没有课程");
        // 清空原课程
        scheduleRepository.deleteByTimetableId(timetableId);
        // 遍历日期范围，匹配星期几，生成课程
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            String dow = cursor.getDayOfWeek().toString();
            for (Schedules s : weekly) {
                if (dow.equals(s.getDayOfWeek())) {
                    Schedules ns = new Schedules();
                    ns.setTimetableId(timetableId);
                    ns.setStudentName(s.getStudentName());
                    ns.setSubject(s.getSubject());
                    ns.setDayOfWeek(s.getDayOfWeek());
                    ns.setStartTime(s.getStartTime());
                    ns.setEndTime(s.getEndTime());
                    ns.setScheduleDate(cursor);
                    ns.setWeekNumber(null);
                    ns.setNote(s.getNote());
                    ns.setCreatedAt(LocalDateTime.now());
                    ns.setUpdatedAt(LocalDateTime.now());
                    scheduleRepository.save(ns);
                }
            }
            cursor = cursor.plusDays(1);
        }
        // 更新课表为日期范围
        t.setIsWeekly((byte)0);
        t.setStartDate(startDate);
        t.setEndDate(endDate);
        t.setUpdatedAt(LocalDateTime.now());
        timetableRepository.save(t);
    }

    /**
     * 复制并转换：日期范围 -> 周固定
     */
    @Transactional
    public Timetables copyAndConvertDateRangeToWeekly(Long sourceTimetableId, LocalDate weekStart, String newName) {
        Timetables source = timetableRepository.findById(sourceTimetableId);
        if (source == null) throw new IllegalArgumentException("源课表不存在");
        if (source.getIsWeekly() != null && source.getIsWeekly() == 1) {
            throw new IllegalArgumentException("源课表不是日期范围课表");
        }
        // 创建新课表
        Timetables newTable = new Timetables();
        newTable.setUserId(source.getUserId());
        newTable.setName((newName != null && !newName.trim().isEmpty()) ? newName.trim() : source.getName() + "(转化)");
        newTable.setDescription(source.getDescription());
        newTable.setIsWeekly((byte)1);
        newTable.setStartDate(null);
        newTable.setEndDate(null);
        newTable.setIsActive((byte)0);
        newTable.setCreatedAt(LocalDateTime.now());
        newTable.setUpdatedAt(LocalDateTime.now());
        newTable = timetableRepository.save(newTable);

        // 用所选周生成周固定课程并写入新课表
        LocalDate ws = weekStart.with(DayOfWeek.MONDAY);
        LocalDate we = ws.with(DayOfWeek.SUNDAY);
        List<Schedules> weekSchedules = scheduleRepository.findByTimetableIdAndScheduleDateBetween(sourceTimetableId, ws, we);
        if (weekSchedules.isEmpty()) throw new IllegalArgumentException("所选周内没有课程");
        for (Schedules s : weekSchedules) {
            Schedules ns = new Schedules();
            ns.setTimetableId(newTable.getId());
            ns.setStudentName(s.getStudentName());
            ns.setSubject(s.getSubject());
            ns.setDayOfWeek(s.getScheduleDate().getDayOfWeek().toString());
            ns.setStartTime(s.getStartTime());
            ns.setEndTime(s.getEndTime());
            ns.setScheduleDate(null);
            ns.setWeekNumber(null);
            ns.setNote(s.getNote());
            ns.setCreatedAt(LocalDateTime.now());
            ns.setUpdatedAt(LocalDateTime.now());
            scheduleRepository.save(ns);
        }
        return newTable;
    }

    /**
     * 复制并转换：周固定 -> 日期范围
     */
    @Transactional
    public Timetables copyAndConvertWeeklyToDateRange(Long sourceTimetableId, LocalDate startDate, LocalDate endDate, String newName) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("日期范围不合法");
        }
        Timetables source = timetableRepository.findById(sourceTimetableId);
        if (source == null) throw new IllegalArgumentException("源课表不存在");
        if (source.getIsWeekly() == null || source.getIsWeekly() != 1) {
            throw new IllegalArgumentException("源课表不是周固定课表");
        }
        // 创建新课表
        Timetables newTable = new Timetables();
        newTable.setUserId(source.getUserId());
        newTable.setName((newName != null && !newName.trim().isEmpty()) ? newName.trim() : source.getName() + "(转化)");
        newTable.setDescription(source.getDescription());
        newTable.setIsWeekly((byte)0);
        newTable.setStartDate(startDate);
        newTable.setEndDate(endDate);
        newTable.setIsActive((byte)0);
        newTable.setCreatedAt(LocalDateTime.now());
        newTable.setUpdatedAt(LocalDateTime.now());
        newTable = timetableRepository.save(newTable);

        // 按日期范围展开课程写入新课表
        List<Schedules> weekly = scheduleRepository.findByTimetableId(sourceTimetableId)
                .stream().filter(sc -> sc.getDayOfWeek() != null).collect(Collectors.toList());
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            String dow = cursor.getDayOfWeek().toString();
            for (Schedules s : weekly) {
                if (dow.equals(s.getDayOfWeek())) {
                    Schedules ns = new Schedules();
                    ns.setTimetableId(newTable.getId());
                    ns.setStudentName(s.getStudentName());
                    ns.setSubject(s.getSubject());
                    ns.setDayOfWeek(s.getDayOfWeek());
                    ns.setStartTime(s.getStartTime());
                    ns.setEndTime(s.getEndTime());
                    ns.setScheduleDate(cursor);
                    ns.setWeekNumber(null);
                    ns.setNote(s.getNote());
                    ns.setCreatedAt(LocalDateTime.now());
                    ns.setUpdatedAt(LocalDateTime.now());
                    scheduleRepository.save(ns);
                }
            }
            cursor = cursor.plusDays(1);
        }
        return newTable;
    }
    
    /**
     * 获取指定课表上周的课程数量
     */
    public int getLastWeekCourseCountForTimetable(Long timetableId) {
        Timetables timetable = timetableRepository.findById(timetableId);
        if (timetable == null) {
            return 0;
        }

        LocalDate today = LocalDate.now();
        LocalDate lastWeekMonday = today.minusWeeks(1).with(DayOfWeek.MONDAY);
        LocalDate lastWeekSunday = today.minusWeeks(1).with(DayOfWeek.SUNDAY);

        if (timetable.getIsWeekly() != null && timetable.getIsWeekly() == 1) {
            // 周固定课表：从上周实例获取
            WeeklyInstance lastWeekInstance = weeklyInstanceService.findInstanceByDate(timetableId, lastWeekMonday);
            if (lastWeekInstance != null) {
                return weeklyInstanceScheduleRepository.countByWeeklyInstanceId(lastWeekInstance.getId());
            }
            return 0;
        } else {
            // 日期范围课表：按上周日期范围查询
            return scheduleRepository.countByTimetableIdAndScheduleDateBetween(timetableId, lastWeekMonday, lastWeekSunday);
        }
    }

    /**
     * 获取指定课表上月课程数量
     */
    public int getLastMonthCourseCountForTimetable(Long timetableId) {
        Timetables timetable = timetableRepository.findById(timetableId);
        if (timetable == null) {
            return 0;
        }

        LocalDate firstDayThisMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate start = firstDayThisMonth.minusMonths(1); // 上月1日
        LocalDate end = firstDayThisMonth.minusDays(1);     // 上月最后一天

        if (timetable.getIsWeekly() != null && timetable.getIsWeekly() == 1) {
            // 周固定课表：找落在上月范围内的所有周实例并累计
            List<WeeklyInstance> instances = weeklyInstanceRepository.findByTemplateIdAndDateRange(timetableId, start, end);
            int total = 0;
            for (WeeklyInstance ins : instances) {
                total += weeklyInstanceScheduleRepository.findByDateRange(ins.getId(),
                        ins.getWeekStartDate().isBefore(start) ? start : ins.getWeekStartDate(),
                        ins.getWeekEndDate().isAfter(end) ? end : ins.getWeekEndDate()).size();
            }
            return total;
        } else {
            // 日期范围课表：直接按日期统计
            return scheduleRepository.countByTimetableIdAndScheduleDateBetween(timetableId, start, end);
        }
    }
}