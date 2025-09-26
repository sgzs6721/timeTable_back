package com.timetable.repository;

import com.timetable.generated.tables.pojos.Schedules;
import org.jooq.DSLContext;
import org.jooq.Condition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import static com.timetable.generated.Tables.SCHEDULES;
import static com.timetable.generated.Tables.TIMETABLES;

@Repository
public class ReportRepository {

    @Autowired
    private DSLContext dsl;

    /**
     * 分页查询指定用户（教练）所有课表下的课程记录
     */
    public List<Schedules> querySchedulesByUserPaged(Long userId, LocalDate start, LocalDate end, int page, int size) {
        Condition cond = TIMETABLES.USER_ID.eq(userId)
                .and(SCHEDULES.TIMETABLE_ID.eq(TIMETABLES.ID))
                // 过滤已删除的课程
                .and(SCHEDULES.IS_DELETED.isNull().or(SCHEDULES.IS_DELETED.eq((byte)0)))
                // 过滤已删除的课表
                .and(TIMETABLES.IS_DELETED.isNull().or(TIMETABLES.IS_DELETED.eq((byte)0)));
        if (start != null && end != null) {
            cond = cond.and(SCHEDULES.SCHEDULE_DATE.between(start, end));
        } else if (start != null) {
            cond = cond.and(SCHEDULES.SCHEDULE_DATE.ge(start));
        } else if (end != null) {
            cond = cond.and(SCHEDULES.SCHEDULE_DATE.le(end));
        }

        int offset = (page - 1) * size;

        return dsl.select(SCHEDULES.fields())
                .from(SCHEDULES.join(TIMETABLES).on(SCHEDULES.TIMETABLE_ID.eq(TIMETABLES.ID)))
                .where(cond)
                .orderBy(SCHEDULES.SCHEDULE_DATE.desc(), SCHEDULES.START_TIME.desc())
                .limit(size)
                .offset(offset)
                .fetchInto(Schedules.class);
    }

    public long countSchedulesByUser(Long userId, LocalDate start, LocalDate end) {
        Condition cond = TIMETABLES.USER_ID.eq(userId)
                .and(SCHEDULES.TIMETABLE_ID.eq(TIMETABLES.ID))
                .and(SCHEDULES.IS_DELETED.isNull().or(SCHEDULES.IS_DELETED.eq((byte)0)))
                .and(TIMETABLES.IS_DELETED.isNull().or(TIMETABLES.IS_DELETED.eq((byte)0)));
        if (start != null && end != null) {
            cond = cond.and(SCHEDULES.SCHEDULE_DATE.between(start, end));
        } else if (start != null) {
            cond = cond.and(SCHEDULES.SCHEDULE_DATE.ge(start));
        } else if (end != null) {
            cond = cond.and(SCHEDULES.SCHEDULE_DATE.le(end));
        }

        return dsl.selectCount()
                .from(SCHEDULES.join(TIMETABLES).on(SCHEDULES.TIMETABLE_ID.eq(TIMETABLES.ID)))
                .where(cond)
                .fetchOne(0, Long.class);
    }
}


