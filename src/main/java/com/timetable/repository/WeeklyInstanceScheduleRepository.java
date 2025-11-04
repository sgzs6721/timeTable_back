package com.timetable.repository;

import com.timetable.entity.WeeklyInstanceSchedule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.jooq.impl.DSL.*;

/**
 * 周实例课程数据访问层
 */
@Repository
public class WeeklyInstanceScheduleRepository extends BaseRepository {

    @Autowired
    private DSLContext dsl;

    /**
     * 保存周实例课程
     */
    public WeeklyInstanceSchedule save(WeeklyInstanceSchedule schedule) {
        if (schedule.getId() == null) {
            // 创建新记录
            int affectedRows = dsl.insertInto(table("weekly_instance_schedules"))
                    .set(field("weekly_instance_id"), schedule.getWeeklyInstanceId())
                    .set(field("template_schedule_id"), schedule.getTemplateScheduleId())
                    .set(field("student_name"), schedule.getStudentName())
                    .set(field("subject"), schedule.getSubject())
                    .set(field("day_of_week"), schedule.getDayOfWeek())
                    .set(field("start_time"), schedule.getStartTime())
                    .set(field("end_time"), schedule.getEndTime())
                    .set(field("schedule_date"), schedule.getScheduleDate())
                    .set(field("note"), schedule.getNote())
                    .set(field("is_manual_added"), schedule.getIsManualAdded())
                    .set(field("is_modified"), schedule.getIsModified())
                    .set(field("is_trial"), schedule.getIsTrial())
                    .set(field("created_at"), schedule.getCreatedAt())
                    .set(field("updated_at"), schedule.getUpdatedAt())
                    .execute();
            
            if (affectedRows > 0) {
                // 查询获取ID (对于实例课程，通常不需要ID，但为了保持一致性)
                // 这里可以选择不查询ID，直接返回
                // 或者使用其他唯一条件查询
            }
        } else {
            // 更新现有记录
            dsl.update(table("weekly_instance_schedules"))
                    .set(field("weekly_instance_id"), schedule.getWeeklyInstanceId())
                    .set(field("template_schedule_id"), schedule.getTemplateScheduleId())
                    .set(field("student_name"), schedule.getStudentName())
                    .set(field("subject"), schedule.getSubject())
                    .set(field("day_of_week"), schedule.getDayOfWeek())
                    .set(field("start_time"), schedule.getStartTime())
                    .set(field("end_time"), schedule.getEndTime())
                    .set(field("schedule_date"), schedule.getScheduleDate())
                    .set(field("note"), schedule.getNote())
                    .set(field("is_manual_added"), schedule.getIsManualAdded())
                    .set(field("is_modified"), schedule.getIsModified())
                    .set(field("is_trial"), schedule.getIsTrial())
                    .set(field("updated_at"), LocalDateTime.now())
                    .where(field("id").eq(schedule.getId()))
                    .execute();
        }
        return schedule;
    }

    /**
     * 根据ID查找周实例课程
     */
    public WeeklyInstanceSchedule findById(Long id) {
        Record record = dsl.select()
                .from(table("weekly_instance_schedules"))
                .where(field("id").eq(id))
                .fetchOne();
        
        return record != null ? mapToWeeklyInstanceSchedule(record) : null;
    }

    /**
     * 根据周实例ID获取所有课程
     */
    public List<WeeklyInstanceSchedule> findByWeeklyInstanceId(Long weeklyInstanceId) {
        Result<Record> records = dsl.select()
                .from(table("weekly_instance_schedules"))
                .where(field("weekly_instance_id").eq(weeklyInstanceId))
                .and(field("is_cancelled").isNull().or(field("is_cancelled").eq(false)))
                .orderBy(field("schedule_date"), field("start_time"))
                .fetch();
        
        return records.map(this::mapToWeeklyInstanceSchedule);
    }

    /**
     * 根据周实例ID和日期获取课程
     */
    public List<WeeklyInstanceSchedule> findByWeeklyInstanceIdAndDate(Long weeklyInstanceId, LocalDate date) {
        Result<Record> records = dsl.select()
                .from(table("weekly_instance_schedules"))
                .where(field("weekly_instance_id").eq(weeklyInstanceId))
                .and(field("schedule_date").eq(date))
                .and(field("is_cancelled").isNull().or(field("is_cancelled").eq(false)))
                .orderBy(field("start_time"))
                .fetch();
        
        return records.map(this::mapToWeeklyInstanceSchedule);
    }

    /**
     * 根据周实例ID和星期几获取课程
     */
    public List<WeeklyInstanceSchedule> findByWeeklyInstanceIdAndDayOfWeek(Long weeklyInstanceId, String dayOfWeek) {
        Result<Record> records = dsl.select()
                .from(table("weekly_instance_schedules"))
                .where(field("weekly_instance_id").eq(weeklyInstanceId))
                .and(field("day_of_week").eq(dayOfWeek))
                .and(field("is_cancelled").isNull().or(field("is_cancelled").eq(false)))
                .orderBy(field("start_time"))
                .fetch();
        
        return records.map(this::mapToWeeklyInstanceSchedule);
    }

    /**
     * 根据模板课程ID获取所有相关的实例课程
     */
    public List<WeeklyInstanceSchedule> findByTemplateScheduleId(Long templateScheduleId) {
        Result<Record> records = dsl.select()
                .from(table("weekly_instance_schedules"))
                .where(field("template_schedule_id").eq(templateScheduleId))
                .orderBy(field("schedule_date"), field("start_time"))
                .fetch();
        
        return records.map(this::mapToWeeklyInstanceSchedule);
    }

    /**
     * 根据学生姓名和周实例ID获取课程
     */
    public List<WeeklyInstanceSchedule> findByWeeklyInstanceIdAndStudentName(Long weeklyInstanceId, String studentName) {
        Result<Record> records = dsl.select()
                .from(table("weekly_instance_schedules"))
                .where(field("weekly_instance_id").eq(weeklyInstanceId))
                .and(field("student_name").eq(studentName))
                .and(field("is_cancelled").isNull().or(field("is_cancelled").eq(false)))
                .orderBy(field("schedule_date"), field("start_time"))
                .fetch();
        
        return records.map(this::mapToWeeklyInstanceSchedule);
    }

    /**
     * 删除周实例课程（标记为已取消）
     */
    public void delete(Long id) {
        dsl.update(table("weekly_instance_schedules"))
                .set(field("is_cancelled"), true)
                .set(field("cancelled_at"), java.time.LocalDateTime.now())
                .where(field("id").eq(id))
                .execute();
    }

    /**
     * 删除周实例的所有课程
     */
    public void deleteByWeeklyInstanceId(Long weeklyInstanceId) {
        dsl.deleteFrom(table("weekly_instance_schedules"))
                .where(field("weekly_instance_id").eq(weeklyInstanceId))
                .execute();
    }

    /**
     * 删除与模板课程相关的所有实例课程
     */
    public void deleteByTemplateScheduleId(Long templateScheduleId) {
        dsl.deleteFrom(table("weekly_instance_schedules"))
                .where(field("template_schedule_id").eq(templateScheduleId))
                .execute();
    }

    /**
     * 批量保存周实例课程
     */
    public void saveAll(List<WeeklyInstanceSchedule> schedules) {
        for (WeeklyInstanceSchedule schedule : schedules) {
            save(schedule);
        }
    }

    /**
     * 更新周实例课程
     */
    public WeeklyInstanceSchedule update(WeeklyInstanceSchedule schedule) {
        dsl.update(table("weekly_instance_schedules"))
                .set(field("weekly_instance_id"), schedule.getWeeklyInstanceId())
                .set(field("template_schedule_id"), schedule.getTemplateScheduleId())
                .set(field("student_name"), schedule.getStudentName())
                .set(field("subject"), schedule.getSubject())
                .set(field("day_of_week"), schedule.getDayOfWeek())
                .set(field("start_time"), schedule.getStartTime())
                .set(field("end_time"), schedule.getEndTime())
                .set(field("schedule_date"), schedule.getScheduleDate())
                .set(field("note"), schedule.getNote())
                .set(field("is_manual_added"), schedule.getIsManualAdded())
                .set(field("is_modified"), schedule.getIsModified())
                .set(field("is_on_leave"), schedule.getIsOnLeave())
                .set(field("leave_reason"), schedule.getLeaveReason())
                .set(field("leave_requested_at"), schedule.getLeaveRequestedAt())
                .set(field("is_trial"), schedule.getIsTrial())
                .set(field("updated_at"), LocalDateTime.now())
                .where(field("id").eq(schedule.getId()))
                .execute();
        return schedule;
    }

    /**
     * 更新课程的修改状态
     */
    public void markAsModified(Long scheduleId) {
        dsl.update(table("weekly_instance_schedules"))
                .set(field("is_modified"), true)
                .set(field("updated_at"), LocalDateTime.now())
                .where(field("id").eq(scheduleId))
                .execute();
    }

    /**
     * 检查时间冲突
     */
    public boolean hasTimeConflict(Long weeklyInstanceId, LocalDate date, LocalTime startTime, LocalTime endTime, Long excludeId) {
        org.jooq.SelectConditionStep<org.jooq.Record1<Integer>> query = dsl.selectCount()
                .from(table("weekly_instance_schedules"))
                .where(field("weekly_instance_id").eq(weeklyInstanceId))
                .and(field("schedule_date").eq(date))
                .and(field("is_cancelled").isNull().or(field("is_cancelled").eq(false)))
                .and(
                    field("start_time").lt(endTime)
                    .and(field("end_time").gt(startTime))
                );
        
        if (excludeId != null) {
            query = query.and(field("id").ne(excludeId));
        }
        
        return query.fetchOne(0, Integer.class) > 0;
    }

    /**
     * 根据日期范围获取课程
     */
    public List<WeeklyInstanceSchedule> findByDateRange(Long weeklyInstanceId, LocalDate startDate, LocalDate endDate) {
        Result<Record> records = dsl.select()
                .from(table("weekly_instance_schedules"))
                .where(field("weekly_instance_id").eq(weeklyInstanceId))
                .and(field("schedule_date").between(startDate, endDate))
                .and(field("is_cancelled").isNull().or(field("is_cancelled").eq(false)))
                .orderBy(field("schedule_date"), field("start_time"))
                .fetch();
        
        return records.map(this::mapToWeeklyInstanceSchedule);
    }

    /**
     * 根据周实例ID统计课程数量
     */
    public int countByWeeklyInstanceId(Long weeklyInstanceId) {
        return dsl.selectCount()
                .from(table("weekly_instance_schedules"))
                .where(field("weekly_instance_id").eq(weeklyInstanceId))
                .and(field("is_cancelled").isNull().or(field("is_cancelled").eq(false)))
                .fetchOne(0, Integer.class);
    }

    /**
     * 映射Record到WeeklyInstanceSchedule
     */
    private WeeklyInstanceSchedule mapToWeeklyInstanceSchedule(Record record) {
        WeeklyInstanceSchedule schedule = new WeeklyInstanceSchedule();
        schedule.setId(record.get("id", Long.class));
        schedule.setWeeklyInstanceId(record.get("weekly_instance_id", Long.class));
        schedule.setTemplateScheduleId(record.get("template_schedule_id", Long.class));
        schedule.setStudentName(record.get("student_name", String.class));
        schedule.setSubject(record.get("subject", String.class));
        schedule.setDayOfWeek(record.get("day_of_week", String.class));
        schedule.setStartTime(record.get("start_time", LocalTime.class));
        schedule.setEndTime(record.get("end_time", LocalTime.class));
        schedule.setScheduleDate(record.get("schedule_date", LocalDate.class));
        schedule.setNote(record.get("note", String.class));
        schedule.setIsManualAdded(record.get("is_manual_added", Boolean.class));
        schedule.setIsModified(record.get("is_modified", Boolean.class));
        schedule.setIsOnLeave(record.get("is_on_leave", Boolean.class));
        schedule.setLeaveReason(record.get("leave_reason", String.class));
        schedule.setLeaveRequestedAt(record.get("leave_requested_at", LocalDateTime.class));
        schedule.setIsTrial(record.get("is_trial", Byte.class));
        schedule.setIsCancelled(record.get("is_cancelled", Boolean.class));
        schedule.setCancelledAt(record.get("cancelled_at", LocalDateTime.class));
        schedule.setCreatedAt(record.get("created_at", LocalDateTime.class));
        schedule.setUpdatedAt(record.get("updated_at", LocalDateTime.class));
        return schedule;
    }

    /**
     * 根据请假状态查找课程
     */
    public List<WeeklyInstanceSchedule> findByIsOnLeave(Boolean isOnLeave) {
        Result<Record> records = dsl.select()
                .from(table("weekly_instance_schedules"))
                .where(field("is_on_leave").eq(isOnLeave))
                .orderBy(field("leave_requested_at").desc())
                .fetch();
        return records.map(this::mapToWeeklyInstanceSchedule);
    }

    /**
     * 根据学生姓名查找所有课程（不包括已取消的）
     */
    public List<WeeklyInstanceSchedule> findByStudentName(String studentName) {
        Result<Record> records = dsl.select()
                .from(table("weekly_instance_schedules"))
                .where(field("student_name").eq(studentName))
                .and(field("is_cancelled").eq(false))
                .orderBy(field("schedule_date").desc(), field("start_time"))
                .fetch();
        return records.map(this::mapToWeeklyInstanceSchedule);
    }

    /**
     * 获取所有实例课程
     */
    public List<WeeklyInstanceSchedule> findAll() {
        Result<Record> records = dsl.select()
                .from(table("weekly_instance_schedules"))
                .orderBy(field("schedule_date").desc(), field("start_time"))
                .fetch();
        return records.map(this::mapToWeeklyInstanceSchedule);
    }
    
    /**
     * 根据周实例ID查找所有课程
     */
    public List<WeeklyInstanceSchedule> findByInstanceId(Long instanceId) {
        Result<Record> records = dsl.select()
                .from(table("weekly_instance_schedules"))
                .where(field("weekly_instance_id").eq(instanceId))
                .and(field("is_cancelled").isNull().or(field("is_cancelled").eq(false)))
                .orderBy(field("schedule_date"), field("start_time"))
                .fetch();
        return records.map(this::mapToWeeklyInstanceSchedule);
    }
}
