package com.timetable.repository;

import com.timetable.generated.tables.pojos.Schedules;
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
    public List<Schedules> querySchedulesByUserPaged(Long userId, LocalDate start, LocalDate end, int page, int size) {
        // 1) 固定课表（schedules）中有具体日期的记录
        LocalDate today = LocalDate.now();
        java.time.LocalTime now = java.time.LocalTime.now();
        Condition baseCond = TIMETABLES.USER_ID.eq(userId)
                .and(SCHEDULES.TIMETABLE_ID.eq(TIMETABLES.ID))
                .and(TIMETABLES.IS_DELETED.isNull().or(TIMETABLES.IS_DELETED.eq((byte)0)))
                .and(SCHEDULES.SCHEDULE_DATE.isNotNull())
                .and(SCHEDULES.SCHEDULE_DATE.lt(today) // 昨天及之前的记录
                        .or(SCHEDULES.SCHEDULE_DATE.eq(today).and(SCHEDULES.START_TIME.le(now)))); // 或者今天的已过时间记录
        if (start != null) baseCond = baseCond.and(SCHEDULES.SCHEDULE_DATE.ge(start));
        if (end != null) baseCond = baseCond.and(SCHEDULES.SCHEDULE_DATE.le(end));
        
        // 1.5) 固定课表（schedules）中只有day_of_week的记录（根据当前周推算日期）
        // 只查询今天及之前的记录，不包含未来日期
        Condition templateCond = TIMETABLES.USER_ID.eq(userId)
                .and(SCHEDULES.TIMETABLE_ID.eq(TIMETABLES.ID))
                .and(TIMETABLES.IS_DELETED.isNull().or(TIMETABLES.IS_DELETED.eq((byte)0)))
                .and(SCHEDULES.SCHEDULE_DATE.isNull())
                .and(SCHEDULES.DAY_OF_WEEK.isNotNull());

        // 2) 周实例（weekly_instance_schedules）中的记录
        // 表结构：
        // weekly_instance_schedules.weekly_instance_id -> weekly_instances.id
        // weekly_instances.template_timetable_id -> timetables.id
        Condition instCond = field(name("timetables", "user_id"), Long.class).eq(userId)
                .and(field(name("timetables", "is_deleted"), Byte.class).isNull()
                        .or(field(name("timetables", "is_deleted"), Byte.class).eq((byte)0)))
                .and(field(name("weekly_instance_schedules", "is_on_leave"), Boolean.class).isNull()
                        .or(field(name("weekly_instance_schedules", "is_on_leave"), Boolean.class).eq(false)))
                .and(field(name("weekly_instance_schedules", "schedule_date"), LocalDate.class).lt(today) // 昨天及之前的记录
                        .or(field(name("weekly_instance_schedules", "schedule_date"), LocalDate.class).eq(today)
                                .and(field(name("weekly_instance_schedules", "start_time"), java.time.LocalTime.class).le(now)))); // 或者今天的已过时间记录
        if (start != null) instCond = instCond.and(field(name("weekly_instance_schedules", "schedule_date"), LocalDate.class).ge(start));
        if (end != null) instCond = instCond.and(field(name("weekly_instance_schedules", "schedule_date"), LocalDate.class).le(end));

        // 统一选择与 SCHEDULES 表一致的列顺序/别名，方便映射到 pojo
        org.jooq.Select<? extends Record> selectTemplate = dsl
                .select(
                        SCHEDULES.ID.as("id"),
                        SCHEDULES.TIMETABLE_ID.as("timetable_id"),
                        SCHEDULES.STUDENT_NAME.as("student_name"),
                        SCHEDULES.SUBJECT.as("subject"),
                        SCHEDULES.DAY_OF_WEEK.as("day_of_week"),
                        SCHEDULES.START_TIME.as("start_time"),
                        SCHEDULES.END_TIME.as("end_time"),
                        SCHEDULES.SCHEDULE_DATE.as("schedule_date"),
                        SCHEDULES.NOTE.as("note"),
                        SCHEDULES.CREATED_AT.as("created_at"),
                        SCHEDULES.UPDATED_AT.as("updated_at")
                )
                .from(SCHEDULES.join(TIMETABLES).on(SCHEDULES.TIMETABLE_ID.eq(TIMETABLES.ID)))
                .where(baseCond);
                
        // 固定课表模板查询（只有day_of_week的记录）
        org.jooq.Select<? extends Record> selectTemplateOnly = dsl
                .select(
                        SCHEDULES.ID.as("id"),
                        SCHEDULES.TIMETABLE_ID.as("timetable_id"),
                        SCHEDULES.STUDENT_NAME.as("student_name"),
                        SCHEDULES.SUBJECT.as("subject"),
                        SCHEDULES.DAY_OF_WEEK.as("day_of_week"),
                        SCHEDULES.START_TIME.as("start_time"),
                        SCHEDULES.END_TIME.as("end_time"),
                        SCHEDULES.SCHEDULE_DATE.as("schedule_date"), // 这里会是null
                        SCHEDULES.NOTE.as("note"),
                        SCHEDULES.CREATED_AT.as("created_at"),
                        SCHEDULES.UPDATED_AT.as("updated_at")
                )
                .from(SCHEDULES.join(TIMETABLES).on(SCHEDULES.TIMETABLE_ID.eq(TIMETABLES.ID)))
                .where(templateCond);

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
                        field(name("weekly_instance_schedules", "updated_at"))
                )
                .from(table("weekly_instance_schedules"))
                .join(table("weekly_instances")).on(field(name("weekly_instance_schedules", "weekly_instance_id")).eq(field(name("weekly_instances", "id"))))
                .join(table("timetables")).on(field(name("weekly_instances", "template_timetable_id")).eq(field(name("timetables", "id"))))
                .where(instCond);

        int offset = (page - 1) * size;

        Result<Record> unionResult = dsl
                .selectFrom(selectTemplate.unionAll((org.jooq.Select)selectTemplateOnly).unionAll((org.jooq.Select)selectInstance).asTable("all_schedules"))
                .orderBy(field(name("schedule_date")).desc().nullsLast(), field(name("start_time")).desc())
                .limit(size)
                .offset(offset)
                .fetch();

        List<Schedules> list = new ArrayList<>();
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
            
            list.add(s);
        }
        return list;
    }

    public long countSchedulesByUser(Long userId, LocalDate start, LocalDate end) {
        LocalDate today = LocalDate.now();
        java.time.LocalTime now = java.time.LocalTime.now();
        Condition baseCond = TIMETABLES.USER_ID.eq(userId)
                .and(SCHEDULES.TIMETABLE_ID.eq(TIMETABLES.ID))
                .and(TIMETABLES.IS_DELETED.isNull().or(TIMETABLES.IS_DELETED.eq((byte)0)))
                .and(SCHEDULES.SCHEDULE_DATE.isNotNull())
                .and(SCHEDULES.SCHEDULE_DATE.lt(today) // 昨天及之前的记录
                        .or(SCHEDULES.SCHEDULE_DATE.eq(today).and(SCHEDULES.START_TIME.le(now)))); // 或者今天的已过时间记录
        if (start != null) baseCond = baseCond.and(SCHEDULES.SCHEDULE_DATE.ge(start));
        if (end != null) baseCond = baseCond.and(SCHEDULES.SCHEDULE_DATE.le(end));
        
        // 固定课表模板条件（只有day_of_week的记录）
        Condition templateCond = TIMETABLES.USER_ID.eq(userId)
                .and(SCHEDULES.TIMETABLE_ID.eq(TIMETABLES.ID))
                .and(TIMETABLES.IS_DELETED.isNull().or(TIMETABLES.IS_DELETED.eq((byte)0)))
                .and(SCHEDULES.SCHEDULE_DATE.isNull())
                .and(SCHEDULES.DAY_OF_WEEK.isNotNull());

        long templateCount = dsl.selectCount()
                .from(SCHEDULES.join(TIMETABLES).on(SCHEDULES.TIMETABLE_ID.eq(TIMETABLES.ID)))
                .where(baseCond)
                .fetchOne(0, Long.class);
                
        long templateOnlyCount = dsl.selectCount()
                .from(SCHEDULES.join(TIMETABLES).on(SCHEDULES.TIMETABLE_ID.eq(TIMETABLES.ID)))
                .where(templateCond)
                .fetchOne(0, Long.class);

        Condition instCond = field(name("timetables", "user_id"), Long.class).eq(userId)
                .and(field(name("timetables", "is_deleted"), Byte.class).isNull()
                        .or(field(name("timetables", "is_deleted"), Byte.class).eq((byte)0)))
                .and(field(name("weekly_instance_schedules", "is_on_leave"), Boolean.class).isNull()
                        .or(field(name("weekly_instance_schedules", "is_on_leave"), Boolean.class).eq(false)))
                .and(field(name("weekly_instance_schedules", "schedule_date"), LocalDate.class).lt(today) // 昨天及之前的记录
                        .or(field(name("weekly_instance_schedules", "schedule_date"), LocalDate.class).eq(today)
                                .and(field(name("weekly_instance_schedules", "start_time"), java.time.LocalTime.class).le(now)))); // 或者今天的已过时间记录
        if (start != null) instCond = instCond.and(field(name("weekly_instance_schedules", "schedule_date"), LocalDate.class).ge(start));
        if (end != null) instCond = instCond.and(field(name("weekly_instance_schedules", "schedule_date"), LocalDate.class).le(end));

        long instanceCount = dsl.selectCount()
                .from(table("weekly_instance_schedules")
                        .join(table("weekly_instances")).on(field(name("weekly_instance_schedules", "weekly_instance_id")).eq(field(name("weekly_instances", "id"))))
                        .join(table("timetables")).on(field(name("weekly_instances", "template_timetable_id")).eq(field(name("timetables", "id")))) )
                .where(instCond)
                .fetchOne(0, Long.class);

        return templateCount + templateOnlyCount + instanceCount;
    }
}


