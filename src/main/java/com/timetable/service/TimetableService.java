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
                    // 周课表：根据星期几获取课程
                    DayOfWeek dayOfWeek = targetDate.getDayOfWeek();
                    String dayOfWeekStr = dayOfWeek.toString();
                    schedules = scheduleRepository.findByTimetableIdAndDayOfWeek(timetable.getId(), dayOfWeekStr);
                } else {
                    // 日期范围课表：根据具体日期获取课程
                    schedules = scheduleRepository.findByTimetableIdAndScheduleDate(timetable.getId(), targetDate);
                }

                if (!schedules.isEmpty()) {
                    Map<String, Object> timetableInfo = new HashMap<>();
                    timetableInfo.put("timetableId", timetable.getId());
                    timetableInfo.put("timetableName", timetable.getName());
                    timetableInfo.put("ownerName", user.getNickname() != null ? user.getNickname() : user.getUsername());
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
}