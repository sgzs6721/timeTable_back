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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 周实例服务类
 */
@Service
public class WeeklyInstanceService {

    @Autowired
    private WeeklyInstanceRepository weeklyInstanceRepository;

    @Autowired
    private WeeklyInstanceScheduleRepository weeklyInstanceScheduleRepository;

    @Autowired
    private TimetableRepository timetableRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    /**
     * 为指定的固定课表生成当前周实例
     */
    @Transactional
    public WeeklyInstance generateCurrentWeekInstance(Long templateTimetableId) {
        // 检查模板课表是否存在且为周课表
        Timetables templateTimetable = timetableRepository.findById(templateTimetableId);
        if (templateTimetable == null) {
            throw new IllegalArgumentException("模板课表不存在");
        }
        
        if (templateTimetable.getIsWeekly() == null || templateTimetable.getIsWeekly() != 1) {
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
        WeeklyInstance instance = new WeeklyInstance(templateTimetableId, weekStart, weekEnd, yearWeek);
        instance = weeklyInstanceRepository.save(instance);

        // 从模板课表复制课程到实例
        syncSchedulesFromTemplate(instance);

        // 清除同一模板课表的其他当前周标记，设置新实例为当前周
        weeklyInstanceRepository.clearCurrentWeekFlagByTemplateId(templateTimetableId);
        weeklyInstanceRepository.setCurrentWeekInstance(instance.getId());

        return instance;
    }

    /**
     * 为所有活动的固定课表生成当前周实例
     */
    @Transactional
    public void generateCurrentWeekInstancesForAllActiveTimetables() {
        // 获取所有活动的周固定课表
        List<Timetables> activeTimetables = timetableRepository.findAll()
                .stream()
                .filter(t -> t.getIsActive() != null && t.getIsActive() == 1)
                .filter(t -> t.getIsWeekly() != null && t.getIsWeekly() == 1)
                .filter(t -> t.getIsDeleted() == null || t.getIsDeleted() == 0)
                .filter(t -> t.getIsArchived() == null || t.getIsArchived() == 0)
                .collect(Collectors.toList());

        for (Timetables timetable : activeTimetables) {
            try {
                generateCurrentWeekInstance(timetable.getId());
            } catch (Exception e) {
                // 记录错误但继续处理其他课表
                System.err.println("生成课表 " + timetable.getId() + " 的当前周实例失败: " + e.getMessage());
            }
        }
    }

    /**
     * 从模板课表同步课程到周实例
     */
    @Transactional
    public void syncSchedulesFromTemplate(WeeklyInstance instance) {
        // 获取模板课表的所有课程
        List<Schedules> templateSchedules = scheduleRepository.findByTimetableId(instance.getTemplateTimetableId());

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
        }

        // 更新实例的同步时间
        weeklyInstanceRepository.updateLastSyncedAt(instance.getId(), LocalDateTime.now());
    }

    /**
     * 获取指定课表的当前周实例
     */
    public WeeklyInstance getCurrentWeekInstance(Long templateTimetableId) {
        return weeklyInstanceRepository.findCurrentWeekInstanceByTemplateId(templateTimetableId);
    }

    /**
     * 获取当前周实例的课程安排
     */
    public List<WeeklyInstanceSchedule> getCurrentWeekInstanceSchedules(Long templateTimetableId) {
        WeeklyInstance currentInstance = getCurrentWeekInstance(templateTimetableId);
        if (currentInstance == null) {
            return new ArrayList<>();
        }
        return weeklyInstanceScheduleRepository.findByWeeklyInstanceId(currentInstance.getId());
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
            syncSchedulesFromTemplate(instance);
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

        existingSchedule.setStudentName(updatedSchedule.getStudentName());
        existingSchedule.setSubject(updatedSchedule.getSubject());
        existingSchedule.setDayOfWeek(updatedSchedule.getDayOfWeek());
        existingSchedule.setStartTime(updatedSchedule.getStartTime());
        existingSchedule.setEndTime(updatedSchedule.getEndTime());
        existingSchedule.setScheduleDate(updatedSchedule.getScheduleDate());
        existingSchedule.setNote(updatedSchedule.getNote());
        existingSchedule.setIsModified(true);
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
     * 检查课表是否有当前周实例
     */
    public boolean hasCurrentWeekInstance(Long templateTimetableId) {
        return getCurrentWeekInstance(templateTimetableId) != null;
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
}
