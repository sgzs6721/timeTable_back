package com.timetable.service;

import com.timetable.entity.WeeklyInstance;
import com.timetable.entity.WeeklyInstanceSchedule;
import com.timetable.entity.StudentOperationRecord;
import com.timetable.dto.StudentSummaryDTO;
import com.timetable.dto.WeeklyInstanceDTO;
import com.timetable.dto.CoachStudentSummaryDTO;
import com.timetable.repository.WeeklyInstanceRepository;
import com.timetable.repository.WeeklyInstanceScheduleRepository;
import com.timetable.repository.TimetableRepository;
import com.timetable.repository.ScheduleRepository;
import com.timetable.repository.UserRepository;
import com.timetable.repository.StudentOperationRecordRepository;
import com.timetable.generated.tables.pojos.Timetables;
import com.timetable.generated.tables.pojos.Schedules;
import com.timetable.generated.tables.pojos.Users;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 周实例服务类
 */
@Service
public class WeeklyInstanceService {

    private static final Logger logger = LoggerFactory.getLogger(WeeklyInstanceService.class);

    @Autowired
    private WeeklyInstanceRepository weeklyInstanceRepository;

    @Autowired
    private WeeklyInstanceScheduleRepository weeklyInstanceScheduleRepository;

    @Autowired
    private TimetableRepository timetableRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private com.timetable.service.UserService userService;

    @Autowired
    private com.timetable.service.StudentMergeService studentMergeService;

    @Autowired
    private com.timetable.service.StudentAliasService studentAliasService;

    @Autowired
    private StudentOperationRecordRepository studentOperationRecordRepository;
    
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    /**
     * 为指定的固定课表生成当前周实例
     */
    @Transactional
    public WeeklyInstance generateCurrentWeekInstance(Long templateTimetableId) {
        System.out.println("开始生成周实例，课表ID: " + templateTimetableId);
        
        // 检查模板课表是否存在且为周课表
        Timetables templateTimetable = timetableRepository.findById(templateTimetableId);
        if (templateTimetable == null) {
            System.err.println("模板课表不存在，ID: " + templateTimetableId);
            throw new IllegalArgumentException("模板课表不存在");
        }
        
        System.out.println("课表名称: " + templateTimetable.getName() + ", isWeekly: " + templateTimetable.getIsWeekly());
        
        if (templateTimetable.getIsWeekly() == null || templateTimetable.getIsWeekly() != 1) {
            System.err.println("课表不是周固定课表，isWeekly: " + templateTimetable.getIsWeekly());
            throw new IllegalArgumentException("只能为周固定课表生成实例");
        }

        // 计算当前周的开始和结束日期
        LocalDate now = LocalDate.now();
        LocalDate weekStart = now.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = now.with(DayOfWeek.SUNDAY);
        
        // 生成年-周格式
        String yearWeek = generateYearWeekString(now);

        // 检查是否已存在该周的实例
        WeeklyInstance existingInstance = weeklyInstanceRepository.findByTemplateIdAndYearWeek(templateTimetableId, yearWeek);
        if (existingInstance != null) {
            // 更新为当前周实例
            weeklyInstanceRepository.clearCurrentWeekFlagByTemplateId(templateTimetableId);
            weeklyInstanceRepository.setCurrentWeekInstance(existingInstance.getId());
            return existingInstance;
        }

        // 创建新的周实例
        System.out.println("创建新周实例，周开始: " + weekStart + ", 周结束: " + weekEnd + ", 年周: " + yearWeek);
        WeeklyInstance instance = new WeeklyInstance(templateTimetableId, weekStart, weekEnd, yearWeek);
        // 设置机构ID（从模板课表中获取）
        instance.setOrganizationId(templateTimetable.getOrganizationId());
        
        System.out.println("保存周实例到数据库...");
        instance = weeklyInstanceRepository.save(instance);

        // 确保实例保存成功并获得了ID
        if (instance.getId() == null) {
            System.err.println("周实例保存失败，ID为null");
            throw new RuntimeException("保存周实例失败，无法获取实例ID");
        }
        
        System.out.println("周实例保存成功，ID: " + instance.getId());

        // 从模板课表复制课程到实例
        System.out.println("开始同步模板课程到实例...");
        syncSchedulesFromTemplate(instance);
        System.out.println("周实例生成完成");

        // 清除同一模板课表的其他当前周标记，设置新实例为当前周
        weeklyInstanceRepository.clearCurrentWeekFlagByTemplateId(templateTimetableId);
        weeklyInstanceRepository.setCurrentWeekInstance(instance.getId());

        return instance;
    }


    /**
     * 为指定模板课表生成"下周"的周实例并同步模板课程
     */
    @Transactional
    public WeeklyInstance generateNextWeekInstance(Long templateTimetableId) {
        LocalDate nextWeekStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY).plusWeeks(1);
        LocalDate nextWeekEnd = nextWeekStart.plusDays(6);
        String yearWeek = generateYearWeekString(nextWeekStart);

        WeeklyInstance existingInstance = weeklyInstanceRepository.findByTemplateIdAndYearWeek(templateTimetableId, yearWeek);
        if (existingInstance != null) {
            // 检查实例是否有课程数据，如果没有则重新同步
            List<WeeklyInstanceSchedule> existingSchedules = weeklyInstanceScheduleRepository.findByWeeklyInstanceId(existingInstance.getId());
            if (existingSchedules == null || existingSchedules.isEmpty()) {
                logger.info("下周实例存在但没有课程数据，重新同步模板课程，实例ID: {}", existingInstance.getId());
                syncSchedulesFromTemplate(existingInstance);
            }
            return existingInstance;
        }

        // 获取模板课表以获取机构ID
        Timetables templateTimetable = timetableRepository.findById(templateTimetableId);
        if (templateTimetable == null) {
            throw new IllegalArgumentException("模板课表不存在");
        }

        WeeklyInstance instance = new WeeklyInstance(templateTimetableId, nextWeekStart, nextWeekEnd, yearWeek);
        // 设置机构ID（从模板课表中获取）
        instance.setOrganizationId(templateTimetable.getOrganizationId());
        instance = weeklyInstanceRepository.save(instance);
        if (instance.getId() == null) {
            throw new RuntimeException("保存下周实例失败");
        }
        syncSchedulesFromTemplate(instance);
        return instance;
    }

    /**
     * 删除"下周"的周实例（如果存在），同时删除其所有实例课程
     */
    @Transactional
    public boolean deleteNextWeekInstance(Long templateTimetableId) {
        LocalDate nextWeekStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY).plusWeeks(1);
        String yearWeek = generateYearWeekString(nextWeekStart);
        WeeklyInstance instance = weeklyInstanceRepository.findByTemplateIdAndYearWeek(templateTimetableId, yearWeek);
        if (instance == null) {
            return false;
        }
        // 先删课程
        weeklyInstanceScheduleRepository.deleteByWeeklyInstanceId(instance.getId());
        // 再删实例
        weeklyInstanceRepository.delete(instance.getId());
        return true;
    }

    /**
     * 为所有活动课表生成"下周"的周实例
     */
    @Transactional
    public void generateNextWeekInstancesForAllActiveTimetables() {
        List<Timetables> activeTimetables = timetableRepository.findAll()
                .stream()
                .filter(t -> t.getIsActive() != null && t.getIsActive() == 1)
                .filter(t -> t.getIsWeekly() != null && t.getIsWeekly() == 1)
                .filter(t -> t.getIsDeleted() == null || t.getIsDeleted() == 0)
                .filter(t -> t.getIsArchived() == null || t.getIsArchived() == 0)
                .collect(Collectors.toList());

        for (Timetables timetable : activeTimetables) {
            try {
                generateNextWeekInstance(timetable.getId());
            } catch (Exception ignored) {}
        }
    }

    /**
     * 从模板课表同步课程到周实例（保留手动添加的课程）
     */
    @Transactional
    public void syncSchedulesFromTemplate(WeeklyInstance instance) {
        // 获取模板课表的模板课程（scheduleDate为null的记录）
        List<Schedules> templateSchedules = scheduleRepository.findTemplateSchedulesByTimetableId(instance.getTemplateTimetableId());
        logger.info("从模板课表 {} 获取到 {} 个模板课程", instance.getTemplateTimetableId(), templateSchedules.size());

        // 删除实例中所有非手动添加的课程
        List<WeeklyInstanceSchedule> existingSchedules = weeklyInstanceScheduleRepository.findByWeeklyInstanceId(instance.getId());
        for (WeeklyInstanceSchedule schedule : existingSchedules) {
            if (schedule.getIsManualAdded() == null || !schedule.getIsManualAdded()) {
                weeklyInstanceScheduleRepository.delete(schedule.getId());
            }
        }

        // 从模板课程创建实例课程
        for (Schedules templateSchedule : templateSchedules) {
            // 计算具体日期
            LocalDate scheduleDate = calculateScheduleDate(instance.getWeekStartDate(), templateSchedule.getDayOfWeek());
            
            WeeklyInstanceSchedule instanceSchedule = new WeeklyInstanceSchedule(
                instance.getId(),
                templateSchedule.getId(),
                templateSchedule.getStudentName(),
                templateSchedule.getSubject(),
                templateSchedule.getDayOfWeek(),
                templateSchedule.getStartTime(),
                templateSchedule.getEndTime(),
                scheduleDate,
                templateSchedule.getNote()
            );
            instanceSchedule.setIsTimeBlock(false); // 普通课程必须明确设为false，防止数据库约束冲突
            
            weeklyInstanceScheduleRepository.save(instanceSchedule);
            logger.debug("创建实例课程: {} {} {}-{} 日期: {}", 
                templateSchedule.getStudentName(), templateSchedule.getDayOfWeek(), 
                templateSchedule.getStartTime(), templateSchedule.getEndTime(), scheduleDate);
        }
        
        logger.info("成功同步 {} 个模板课程到周实例 {}", templateSchedules.size(), instance.getId());

        // 更新实例的同步时间
        weeklyInstanceRepository.updateLastSyncedAt(instance.getId(), LocalDateTime.now());
    }

    /**
     * 智能同步模板课程到周实例（以固定课表为准覆盖实例内容）
     */
    @Transactional
    public void syncTemplateToInstanceWithOverride(WeeklyInstance instance) {
        // 获取模板课表的模板课程（scheduleDate为null的记录）
        List<Schedules> templateSchedules = scheduleRepository.findTemplateSchedulesByTimetableId(instance.getTemplateTimetableId());
        
        // 获取实例中现有的所有课程
        List<WeeklyInstanceSchedule> existingSchedules = weeklyInstanceScheduleRepository.findByWeeklyInstanceId(instance.getId());
        
        // 创建现有课程的映射，用于快速查找
        Map<String, WeeklyInstanceSchedule> existingScheduleMap = new HashMap<>();
        for (WeeklyInstanceSchedule schedule : existingSchedules) {
            String key = schedule.getDayOfWeek() + "_" + schedule.getStartTime() + "_" + schedule.getEndTime();
            existingScheduleMap.put(key, schedule);
        }
        
        // 处理模板课程
        for (Schedules templateSchedule : templateSchedules) {
            // 计算具体日期
            LocalDate scheduleDate = calculateScheduleDate(instance.getWeekStartDate(), templateSchedule.getDayOfWeek());
            
            String key = templateSchedule.getDayOfWeek() + "_" + templateSchedule.getStartTime() + "_" + templateSchedule.getEndTime();
            WeeklyInstanceSchedule existingSchedule = existingScheduleMap.get(key);
            
            if (existingSchedule != null) {
                // 如果实例中已存在相同时间段的课程，以固定课表为准进行覆盖
                existingSchedule.setTemplateScheduleId(templateSchedule.getId());
                existingSchedule.setStudentName(templateSchedule.getStudentName());
                existingSchedule.setSubject(templateSchedule.getSubject());
                existingSchedule.setNote(templateSchedule.getNote());
                existingSchedule.setIsManualAdded(false); // 标记为模板同步的课程
                existingSchedule.setIsModified(false); // 重置修改标记
                existingSchedule.setUpdatedAt(LocalDateTime.now());
                
                weeklyInstanceScheduleRepository.save(existingSchedule);
                
                // 从映射中移除，表示已处理
                existingScheduleMap.remove(key);
            } else {
                // 如果实例中不存在，创建新的课程
                WeeklyInstanceSchedule newSchedule = new WeeklyInstanceSchedule(
                    instance.getId(),
                    templateSchedule.getId(),
                    templateSchedule.getStudentName(),
                    templateSchedule.getSubject(),
                    templateSchedule.getDayOfWeek(),
                    templateSchedule.getStartTime(),
                    templateSchedule.getEndTime(),
                    scheduleDate,
                    templateSchedule.getNote()
                );
                newSchedule.setIsManualAdded(false);
                newSchedule.setIsModified(false);
                newSchedule.setCreatedAt(LocalDateTime.now());
                newSchedule.setUpdatedAt(LocalDateTime.now());
                
                weeklyInstanceScheduleRepository.save(newSchedule);
            }
        }
        
        // 删除实例中不再存在于模板中的非手动添加课程
        for (WeeklyInstanceSchedule schedule : existingScheduleMap.values()) {
            if (schedule.getIsManualAdded() == null || !schedule.getIsManualAdded()) {
                weeklyInstanceScheduleRepository.delete(schedule.getId());
            }
        }
        
        // 更新实例的同步时间
        weeklyInstanceRepository.updateLastSyncedAt(instance.getId(), LocalDateTime.now());
    }

    /**
     * 完全恢复周实例为固定课表状态（删除所有课程，包括手动添加的）
     */
    @Transactional
    public void restoreInstanceToTemplate(WeeklyInstance instance) {
        // 删除实例中的所有课程（包括手动添加的）
        weeklyInstanceScheduleRepository.deleteByWeeklyInstanceId(instance.getId());

        // 获取模板课表的模板课程（scheduleDate为null的记录）
        List<Schedules> templateSchedules = scheduleRepository.findTemplateSchedulesByTimetableId(instance.getTemplateTimetableId());

        // 从模板课程创建实例课程
        for (Schedules templateSchedule : templateSchedules) {
            // 计算具体日期
            LocalDate scheduleDate = calculateScheduleDate(instance.getWeekStartDate(), templateSchedule.getDayOfWeek());
            
            // 检查是否已存在相同的课程（防止重复创建）
            List<WeeklyInstanceSchedule> existingSchedules = weeklyInstanceScheduleRepository.findByWeeklyInstanceIdAndDate(
                instance.getId(), scheduleDate);
            
            boolean alreadyExists = existingSchedules.stream().anyMatch(schedule -> 
                schedule.getStudentName().equals(templateSchedule.getStudentName()) &&
                schedule.getStartTime().equals(templateSchedule.getStartTime()) &&
                schedule.getEndTime().equals(templateSchedule.getEndTime())
            );
            
            if (!alreadyExists) {
                WeeklyInstanceSchedule instanceSchedule = new WeeklyInstanceSchedule(
                    instance.getId(),
                    templateSchedule.getId(),
                    templateSchedule.getStudentName(),
                    templateSchedule.getSubject(),
                    templateSchedule.getDayOfWeek(),
                    templateSchedule.getStartTime(),
                    templateSchedule.getEndTime(),
                    scheduleDate,
                    templateSchedule.getNote()
                );
                
                weeklyInstanceScheduleRepository.save(instanceSchedule);
                logger.debug("创建实例课程: {} {} {}-{}", 
                    templateSchedule.getStudentName(), scheduleDate, 
                    templateSchedule.getStartTime(), templateSchedule.getEndTime());
            } else {
                logger.debug("跳过重复课程: {} {} {}-{}", 
                    templateSchedule.getStudentName(), scheduleDate, 
                    templateSchedule.getStartTime(), templateSchedule.getEndTime());
            }
        }

        // 更新实例的同步时间
        weeklyInstanceRepository.updateLastSyncedAt(instance.getId(), LocalDateTime.now());
    }

    /**
     * 清理指定周实例中的重复课程数据
     */
    @Transactional
    public void cleanDuplicateSchedules(Long weeklyInstanceId) {
        List<WeeklyInstanceSchedule> allSchedules = weeklyInstanceScheduleRepository.findByWeeklyInstanceId(weeklyInstanceId);
        
        // 使用Map来跟踪已处理的课程，key为唯一标识
        Map<String, WeeklyInstanceSchedule> uniqueSchedules = new HashMap<>();
        List<Long> duplicateIds = new ArrayList<>();
        
        for (WeeklyInstanceSchedule schedule : allSchedules) {
            String key = schedule.getStudentName() + "_" + schedule.getStartTime() + "_" + 
                        schedule.getEndTime() + "_" + schedule.getScheduleDate();
            
            if (uniqueSchedules.containsKey(key)) {
                // 发现重复，保留ID较小的（通常是先创建的）
                WeeklyInstanceSchedule existing = uniqueSchedules.get(key);
                if (schedule.getId() > existing.getId()) {
                    duplicateIds.add(schedule.getId());
                } else {
                    duplicateIds.add(existing.getId());
                    uniqueSchedules.put(key, schedule);
                }
            } else {
                uniqueSchedules.put(key, schedule);
            }
        }
        
        // 删除重复的课程
        for (Long duplicateId : duplicateIds) {
            weeklyInstanceScheduleRepository.delete(duplicateId);
            logger.info("删除重复课程，ID: {}", duplicateId);
        }
        
        logger.info("清理完成，删除了 {} 个重复课程", duplicateIds.size());
    }

    /**
     * 清理所有周实例中的重复课程数据
     */
    @Transactional
    public Map<String, Object> cleanAllDuplicateSchedules() {
        Map<String, Object> result = new HashMap<>();
        int totalCleaned = 0;
        int instancesProcessed = 0;
        
        List<WeeklyInstance> allInstances = weeklyInstanceRepository.findAll();
        
        for (WeeklyInstance instance : allInstances) {
            try {
                List<WeeklyInstanceSchedule> beforeSchedules = weeklyInstanceScheduleRepository.findByWeeklyInstanceId(instance.getId());
                cleanDuplicateSchedules(instance.getId());
                List<WeeklyInstanceSchedule> afterSchedules = weeklyInstanceScheduleRepository.findByWeeklyInstanceId(instance.getId());
                
                int cleaned = beforeSchedules.size() - afterSchedules.size();
                totalCleaned += cleaned;
                instancesProcessed++;
                
                if (cleaned > 0) {
                    logger.info("实例 {} 清理了 {} 个重复课程", instance.getId(), cleaned);
                }
            } catch (Exception e) {
                logger.error("清理实例 {} 的重复课程失败: {}", instance.getId(), e.getMessage());
            }
        }
        
        result.put("instancesProcessed", instancesProcessed);
        result.put("totalCleaned", totalCleaned);
        result.put("message", String.format("处理了 %d 个实例，清理了 %d 个重复课程", instancesProcessed, totalCleaned));
        
        return result;
    }

    /**
     * 获取指定课表的当前周实例
     */
    public WeeklyInstance getCurrentWeekInstance(Long templateTimetableId) {
        // 首先尝试获取标记为当前周的实例
        WeeklyInstance currentInstance = weeklyInstanceRepository.findCurrentWeekInstanceByTemplateId(templateTimetableId);
        
        if (currentInstance != null) {
            // 验证这个实例是否真的是当前周
            LocalDate now = LocalDate.now();
            LocalDate weekStart = now.with(DayOfWeek.MONDAY);
            LocalDate weekEnd = now.with(DayOfWeek.SUNDAY);
            
            if (currentInstance.getWeekStartDate().equals(weekStart) && currentInstance.getWeekEndDate().equals(weekEnd)) {
                return currentInstance;
            } else {
                // 标记的当前周实例不是真正的当前周，清除标记
                logger.warn("发现错误的当前周标记，课表ID: {}, 实例周: {} - {}, 实际当前周: {} - {}", 
                    templateTimetableId, 
                    currentInstance.getWeekStartDate(), currentInstance.getWeekEndDate(),
                    weekStart, weekEnd);
                weeklyInstanceRepository.clearCurrentWeekFlagByTemplateId(templateTimetableId);
            }
        }
        
        // 如果没有标记的当前周实例或标记错误，尝试根据当前日期查找本周实例
        LocalDate now = LocalDate.now();
        String yearWeek = generateYearWeekString(now);
        WeeklyInstance thisWeekInstance = weeklyInstanceRepository.findByTemplateIdAndYearWeek(templateTimetableId, yearWeek);
        
        if (thisWeekInstance != null) {
            // 找到本周实例，设置为当前周
            weeklyInstanceRepository.clearCurrentWeekFlagByTemplateId(templateTimetableId);
            weeklyInstanceRepository.setCurrentWeekInstance(thisWeekInstance.getId());
            return thisWeekInstance;
        }
        
        return null;
    }

    /**
     * 获取指定周实例的课程安排
     */
    public List<WeeklyInstanceSchedule> getInstanceSchedules(Long instanceId) {
        WeeklyInstance instance = weeklyInstanceRepository.findById(instanceId);
        if (instance == null) {
            logger.warn("周实例不存在，ID: {}", instanceId);
            return new ArrayList<>();
        }
        
        // 获取实例的所有课程
        List<WeeklyInstanceSchedule> allSchedules = weeklyInstanceScheduleRepository.findByWeeklyInstanceId(instanceId);
        
        // 过滤出该周范围内的课程（周一到周日）
        LocalDate weekStart = instance.getWeekStartDate();
        LocalDate weekEnd = instance.getWeekEndDate();
        
        List<WeeklyInstanceSchedule> filteredSchedules = allSchedules.stream()
            .filter(schedule -> {
                if (schedule.getScheduleDate() == null) {
                    return false; // 没有日期的课程不显示
                }
                LocalDate scheduleDate = schedule.getScheduleDate();
                return !scheduleDate.isBefore(weekStart) && !scheduleDate.isAfter(weekEnd);
            })
            .filter(schedule -> schedule.getIsOnLeave() == null || !schedule.getIsOnLeave()) // 过滤掉请假的课程
            .collect(Collectors.toList());
        
        // 重新检查每个课程是否与模板一致，更新 isModified 标记
        for (WeeklyInstanceSchedule schedule : filteredSchedules) {
            boolean originalModified = schedule.getIsModified() != null && schedule.getIsModified();
            checkAndSetModifiedFlag(schedule);
            boolean newModified = schedule.getIsModified() != null && schedule.getIsModified();
            
            // 如果状态发生变化，保存到数据库
            if (originalModified != newModified) {
                weeklyInstanceScheduleRepository.save(schedule);
            }
        }
        
        return filteredSchedules;
    }

    /**
     * 获取指定周实例的课程安排（包含请假的课程）
     */
    public List<WeeklyInstanceSchedule> getInstanceSchedulesIncludingLeaves(Long instanceId) {
        WeeklyInstance instance = weeklyInstanceRepository.findById(instanceId);
        if (instance == null) {
            logger.warn("周实例不存在，ID: {}", instanceId);
            return new ArrayList<>();
        }

        List<WeeklyInstanceSchedule> allSchedules = weeklyInstanceScheduleRepository.findByWeeklyInstanceId(instanceId);

        LocalDate weekStart = instance.getWeekStartDate();
        LocalDate weekEnd = instance.getWeekEndDate();

        List<WeeklyInstanceSchedule> filteredSchedules = allSchedules.stream()
            .filter(schedule -> {
                if (schedule.getScheduleDate() == null) {
                    return false;
                }
                LocalDate scheduleDate = schedule.getScheduleDate();
                return !scheduleDate.isBefore(weekStart) && !scheduleDate.isAfter(weekEnd);
            })
            .collect(Collectors.toList());
        
        // 重新检查每个课程是否与模板一致，更新 isModified 标记
        for (WeeklyInstanceSchedule schedule : filteredSchedules) {
            boolean originalModified = schedule.getIsModified() != null && schedule.getIsModified();
            checkAndSetModifiedFlag(schedule);
            boolean newModified = schedule.getIsModified() != null && schedule.getIsModified();
            
            // 如果状态发生变化，保存到数据库
            if (originalModified != newModified) {
                weeklyInstanceScheduleRepository.save(schedule);
            }
        }
        
        return filteredSchedules;
    }

    /**
     * 获取当前周实例的课程安排（只返回本周范围内的课程）
     */
    public List<WeeklyInstanceSchedule> getCurrentWeekInstanceSchedules(Long templateTimetableId) {
        WeeklyInstance currentInstance = getCurrentWeekInstance(templateTimetableId);
        if (currentInstance == null) {
            // 如果没有当前周实例，尝试自动生成一个
            try {
                logger.info("未找到当前周实例，尝试自动生成，课表ID: {}", templateTimetableId);
                currentInstance = generateCurrentWeekInstance(templateTimetableId);
            } catch (Exception e) {
                logger.warn("自动生成当前周实例失败，课表ID: {}, 错误: {}", templateTimetableId, e.getMessage());
                return new ArrayList<>();
            }
        }
        
        if (currentInstance == null) {
            return new ArrayList<>();
        }
        
        // 使用新的方法获取指定实例的课程（默认不包含请假的）
        return getInstanceSchedules(currentInstance.getId());
    }

    /**
     * 获取当前周实例的课程安排（包含请假的课程）
     */
    public List<WeeklyInstanceSchedule> getCurrentWeekInstanceSchedulesIncludingLeaves(Long templateTimetableId) {
        WeeklyInstance currentInstance = getCurrentWeekInstance(templateTimetableId);
        if (currentInstance == null) {
            try {
                currentInstance = generateCurrentWeekInstance(templateTimetableId);
            } catch (Exception e) {
                logger.warn("自动生成当前周实例失败，课表ID: {}, 错误: {}", templateTimetableId, e.getMessage());
                return new ArrayList<>();
            }
        }
        if (currentInstance == null) {
            return new ArrayList<>();
        }
        return getInstanceSchedulesIncludingLeaves(currentInstance.getId());
    }

    /**
     * 获取指定课表的所有周实例
     */
    public List<WeeklyInstanceDTO> getWeeklyInstancesByTemplateId(Long templateTimetableId) {
        List<WeeklyInstance> instances = weeklyInstanceRepository.findByTemplateTimetableId(templateTimetableId);
        Timetables timetable = timetableRepository.findById(templateTimetableId);
        String timetableName = timetable != null ? timetable.getName() : "未知课表";

        return instances.stream().map(instance -> {
            List<WeeklyInstanceSchedule> schedules = weeklyInstanceScheduleRepository.findByWeeklyInstanceId(instance.getId());
            return new WeeklyInstanceDTO(
                instance.getId(),
                instance.getTemplateTimetableId(),
                timetableName,
                instance.getWeekStartDate(),
                instance.getWeekEndDate(),
                instance.getYearWeek(),
                instance.getIsCurrent(),
                instance.getGeneratedAt(),
                instance.getLastSyncedAt(),
                schedules.size()
            );
        }).collect(Collectors.toList());
    }

    /**
     * 切换到指定的周实例
     */
    @Transactional
    public WeeklyInstance switchToWeekInstance(Long instanceId) {
        WeeklyInstance instance = weeklyInstanceRepository.findById(instanceId);
        if (instance == null) {
            throw new IllegalArgumentException("周实例不存在");
        }

        // 设置为当前周实例
        weeklyInstanceRepository.setCurrentWeekInstance(instanceId);
        return instance;
    }

    /**
     * 当模板课表的课程发生变化时，同步到所有相关的周实例
     */
    @Transactional
    public void syncTemplateChangesToInstances(Long templateTimetableId) {
        List<WeeklyInstance> instances = weeklyInstanceRepository.findByTemplateTimetableId(templateTimetableId);
        
        for (WeeklyInstance instance : instances) {
            // 使用智能同步逻辑，以固定课表为准覆盖实例内容
            syncTemplateToInstanceWithOverride(instance);
        }
    }

    /**
     * 只对未来实例进行同步（不影响当前周和过去的实例）
     */
    @Transactional
    public void syncTemplateChangesToFutureInstances(Long templateTimetableId) {
        List<WeeklyInstance> instances = weeklyInstanceRepository.findByTemplateTimetableId(templateTimetableId);
        LocalDate now = LocalDate.now();
        LocalDate currentWeekStart = now.with(DayOfWeek.MONDAY);
        
        for (WeeklyInstance instance : instances) {
            // 只同步下周及以后的实例，不影响当前周
            if (instance.getWeekStartDate().isAfter(currentWeekStart)) {
                // 未来周：全部同步
                logger.info("同步模板到未来实例: 周 {} - {}", 
                    instance.getWeekStartDate(), instance.getWeekEndDate());
                syncTemplateToInstanceWithOverride(instance);
            } else {
                logger.info("跳过当前周/过去的实例: 周 {} - {} (当前周开始: {})", 
                    instance.getWeekStartDate(), instance.getWeekEndDate(), currentWeekStart);
            }
        }
    }

    /**
     * 智能同步当前周实例，只同步当前时间之后的课程
     */
    @Transactional
    public void syncTemplateToCurrentInstanceSelectively(WeeklyInstance instance) {
        // 获取模板课程
        List<Schedules> templateSchedules = scheduleRepository.findTemplateSchedulesByTimetableId(instance.getTemplateTimetableId());
        if (templateSchedules.isEmpty()) {
            return;
        }

        // 获取实例课程
        List<WeeklyInstanceSchedule> instanceSchedules = weeklyInstanceScheduleRepository.findByWeeklyInstanceId(instance.getId());
        Map<String, WeeklyInstanceSchedule> instanceScheduleMap = new HashMap<>();
        for (WeeklyInstanceSchedule schedule : instanceSchedules) {
            String key = schedule.getDayOfWeek() + "_" + schedule.getStartTime() + "_" + schedule.getEndTime();
            instanceScheduleMap.put(key, schedule);
        }

        LocalDateTime now = LocalDateTime.now();

        for (Schedules templateSchedule : templateSchedules) {
            // 计算课程具体时间
            LocalDate scheduleDate = calculateScheduleDate(instance.getWeekStartDate(), templateSchedule.getDayOfWeek());
            LocalDateTime scheduleDateTime = LocalDateTime.of(scheduleDate, templateSchedule.getStartTime());

            // 只对当前时间之后的课程进行同步
            if (scheduleDateTime.isAfter(now)) {
                String key = templateSchedule.getDayOfWeek() + "_" + templateSchedule.getStartTime() + "_" + templateSchedule.getEndTime();
                WeeklyInstanceSchedule existingSchedule = instanceScheduleMap.get(key);

                if (existingSchedule != null) {
                    // 更新现有课程（只有非手动添加的课程才能被覆盖）
                    if (existingSchedule.getIsManualAdded() == null || !existingSchedule.getIsManualAdded()) {
                        existingSchedule.setTemplateScheduleId(templateSchedule.getId());
                        existingSchedule.setStudentName(templateSchedule.getStudentName());
                        existingSchedule.setSubject(templateSchedule.getSubject());
                        existingSchedule.setNote(templateSchedule.getNote());
                        existingSchedule.setIsModified(false);
                        existingSchedule.setUpdatedAt(LocalDateTime.now());
                        weeklyInstanceScheduleRepository.save(existingSchedule);
                    }
                } else {
                    // 创建新课程
                    WeeklyInstanceSchedule newSchedule = new WeeklyInstanceSchedule(
                        instance.getId(),
                        templateSchedule.getId(),
                        templateSchedule.getStudentName(),
                        templateSchedule.getSubject(),
                        templateSchedule.getDayOfWeek(),
                        templateSchedule.getStartTime(),
                        templateSchedule.getEndTime(),
                        scheduleDate,
                        templateSchedule.getNote()
                    );
                    weeklyInstanceScheduleRepository.save(newSchedule);
                }
            }
        }

        // 更新同步时间
        weeklyInstanceRepository.updateLastSyncedAt(instance.getId(), LocalDateTime.now());
    }

    /**
     * 根据课程时间智能判断是否同步到当前周实例
     * 只有未来时间的课程才会被同步
     */
    @Transactional
    public void syncSpecificTemplateSchedulesToCurrentInstanceByTime(Long templateTimetableId, java.util.List<Schedules> templateSchedules) {
        if (templateSchedules == null || templateSchedules.isEmpty()) {
            return;
        }
        WeeklyInstance currentInstance = getCurrentWeekInstance(templateTimetableId);
        if (currentInstance == null) {
            logger.info("当前周没有实例，自动生成当前周实例");
            try {
                currentInstance = generateCurrentWeekInstance(templateTimetableId);
            } catch (Exception e) {
                logger.error("生成当前周实例失败: {}", e.getMessage(), e);
                return;
            }
        }

        LocalDateTime now = LocalDateTime.now();
        logger.info("当前时间: {}", now);

        for (Schedules templateSchedule : templateSchedules) {
            // 计算课程在当前周的具体时间
            LocalDate scheduleDate = calculateScheduleDate(currentInstance.getWeekStartDate(), templateSchedule.getDayOfWeek());
            LocalDateTime scheduleDateTime = LocalDateTime.of(scheduleDate, templateSchedule.getStartTime());
            
            logger.info("课程时间: {} {}, 是否未来: {}", 
                scheduleDate, templateSchedule.getStartTime(), scheduleDateTime.isAfter(now));

            // 只有未来时间的课程才同步到当前周实例
            if (scheduleDateTime.isAfter(now)) {
                // 检查实例中是否已存在相同时间段的课程
                String key = templateSchedule.getDayOfWeek() + "_" + templateSchedule.getStartTime() + "_" + templateSchedule.getEndTime();
                List<WeeklyInstanceSchedule> existingSchedules = weeklyInstanceScheduleRepository.findByWeeklyInstanceId(currentInstance.getId());
                
                WeeklyInstanceSchedule existingSchedule = null;
                for (WeeklyInstanceSchedule s : existingSchedules) {
                    String existingKey = s.getDayOfWeek() + "_" + s.getStartTime() + "_" + s.getEndTime();
                    if (existingKey.equals(key)) {
                        existingSchedule = s;
                        break;
                    }
                }

                if (existingSchedule != null) {
                    // 如果实例中已存在相同时间段的课程，且不是手动添加的，则覆盖
                    if (existingSchedule.getIsManualAdded() == null || !existingSchedule.getIsManualAdded()) {
                        existingSchedule.setTemplateScheduleId(templateSchedule.getId());
                        existingSchedule.setStudentName(templateSchedule.getStudentName());
                        existingSchedule.setSubject(templateSchedule.getSubject());
                        existingSchedule.setNote(templateSchedule.getNote());
                        existingSchedule.setIsTrial(templateSchedule.getIsTrial());
                        existingSchedule.setIsModified(false);
                        existingSchedule.setUpdatedAt(LocalDateTime.now());
                        weeklyInstanceScheduleRepository.save(existingSchedule);
                        logger.info("覆盖实例中的课程: {} {}", templateSchedule.getStudentName(), key);
                    } else {
                        logger.info("跳过手动添加的课程: {} {}", existingSchedule.getStudentName(), key);
                    }
                } else {
                    // 如果实例中不存在，则新增
                    WeeklyInstanceSchedule newSchedule = new WeeklyInstanceSchedule(
                        currentInstance.getId(),
                        templateSchedule.getId(),
                        templateSchedule.getStudentName(),
                        templateSchedule.getSubject(),
                        templateSchedule.getDayOfWeek(),
                        templateSchedule.getStartTime(),
                        templateSchedule.getEndTime(),
                        scheduleDate,
                        templateSchedule.getNote()
                    );
                    newSchedule.setIsManualAdded(false);
                    newSchedule.setIsModified(false);
                    newSchedule.setIsTrial(templateSchedule.getIsTrial());
                    weeklyInstanceScheduleRepository.save(newSchedule);
                    logger.info("在实例中新增课程: {} {}", templateSchedule.getStudentName(), key);
                }
            } else {
                logger.info("跳过过去时间的课程: {} {} {}", 
                    templateSchedule.getStudentName(), scheduleDate, templateSchedule.getStartTime());
            }
        }

        // 更新同步时间
        weeklyInstanceRepository.updateLastSyncedAt(currentInstance.getId(), LocalDateTime.now());
    }
    
    /**
     * 仅对当前周实例同步"特定模板课程"，并且只覆盖"当前时间之后"的时段。
     * 用于在固定课表新增课程时的选择性同步。
     * @deprecated 使用 syncSpecificTemplateSchedulesToCurrentInstanceByTime 代替
     */
    @Transactional
    @Deprecated
    public void syncSpecificTemplateSchedulesToCurrentInstanceSelective(Long templateTimetableId, java.util.List<Schedules> templateSchedules) {
        if (templateSchedules == null || templateSchedules.isEmpty()) {
            return;
        }
        WeeklyInstance currentInstance = getCurrentWeekInstance(templateTimetableId);
        if (currentInstance == null) {
            return;
        }

        // 现有实例课程映射（key: DOW_start_end）
        java.util.List<WeeklyInstanceSchedule> existing = weeklyInstanceScheduleRepository.findByWeeklyInstanceId(currentInstance.getId());
        java.util.Map<String, WeeklyInstanceSchedule> existingMap = new java.util.HashMap<>();
        for (WeeklyInstanceSchedule s : existing) {
            String key = s.getDayOfWeek() + "_" + s.getStartTime() + "_" + s.getEndTime();
            existingMap.put(key, s);
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        for (Schedules templateSchedule : templateSchedules) {
            // 计算本周具体日期
            java.time.LocalDate date = calculateScheduleDate(currentInstance.getWeekStartDate(), templateSchedule.getDayOfWeek());
            java.time.LocalDateTime scheduleStart = java.time.LocalDateTime.of(date, templateSchedule.getStartTime());

            // 仅当课程开始时间晚于当前时间才同步覆盖
            if (scheduleStart.isAfter(now)) {
                String key = templateSchedule.getDayOfWeek() + "_" + templateSchedule.getStartTime() + "_" + templateSchedule.getEndTime();
                WeeklyInstanceSchedule exist = existingMap.get(key);
                if (exist != null) {
                    // 覆盖
                    exist.setTemplateScheduleId(templateSchedule.getId());
                    exist.setStudentName(templateSchedule.getStudentName());
                    exist.setSubject(templateSchedule.getSubject());
                    exist.setNote(templateSchedule.getNote());
                    exist.setIsTrial(templateSchedule.getIsTrial());
                    exist.setIsManualAdded(false);
                    exist.setIsModified(false);
                    exist.setUpdatedAt(java.time.LocalDateTime.now());
                    weeklyInstanceScheduleRepository.save(exist);
                } else {
                    // 新增
                    WeeklyInstanceSchedule instanceSchedule = new WeeklyInstanceSchedule(
                            currentInstance.getId(),
                            templateSchedule.getId(),
                            templateSchedule.getStudentName(),
                            templateSchedule.getSubject(),
                            templateSchedule.getDayOfWeek(),
                            templateSchedule.getStartTime(),
                            templateSchedule.getEndTime(),
                            date,
                            templateSchedule.getNote()
                    );
                    instanceSchedule.setIsTrial(templateSchedule.getIsTrial());
                    weeklyInstanceScheduleRepository.save(instanceSchedule);
                }
            }
        }

        // 更新同步时间
        weeklyInstanceRepository.updateLastSyncedAt(currentInstance.getId(), java.time.LocalDateTime.now());
    }

    /**
     * 完全恢复当前周实例为固定课表状态
     */
    @Transactional
    public void restoreCurrentWeekInstanceToTemplate(Long templateTimetableId) {
        WeeklyInstance currentInstance = getCurrentWeekInstance(templateTimetableId);
        if (currentInstance != null) {
            restoreInstanceToTemplate(currentInstance);
        }
    }

    /**
     * 当模板课程被删除时，删除相关的实例课程
     */
    @Transactional
    public void handleTemplateScheduleDeleted(Long templateScheduleId) {
        weeklyInstanceScheduleRepository.deleteByTemplateScheduleId(templateScheduleId);
    }

    /**
     * 当模板课程被删除时：仅删除"当前周实例"中对应且属于未来时间段的实例课程
     * 仅适用于周固定课表的本周实例
     */
    @Transactional
    public void deleteCurrentWeekInstanceScheduleIfFuture(Long templateTimetableId, Schedules templateSchedule) {
        if (templateSchedule == null || templateSchedule.getDayOfWeek() == null
                || templateSchedule.getStartTime() == null) {
            return;
        }

        WeeklyInstance currentInstance = getCurrentWeekInstance(templateTimetableId);
        if (currentInstance == null) {
            return;
        }

        LocalDate scheduleDate = calculateScheduleDate(currentInstance.getWeekStartDate(), templateSchedule.getDayOfWeek());
        LocalDateTime scheduleStart = LocalDateTime.of(scheduleDate, templateSchedule.getStartTime());
        if (!scheduleStart.isAfter(LocalDateTime.now())) {
            // 仅处理未来时间段
            return;
        }

        // 精确定位当前周实例当天对应时间段的课程，仅删除非手动添加的记录
        List<WeeklyInstanceSchedule> daySchedules = weeklyInstanceScheduleRepository
                .findByWeeklyInstanceIdAndDate(currentInstance.getId(), scheduleDate);
        for (WeeklyInstanceSchedule s : daySchedules) {
            boolean sameSlot = Objects.equals(s.getDayOfWeek(), templateSchedule.getDayOfWeek())
                    && Objects.equals(s.getStartTime(), templateSchedule.getStartTime())
                    && Objects.equals(s.getEndTime(), templateSchedule.getEndTime());
            if (sameSlot && (s.getIsManualAdded() == null || !s.getIsManualAdded())) {
                weeklyInstanceScheduleRepository.delete(s.getId());
            }
        }
        weeklyInstanceRepository.updateLastSyncedAt(currentInstance.getId(), LocalDateTime.now());
    }

    /**
     * 当模板课程被更新时，更新相关的实例课程
     */
    @Transactional
    public void handleTemplateScheduleUpdated(Schedules updatedTemplateSchedule) {
        List<WeeklyInstanceSchedule> instanceSchedules = weeklyInstanceScheduleRepository.findByTemplateScheduleId(updatedTemplateSchedule.getId());
        
        for (WeeklyInstanceSchedule instanceSchedule : instanceSchedules) {
            // 只更新非手动修改的实例课程
            if (instanceSchedule.getIsModified() == null || !instanceSchedule.getIsModified()) {
                WeeklyInstance instance = weeklyInstanceRepository.findById(instanceSchedule.getWeeklyInstanceId());
                if (instance != null) {
                    LocalDate scheduleDate = calculateScheduleDate(instance.getWeekStartDate(), updatedTemplateSchedule.getDayOfWeek());
                    
                    instanceSchedule.setStudentName(updatedTemplateSchedule.getStudentName());
                    instanceSchedule.setSubject(updatedTemplateSchedule.getSubject());
                    instanceSchedule.setDayOfWeek(updatedTemplateSchedule.getDayOfWeek());
                    instanceSchedule.setStartTime(updatedTemplateSchedule.getStartTime());
                    instanceSchedule.setEndTime(updatedTemplateSchedule.getEndTime());
                    instanceSchedule.setScheduleDate(scheduleDate);
                    instanceSchedule.setNote(updatedTemplateSchedule.getNote());
                    instanceSchedule.setUpdatedAt(LocalDateTime.now());
                    
                    weeklyInstanceScheduleRepository.save(instanceSchedule);
                }
            }
        }
    }

    /**
     * 创建新的实例课程（手动添加）
     */
    @Transactional
    public WeeklyInstanceSchedule createInstanceSchedule(Long instanceId, WeeklyInstanceSchedule schedule) {
        schedule.setWeeklyInstanceId(instanceId);
        schedule.setIsManualAdded(true);
        schedule.setIsModified(false);
        schedule.setCreatedAt(LocalDateTime.now());
        schedule.setUpdatedAt(LocalDateTime.now());
        
        // 如果没有提供scheduleDate，则根据周实例的开始日期和星期几自动计算
        if (schedule.getScheduleDate() == null && schedule.getDayOfWeek() != null) {
            WeeklyInstance instance = weeklyInstanceRepository.findById(instanceId);
            if (instance != null) {
                LocalDate calculatedDate = calculateScheduleDate(instance.getWeekStartDate(), schedule.getDayOfWeek());
                schedule.setScheduleDate(calculatedDate);
            }
        }
        
        return weeklyInstanceScheduleRepository.save(schedule);
    }

    /**
     * 批量创建实例课程（手动添加）
     */
    @Transactional
    public void createInstanceSchedulesBatch(Long instanceId, List<WeeklyInstanceSchedule> schedules) {
        LocalDateTime now = LocalDateTime.now();
        WeeklyInstance instance = weeklyInstanceRepository.findById(instanceId);
        
        for (WeeklyInstanceSchedule schedule : schedules) {
            schedule.setWeeklyInstanceId(instanceId);
            schedule.setIsManualAdded(true);
            schedule.setIsModified(false);
            schedule.setCreatedAt(now);
            schedule.setUpdatedAt(now);
            
            // 如果没有提供scheduleDate，则根据周实例的开始日期和星期几自动计算
            if (schedule.getScheduleDate() == null && schedule.getDayOfWeek() != null && instance != null) {
                LocalDate calculatedDate = calculateScheduleDate(instance.getWeekStartDate(), schedule.getDayOfWeek());
                schedule.setScheduleDate(calculatedDate);
            }
            
            weeklyInstanceScheduleRepository.save(schedule);
        }
    }

    /**
     * 更新实例课程
     */
    @Transactional
    public WeeklyInstanceSchedule updateInstanceSchedule(Long scheduleId, WeeklyInstanceSchedule updatedSchedule) {
        WeeklyInstanceSchedule existingSchedule = weeklyInstanceScheduleRepository.findById(scheduleId);
        if (existingSchedule == null) {
            throw new IllegalArgumentException("实例课程不存在");
        }

        // 只更新非null的字段，避免将null值设置到不允许为空的字段
        if (updatedSchedule.getStudentName() != null) {
            existingSchedule.setStudentName(updatedSchedule.getStudentName());
        }
        if (updatedSchedule.getSubject() != null) {
            existingSchedule.setSubject(updatedSchedule.getSubject());
        }
        if (updatedSchedule.getDayOfWeek() != null) {
            existingSchedule.setDayOfWeek(updatedSchedule.getDayOfWeek());
        }
        if (updatedSchedule.getStartTime() != null) {
            existingSchedule.setStartTime(updatedSchedule.getStartTime());
        }
        if (updatedSchedule.getEndTime() != null) {
            existingSchedule.setEndTime(updatedSchedule.getEndTime());
        }
        if (updatedSchedule.getScheduleDate() != null) {
            existingSchedule.setScheduleDate(updatedSchedule.getScheduleDate());
        }
        if (updatedSchedule.getNote() != null) {
            existingSchedule.setNote(updatedSchedule.getNote());
        }
        if (updatedSchedule.getIsTrial() != null) {
            existingSchedule.setIsTrial(updatedSchedule.getIsTrial());
        }
        
        // 检查是否与原始模板不同
        boolean isDifferentFromTemplate = false;
        
        // 如果有模板ID，则与模板比较
        if (existingSchedule.getTemplateScheduleId() != null) {
            Schedules templateSchedule = scheduleRepository.findById(existingSchedule.getTemplateScheduleId());
            if (templateSchedule != null) {
                // 比较各个字段是否与模板不同
                if (!Objects.equals(existingSchedule.getStudentName(), templateSchedule.getStudentName())) {
                    isDifferentFromTemplate = true;
                }
                if (!Objects.equals(existingSchedule.getSubject(), templateSchedule.getSubject())) {
                    isDifferentFromTemplate = true;
                }
                if (!Objects.equals(existingSchedule.getDayOfWeek(), templateSchedule.getDayOfWeek())) {
                    isDifferentFromTemplate = true;
                }
                if (!Objects.equals(existingSchedule.getStartTime(), templateSchedule.getStartTime())) {
                    isDifferentFromTemplate = true;
                }
                if (!Objects.equals(existingSchedule.getEndTime(), templateSchedule.getEndTime())) {
                    isDifferentFromTemplate = true;
                }
                if (!Objects.equals(existingSchedule.getNote(), templateSchedule.getNote())) {
                    isDifferentFromTemplate = true;
                }
                if (!Objects.equals(existingSchedule.getIsTrial(), templateSchedule.getIsTrial())) {
                    isDifferentFromTemplate = true;
                }
            }
        } else {
            // 如果没有模板ID，说明是手动添加的课程，不应该标记为已修改
            isDifferentFromTemplate = false;
        }
        
        // 根据是否与模板不同来设置修改标记
        existingSchedule.setIsModified(isDifferentFromTemplate);
        existingSchedule.setUpdatedAt(LocalDateTime.now());

        return weeklyInstanceScheduleRepository.save(existingSchedule);
    }

    /**
     * 调换两个周实例课程
     */
    @Transactional
    public boolean swapInstanceSchedules(Long scheduleId1, Long scheduleId2) {
        try {
            // 获取两个周实例课程
            WeeklyInstanceSchedule schedule1 = weeklyInstanceScheduleRepository.findById(scheduleId1);
            WeeklyInstanceSchedule schedule2 = weeklyInstanceScheduleRepository.findById(scheduleId2);
            
            if (schedule1 == null || schedule2 == null) {
                logger.warn("调换周实例课程失败：课程不存在，scheduleId1={}, scheduleId2={}", 
                    scheduleId1, scheduleId2);
                return false;
            }
            
            // 交换学生姓名
            String tempStudentName = schedule1.getStudentName();
            schedule1.setStudentName(schedule2.getStudentName());
            schedule2.setStudentName(tempStudentName);
            
            // 检查调换后是否与模板一致，并设置修改标记
            checkAndSetModifiedFlag(schedule1);
            checkAndSetModifiedFlag(schedule2);
            
            schedule1.setUpdatedAt(LocalDateTime.now());
            schedule2.setUpdatedAt(LocalDateTime.now());
            
            // 保存更新
            weeklyInstanceScheduleRepository.save(schedule1);
            weeklyInstanceScheduleRepository.save(schedule2);
            
            logger.info("周实例课程调换成功：{} <-> {}, scheduleId1={}, scheduleId2={}", 
                schedule1.getStudentName(), schedule2.getStudentName(), scheduleId1, scheduleId2);
            
            return true;
        } catch (Exception e) {
            logger.error("调换周实例课程失败", e);
            return false;
        }
    }
    
    /**
     * 检查周实例课程是否与模板一致，并设置修改标记和备注
     */
    private void checkAndSetModifiedFlag(WeeklyInstanceSchedule schedule) {
        boolean isDifferentFromTemplate = false;
        
        // 如果有模板ID，则与模板比较
        if (schedule.getTemplateScheduleId() != null) {
            Schedules templateSchedule = scheduleRepository.findById(schedule.getTemplateScheduleId());
            if (templateSchedule != null) {
                // 比较各个字段是否与模板不同
                if (!Objects.equals(schedule.getStudentName(), templateSchedule.getStudentName())) {
                    isDifferentFromTemplate = true;
                }
                if (!Objects.equals(schedule.getSubject(), templateSchedule.getSubject())) {
                    isDifferentFromTemplate = true;
                }
                if (!Objects.equals(schedule.getDayOfWeek(), templateSchedule.getDayOfWeek())) {
                    isDifferentFromTemplate = true;
                }
                if (!Objects.equals(schedule.getStartTime(), templateSchedule.getStartTime())) {
                    isDifferentFromTemplate = true;
                }
                if (!Objects.equals(schedule.getEndTime(), templateSchedule.getEndTime())) {
                    isDifferentFromTemplate = true;
                }
            }
        } else {
            // 如果没有模板ID，说明是手动添加的课程，标记为已修改
            isDifferentFromTemplate = true;
        }
        
        // 根据是否与模板不同来设置修改标记和备注
        schedule.setIsModified(isDifferentFromTemplate);
        if (isDifferentFromTemplate) {
            schedule.setNote("调换课程");
        } else {
            // 如果与模板一致，清除备注（或保留原备注）
            if ("调换课程".equals(schedule.getNote())) {
                schedule.setNote(null);
            }
        }
    }

    /**
     * 删除实例课程
     */
    @Transactional
    public void deleteInstanceSchedule(Long scheduleId) {
        weeklyInstanceScheduleRepository.delete(scheduleId);
    }

    /**
     * 批量删除周实例中的课程
     */
    public int deleteInstanceSchedulesBatch(List<Long> scheduleIds) {
        int deletedCount = 0;
        for (Long scheduleId : scheduleIds) {
            try {
                weeklyInstanceScheduleRepository.delete(scheduleId);
                deletedCount++;
            } catch (Exception e) {
                // 记录错误但继续删除其他课程
                logger.error("删除实例课程失败，ID: {}, 错误: {}", scheduleId, e.getMessage());
            }
        }
        return deletedCount;
    }

    /**
     * 学生请假
     */
    @Transactional
    public WeeklyInstanceSchedule requestLeave(Long scheduleId, String leaveReason) {
        WeeklyInstanceSchedule schedule = weeklyInstanceScheduleRepository.findById(scheduleId);
        if (schedule == null) {
            throw new IllegalArgumentException("课程不存在");
        }
        
        if (schedule.getIsOnLeave() != null && schedule.getIsOnLeave()) {
            throw new IllegalArgumentException("该课程已经请假");
        }
        
        schedule.setIsOnLeave(true);
        schedule.setLeaveReason(leaveReason);
        schedule.setLeaveRequestedAt(LocalDateTime.now());
        schedule.setUpdatedAt(LocalDateTime.now());
        
        return weeklyInstanceScheduleRepository.update(schedule);
    }

    /**
     * 取消请假或恢复取消的课程
     */
    @Transactional
    public WeeklyInstanceSchedule cancelLeave(Long scheduleId) {
        WeeklyInstanceSchedule schedule = weeklyInstanceScheduleRepository.findById(scheduleId);
        if (schedule == null) {
            throw new IllegalArgumentException("课程不存在");
        }
        
        // 如果是请假状态，则恢复请假
        if (schedule.getIsOnLeave() != null && schedule.getIsOnLeave()) {
            schedule.setIsOnLeave(false);
            schedule.setLeaveReason(null);
            schedule.setLeaveRequestedAt(null);
            schedule.setUpdatedAt(LocalDateTime.now());
            return weeklyInstanceScheduleRepository.update(schedule);
        }
        
        // 如果不是请假状态，说明是取消的课程，直接返回（课程已存在，无需恢复）
        return schedule;
    }

    /**
     * 检查课表是否有当前周实例
     */
    public boolean hasCurrentWeekInstance(Long templateTimetableId) {
        return getCurrentWeekInstance(templateTimetableId) != null;
    }

    /**
     * 清空指定模板课表的当前周实例内的所有课程（包括手动添加）
     * 返回被删除的课程数量
     */
    @Transactional
    public int clearCurrentWeekInstanceSchedules(Long templateTimetableId) {
        WeeklyInstance current = getCurrentWeekInstance(templateTimetableId);
        if (current == null) {
            return 0;
        }
        List<WeeklyInstanceSchedule> existing = weeklyInstanceScheduleRepository.findByWeeklyInstanceId(current.getId());
        int count = existing.size();
        weeklyInstanceScheduleRepository.deleteByWeeklyInstanceId(current.getId());
        return count;
    }

    /**
     * 生成年-周字符串
     */
    private String generateYearWeekString(LocalDate date) {
        WeekFields weekFields = WeekFields.ISO;
        int year = date.getYear();
        int week = date.get(weekFields.weekOfYear());
        return String.format("%d-%02d", year, week);
    }

    /**
     * 根据周开始日期和星期几计算具体日期
     */
    private LocalDate calculateScheduleDate(LocalDate weekStartDate, String dayOfWeekStr) {
        // 周一(MONDAY)是周的第一天，支持中英文各种写法及数字
        String k = dayOfWeekStr == null ? "" : dayOfWeekStr.trim().toUpperCase();
        switch (k) {
            case "1": case "MONDAY": case "星期一": case "周一": case "一":
                return weekStartDate.with(DayOfWeek.MONDAY);
            case "2": case "TUESDAY": case "星期二": case "周二": case "二":
                return weekStartDate.with(DayOfWeek.TUESDAY);
            case "3": case "WEDNESDAY": case "星期三": case "周三": case "三":
                return weekStartDate.with(DayOfWeek.WEDNESDAY);
            case "4": case "THURSDAY": case "星期四": case "周四": case "四":
                return weekStartDate.with(DayOfWeek.THURSDAY);
            case "5": case "FRIDAY": case "星期五": case "周五": case "五":
                return weekStartDate.with(DayOfWeek.FRIDAY);
            case "6": case "SATURDAY": case "星期六": case "周六": case "六":
                return weekStartDate.with(DayOfWeek.SATURDAY);
            case "7": case "SUNDAY": case "星期日": case "星期天": case "周日": case "日": case "天":
                return weekStartDate.with(DayOfWeek.SUNDAY);
            default:
                throw new IllegalArgumentException("无效的星期几: " + dayOfWeekStr);
        }
    }

    /**
     * 根据日期返回"实例逻辑"的活动课表课程（今日从本周实例；跨周日期取对应周实例）
     */
    public Map<String, Object> getActiveInstanceSchedulesByDate(String dateStr, Long organizationId) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> timetableSchedules = new ArrayList<>();

        LocalDate targetDate = LocalDate.parse(dateStr);

        // 获取指定机构的所有活动课表（未删除未归档）
        List<Timetables> activeTimetables = timetableRepository.findAll()
                .stream()
                .filter(t -> t.getOrganizationId() != null && t.getOrganizationId().equals(organizationId))
                .filter(t -> t.getIsActive() != null && t.getIsActive() == 1)
                .filter(t -> t.getIsDeleted() == null || t.getIsDeleted() == 0)
                .filter(t -> t.getIsArchived() == null || t.getIsArchived() == 0)
                .collect(Collectors.toList());

        // 注意：在"其他教练课程"视图中，不应用隐藏学员规则
        // 这样可以完整显示所有教练的课程安排，便于查看时间段占用情况

        for (Timetables timetable : activeTimetables) {
            List<WeeklyInstanceSchedule> instanceSchedules = new ArrayList<>();

            if (timetable.getIsWeekly() != null && timetable.getIsWeekly() == 1) {
                // 周固定：以目标日期归属的周为准选择实例
                WeeklyInstance instance;
                LocalDate targetMonday = targetDate.with(java.time.DayOfWeek.MONDAY);
                String yearWeek = generateYearWeekString(targetMonday);
                instance = weeklyInstanceRepository.findByTemplateIdAndYearWeek(timetable.getId(), yearWeek);
                if (instance == null) {
                    // 按周生成：若目标周是当前周，则生成当前周；若是下周，则生成下周
                    LocalDate thisMonday = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
                    if (targetMonday.isAfter(thisMonday)) {
                        try { 
                            generateNextWeekInstance(timetable.getId()); 
                            logger.info("自动生成课表 {} 的未来周实例", timetable.getId());
                        } catch (Exception e) {
                            logger.warn("自动生成课表 {} 的未来周实例失败: {}", timetable.getId(), e.getMessage());
                        }
                        instance = weeklyInstanceRepository.findByTemplateIdAndYearWeek(timetable.getId(), yearWeek);
                    } else if (targetMonday.isEqual(thisMonday)) {
                        try { 
                            generateCurrentWeekInstance(timetable.getId()); 
                            logger.info("自动生成课表 {} 的当前周实例", timetable.getId());
                        } catch (Exception e) {
                            logger.warn("自动生成课表 {} 的当前周实例失败: {}", timetable.getId(), e.getMessage());
                        }
                        instance = weeklyInstanceRepository.findByTemplateIdAndYearWeek(timetable.getId(), yearWeek);
                    }
                }

                if (instance != null) {
                    List<WeeklyInstanceSchedule> all = weeklyInstanceScheduleRepository.findByWeeklyInstanceId(instance.getId());
                    
                    instanceSchedules = all.stream()
                            .filter(s -> targetDate.equals(s.getScheduleDate()))
                            // 在"其他教练课程"视图中，显示所有课程（包括请假和隐藏的学员）
                            // 这样可以完整地看到时间段的占用情况
                            .collect(Collectors.toList());
                    
                    // 去重：基于学生姓名、开始时间、结束时间的组合
                    Map<String, WeeklyInstanceSchedule> uniqueSchedules = new HashMap<>();
                    for (WeeklyInstanceSchedule schedule : instanceSchedules) {
                        String key = schedule.getStudentName() + "_" + schedule.getStartTime() + "_" + schedule.getEndTime();
                        if (!uniqueSchedules.containsKey(key)) {
                            uniqueSchedules.put(key, schedule);
                        } else {
                            logger.warn("发现重复课程数据，课表ID: {}, 学生: {}, 时间: {}-{}", 
                                timetable.getId(), schedule.getStudentName(), 
                                schedule.getStartTime(), schedule.getEndTime());
                        }
                    }
                    instanceSchedules = new ArrayList<>(uniqueSchedules.values());
                }
            } else {
                // 日期范围课表：直接取具体日期（日期范围课表没有请假功能）
                List<Schedules> daySchedules = scheduleRepository.findByTimetableIdAndScheduleDate(timetable.getId(), targetDate);
                
                // 映射为实例样式，在"其他教练课程"中显示所有课程
                instanceSchedules = daySchedules.stream()
                        .map(s -> {
                            WeeklyInstanceSchedule w = new WeeklyInstanceSchedule();
                            w.setStudentName(s.getStudentName());
                            w.setSubject(s.getSubject());
                            w.setDayOfWeek(s.getDayOfWeek());
                            w.setStartTime(s.getStartTime());
                            w.setEndTime(s.getEndTime());
                            w.setScheduleDate(s.getScheduleDate());
                            return w;
                        }).collect(Collectors.toList());
            }

            if (!instanceSchedules.isEmpty()) {
                Map<String, Object> item = new HashMap<>();
                // owner 与 timetable 信息
                com.timetable.generated.tables.pojos.Users u = userService.findById(timetable.getUserId());
                item.put("ownerName", u != null ? (u.getNickname() != null ? u.getNickname() : u.getUsername()) : "");
                item.put("ownerNickname", u != null ? u.getNickname() : null);
                item.put("ownerUsername", u != null ? u.getUsername() : "");
                item.put("ownerRole", u != null ? u.getRole() : null);
                item.put("timetableId", timetable.getId());
                item.put("timetableName", timetable.getName());
                item.put("isWeekly", timetable.getIsWeekly());
                // 明细 - 过滤掉已取消的体验课
                List<Map<String, Object>> schedules = instanceSchedules.stream()
                    .filter(s -> {
                        // 如果不是体验课，直接保留
                        if (s.getIsTrial() == null || s.getIsTrial() != 1) {
                            return true;
                        }
                        
                        // 如果是体验课，检查是否已被取消
                        try {
                            String sql = "SELECT COUNT(*) FROM customer_status_history " +
                                "WHERE trial_student_name = ? " +
                                "AND trial_schedule_date = ? " +
                                "AND trial_start_time = ? " +
                                "AND trial_end_time = ? " +
                                "AND (trial_cancelled IS NULL OR trial_cancelled = FALSE)";
                            
                            Integer count = jdbcTemplate.queryForObject(sql, Integer.class,
                                s.getStudentName(),
                                s.getScheduleDate(),
                                s.getStartTime(),
                                s.getEndTime());
                            
                            // 如果存在未取消的记录，保留；否则过滤掉
                            return count != null && count > 0;
                        } catch (Exception e) {
                            // 查询失败，保守处理：保留该课程
                            logger.warn("Failed to check trial schedule cancellation status: {}", e.getMessage());
                            return true;
                        }
                    })
                    .map(s -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("studentName", s.getStudentName());
                        m.put("startTime", s.getStartTime());
                        m.put("endTime", s.getEndTime());
                        // 标记是否来自实例（周固定课表的实例数据）
                        m.put("isFromInstance", timetable.getIsWeekly() != null && timetable.getIsWeekly() == 1);
                        // 传递请假标记到前端
                        m.put("isOnLeave", s.getIsOnLeave());
                        return m;
                    }).collect(Collectors.toList());
                item.put("schedules", schedules);
                timetableSchedules.add(item);
            }
        }

        result.put("timetableSchedules", timetableSchedules);
        return result;
    }
    
    /**
     * 根据日期查找周实例
     */
    public WeeklyInstance findInstanceByDate(Long templateTimetableId, LocalDate date) {
        String yearWeek = generateYearWeekString(date);
        return weeklyInstanceRepository.findByTemplateIdAndYearWeek(templateTimetableId, yearWeek);
    }

    /**
     * 根据ID查找周实例
     */
    public WeeklyInstance findById(Long instanceId) {
        return weeklyInstanceRepository.findById(instanceId);
    }

    /**
     * 根据课表ID和日期获取实例课程
     */
    public List<WeeklyInstanceSchedule> getSchedulesByDate(Long timetableId, LocalDate date) {
        // 获取课表信息
        Timetables timetable = timetableRepository.findById(timetableId);
        if (timetable == null || timetable.getIsWeekly() == null || timetable.getIsWeekly() != 1) {
            return new ArrayList<>();
        }

        // 查找对应的周实例
        WeeklyInstance instance = findInstanceByDate(timetableId, date);
        if (instance == null) {
            // 如果找不到实例，尝试自动生成
            try {
                LocalDate today = LocalDate.now();
                LocalDate targetMonday = date.with(java.time.DayOfWeek.MONDAY);
                LocalDate thisMonday = today.with(java.time.DayOfWeek.MONDAY);
                
                if (targetMonday.isEqual(thisMonday)) {
                    // 目标日期是本周，生成当前周实例
                    logger.info("自动生成课表 {} 的当前周实例", timetableId);
                    instance = generateCurrentWeekInstance(timetableId);
                } else if (targetMonday.isAfter(thisMonday)) {
                    // 目标日期是未来周，生成对应周实例
                    logger.info("自动生成课表 {} 的未来周实例", timetableId);
                    instance = generateNextWeekInstance(timetableId);
                }
            } catch (Exception e) {
                logger.warn("自动生成课表 {} 的周实例失败: {}", timetableId, e.getMessage());
            }
            
            if (instance == null) {
                return new ArrayList<>();
            }
        }

        // 获取该实例在指定日期的课程
        List<WeeklyInstanceSchedule> allSchedules = weeklyInstanceScheduleRepository.findByWeeklyInstanceId(instance.getId());
        List<WeeklyInstanceSchedule> filteredSchedules = allSchedules.stream()
                .filter(schedule -> date.equals(schedule.getScheduleDate()))
                .collect(Collectors.toList());
        
        // 去重：基于学生姓名、开始时间、结束时间的组合
        Map<String, WeeklyInstanceSchedule> uniqueSchedules = new HashMap<>();
        for (WeeklyInstanceSchedule schedule : filteredSchedules) {
            String key = schedule.getStudentName() + "_" + schedule.getStartTime() + "_" + schedule.getEndTime();
            if (!uniqueSchedules.containsKey(key)) {
                uniqueSchedules.put(key, schedule);
            } else {
                logger.warn("发现重复课程数据，实例ID: {}, 学生: {}, 时间: {}-{}", 
                    instance.getId(), schedule.getStudentName(), 
                    schedule.getStartTime(), schedule.getEndTime());
            }
        }
        
        return new ArrayList<>(uniqueSchedules.values());
    }

    /**
     * 批量为所有活动的周固定课表生成当前周实例
     */
    @Transactional
    public Map<String, Object> generateCurrentWeekInstancesForAllActiveTimetables() {
        logger.info("开始批量生成所有活动周固定课表的当前周实例");
        
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int failedCount = 0;
        int skippedCount = 0;
        List<String> errors = new ArrayList<>();
        
        // 获取所有活动的周固定课表
        List<Timetables> weeklyTimetables = timetableRepository.findAll()
                .stream()
                .filter(t -> t.getIsWeekly() != null && t.getIsWeekly() == 1)
                .filter(t -> t.getIsActive() != null && t.getIsActive() == 1)
                .filter(t -> t.getIsDeleted() == null || t.getIsDeleted() == 0)
                .filter(t -> t.getIsArchived() == null || t.getIsArchived() == 0)
                .collect(Collectors.toList());
        
        logger.info("找到 {} 个活动的周固定课表", weeklyTimetables.size());
        
        for (Timetables timetable : weeklyTimetables) {
            try {
                // 检查是否已经存在当前周实例
                WeeklyInstance existingInstance = getCurrentWeekInstance(timetable.getId());
                if (existingInstance != null) {
                    logger.debug("课表 {} ({}) 已有当前周实例，跳过", timetable.getId(), timetable.getName());
                    skippedCount++;
                    continue;
                }
                
                // 生成当前周实例
                WeeklyInstance newInstance = generateCurrentWeekInstance(timetable.getId());
                if (newInstance != null) {
                    logger.info("成功为课表 {} ({}) 生成当前周实例", timetable.getId(), timetable.getName());
                    successCount++;
                } else {
                    logger.warn("为课表 {} ({}) 生成当前周实例失败", timetable.getId(), timetable.getName());
                    failedCount++;
                    errors.add(String.format("课表 %s (%s): 生成失败", timetable.getId(), timetable.getName()));
                }
            } catch (Exception e) {
                logger.error("为课表 {} ({}) 生成当前周实例时发生异常: {}", 
                    timetable.getId(), timetable.getName(), e.getMessage(), e);
                failedCount++;
                errors.add(String.format("课表 %s (%s): %s", timetable.getId(), timetable.getName(), e.getMessage()));
            }
        }
        
        result.put("totalTimetables", weeklyTimetables.size());
        result.put("successCount", successCount);
        result.put("failedCount", failedCount);
        result.put("skippedCount", skippedCount);
        result.put("errors", errors);
        
        logger.info("批量生成完成: 总数={}, 成功={}, 失败={}, 跳过={}", 
            weeklyTimetables.size(), successCount, failedCount, skippedCount);
        
        return result;
    }

    /**
     * 检查并生成缺失的当前周实例（自动修复功能）
     */
    @Transactional
    public void ensureCurrentWeekInstancesExist() {
        logger.info("开始检查并生成缺失的当前周实例");
        
        // 获取所有活动的周固定课表
        List<Timetables> weeklyTimetables = timetableRepository.findAll()
                .stream()
                .filter(t -> t.getIsWeekly() != null && t.getIsWeekly() == 1)
                .filter(t -> t.getIsActive() != null && t.getIsActive() == 1)
                .filter(t -> t.getIsDeleted() == null || t.getIsDeleted() == 0)
                .filter(t -> t.getIsArchived() == null || t.getIsArchived() == 0)
                .collect(Collectors.toList());
        
        for (Timetables timetable : weeklyTimetables) {
            try {
                WeeklyInstance existingInstance = getCurrentWeekInstance(timetable.getId());
                if (existingInstance == null) {
                    generateCurrentWeekInstance(timetable.getId());
                    logger.info("自动为课表 {} ({}) 生成了当前周实例", timetable.getId(), timetable.getName());
                }
            } catch (Exception e) {
                logger.warn("自动为课表 {} ({}) 生成当前周实例失败: {}", 
                    timetable.getId(), timetable.getName(), e.getMessage());
            }
        }
    }

    /**
     * 获取所有请假记录
     */
    public List<Map<String, Object>> getAllLeaveRecords() {
        List<WeeklyInstanceSchedule> leaveSchedules = weeklyInstanceScheduleRepository.findByIsOnLeave(true);
        List<Map<String, Object>> leaveRecords = new ArrayList<>();
        
        for (WeeklyInstanceSchedule schedule : leaveSchedules) {
            // 获取课表信息
            WeeklyInstance instance = weeklyInstanceRepository.findById(schedule.getWeeklyInstanceId());
            if (instance == null) continue;
            
            Timetables timetable = timetableRepository.findById(instance.getTemplateTimetableId());
            if (timetable == null) continue;
            
            // 获取教练信息
            Users coach = userService.findById(timetable.getUserId());
            String coachName = coach != null ? (coach.getNickname() != null ? coach.getNickname() : coach.getUsername()) : "未知教练";
            
            Map<String, Object> record = new HashMap<>();
            record.put("id", schedule.getId());
            record.put("coachName", coachName);
            record.put("timetableName", timetable.getName());
            record.put("studentName", schedule.getStudentName());
            record.put("subject", schedule.getSubject());
            record.put("scheduleDate", schedule.getScheduleDate());
            record.put("startTime", schedule.getStartTime());
            record.put("endTime", schedule.getEndTime());
            record.put("leaveReason", schedule.getLeaveReason());
            record.put("leaveRequestedAt", schedule.getLeaveRequestedAt());
            record.put("weekStartDate", instance.getWeekStartDate());
            record.put("weekEndDate", instance.getWeekEndDate());
            
            leaveRecords.add(record);
        }
        
        // 按请假时间倒序排列
        leaveRecords.sort((a, b) -> {
            LocalDateTime timeA = (LocalDateTime) a.get("leaveRequestedAt");
            LocalDateTime timeB = (LocalDateTime) b.get("leaveRequestedAt");
            if (timeA == null && timeB == null) return 0;
            if (timeA == null) return 1;
            if (timeB == null) return -1;
            return timeB.compareTo(timeA);
        });
        
        return leaveRecords;
    }

    /**
     * 删除请假记录（取消请假状态）
     */
    @Transactional
    public boolean deleteLeaveRecord(Long scheduleId) {
        try {
            WeeklyInstanceSchedule schedule = weeklyInstanceScheduleRepository.findById(scheduleId);
            if (schedule == null) {
                return false;
            }
            
            // 取消请假状态
            schedule.setIsOnLeave(false);
            schedule.setLeaveReason(null);
            schedule.setLeaveRequestedAt(null);
            schedule.setUpdatedAt(LocalDateTime.now());
            
            weeklyInstanceScheduleRepository.update(schedule);
            return true;
        } catch (Exception e) {
            logger.error("删除请假记录失败，ID: {}, 错误: {}", scheduleId, e.getMessage());
            return false;
        }
    }

    /**
     * 批量删除请假记录
     */
    @Transactional
    public int deleteLeaveRecordsBatch(List<Long> scheduleIds) {
        int deletedCount = 0;
        for (Long scheduleId : scheduleIds) {
            try {
                if (deleteLeaveRecord(scheduleId)) {
                    deletedCount++;
                }
            } catch (Exception e) {
                logger.error("批量删除请假记录失败，ID: {}, 错误: {}", scheduleId, e.getMessage());
            }
        }
        return deletedCount;
    }

    /**
     * 获取学员记录（上课记录和请假记录）
     */
    public Map<String, Object> getStudentRecords(String studentName, String coachName) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> schedules = new ArrayList<>();
        List<Map<String, Object>> leaves = new ArrayList<>();
        
        // 需要查询的学员名称列表（包括原始名和所有被合并的学员名）
        List<String> studentNamesToQuery = new ArrayList<>();
        String displayName = studentName;
        
        try {
            // 查找所有操作记录
            List<StudentOperationRecord> allRecords = studentOperationRecordRepository.findAll();
            
            // 1. 反向查找：如果传入的是重命名后的名字，需要找到原始名字
            String originalStudentName = studentName;
            for (StudentOperationRecord record : allRecords) {
                if ("RENAME".equals(record.getOperationType()) && 
                    studentName.equals(record.getNewName())) {
                    // 找到了！这是重命名后的名字，原始名字是oldName
                    originalStudentName = record.getOldName();
                    displayName = record.getNewName();
                    logger.info("学员详情反向查找(重命名): 显示名='{}' -> 原始名='{}'", displayName, originalStudentName);
                    break;
                }
            }
            studentNamesToQuery.add(originalStudentName);
            
            // 注释掉合并查询逻辑：只查询当前学员名的记录，不查询被合并进来的其他学员
            // 合并规则只影响显示名称，不应该把其他学员的上课记录混在一起
            /*
            // 2. 查找合并规则：如果当前学员是合并后的结果，需要找到所有被合并的学员
            for (StudentOperationRecord record : allRecords) {
                if ("MERGE".equals(record.getOperationType()) && 
                    studentName.equals(record.getNewName())) {
                    // 找到了合并规则！解析所有被合并的学员名
                    String oldNames = record.getOldName();
                    if (oldNames != null && !oldNames.trim().isEmpty()) {
                        String[] names = oldNames.split(",");
                        for (String name : names) {
                            String trimmedName = name.trim();
                            if (!trimmedName.isEmpty() && !studentNamesToQuery.contains(trimmedName)) {
                                studentNamesToQuery.add(trimmedName);
                                logger.info("学员详情反向查找(合并): 显示名='{}' -> 包含被合并学员='{}'", displayName, trimmedName);
                            }
                        }
                    }
                    break;
                }
            }
            */
        } catch (Exception e) {
            logger.error("反向查找学员原始名字失败: {}", e.getMessage());
            // 如果查找失败，至少要查询原始的studentName
            if (studentNamesToQuery.isEmpty()) {
                studentNamesToQuery.add(studentName);
            }
        }
        
        logger.info("获取学员记录: 显示名={}, 需要查询的学员名={}, 教练={}", displayName, studentNamesToQuery, coachName);
        
        // 将查询的学员名列表添加到返回结果中，方便调试
        result.put("queriedNames", studentNamesToQuery);
        result.put("displayName", displayName);
        
        try {
            // 获取当前日期和时间，只显示过去的课程记录
            LocalDate today = LocalDate.now();
            LocalTime currentTime = LocalTime.now();
            
            // 遍历所有需要查询的学员名称（包括原始名和所有被合并的学员名）
            for (String queryName : studentNamesToQuery) {
                logger.info("查询学员记录: {}", queryName);
                
                // 1. 获取实例课表的记录
                List<WeeklyInstanceSchedule> instanceSchedules = weeklyInstanceScheduleRepository.findByStudentName(queryName);
            
            for (WeeklyInstanceSchedule schedule : instanceSchedules) {
                // 只显示过去的课程记录
                if (schedule.getScheduleDate() != null) {
                    LocalDate scheduleDate = schedule.getScheduleDate();
                    if (scheduleDate.isAfter(today)) {
                        // 未来日期的课程，跳过
                        continue;
                    } else if (scheduleDate.isEqual(today)) {
                        // 今天的课程，需要检查时间
                        if (schedule.getEndTime() != null && schedule.getEndTime().isAfter(currentTime)) {
                            // 今天的课程但结束时间还没到，跳过
                            continue;
                        }
                    }
                }
                // 获取课表信息
                WeeklyInstance instance = weeklyInstanceRepository.findById(schedule.getWeeklyInstanceId());
                if (instance == null) continue;
                
                Timetables timetable = timetableRepository.findById(instance.getTemplateTimetableId());
                if (timetable == null) continue;
                
                // 获取教练信息
                Users coach = userService.findById(timetable.getUserId());
                String scheduleCoachName = coach != null ? (coach.getNickname() != null ? coach.getNickname() : coach.getUsername()) : "未知教练";
                
                // 如果指定了教练，只返回该教练的记录（但管理员可以查看所有记录）
                if (coachName != null && !coachName.equals(scheduleCoachName)) {
                    // 检查当前用户是否为管理员，如果是管理员，则允许查看所有记录
                    Users currentUser = userService.findByUsername(coachName);
                    if (currentUser == null || !"MANAGER".equals(currentUser.getPosition())) {
                        continue;
                    }
                }
                
                // 如果是请假记录，只添加到请假列表
                if (schedule.getIsOnLeave() != null && schedule.getIsOnLeave()) {
                    Map<String, Object> leaveRecord = new HashMap<>();
                    leaveRecord.put("id", schedule.getId());
                    leaveRecord.put("leaveDate", schedule.getScheduleDate());
                    leaveRecord.put("timeRange", schedule.getStartTime() + "-" + schedule.getEndTime());
                    leaveRecord.put("leaveReason", schedule.getLeaveReason());
                    leaveRecord.put("timetableName", timetable.getName());
                    leaveRecord.put("coachName", scheduleCoachName);
                    
                    leaves.add(leaveRecord);
                } else {
                    // 只有正常上课记录才添加到上课记录列表
                    Map<String, Object> scheduleRecord = new HashMap<>();
                    scheduleRecord.put("id", schedule.getId());
                    scheduleRecord.put("scheduleDate", schedule.getScheduleDate());
                    scheduleRecord.put("timeRange", schedule.getStartTime() + "-" + schedule.getEndTime());
                    scheduleRecord.put("timetableType", "实例课表");
                    scheduleRecord.put("timetableName", timetable.getName());
                    scheduleRecord.put("status", "正常");
                    scheduleRecord.put("coachName", scheduleCoachName);
                    scheduleRecord.put("queriedName", queryName); // 记录是从哪个学员名查出来的
                    scheduleRecord.put("actualStudentName", schedule.getStudentName()); // 记录实际的学员名
                    
                    logger.info("添加上课记录: 查询名={}, 实际名={}, 日期={}, 时间={}", 
                        queryName, schedule.getStudentName(), schedule.getScheduleDate(), 
                        schedule.getStartTime() + "-" + schedule.getEndTime());
                    
                    schedules.add(scheduleRecord);
                }
            }
            
                // 2. 获取日期类课表的记录
                List<Timetables> allTimetables = timetableRepository.findAll();
                for (Timetables timetable : allTimetables) {
                    // 只处理日期类课表（非周课表）
                    if (timetable.getIsWeekly() != null && timetable.getIsWeekly() == 1) {
                        continue;
                    }
                    
                    // 获取教练信息
                    Users coach = userService.findById(timetable.getUserId());
                    String scheduleCoachName = coach != null ? (coach.getNickname() != null ? coach.getNickname() : coach.getUsername()) : "未知教练";
                    
                    // 如果指定了教练，只返回该教练的记录（但管理员可以查看所有记录）
                    if (coachName != null && !coachName.equals(scheduleCoachName)) {
                        // 检查当前用户是否为管理员，如果是管理员，则允许查看所有记录
                        Users currentUser = userService.findByUsername(coachName);
                        if (currentUser == null || !"MANAGER".equals(currentUser.getPosition())) {
                            continue;
                        }
                    }
                    
                    // 获取该课表中该学员的所有课程记录
                    List<Schedules> dateSchedules = scheduleRepository.findByTimetableIdAndStudentName(timetable.getId(), queryName);
                
                for (Schedules dateSchedule : dateSchedules) {
                    // 只处理有具体日期的记录
                    if (dateSchedule.getScheduleDate() == null) {
                        continue;
                    }
                    
                    // 只显示过去的课程记录
                    LocalDate scheduleDate = dateSchedule.getScheduleDate();
                    if (scheduleDate.isAfter(today)) {
                        // 未来日期的课程，跳过
                        continue;
                    } else if (scheduleDate.isEqual(today)) {
                        // 今天的课程，需要检查时间
                        if (dateSchedule.getEndTime() != null && dateSchedule.getEndTime().isAfter(currentTime)) {
                            // 今天的课程但结束时间还没到，跳过
                            continue;
                        }
                    }
                    
                    Map<String, Object> scheduleRecord = new HashMap<>();
                    scheduleRecord.put("id", dateSchedule.getId());
                    scheduleRecord.put("scheduleDate", dateSchedule.getScheduleDate());
                    scheduleRecord.put("timeRange", dateSchedule.getStartTime() + "-" + dateSchedule.getEndTime());
                    scheduleRecord.put("timetableType", "日期类课表");
                    scheduleRecord.put("timetableName", timetable.getName());
                    scheduleRecord.put("status", "正常"); // 日期类课表暂时不支持请假功能
                    scheduleRecord.put("coachName", scheduleCoachName);
                    scheduleRecord.put("queriedName", queryName); // 记录是从哪个学员名查出来的
                    scheduleRecord.put("actualStudentName", dateSchedule.getStudentName()); // 记录实际的学员名
                    
                    logger.info("添加上课记录(日期类): 查询名={}, 实际名={}, 日期={}, 时间={}", 
                        queryName, dateSchedule.getStudentName(), dateSchedule.getScheduleDate(), 
                        dateSchedule.getStartTime() + "-" + dateSchedule.getEndTime());
                    
                    schedules.add(scheduleRecord);
                }
                }
            } // 结束遍历studentNamesToQuery的循环
            
            // 3. 应用分配课时规则：检查是否有源课程分配课时给当前学员
            try {
                List<StudentOperationRecord> assignHoursRecords = studentOperationRecordRepository.findAll()
                    .stream()
                    .filter(r -> "ASSIGN_HOURS".equals(r.getOperationType()) && studentName.equals(r.getOldName()))
                    .collect(java.util.stream.Collectors.toList());
                
                for (StudentOperationRecord assignRecord : assignHoursRecords) {
                    String sourceCourse = assignRecord.getNewName(); // 源课程名称
                    int hoursPerStudent = 1; // 默认每1个课时分配1课时
                    
                    // 解析details获取hoursPerStudent
                    if (assignRecord.getDetails() != null && !assignRecord.getDetails().isEmpty()) {
                        try {
                            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                            java.util.Map<String, Object> details = mapper.readValue(assignRecord.getDetails(), java.util.Map.class);
                            if (details.containsKey("hoursPerStudent")) {
                                hoursPerStudent = ((Number) details.get("hoursPerStudent")).intValue();
                            }
                        } catch (Exception e) {
                            logger.warn("解析分配课时规则details失败: {}", e.getMessage());
                        }
                    }
                    
                    logger.info("应用分配课时规则: 源课程='{}', 目标学员='{}', 每{}课时分配1课时", sourceCourse, studentName, hoursPerStudent);
                    
                    // 获取源课程的所有课时记录（查询源课程学员的上课记录）
                    List<Map<String, Object>> sourceCourseSchedules = new ArrayList<>();
                    
                    // 获取源课程的实例课表记录
                    List<WeeklyInstanceSchedule> sourceInstanceSchedules = weeklyInstanceScheduleRepository.findByStudentName(sourceCourse);
                    
                    for (WeeklyInstanceSchedule schedule : sourceInstanceSchedules) {
                        // 只处理过去的课程记录
                        if (schedule.getScheduleDate() != null) {
                            LocalDate scheduleDate = schedule.getScheduleDate();
                            if (scheduleDate.isAfter(today)) {
                                continue;
                            } else if (scheduleDate.isEqual(today)) {
                                if (schedule.getEndTime() != null && schedule.getEndTime().isAfter(currentTime)) {
                                    continue;
                                }
                            }
                        }
                        
                        // 跳过请假记录
                        if (schedule.getIsOnLeave() != null && schedule.getIsOnLeave()) {
                            continue;
                        }
                        
                        WeeklyInstance instance = weeklyInstanceRepository.findById(schedule.getWeeklyInstanceId());
                        if (instance == null) continue;
                        
                        Timetables timetable = timetableRepository.findById(instance.getTemplateTimetableId());
                        if (timetable == null) continue;
                        
                        Users coach = userService.findById(timetable.getUserId());
                        String scheduleCoachName = coach != null ? (coach.getNickname() != null ? coach.getNickname() : coach.getUsername()) : "未知教练";
                        
                        Map<String, Object> sourceRecord = new HashMap<>();
                        sourceRecord.put("scheduleDate", schedule.getScheduleDate().toString());
                        sourceRecord.put("timeRange", schedule.getStartTime() + "-" + schedule.getEndTime());
                        sourceRecord.put("coachName", scheduleCoachName);
                        sourceCourseSchedules.add(sourceRecord);
                    }
                    
                    // 获取源课程的日期类课表记录
                    List<Timetables> allTimetables = timetableRepository.findAll();
                    for (Timetables timetable : allTimetables) {
                        if (timetable.getIsWeekly() != null && timetable.getIsWeekly() == 1) {
                            continue;
                        }
                        
                        List<Schedules> dateSchedules = scheduleRepository.findByTimetableId(timetable.getId())
                            .stream()
                            .filter(s -> sourceCourse.equals(s.getStudentName()))
                            .collect(java.util.stream.Collectors.toList());
                        
                        for (Schedules dateSchedule : dateSchedules) {
                            if (dateSchedule.getScheduleDate() == null) continue;
                            
                            LocalDate scheduleDate = dateSchedule.getScheduleDate();
                            if (scheduleDate.isAfter(today)) {
                                continue;
                            } else if (scheduleDate.isEqual(today)) {
                                if (dateSchedule.getEndTime() != null && dateSchedule.getEndTime().isAfter(currentTime)) {
                                    continue;
                                }
                            }
                            
                            Users coach = userService.findById(timetable.getUserId());
                            String scheduleCoachName = coach != null ? (coach.getNickname() != null ? coach.getNickname() : coach.getUsername()) : "未知教练";
                            
                            Map<String, Object> sourceRecord = new HashMap<>();
                            sourceRecord.put("scheduleDate", dateSchedule.getScheduleDate().toString());
                            sourceRecord.put("timeRange", dateSchedule.getStartTime() + "-" + dateSchedule.getEndTime());
                            sourceRecord.put("coachName", scheduleCoachName);
                            sourceCourseSchedules.add(sourceRecord);
                        }
                    }
                    
                    // 按日期和时间排序源课程的记录
                    sourceCourseSchedules.sort((a, b) -> {
                        try {
                            LocalDate dateA = LocalDate.parse((String) a.get("scheduleDate"));
                            LocalDate dateB = LocalDate.parse((String) b.get("scheduleDate"));
                            int dateCompare = dateA.compareTo(dateB);
                            if (dateCompare != 0) return dateCompare;
                            
                            String timeA = (String) a.get("timeRange");
                            String timeB = (String) b.get("timeRange");
                            if (timeA != null && timeB != null) {
                                String startTimeA = timeA.split("-")[0].trim();
                                String startTimeB = timeB.split("-")[0].trim();
                                return LocalTime.parse(startTimeA).compareTo(LocalTime.parse(startTimeB));
                            }
                            return 0;
                        } catch (Exception e) {
                            return 0;
                        }
                    });
                    
                    // 根据规则生成分配的课时记录
                    logger.info("源课程 '{}' 共有 {} 个课时记录", sourceCourse, sourceCourseSchedules.size());
                    for (int i = 0; i < sourceCourseSchedules.size(); i++) {
                        // 每hoursPerStudent个课时，生成1条分配记录
                        if ((i + 1) % hoursPerStudent == 0) {
                            // 获取这一组课时的起始和结束索引
                            int startIndex = i - hoursPerStudent + 1;
                            int endIndex = i;
                            
                            // 获取起始和结束记录
                            Map<String, Object> startRecord = sourceCourseSchedules.get(startIndex);
                            Map<String, Object> endRecord = sourceCourseSchedules.get(endIndex);
                            
                            // 合并时间范围：从第一个课时的开始时间到最后一个课时的结束时间
                            String startTimeRange = (String) startRecord.get("timeRange");
                            String endTimeRange = (String) endRecord.get("timeRange");
                            String startTime = startTimeRange.split("-")[0];
                            String endTime = endTimeRange.split("-")[1];
                            String mergedTimeRange = startTime + "-" + endTime;
                            
                            Map<String, Object> assignedSchedule = new HashMap<>();
                            assignedSchedule.put("id", -1L); // 使用负数ID标识这是分配的课时
                            assignedSchedule.put("scheduleDate", startRecord.get("scheduleDate"));
                            assignedSchedule.put("timeRange", mergedTimeRange);
                            assignedSchedule.put("timetableType", "大课分配课时");
                            assignedSchedule.put("timetableName", sourceCourse + " -> " + studentName);
                            assignedSchedule.put("status", "正常");
                            assignedSchedule.put("coachName", startRecord.get("coachName"));
                            
                            schedules.add(assignedSchedule);
                            logger.info("生成大课分配课时记录: 日期={}, 时间={}, 源课程={}, 合并了{}个课时", 
                                startRecord.get("scheduleDate"), mergedTimeRange, sourceCourse, hoursPerStudent);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("应用分配课时规则失败: {}", e.getMessage());
            }
            
            // 4. 统一按日期和时间倒序排列所有记录
            schedules.sort((a, b) -> {
                try {
                    String dateA = (String) a.get("scheduleDate");
                    String dateB = (String) b.get("scheduleDate");
                    String timeA = (String) a.get("timeRange");
                    String timeB = (String) b.get("timeRange");
                    
                    // 先按日期比较（使用LocalDate进行正确比较）
                    LocalDate localDateA = LocalDate.parse(dateA);
                    LocalDate localDateB = LocalDate.parse(dateB);
                    
                    // 日期倒序：较新的日期排在前面
                    int dateCompare = localDateB.compareTo(localDateA);
                    if (dateCompare != 0) {
                        return dateCompare;
                    }
                    
                    // 日期相同时按时间比较
                    if (timeA != null && timeB != null && !timeA.isEmpty() && !timeB.isEmpty()) {
                        // 提取开始时间进行比较
                        String startTimeA = timeA.split("-")[0].trim();
                        String startTimeB = timeB.split("-")[0].trim();
                        LocalTime localTimeA = LocalTime.parse(startTimeA);
                        LocalTime localTimeB = LocalTime.parse(startTimeB);
                        
                        // 时间倒序：较晚的时间排在前面
                        return localTimeB.compareTo(localTimeA);
                    }
                    
                    return 0;
                } catch (Exception e) {
                    logger.warn("排序比较失败: dateA={}, dateB={}, timeA={}, timeB={}, error={}", 
                        a.get("scheduleDate"), b.get("scheduleDate"), 
                        a.get("timeRange"), b.get("timeRange"), e.getMessage());
                    return 0;
                }
            });
            
            // 5. 合并连续的课时记录（同一天、同一课表类型、时间连续的记录）
            schedules = mergeConsecutiveSchedules(schedules);
            
            // 6. 请假记录也按日期和时间倒序排列
            leaves.sort((a, b) -> {
                try {
                    String dateA = (String) a.get("leaveDate");
                    String dateB = (String) b.get("leaveDate");
                    String timeA = (String) a.get("timeRange");
                    String timeB = (String) b.get("timeRange");
                    
                    // 先按日期比较（使用LocalDate进行正确比较）
                    LocalDate localDateA = LocalDate.parse(dateA);
                    LocalDate localDateB = LocalDate.parse(dateB);
                    int dateCompare = localDateB.compareTo(localDateA);
                    if (dateCompare != 0) {
                        return dateCompare;
                    }
                    
                    // 日期相同时按时间比较
                    if (timeA != null && timeB != null) {
                        // 提取开始时间进行比较
                        String startTimeA = timeA.split("-")[0];
                        String startTimeB = timeB.split("-")[0];
                        LocalTime localTimeA = LocalTime.parse(startTimeA);
                        LocalTime localTimeB = LocalTime.parse(startTimeB);
                        return localTimeB.compareTo(localTimeA);
                    }
                    
                    return 0;
                } catch (Exception e) {
                    logger.warn("请假记录排序比较失败: {}", e.getMessage());
                    return 0;
                }
            });
            
        } catch (Exception e) {
            logger.error("获取学员记录失败，学员: {}, 教练: {}, 错误: {}", studentName, coachName, e.getMessage());
        }
        
        // 获取学员真正的教练信息
        String actualCoachName = "未知教练";
        String primaryStudentName = studentNamesToQuery.isEmpty() ? studentName : studentNamesToQuery.get(0);
        
        // 优先从上课记录中获取教练信息
        if (!schedules.isEmpty()) {
            actualCoachName = (String) schedules.get(0).get("coachName");
        } else if (!leaves.isEmpty()) {
            // 如果上课记录为空，从请假记录中获取
            actualCoachName = (String) leaves.get(0).get("coachName");
        } else {
            // 如果都没有记录，尝试从课表中查找该学员的教练
            try {
                // 从实例课表中查找（使用第一个学员名）
                List<WeeklyInstanceSchedule> instanceSchedules = weeklyInstanceScheduleRepository.findByStudentName(primaryStudentName);
                for (WeeklyInstanceSchedule schedule : instanceSchedules) {
                    WeeklyInstance instance = weeklyInstanceRepository.findById(schedule.getWeeklyInstanceId());
                    if (instance != null) {
                        Timetables timetable = timetableRepository.findById(instance.getTemplateTimetableId());
                        if (timetable != null) {
                            Users coach = userService.findById(timetable.getUserId());
                            if (coach != null) {
                                actualCoachName = coach.getNickname() != null ? coach.getNickname() : coach.getUsername();
                                break;
                            }
                        }
                    }
                }
                
                // 如果实例课表中没找到，从日期类课表中查找
                if ("未知教练".equals(actualCoachName)) {
                    List<Timetables> allTimetables = timetableRepository.findAll();
                    for (Timetables timetable : allTimetables) {
                        if (timetable.getIsDeleted() != null && timetable.getIsDeleted() == 1) {
                            continue;
                        }
                        if (timetable.getIsWeekly() != null && timetable.getIsWeekly() == 1) {
                            continue;
                        }
                        
                        List<Schedules> dateSchedules = scheduleRepository.findByTimetableIdAndStudentName(timetable.getId(), primaryStudentName);
                        if (!dateSchedules.isEmpty()) {
                            Users coach = userService.findById(timetable.getUserId());
                            if (coach != null) {
                                actualCoachName = coach.getNickname() != null ? coach.getNickname() : coach.getUsername();
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("获取学员教练信息失败，学员: {}, 错误: {}", studentName, e.getMessage());
            }
        }
        
        result.put("schedules", schedules);
        result.put("leaves", leaves);
        result.put("actualCoachName", actualCoachName);
        result.put("studentDisplayName", displayName);  // 重命名后的显示名称
        result.put("studentOriginalName", primaryStudentName);  // 原始名称
        result.put("mergedStudentNames", studentNamesToQuery);  // 所有被合并的学员名称
        
        logger.info("返回学员记录: 显示名={}, 查询的学员名={}, 上课记录数={}, 请假记录数={}", 
            displayName, studentNamesToQuery, schedules.size(), leaves.size());
        
        return result;
    }
    
    /**
     * 合并连续的课时记录
     * 如果多条记录是同一天、同一课表类型、时间连续，则合并为一条记录
     */
    private List<Map<String, Object>> mergeConsecutiveSchedules(List<Map<String, Object>> schedules) {
        if (schedules == null || schedules.size() <= 1) {
            return schedules;
        }
        
        // 先按日期和时间正序排序（用于合并）
        List<Map<String, Object>> sortedSchedules = new ArrayList<>(schedules);
        sortedSchedules.sort((a, b) -> {
            try {
                LocalDate dateA = LocalDate.parse((String) a.get("scheduleDate"));
                LocalDate dateB = LocalDate.parse((String) b.get("scheduleDate"));
                int dateCompare = dateA.compareTo(dateB);
                if (dateCompare != 0) return dateCompare;
                
                String timeA = (String) a.get("timeRange");
                String timeB = (String) b.get("timeRange");
                String startTimeA = timeA.split("-")[0].trim();
                String startTimeB = timeB.split("-")[0].trim();
                return LocalTime.parse(startTimeA).compareTo(LocalTime.parse(startTimeB));
            } catch (Exception e) {
                return 0;
            }
        });
        
        List<Map<String, Object>> mergedSchedules = new ArrayList<>();
        Map<String, Object> current = null;
        
        for (Map<String, Object> schedule : sortedSchedules) {
            if (current == null) {
                // 第一条记录，直接作为当前记录
                current = new HashMap<>(schedule);
            } else {
                // 检查是否可以与当前记录合并
                boolean canMerge = false;
                try {
                    // 同一天
                    if (current.get("scheduleDate").equals(schedule.get("scheduleDate"))) {
                        // 同一课表类型
                        if (current.get("timetableType").equals(schedule.get("timetableType"))) {
                            // 检查时间是否连续
                            String currentTimeRange = (String) current.get("timeRange");
                            String scheduleTimeRange = (String) schedule.get("timeRange");
                            
                            String currentEndTime = currentTimeRange.split("-")[1].trim();
                            String scheduleStartTime = scheduleTimeRange.split("-")[0].trim();
                            
                            // 如果当前记录的结束时间等于下一条记录的开始时间，则可以合并
                            if (currentEndTime.equals(scheduleStartTime)) {
                                canMerge = true;
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("检查记录是否可合并时失败: {}", e.getMessage());
                }
                
                if (canMerge) {
                    // 合并：更新当前记录的结束时间
                    String currentTimeRange = (String) current.get("timeRange");
                    String scheduleTimeRange = (String) schedule.get("timeRange");
                    String startTime = currentTimeRange.split("-")[0].trim();
                    String endTime = scheduleTimeRange.split("-")[1].trim();
                    current.put("timeRange", startTime + "-" + endTime);
                    logger.info("合并课时记录: 日期={}, 时间={}", current.get("scheduleDate"), current.get("timeRange"));
                } else {
                    // 不能合并，保存当前记录，开始新的记录
                    mergedSchedules.add(current);
                    current = new HashMap<>(schedule);
                }
            }
        }
        
        // 添加最后一条记录
        if (current != null) {
            mergedSchedules.add(current);
        }
        
        // 最后按日期和时间倒序排列（恢复原来的顺序）
        mergedSchedules.sort((a, b) -> {
            try {
                LocalDate dateA = LocalDate.parse((String) a.get("scheduleDate"));
                LocalDate dateB = LocalDate.parse((String) b.get("scheduleDate"));
                int dateCompare = dateB.compareTo(dateA);
                if (dateCompare != 0) return dateCompare;
                
                String timeA = (String) a.get("timeRange");
                String timeB = (String) b.get("timeRange");
                String startTimeA = timeA.split("-")[0].trim();
                String startTimeB = timeB.split("-")[0].trim();
                return LocalTime.parse(startTimeB).compareTo(LocalTime.parse(startTimeA));
            } catch (Exception e) {
                return 0;
            }
        });
        
        return mergedSchedules;
    }

    /**
     * 获取当前教练的所有学员列表
     */
    public List<String> getAllStudents(Long coachId) {
        Set<String> studentSet = new HashSet<>();
        
        try {
            // 1. 从实例课表中获取当前教练的学员
            List<WeeklyInstanceSchedule> instanceSchedules = weeklyInstanceScheduleRepository.findAll();
            for (WeeklyInstanceSchedule schedule : instanceSchedules) {
                if (schedule.getStudentName() != null && !schedule.getStudentName().trim().isEmpty()) {
                    // 获取该课程所属的课表
                    WeeklyInstance instance = weeklyInstanceRepository.findById(schedule.getWeeklyInstanceId());
                    if (instance != null) {
                        Timetables timetable = timetableRepository.findById(instance.getTemplateTimetableId());
                        if (timetable != null && timetable.getUserId().equals(coachId)) {
                            // 只处理未删除的课表
                            if (timetable.getIsDeleted() == null || timetable.getIsDeleted() == 0) {
                                studentSet.add(schedule.getStudentName().trim());
                            }
                        }
                    }
                }
            }
            
            // 2. 从日期类课表中获取当前教练的学员
            List<Timetables> coachTimetables = timetableRepository.findByUserId(coachId);
            for (Timetables timetable : coachTimetables) {
                // 只处理未删除的课表
                if (timetable.getIsDeleted() != null && timetable.getIsDeleted() == 1) {
                    continue;
                }
                
                // 只处理日期类课表（非周课表）
                if (timetable.getIsWeekly() != null && timetable.getIsWeekly() == 1) {
                    continue;
                }
                
                // 获取该课表中的所有课程安排
                List<Schedules> dateSchedules = scheduleRepository.findByTimetableId(timetable.getId());
                for (Schedules schedule : dateSchedules) {
                    if (schedule.getStudentName() != null && !schedule.getStudentName().trim().isEmpty()) {
                        studentSet.add(schedule.getStudentName().trim());
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("获取学员列表失败，错误: {}", e.getMessage());
        }
        
        // 转换为列表并排序
        List<String> students = new ArrayList<>(studentSet);
        students.sort(String::compareTo);
        
        return students;
    }

    /**
     * 获取所有课表中的学员列表
     */
    public List<String> getAllStudentsFromAllTimetables() {
        Set<String> studentSet = new HashSet<>();
        
        try {
            // 1. 从实例课表中获取所有学员
            List<WeeklyInstanceSchedule> instanceSchedules = weeklyInstanceScheduleRepository.findAll();
            for (WeeklyInstanceSchedule schedule : instanceSchedules) {
                if (schedule.getStudentName() != null && !schedule.getStudentName().trim().isEmpty()) {
                    // 获取该课程所属的课表
                    WeeklyInstance instance = weeklyInstanceRepository.findById(schedule.getWeeklyInstanceId());
                    if (instance != null) {
                        Timetables timetable = timetableRepository.findById(instance.getTemplateTimetableId());
                        if (timetable != null) {
                            // 只处理未删除的课表
                            if (timetable.getIsDeleted() == null || timetable.getIsDeleted() == 0) {
                                studentSet.add(schedule.getStudentName().trim());
                            }
                        }
                    }
                }
            }
            
            // 2. 从日期类课表中获取所有学员
            List<Timetables> allTimetables = timetableRepository.findAll();
            for (Timetables timetable : allTimetables) {
                // 只处理未删除的课表
                if (timetable.getIsDeleted() != null && timetable.getIsDeleted() == 1) {
                    continue;
                }
                
                // 只处理日期类课表（非周课表）
                if (timetable.getIsWeekly() != null && timetable.getIsWeekly() == 1) {
                    continue;
                }
                
                // 获取该课表中的所有课程安排
                List<Schedules> dateSchedules = scheduleRepository.findByTimetableId(timetable.getId());
                for (Schedules schedule : dateSchedules) {
                    if (schedule.getStudentName() != null && !schedule.getStudentName().trim().isEmpty()) {
                        studentSet.add(schedule.getStudentName().trim());
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("获取所有学员列表失败，错误: {}", e.getMessage());
        }
        
        // 转换为列表并排序
        List<String> students = new ArrayList<>(studentSet);
        students.sort(String::compareTo);
        
        return students;
    }

    /**
     * 统计某个教练下每个学员的已上课程数（不含请假），并按课程数倒序返回
     */
    public List<com.timetable.entity.StudentOperationRecord> getOperationRecordsByCoachId(Long coachId) {
        return studentOperationRecordRepository.findByCoachId(coachId);
    }
    
    
    public void saveOrUpdateRenameRule(com.timetable.entity.StudentOperationRecord record) {
        try {
            // 检查是否已存在相同的重命名规则
            com.timetable.entity.StudentOperationRecord existing = studentOperationRecordRepository.findByCoachIdAndOperationTypeAndOldName(
                record.getCoachId(), "RENAME", record.getOldName());
            
            if (existing != null) {
                // 更新现有规则
                existing.setNewName(record.getNewName());
                existing.setDetails(record.getDetails());
                existing.setUpdatedAt(java.time.LocalDateTime.now());
                studentOperationRecordRepository.update(existing);
                logger.info("更新重命名规则成功: {} -> {} (教练ID: {})", record.getOldName(), record.getNewName(), record.getCoachId());
            } else {
                // 创建新规则
                Long id = studentOperationRecordRepository.save(record);
                logger.info("创建重命名规则成功: {} -> {} (ID: {}, 教练ID: {})", record.getOldName(), record.getNewName(), id, record.getCoachId());
            }
        } catch (Exception e) {
            logger.error("保存重命名规则失败: {}", e.getMessage(), e);
            throw new RuntimeException("保存重命名规则失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据开始时间和结束时间计算课时数
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 课时数（以小时为单位，如0.5、1.0、1.5等）
     */
    private double calculateCourseHours(java.time.LocalTime startTime, java.time.LocalTime endTime) {
        if (startTime == null || endTime == null) {
            return 1.0; // 如果时间为空，默认算1课时
        }
        
        try {
            // 计算时间差（分钟）
            long minutes = java.time.Duration.between(startTime, endTime).toMinutes();
            // 转换为小时（保留一位小数）
            double hours = minutes / 60.0;
            // 最小0.5小时
            return Math.max(0.5, hours);
        } catch (Exception e) {
            logger.warn("计算课时失败: startTime={}, endTime={}, error={}", startTime, endTime, e.getMessage());
            return 1.0;
        }
    }
    
    public List<StudentSummaryDTO> getStudentSummariesByCoach(Long coachId) {
        logger.info("开始获取教练 {} 的学员列表", coachId);
        Map<String, Double> studentToCount = new HashMap<>();

        // 获取该教练的所有学员操作规则
        Map<String, String> renameRules = new HashMap<>();
        Set<String> hiddenStudents = new HashSet<>();
        Map<String, String> aliasRules = new HashMap<>();
        
        try {
            // 强制刷新缓存，获取最新数据
            List<StudentOperationRecord> operationRecords = studentOperationRecordRepository.findByCoachId(coachId);
            operationRecords.sort(java.util.Comparator.comparing(StudentOperationRecord::getCreatedAt));
            logger.info("教练 {} 共有 {} 条操作记录", coachId, operationRecords.size());
            
            // 详细打印每条操作记录
            for (StudentOperationRecord record : operationRecords) {
                logger.info("操作记录详情: ID={}, 教练ID={}, 操作类型={}, 原名={}, 新名={}",
                    record.getId(), record.getCoachId(), record.getOperationType(),
                    record.getOldName(), record.getNewName());
            }
            
            // 打印所有重命名规则以便调试
            logger.info("所有重命名规则:");
            operationRecords.stream()
                .filter(r -> "RENAME".equals(r.getOperationType()))
                .forEach(r -> logger.info("  {} -> {}", r.getOldName(), r.getNewName()));
            
            // 处理操作记录，构建规则映射
            for (StudentOperationRecord record : operationRecords) {
                String operationType = record.getOperationType();
                String oldName = record.getOldName();
                String newName = record.getNewName();
                
                logger.info("操作记录: 类型={}, 原名={}, 新名={}", operationType, oldName, newName);
                
                switch (operationType) {
                    case "RENAME":
                        if (newName != null && !newName.trim().isEmpty()) {
                            // 确保oldName也被trim，保持一致性
                            String trimmedOldName = oldName != null ? oldName.trim() : oldName;
                            String trimmedNewName = newName.trim();
                            renameRules.put(trimmedOldName, trimmedNewName);
                            logger.info("添加重命名规则: '{}' -> '{}'", trimmedOldName, trimmedNewName);
                        }
                        break;
                    case "DELETE":
                        hiddenStudents.add(oldName);
                        logger.info("添加隐藏规则: {}", oldName);
                        break;
                    case "ASSIGN_ALIAS":
                        if (newName != null && !newName.trim().isEmpty()) {
                            aliasRules.put(oldName, newName);
                            logger.info("添加别名规则: {} -> {}", oldName, newName);
                        }
                        break;
                    case "MERGE":
                        // 合并操作暂时不在这里处理，因为需要更复杂的逻辑
                        break;
                }
            }
            
            logger.info("最终规则: 重命名={}, 隐藏={}, 别名={}", renameRules, hiddenStudents, aliasRules);

            // 1) 周实例课程：只统计该教练课表下，且 scheduleDate 不在未来，且未请假的课程
            List<WeeklyInstanceSchedule> instanceSchedules = weeklyInstanceScheduleRepository.findAll();
            LocalDate today = LocalDate.now();
            LocalTime now = LocalTime.now();
            for (WeeklyInstanceSchedule s : instanceSchedules) {
                if (s.getStudentName() == null || s.getStudentName().trim().isEmpty()) continue;
                if (s.getScheduleDate() == null) continue;
                if (s.getScheduleDate().isAfter(today)) continue; // 未来的不算已上
                
                // 如果是今天的课程，需要检查时间是否已经过去
                if (s.getScheduleDate().isEqual(today)) {
                    if (s.getEndTime() == null) continue; // 没有结束时间则不计入
                    if (s.getEndTime().isAfter(now)) continue; // 结束时间还未到，不算已上
                }
                
                if (s.getIsOnLeave() != null && s.getIsOnLeave()) continue; // 请假不计入
                if (s.getIsCancelled() != null && s.getIsCancelled()) continue; // 已取消不计入

                WeeklyInstance instance = weeklyInstanceRepository.findById(s.getWeeklyInstanceId());
                if (instance == null) continue;
                Timetables timetable = timetableRepository.findById(instance.getTemplateTimetableId());
                if (timetable == null) continue;
                if (!Objects.equals(timetable.getUserId(), coachId)) continue;
                if (timetable.getIsDeleted() != null && timetable.getIsDeleted() == 1) continue;

                String name = s.getStudentName().trim();
                // 过滤掉隐藏的学员
                if (hiddenStudents.contains(name)) continue;
                
                // 根据课程时长计算课时数
                double hours = calculateCourseHours(s.getStartTime(), s.getEndTime());
                studentToCount.put(name, studentToCount.getOrDefault(name, 0.0) + hours);
            }

            // 2) 日期类课表：只统计该教练的日期课表，scheduleDate 不在未来
            List<Timetables> coachTimetables = timetableRepository.findByUserId(coachId);
            for (Timetables timetable : coachTimetables) {
                if (timetable.getIsDeleted() != null && timetable.getIsDeleted() == 1) continue;
                if (timetable.getIsWeekly() != null && timetable.getIsWeekly() == 1) continue; // 只看日期类

                List<com.timetable.generated.tables.pojos.Schedules> dateSchedules = scheduleRepository.findByTimetableId(timetable.getId());
                for (com.timetable.generated.tables.pojos.Schedules s : dateSchedules) {
                    if (s.getStudentName() == null || s.getStudentName().trim().isEmpty()) continue;
                    if (s.getScheduleDate() == null) continue;
                    if (s.getScheduleDate().isAfter(today)) continue;
                    
                    // 如果是今天的课程，需要检查时间是否已经过去
                    if (s.getScheduleDate().isEqual(today)) {
                        if (s.getEndTime() == null) continue; // 没有结束时间则不计入
                        if (s.getEndTime().isAfter(now)) continue; // 结束时间还未到，不算已上
                    }
                    
                    String name = s.getStudentName().trim();
                    // 过滤掉隐藏的学员
                    if (hiddenStudents.contains(name)) continue;
                    
                    // 根据课程时长计算课时数
                    double hours = calculateCourseHours(s.getStartTime(), s.getEndTime());
                    studentToCount.put(name, studentToCount.getOrDefault(name, 0.0) + hours);
                }
            }
        } catch (Exception e) {
            logger.error("统计学员课程数失败(教练): {}", e.getMessage());
        }

        // 聚合最终结果，处理重命名链和别名
        Map<String, Double> finalStudentToCount = new HashMap<>();
        logger.info("开始处理重命名规则，学员数量: {}", studentToCount.size());
        logger.info("原始学员列表: {}", studentToCount.keySet());
        logger.info("重命名规则映射: {}", renameRules);
        
        for (Map.Entry<String, Double> entry : studentToCount.entrySet()) {
            String originalName = entry.getKey();
            Double count = entry.getValue();

            // 解析重命名链
            String currentName = originalName;
            Set<String> visited = new HashSet<>();
            logger.info("处理学员: '{}', 课时数: {}", originalName, count);
            
            while (renameRules.containsKey(currentName) && visited.add(currentName)) {
                String nextName = renameRules.get(currentName);
                logger.info("  应用重命名规则: '{}' -> '{}'", currentName, nextName);
                currentName = nextName;
            }

            // 应用别名
            String displayName = aliasRules.getOrDefault(currentName, currentName);
            if (!displayName.equals(currentName)) {
                logger.info("  应用别名规则: '{}' -> '{}'", currentName, displayName);
            }
            
            logger.info("  最终显示名称: '{}'", displayName);
            finalStudentToCount.put(displayName, finalStudentToCount.getOrDefault(displayName, 0.0) + count);
        }

        // 转为 DTO 并排序（将Double转为Integer）
        List<StudentSummaryDTO> list = finalStudentToCount.entrySet().stream()
                .map(e -> new StudentSummaryDTO(null, coachId, e.getKey(), (int) Math.round(e.getValue() * 2)))
                .sorted((a, b) -> b.getAttendedCount().compareTo(a.getAttendedCount()))
                .collect(Collectors.toList());
        
        logger.info("最终学员列表: {}", list.stream().map(StudentSummaryDTO::getStudentName).collect(Collectors.toList()));
        return list;
    }

    /**
     * 统计所有教练范围内每个学员的已上课程数（不含请假），并倒序
     */
    public List<StudentSummaryDTO> getStudentSummariesAll() {
        Map<String, Double> studentToCount = new HashMap<>();
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        
        // 获取所有教练的学员操作规则
        Map<String, String> renameRules = new HashMap<>();
        Set<String> hiddenStudents = new HashSet<>();
        Map<String, String> aliasRules = new HashMap<>();
        
        try {
            // 获取所有教练的操作规则
            List<Users> allCoaches = userService.getAllApprovedUsers();
            for (Users coach : allCoaches) {
                List<StudentOperationRecord> operationRecords = studentOperationRecordRepository.findByCoachId(coach.getId());
                
                // 处理操作记录，构建规则映射
                for (StudentOperationRecord record : operationRecords) {
                    String operationType = record.getOperationType();
                    String oldName = record.getOldName();
                    String newName = record.getNewName();
                    
                    switch (operationType) {
                        case "RENAME":
                            if (newName != null && !newName.trim().isEmpty()) {
                                renameRules.put(oldName, newName);
                            }
                            break;
                        case "DELETE":
                            hiddenStudents.add(oldName);
                            break;
                        case "ASSIGN_ALIAS":
                            if (newName != null && !newName.trim().isEmpty()) {
                                aliasRules.put(oldName, newName);
                            }
                            break;
                        case "MERGE":
                            // 合并操作暂时不在这里处理，因为需要更复杂的逻辑
                            break;
                    }
                }
            }
            
            // 周实例（所有课表）
            List<WeeklyInstanceSchedule> instanceSchedules = weeklyInstanceScheduleRepository.findAll();
            for (WeeklyInstanceSchedule s : instanceSchedules) {
                if (s.getStudentName() == null || s.getStudentName().trim().isEmpty()) continue;
                if (s.getScheduleDate() == null) continue;
                if (s.getScheduleDate().isAfter(today)) continue;
                
                // 如果是今天的课程，需要检查时间是否已经过去
                if (s.getScheduleDate().isEqual(today)) {
                    if (s.getEndTime() == null) continue; // 没有结束时间则不计入
                    if (s.getEndTime().isAfter(now)) continue; // 结束时间还未到，不算已上
                }
                
                if (s.getIsOnLeave() != null && s.getIsOnLeave()) continue;
                String name = s.getStudentName().trim();
                // 过滤掉隐藏的学员
                if (hiddenStudents.contains(name)) continue;
                
                // 根据课程时长计算课时数
                double hours = calculateCourseHours(s.getStartTime(), s.getEndTime());
                studentToCount.put(name, studentToCount.getOrDefault(name, 0.0) + hours);
            }

            // 日期类（所有课表）
            List<Timetables> all = timetableRepository.findAll();
            for (Timetables timetable : all) {
                if (timetable.getIsDeleted() != null && timetable.getIsDeleted() == 1) continue;
                if (timetable.getIsWeekly() != null && timetable.getIsWeekly() == 1) continue;
                List<com.timetable.generated.tables.pojos.Schedules> dateSchedules = scheduleRepository.findByTimetableId(timetable.getId());
                for (com.timetable.generated.tables.pojos.Schedules s : dateSchedules) {
                    if (s.getStudentName() == null || s.getStudentName().trim().isEmpty()) continue;
                    if (s.getScheduleDate() == null) continue;
                    if (s.getScheduleDate().isAfter(today)) continue;
                    
                    // 如果是今天的课程，需要检查时间是否已经过去
                    if (s.getScheduleDate().isEqual(today)) {
                        if (s.getEndTime() == null) continue; // 没有结束时间则不计入
                        if (s.getEndTime().isAfter(now)) continue; // 结束时间还未到，不算已上
                    }
                    
                    String name = s.getStudentName().trim();
                    // 过滤掉隐藏的学员
                    if (hiddenStudents.contains(name)) continue;
                    
                    // 根据课程时长计算课时数
                    double hours = calculateCourseHours(s.getStartTime(), s.getEndTime());
                    studentToCount.put(name, studentToCount.getOrDefault(name, 0.0) + hours);
                }
            }
        } catch (Exception e) {
            logger.error("统计学员课程数失败(全部): {}", e.getMessage());
        }
        
        // 转为 DTO 并排序，应用重命名和别名规则（将Double转为Integer，乘以2表示半小时为单位）
        List<StudentSummaryDTO> list = studentToCount.entrySet().stream()
                .map(e -> {
                    String originalName = e.getKey();
                    // 应用重命名规则（支持重命名链）
                    String displayName = originalName;
                    Set<String> visited = new HashSet<>();
                    while (renameRules.containsKey(displayName) && visited.add(displayName)) {
                        displayName = renameRules.get(displayName);
                    }
                    // 如果没有重命名但有别名，使用别名
                    if (!visited.contains(originalName) && aliasRules.containsKey(displayName)) {
                        displayName = aliasRules.get(displayName);
                    }
                    // 将Double转为Integer，乘以2表示半小时为单位（0.5小时=1课时，1小时=2课时）
                    return new StudentSummaryDTO(null, null, displayName, (int) Math.round(e.getValue() * 2));
                })
                .sorted((a, b) -> b.getAttendedCount().compareTo(a.getAttendedCount()))
                .collect(Collectors.toList());
        return list;
    }

    /**
     * 统计所有学员，按教练分组返回（含教练id/名/组课时总数/分组学员列表）
     * @param organizationId 机构ID，只返回该机构的教练和学员
     * @return List<CoachStudentSummaryDTO>
     */
    public List<com.timetable.dto.CoachStudentSummaryDTO> getStudentGroupByCoachSummaryAll(Long organizationId) {
        Map<Long, String> coachNameMap = new HashMap<>();
        Map<Long, List<com.timetable.dto.StudentSummaryDTO>> coachStudents = new HashMap<>();
        Map<Long, Double> coachTotal = new HashMap<>();
        java.time.LocalDate today = java.time.LocalDate.now();
        Set<Long> deletedCoachIds = new HashSet<>();
        
        // 获取所有教练的合并和别名设置
        Map<Long, List<com.timetable.dto.StudentMergeDTO>> coachMerges = new HashMap<>();
        Map<Long, List<com.timetable.dto.StudentAliasDTO>> coachAliases = new HashMap<>();
        
        // 获取所有教练的学员操作规则
        Map<Long, Map<String, String>> coachRenameRules = new HashMap<>();
        Map<Long, Set<String>> coachHiddenStudents = new HashMap<>();
        
        // 创建该机构的所有有效教练ID集合，用于过滤课程数据
        Set<Long> validCoachIds = new HashSet<>();
        
        try {
            // 预加载指定机构的所有教练的合并和别名设置
            List<com.timetable.generated.tables.pojos.Users> allCoaches = userService.getUsersByOrganizationId(organizationId)
                    .stream()
                    .filter(u -> "APPROVED".equals(u.getStatus()))
                    .filter(u -> u.getIsDeleted() == null || u.getIsDeleted() == 0)
                    .collect(java.util.stream.Collectors.toList());
            for (com.timetable.generated.tables.pojos.Users coach : allCoaches) {
                if (coach.getIsDeleted() == null || coach.getIsDeleted() == 0) {
                    // 将该教练ID添加到有效教练ID集合
                    validCoachIds.add(coach.getId());
                    
                    coachMerges.put(coach.getId(), studentMergeService.getMergesByCoach(coach.getId()));
                    coachAliases.put(coach.getId(), studentAliasService.getAliasesByCoach(coach.getId()));
                    
                    // 获取学员操作规则
                    List<StudentOperationRecord> operationRecords = studentOperationRecordRepository.findByCoachId(coach.getId());
                    operationRecords.sort(java.util.Comparator.comparing(StudentOperationRecord::getCreatedAt));
                    Map<String, String> renameRules = new HashMap<>();
                    Set<String> hiddenStudents = new HashSet<>();
                    
                    logger.info("getStudentGroupByCoachSummaryAll - 教练 {} ({}) 共有 {} 条操作记录", 
                        coach.getId(), coach.getNickname() != null ? coach.getNickname() : coach.getUsername(), operationRecords.size());
                    
                    // 如果是杨教练，输出详细信息
                    if (coach.getId().equals(6L)) {
                        logger.info("DEBUG: 杨教练的所有操作记录:");
                        for (StudentOperationRecord record : operationRecords) {
                            logger.info("  记录ID: {}, 操作类型: {}, 原名: {}, 新名: {}, 创建时间: {}", 
                                record.getId(), record.getOperationType(), record.getOldName(), record.getNewName(), record.getCreatedAt());
                        }
                    }
                    
                    for (StudentOperationRecord record : operationRecords) {
                        String operationType = record.getOperationType();
                        String oldName = record.getOldName();
                        String newName = record.getNewName();
                        
                        logger.info("getStudentGroupByCoachSummaryAll - 操作记录: 教练ID={}, 类型={}, 原名={}, 新名={}",
                            coach.getId(), operationType, oldName, newName);
                        
                        switch (operationType) {
                            case "RENAME":
                                if (newName != null && !newName.trim().isEmpty()) {
                                    // 确保oldName也被trim，保持一致性
                                    String trimmedOldName = oldName != null ? oldName.trim() : oldName;
                                    String trimmedNewName = newName.trim();
                                    renameRules.put(trimmedOldName, trimmedNewName);
                                    logger.info("getStudentGroupByCoachSummaryAll - 添加重命名规则: '{}' -> '{}' (教练ID: {})", trimmedOldName, trimmedNewName, coach.getId());
                                }
                                break;
                            case "DELETE":
                                hiddenStudents.add(oldName);
                                logger.info("添加隐藏规则: {}", oldName);
                                break;
                        }
                    }
                    
                    logger.info("教练 {} 最终重命名规则: {}", coach.getId(), renameRules);
                    
                    
                    coachRenameRules.put(coach.getId(), renameRules);
                    coachHiddenStudents.put(coach.getId(), hiddenStudents);
                    
                    // 调试信息：打印加载的重命名规则
                    System.out.println("*** 教练 " + coach.getId() + " (" + coach.getUsername() + ") 的重命名规则: " + renameRules);
                }
            }
            // 实例课表
            java.util.List<com.timetable.entity.WeeklyInstanceSchedule> instanceSchedules = weeklyInstanceScheduleRepository.findAll();
            java.time.LocalTime now = java.time.LocalTime.now();
            for (com.timetable.entity.WeeklyInstanceSchedule s : instanceSchedules) {
                if (s.getStudentName() == null || s.getStudentName().trim().isEmpty()) continue;
                if (s.getScheduleDate() == null) continue;
                if (s.getScheduleDate().isAfter(today)) continue;
                
                // 如果是今天的课程，需要检查时间是否已经过去
                if (s.getScheduleDate().isEqual(today)) {
                    if (s.getEndTime() == null) continue; // 没有结束时间则不计入
                    if (s.getEndTime().isAfter(now)) continue; // 结束时间还未到，不算已上
                }
                
                if (s.getIsOnLeave() != null && s.getIsOnLeave()) continue; // 请假不计入
                if (s.getIsCancelled() != null && s.getIsCancelled()) continue; // 已取消不计入
                com.timetable.entity.WeeklyInstance instance = weeklyInstanceRepository.findById(s.getWeeklyInstanceId());
                if (instance == null) continue;
                com.timetable.generated.tables.pojos.Timetables timetable = timetableRepository.findById(instance.getTemplateTimetableId());
                if (timetable == null) continue;
                if (timetable.getIsDeleted() != null && timetable.getIsDeleted() == 1) continue;
                Long coachId = timetable.getUserId();
                if (coachId == null) continue;
                
                // 只处理当前机构的教练的课程
                if (!validCoachIds.contains(coachId)) continue;
                if (!coachNameMap.containsKey(coachId)) {
                    com.timetable.generated.tables.pojos.Users coach = userService.findById(coachId);
                    // 过滤掉已删除教练
                    if (coach != null && coach.getIsDeleted() != null && coach.getIsDeleted() == 1) {
                      deletedCoachIds.add(coachId);
                      continue;
                    }
                    coachNameMap.put(coachId, coach != null ? (coach.getNickname() != null ? coach.getNickname() : coach.getUsername()) : "未知教练");
                }
                if (deletedCoachIds.contains(coachId)) continue;
                String studentName = s.getStudentName().trim();
                
                // 检查是否是隐藏的学员
                Set<String> hiddenStudents = coachHiddenStudents.get(coachId);
                if (hiddenStudents != null && hiddenStudents.contains(studentName)) {
                    logger.debug("学员 {} 被隐藏，跳过", studentName);
                    continue;
                }
                
                // 处理学员合并和别名，同时应用重命名规则
                String displayName = getDisplayStudentName(studentName, coachId, coachMerges, coachAliases, coachRenameRules);
                
                logger.info("getStudentGroupByCoachSummaryAll - 学员名称处理: 原始={}, 显示={}, 教练ID={}", studentName, displayName, coachId);
                if (!studentName.equals(displayName)) {
                    logger.info("getStudentGroupByCoachSummaryAll - 学员名称被重命名: {} -> {} (教练ID: {})", studentName, displayName, coachId);
                    System.out.println("*** 重命名生效 *** " + studentName + " -> " + displayName + " (教练ID: " + coachId + ")");
                } else if ("跃跃".equals(studentName) && coachId != null && coachId == 6L) {
                    System.out.println("*** 重命名未生效 *** " + studentName + " (教练ID: " + coachId + ") 规则: " + coachRenameRules.get(coachId));
                }
                List<String> relatedStudents = getRelatedStudents(studentName, coachId, coachMerges, coachAliases);
                
                // 根据课程时长计算课时数
                double hours = calculateCourseHours(s.getStartTime(), s.getEndTime());
                int courseCount = (int) Math.round(hours * 2); // 乘以2表示半小时为单位
                
                List<com.timetable.dto.StudentSummaryDTO> list = coachStudents.computeIfAbsent(coachId, k -> new java.util.ArrayList<>());
                com.timetable.dto.StudentSummaryDTO found = list.stream().filter(dto -> dto.getStudentName().equals(displayName)).findFirst().orElse(null);
                if (found == null) {
                    found = new com.timetable.dto.StudentSummaryDTO(null, coachId, displayName, courseCount);
                    list.add(found);
                } else {
                    found.setAttendedCount(found.getAttendedCount() + courseCount);
                }
                coachTotal.put(coachId, coachTotal.getOrDefault(coachId, 0.0) + hours);
            }
            // 日期类课表
            java.util.List<com.timetable.generated.tables.pojos.Timetables> all = timetableRepository.findAll();
            for (com.timetable.generated.tables.pojos.Timetables timetable : all) {
                if (timetable.getIsDeleted() != null && timetable.getIsDeleted() == 1) continue;
                if (timetable.getIsWeekly() != null && timetable.getIsWeekly() == 1) continue;
                Long coachId = timetable.getUserId();
                if (coachId == null) continue;
                
                // 只处理当前机构的教练的课程
                if (!validCoachIds.contains(coachId)) continue;
                if (!coachNameMap.containsKey(coachId)) {
                    com.timetable.generated.tables.pojos.Users coach = userService.findById(coachId);
                    if (coach != null && coach.getIsDeleted() != null && coach.getIsDeleted() == 1) {
                      deletedCoachIds.add(coachId);
                      continue;
                    }
                    coachNameMap.put(coachId, coach != null ? (coach.getNickname() != null ? coach.getNickname() : coach.getUsername()) : "未知教练");
                }
                if (deletedCoachIds.contains(coachId)) continue;
                java.util.List<com.timetable.generated.tables.pojos.Schedules> dateSchedules = scheduleRepository.findByTimetableId(timetable.getId());
                for (com.timetable.generated.tables.pojos.Schedules s : dateSchedules) {
                    if (s.getStudentName() == null || s.getStudentName().trim().isEmpty()) continue;
                    if (s.getScheduleDate() == null) continue;
                    if (s.getScheduleDate().isAfter(today)) continue;
                    
                    // 如果是今天的课程，需要检查时间是否已经过去
                    if (s.getScheduleDate().isEqual(today)) {
                        if (s.getEndTime() == null) continue; // 没有结束时间则不计入
                        if (s.getEndTime().isAfter(now)) continue; // 结束时间还未到，不算已上
                    }
                    String studentName = s.getStudentName().trim();
                    
                    // 检查是否是隐藏的学员
                    Set<String> hiddenStudents = coachHiddenStudents.get(coachId);
                    if (hiddenStudents != null && hiddenStudents.contains(studentName)) continue;
                    
                    // 处理学员合并和别名，同时应用重命名规则
                    String displayName = getDisplayStudentName(studentName, coachId, coachMerges, coachAliases, coachRenameRules);
                    
                    // 根据课程时长计算课时数
                    double hours = calculateCourseHours(s.getStartTime(), s.getEndTime());
                    int courseCount = (int) Math.round(hours * 2); // 乘以2表示半小时为单位
                    
                    List<com.timetable.dto.StudentSummaryDTO> list = coachStudents.computeIfAbsent(coachId, k -> new java.util.ArrayList<>());
                    com.timetable.dto.StudentSummaryDTO found = list.stream().filter(dto -> dto.getStudentName().equals(displayName)).findFirst().orElse(null);
                    if (found == null) {
                        found = new com.timetable.dto.StudentSummaryDTO(null, coachId, displayName, courseCount);
                        list.add(found);
                    } else {
                        found.setAttendedCount(found.getAttendedCount() + courseCount);
                    }
                    coachTotal.put(coachId, coachTotal.getOrDefault(coachId, 0.0) + hours);
                }
            }
        } catch (Exception e) {
            logger.error("统计分组学员课程数失败:", e);
        }
        List<com.timetable.dto.CoachStudentSummaryDTO> result = new ArrayList<>();
        coachStudents.forEach((coachId, stuList) -> {
            // 只添加有有效教练名称和学员的数据
            String coachName = coachNameMap.get(coachId);
            if (coachName != null && !coachName.trim().isEmpty() && !stuList.isEmpty()) {
                stuList.sort((a, b) -> b.getAttendedCount().compareTo(a.getAttendedCount()));
                // 将Double转为Integer，乘以2表示半小时为单位
                result.add(new com.timetable.dto.CoachStudentSummaryDTO(coachId, coachName, (int) Math.round(coachTotal.getOrDefault(coachId, 0.0) * 2), stuList));
            } else {
                logger.warn("跳过无效教练数据: coachId={}, coachName='{}', 学员数量={}", coachId, coachName, stuList.size());
            }
        });
        result.sort((a, b) -> b.getTotalCount().compareTo(a.getTotalCount()));
        return result;
    }
    
    /**
     * 获取学员的显示名称（考虑合并和别名）
     */
    private String getDisplayStudentName(String studentName, Long coachId,
            Map<Long, List<com.timetable.dto.StudentMergeDTO>> coachMerges,
            Map<Long, List<com.timetable.dto.StudentAliasDTO>> coachAliases,
            Map<Long, Map<String, String>> coachRenameRules) {
        
                logger.info("getDisplayStudentName - 处理学员名称: 原始名称={}, 教练ID={}", studentName, coachId);
        
        // 首先应用重命名规则
        Map<String, String> renameRules = coachRenameRules.get(coachId);
        String processedName = studentName != null ? studentName.trim() : studentName; // 确保处理名称时去除空格
        System.out.println("*** getDisplayStudentName DEBUG *** 教练ID: " + coachId + ", 原始名称: '" + studentName + "', 处理后名称: '" + processedName + "'");
        
        if (renameRules != null) {
            System.out.println("*** getDisplayStudentName DEBUG *** 教练 " + coachId + " 的重命名规则: " + renameRules);
            logger.info("getDisplayStudentName - 教练 {} 的重命名规则: {}", coachId, renameRules);
            Set<String> visited = new HashSet<>();
            while (renameRules.containsKey(processedName) && visited.add(processedName)) {
                String oldName = processedName;
                processedName = renameRules.get(processedName);
                System.out.println("*** getDisplayStudentName DEBUG *** 重命名规则匹配: '" + oldName + "' -> '" + processedName + "'");
                logger.info("getDisplayStudentName - 应用重命名规则: {} -> {}", oldName, processedName);
            }
            // 检查是否有匹配但没有应用的情况
            if (renameRules.containsKey(processedName)) {
                System.out.println("*** getDisplayStudentName DEBUG *** 规则中找到名称但没有应用，可能是循环依赖");
            }
        } else {
            System.out.println("*** getDisplayStudentName DEBUG *** 教练 " + coachId + " 没有重命名规则");
            logger.info("getDisplayStudentName - 教练 {} 没有重命名规则", coachId);
        }
        
        // 检查是否在合并设置中
        List<com.timetable.dto.StudentMergeDTO> merges = coachMerges.get(coachId);
        if (merges != null) {
            for (com.timetable.dto.StudentMergeDTO merge : merges) {
                if (merge.getStudentNames().contains(processedName)) {
                    logger.debug("学员 {} 在合并设置中，显示名称: {}", processedName, merge.getDisplayName());
                    return merge.getDisplayName();
                }
            }
        }
        
        // 检查是否在别名设置中
        List<com.timetable.dto.StudentAliasDTO> aliases = coachAliases.get(coachId);
        if (aliases != null) {
            for (com.timetable.dto.StudentAliasDTO alias : aliases) {
                if (alias.getStudentNames().contains(processedName)) {
                    logger.debug("学员 {} 在别名设置中，别名: {}", processedName, alias.getAliasName());
                    return alias.getAliasName();
                }
            }
        }
        
        logger.info("getDisplayStudentName - 学员 {} 最终显示名称: {}", studentName, processedName);
        return processedName; // 返回处理后的名称（可能已被重命名）
    }
    
    /**
     * 获取学员关联的所有学员名称
     */
    private List<String> getRelatedStudents(String studentName, Long coachId,
            Map<Long, List<com.timetable.dto.StudentMergeDTO>> coachMerges,
            Map<Long, List<com.timetable.dto.StudentAliasDTO>> coachAliases) {
        
        // 检查合并设置
        List<com.timetable.dto.StudentMergeDTO> merges = coachMerges.get(coachId);
        if (merges != null) {
            for (com.timetable.dto.StudentMergeDTO merge : merges) {
                if (merge.getStudentNames().contains(studentName)) {
                    return merge.getStudentNames();
                }
            }
        }
        
        // 检查别名设置
        List<com.timetable.dto.StudentAliasDTO> aliases = coachAliases.get(coachId);
        if (aliases != null) {
            for (com.timetable.dto.StudentAliasDTO alias : aliases) {
                if (alias.getStudentNames().contains(studentName)) {
                    return alias.getStudentNames();
                }
            }
        }
        
        return java.util.Arrays.asList(studentName); // 默认只包含自己
    }
    
    /**
     * 测试方法：检查指定教练的重命名规则
     */
    public Map<String, Object> testRenameRules(Long coachId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 获取该教练的操作记录
            List<StudentOperationRecord> operationRecords = studentOperationRecordRepository.findByCoachId(coachId);
            operationRecords.sort(java.util.Comparator.comparing(StudentOperationRecord::getCreatedAt));
            
            logger.info("测试方法 - 教练 {} 共有 {} 条操作记录", coachId, operationRecords.size());
            
            Map<String, String> renameRules = new HashMap<>();
            Set<String> hiddenStudents = new HashSet<>();
            
            for (StudentOperationRecord record : operationRecords) {
                String operationType = record.getOperationType();
                String oldName = record.getOldName();
                String newName = record.getNewName();
                
                logger.info("测试方法 - 操作记录: 类型={}, 原名={}, 新名={}", operationType, oldName, newName);
                
                switch (operationType) {
                    case "RENAME":
                        if (newName != null && !newName.trim().isEmpty()) {
                            renameRules.put(oldName, newName);
                            logger.info("测试方法 - 添加重命名规则: {} -> {}", oldName, newName);
                        }
                        break;
                    case "DELETE":
                        hiddenStudents.add(oldName);
                        logger.info("测试方法 - 添加隐藏规则: {}", oldName);
                        break;
                }
            }
            
            result.put("coachId", coachId);
            result.put("totalRecords", operationRecords.size());
            result.put("renameRules", renameRules);
            result.put("hiddenStudents", hiddenStudents);
            
            // 测试特定名称的重命名
            String testName = "跃跃";
            String processedName = testName;
            if (renameRules.containsKey(testName)) {
                processedName = renameRules.get(testName);
                result.put("testResult", testName + " -> " + processedName);
            } else {
                result.put("testResult", testName + " 没有重命名规则");
            }
            
        } catch (Exception e) {
            logger.error("测试重命名规则失败", e);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    public void saveOrUpdateHideRule(StudentOperationRecord record) {
        studentOperationRecordRepository.save(record);
    }
    
    public void saveOrUpdateMergeRule(StudentOperationRecord record) {
        studentOperationRecordRepository.save(record);
    }
    
    /**
     * 根据学员名称查找其所属的教练ID
     */
    public Long findCoachIdByStudentName(String studentName) {
        try {
            // 1. 先从周实例课程中查找
            List<WeeklyInstanceSchedule> schedules = weeklyInstanceScheduleRepository.findAll();
            for (WeeklyInstanceSchedule schedule : schedules) {
                if (studentName.equals(schedule.getStudentName())) {
                    WeeklyInstance instance = weeklyInstanceRepository.findById(schedule.getWeeklyInstanceId());
                    if (instance != null) {
                        Timetables timetable = timetableRepository.findById(instance.getTemplateTimetableId());
                        if (timetable != null) {
                            logger.info("从周实例课程找到学员 '{}' 属于教练ID: {}", studentName, timetable.getUserId());
                            return timetable.getUserId();
                        }
                    }
                }
            }
            
            // 2. 如果周实例中没找到，从日期类课表中查找
            List<Timetables> allTimetables = timetableRepository.findAll();
            for (Timetables timetable : allTimetables) {
                if (timetable.getIsDeleted() != null && timetable.getIsDeleted() == 1) continue;
                if (timetable.getIsWeekly() != null && timetable.getIsWeekly() == 1) continue;
                
                List<com.timetable.generated.tables.pojos.Schedules> schedulesList = scheduleRepository.findByTimetableId(timetable.getId());
                for (com.timetable.generated.tables.pojos.Schedules s : schedulesList) {
                    if (studentName.equals(s.getStudentName())) {
                        logger.info("从日期课表找到学员 '{}' 属于教练ID: {}", studentName, timetable.getUserId());
                        return timetable.getUserId();
                    }
                }
            }
            
            logger.warn("未找到学员 '{}' 所属的教练", studentName);
            return null;
        } catch (Exception e) {
            logger.error("查找学员所属教练失败: {}", e.getMessage(), e);
            return null;
        }
    }
}
