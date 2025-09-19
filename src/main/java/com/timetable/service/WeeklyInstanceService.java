package com.timetable.service;

import com.timetable.entity.WeeklyInstance;
import com.timetable.entity.WeeklyInstanceSchedule;
import com.timetable.dto.WeeklyInstanceDTO;
import com.timetable.repository.WeeklyInstanceRepository;
import com.timetable.repository.WeeklyInstanceScheduleRepository;
import com.timetable.repository.TimetableRepository;
import com.timetable.repository.ScheduleRepository;
import com.timetable.generated.tables.pojos.Timetables;
import com.timetable.generated.tables.pojos.Schedules;

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
import java.util.List;
import java.util.Map;
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
     * 为指定模板课表生成“下周”的周实例并同步模板课程
     */
    @Transactional
    public WeeklyInstance generateNextWeekInstance(Long templateTimetableId) {
        LocalDate nextWeekStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY).plusWeeks(1);
        LocalDate nextWeekEnd = nextWeekStart.plusDays(6);
        String yearWeek = generateYearWeekString(nextWeekStart);

        WeeklyInstance existingInstance = weeklyInstanceRepository.findByTemplateIdAndYearWeek(templateTimetableId, yearWeek);
        if (existingInstance != null) {
            return existingInstance;
        }

        WeeklyInstance instance = new WeeklyInstance(templateTimetableId, nextWeekStart, nextWeekEnd, yearWeek);
        instance = weeklyInstanceRepository.save(instance);
        if (instance.getId() == null) {
            throw new RuntimeException("保存下周实例失败");
        }
        syncSchedulesFromTemplate(instance);
        return instance;
    }

    /**
     * 为所有活动课表生成“下周”的周实例
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
        
        return allSchedules.stream()
            .filter(schedule -> {
                if (schedule.getScheduleDate() == null) {
                    return false; // 没有日期的课程不显示
                }
                LocalDate scheduleDate = schedule.getScheduleDate();
                return !scheduleDate.isBefore(weekStart) && !scheduleDate.isAfter(weekEnd);
            })
            .filter(schedule -> schedule.getIsOnLeave() == null || !schedule.getIsOnLeave()) // 过滤掉请假的课程
            .collect(Collectors.toList());
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

        return allSchedules.stream()
            .filter(schedule -> {
                if (schedule.getScheduleDate() == null) {
                    return false;
                }
                LocalDate scheduleDate = schedule.getScheduleDate();
                return !scheduleDate.isBefore(weekStart) && !scheduleDate.isAfter(weekEnd);
            })
            .collect(Collectors.toList());
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
            logger.info("当前周没有实例，不需要同步");
            return;
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
     * 仅对当前周实例同步“特定模板课程”，并且只覆盖“当前时间之后”的时段。
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
     * 当模板课程被删除时：仅删除“当前周实例”中对应且属于未来时间段的实例课程
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
     * 取消请假
     */
    @Transactional
    public WeeklyInstanceSchedule cancelLeave(Long scheduleId) {
        WeeklyInstanceSchedule schedule = weeklyInstanceScheduleRepository.findById(scheduleId);
        if (schedule == null) {
            throw new IllegalArgumentException("课程不存在");
        }
        
        if (schedule.getIsOnLeave() == null || !schedule.getIsOnLeave()) {
            throw new IllegalArgumentException("该课程未请假");
        }
        
        schedule.setIsOnLeave(false);
        schedule.setLeaveReason(null);
        schedule.setLeaveRequestedAt(null);
        schedule.setUpdatedAt(LocalDateTime.now());
        
        return weeklyInstanceScheduleRepository.update(schedule);
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
        DayOfWeek dayOfWeek;
        switch (dayOfWeekStr.toUpperCase()) {
            case "MONDAY": dayOfWeek = DayOfWeek.MONDAY; break;
            case "TUESDAY": dayOfWeek = DayOfWeek.TUESDAY; break;
            case "WEDNESDAY": dayOfWeek = DayOfWeek.WEDNESDAY; break;
            case "THURSDAY": dayOfWeek = DayOfWeek.THURSDAY; break;
            case "FRIDAY": dayOfWeek = DayOfWeek.FRIDAY; break;
            case "SATURDAY": dayOfWeek = DayOfWeek.SATURDAY; break;
            case "SUNDAY": dayOfWeek = DayOfWeek.SUNDAY; break;
            default: throw new IllegalArgumentException("无效的星期几: " + dayOfWeekStr);
        }
        
        return weekStartDate.with(dayOfWeek);
    }

    /**
     * 根据日期返回“实例逻辑”的活动课表课程（今日从本周实例；跨周日期取对应周实例）
     */
    public Map<String, Object> getActiveInstanceSchedulesByDate(String dateStr) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> timetableSchedules = new ArrayList<>();

        LocalDate targetDate = LocalDate.parse(dateStr);

        // 获取所有活动课表（未删除未归档）
        List<Timetables> activeTimetables = timetableRepository.findAll()
                .stream()
                .filter(t -> t.getIsActive() != null && t.getIsActive() == 1)
                .filter(t -> t.getIsDeleted() == null || t.getIsDeleted() == 0)
                .filter(t -> t.getIsArchived() == null || t.getIsArchived() == 0)
                .collect(Collectors.toList());

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
                            // 不过滤请假课程，让前端显示所有课程（包括请假的）
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
                // 映射为实例样式
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
                item.put("timetableId", timetable.getId());
                item.put("timetableName", timetable.getName());
                item.put("isWeekly", timetable.getIsWeekly());
                // 明细
                List<Map<String, Object>> schedules = instanceSchedules.stream().map(s -> {
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
}
