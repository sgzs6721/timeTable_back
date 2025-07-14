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
        List<Timetables> timetables = getAllTimetables(); // 获取所有有效课表
        return timetables.stream().map(t -> {
            String username = null;
            String nickname = null;
            try {
                com.timetable.generated.tables.pojos.Users u = userService.findById(t.getUserId());
                if (u != null) {
                    username = u.getUsername();
                    nickname = u.getNickname();
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
        }).collect(java.util.stream.Collectors.toList());
    }

    /**
     * 根据ID列表批量获取课表信息（包含用户信息）
     */
    public List<AdminTimetableDTO> getTimetablesByIds(List<Long> timetableIds) {
        List<Timetables> timetables = timetableRepository.findByIdIn(timetableIds);

        return timetables.stream().map(t -> {
            String username = null;
            String nickname = null;
            try {
                com.timetable.generated.tables.pojos.Users u = userService.findById(t.getUserId());
                if (u != null) {
                    username = u.getUsername();
                    nickname = u.getNickname();
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
        }).collect(java.util.stream.Collectors.toList());
    }

    public List<AdminTimetableDTO> findArchivedByUserId(Long userId) {
        List<Timetables> timetables = timetableRepository.findArchivedByUserId(userId);
        String username = null;
        String nickname = null;
        try {
            com.timetable.generated.tables.pojos.Users u = userService.findById(userId);
            if (u != null) {
                username = u.getUsername();
                nickname = u.getNickname();
            }
        } catch (Exception ignored) {}

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


}