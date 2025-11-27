package com.timetable.repository;

import com.timetable.generated.tables.pojos.Schedules;
import com.timetable.dto.ScheduleWithCoachDTO;
import org.jooq.DSLContext;
import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

import static com.timetable.generated.Tables.SCHEDULES;
import static com.timetable.generated.Tables.TIMETABLES;
import static org.jooq.impl.DSL.*;

@Repository
public class ReportRepository {

    @Autowired
    private DSLContext dsl;

    /**
     * 分页查询指定用户（教练）所有课表下的课程记录
     * 包括有具体日期的课程和固定课表模板（需要根据day_of_week推算日期）
     */
    public List<ScheduleWithCoachDTO> querySchedulesByUserPaged(Long userId, Long organizationId, LocalDate start, LocalDate end, int page, int size, String sortOrder) {
        // 注意：“我的课时”只查询周实例课表，不查询固定课表（schedules）
        // 因为固定课表只是模板，实际上课记录都在周实例中

        // 查询周实例（weekly_instance_schedules）中的记录
        // 表结构：
        // weekly_instance_schedules.weekly_instance_id -> weekly_instances.id
        // weekly_instances.template_timetable_id -> timetables.id
        
        // 构建周实例条件，如果userId为null则不添加用户ID过滤
        Condition instCond = field(name("timetables", "is_deleted"), Byte.class).isNull()
                        .or(field(name("timetables", "is_deleted"), Byte.class).eq((byte)0))
                .and(field(name("weekly_instance_schedules", "is_on_leave"), Boolean.class).isNull()
                        .or(field(name("weekly_instance_schedules", "is_on_leave"), Boolean.class).eq(false)))
                .and(field(name("weekly_instance_schedules", "is_cancelled"), Boolean.class).isNull()
                        .or(field(name("weekly_instance_schedules", "is_cancelled"), Boolean.class).eq(false)))
                .and(field(name("weekly_instance_schedules", "schedule_date"), LocalDate.class).lt(LocalDate.now()) // 昨天及之前的记录
                        .or(field(name("weekly_instance_schedules", "schedule_date"), LocalDate.class).eq(LocalDate.now())
                                .and(field(name("weekly_instance_schedules", "start_time"), java.time.LocalTime.class).le(java.time.LocalTime.now())))); // 或者今天的已过时间记录
        
        // 添加用户ID过滤（如果提供了userId）
        if (userId != null) {
            instCond = instCond.and(field(name("timetables", "user_id"), Long.class).eq(userId));
        }
        
        // 添加机构ID过滤（如果提供了organizationId）
        if (organizationId != null) {
            instCond = instCond.and(field(name("timetables", "organization_id"), Long.class).eq(organizationId));
        }
        
        if (start != null) instCond = instCond.and(field(name("weekly_instance_schedules", "schedule_date"), LocalDate.class).ge(start));
        if (end != null) instCond = instCond.and(field(name("weekly_instance_schedules", "schedule_date"), LocalDate.class).le(end));

        org.jooq.Select<? extends Record> selectInstance = dsl
                .select(
                        field(name("weekly_instance_schedules", "id")).as("id"),
                        // 这里用模板课表ID当作 timetable_id 以保持含义一致
                        field(name("weekly_instances", "template_timetable_id"), Long.class).as("timetable_id"),
                        field(name("weekly_instance_schedules", "student_name"), String.class).as("student_name"),
                        field(name("weekly_instance_schedules", "subject"), String.class).as("subject"),
                        field(name("weekly_instance_schedules", "day_of_week"), String.class).as("day_of_week"),
                        field(name("weekly_instance_schedules", "start_time")),
                        field(name("weekly_instance_schedules", "end_time")),
                        field(name("weekly_instance_schedules", "schedule_date"), LocalDate.class).as("schedule_date"),
                        field(name("weekly_instance_schedules", "note"), String.class).as("note"),
                        field(name("weekly_instance_schedules", "created_at")),
                        field(name("weekly_instance_schedules", "updated_at")),
                        // 添加教练信息
                        field(name("users", "nickname"), String.class).as("coach_name")
                )
                .from(table("weekly_instance_schedules"))
                .join(table("weekly_instances")).on(field(name("weekly_instance_schedules", "weekly_instance_id")).eq(field(name("weekly_instances", "id"))))
                .join(table("timetables")).on(field(name("weekly_instances", "template_timetable_id")).eq(field(name("timetables", "id"))))
                .join(table("users")).on(field(name("timetables", "user_id"), Long.class).eq(field(name("users", "id"), Long.class)))
                .where(instCond);

        int offset = (page - 1) * size;

        // 根据sortOrder参数决定排序方向
        org.jooq.SortField<?> dateSortField;
        org.jooq.SortField<?> timeSortField;
        
        if ("asc".equalsIgnoreCase(sortOrder)) {
            // 正序：日期从早到晚，时间从早到晚
            dateSortField = field(name("schedule_date")).asc().nullsFirst();
            timeSortField = field(name("start_time")).asc();
        } else {
            // 倒序：日期从晚到早，时间从晚到早（默认）
            dateSortField = field(name("schedule_date")).desc().nullsLast();
            timeSortField = field(name("start_time")).desc();
        }
        
        // 只查询周实例数据，不包含固定课表
        Result<Record> unionResult = dsl
                .selectFrom(selectInstance.asTable("all_schedules"))
                .orderBy(dateSortField, timeSortField)
                .limit(size)
                .offset(offset)
                .fetch();

        List<ScheduleWithCoachDTO> list = new ArrayList<>();
        for (Record r : unionResult) {
            Schedules s = new Schedules();
            s.setId(r.get("id", Long.class));
            s.setTimetableId(r.get("timetable_id", Long.class));
            s.setStudentName(r.get("student_name", String.class));
            s.setSubject(r.get("subject", String.class));
            s.setDayOfWeek(r.get("day_of_week", String.class));
            s.setStartTime(r.get("start_time", java.time.LocalTime.class));
            s.setEndTime(r.get("end_time", java.time.LocalTime.class));
            s.setScheduleDate(r.get("schedule_date", LocalDate.class));
            s.setNote(r.get("note", String.class));
            s.setCreatedAt(r.get("created_at", java.time.LocalDateTime.class));
            s.setUpdatedAt(r.get("updated_at", java.time.LocalDateTime.class));
            
            // 过滤掉未来的记录（包括今天的未来时间）
            LocalDate recordDate = s.getScheduleDate();
            if (recordDate != null) {
                if (recordDate.isAfter(today)) {
                    continue; // 跳过未来日期的记录
                } else if (recordDate.isEqual(today)) {
                    // 如果是今天，还要检查时间是否已过
                    java.time.LocalTime startTime = s.getStartTime();
                    if (startTime != null && startTime.isAfter(now)) {
                        continue; // 跳过今天未来时间的记录
                    }
                }
            }
            
            // 创建包含教练信息的DTO
            String coachName = r.get("coach_name", String.class);
            ScheduleWithCoachDTO dto = new ScheduleWithCoachDTO(s, coachName);
            list.add(dto);
        }
        return list;
    }

    public long countSchedulesByUser(Long userId, Long organizationId, LocalDate start, LocalDate end) {
        LocalDate today = LocalDate.now();
        java.time.LocalTime now = java.time.LocalTime.now();
        
        // 注意：只统计周实例课表，不统计固定课表
        // 构建周实例条件，如果userId为null则不添加用户ID过滤
        Condition instCond = field(name("timetables", "is_deleted"), Byte.class).isNull()
                        .or(field(name("timetables", "is_deleted"), Byte.class).eq((byte)0))
                .and(field(name("weekly_instance_schedules", "is_on_leave"), Boolean.class).isNull()
                        .or(field(name("weekly_instance_schedules", "is_on_leave"), Boolean.class).eq(false)))
                .and(field(name("weekly_instance_schedules", "is_cancelled"), Boolean.class).isNull()
                        .or(field(name("weekly_instance_schedules", "is_cancelled"), Boolean.class).eq(false)))
                .and(field(name("weekly_instance_schedules", "schedule_date"), LocalDate.class).lt(today) // 昨天及之前的记录
                        .or(field(name("weekly_instance_schedules", "schedule_date"), LocalDate.class).eq(today)
                                .and(field(name("weekly_instance_schedules", "start_time"), java.time.LocalTime.class).le(now)))); // 或者今天的已过时间记录
        
        // 添加用户ID过滤（如果提供了userId）
        if (userId != null) {
            instCond = instCond.and(field(name("timetables", "user_id"), Long.class).eq(userId));
        }
        
        // 添加机构ID过滤（如果提供了organizationId）
        if (organizationId != null) {
            instCond = instCond.and(field(name("timetables", "organization_id"), Long.class).eq(organizationId));
        }
        
        if (start != null) instCond = instCond.and(field(name("weekly_instance_schedules", "schedule_date"), LocalDate.class).ge(start));
        if (end != null) instCond = instCond.and(field(name("weekly_instance_schedules", "schedule_date"), LocalDate.class).le(end));

        long instanceCount = dsl.selectCount()
                .from(table("weekly_instance_schedules")
                        .join(table("weekly_instances")).on(field(name("weekly_instance_schedules", "weekly_instance_id")).eq(field(name("weekly_instances", "id"))))
                        .join(table("timetables")).on(field(name("weekly_instances", "template_timetable_id")).eq(field(name("timetables", "id")))) )
                .where(instCond)
                .fetchOne(0, Long.class);

        // 只返回周实例的统计数量
        return instanceCount;
    }
}


