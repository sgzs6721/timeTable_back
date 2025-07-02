package com.timetable.repository;

import org.jooq.DSLContext;
import com.timetable.generated.tables.daos.SchedulesDao;
import com.timetable.generated.tables.pojos.Schedules;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * 排课Repository - 内存实现（用于测试）
 */
@Repository
public class ScheduleRepository {
    
    @Autowired
    private DSLContext dsl;
    @Autowired
    private SchedulesDao schedulesDao;
    
    public Schedules findById(Long id) {
        return schedulesDao.findById(id);
    }

    public void save(Schedules schedule) {
        schedulesDao.insert(schedule);
    }

    public void update(Schedules schedule) {
        schedulesDao.update(schedule);
    }

    public void deleteById(Long id) {
        schedulesDao.deleteById(id);
    }

    public List<Schedules> findByTimetableId(Long timetableId) {
        return dsl.selectFrom(com.timetable.generated.tables.Schedules.SCHEDULES)
                .where(com.timetable.generated.tables.Schedules.SCHEDULES.TIMETABLE_ID.eq(timetableId))
                .fetchInto(Schedules.class);
    }

    public List<Schedules> findByTimetableIdAndWeekNumber(Long timetableId, Integer weekNumber) {
        return dsl.selectFrom(com.timetable.generated.tables.Schedules.SCHEDULES)
                .where(com.timetable.generated.tables.Schedules.SCHEDULES.TIMETABLE_ID.eq(timetableId)
                        .and(com.timetable.generated.tables.Schedules.SCHEDULES.WEEK_NUMBER.eq(weekNumber)))
                .fetchInto(Schedules.class);
    }

    public Schedules findByIdAndTimetableId(Long id, Long timetableId) {
        return dsl.selectFrom(com.timetable.generated.tables.Schedules.SCHEDULES)
                .where(com.timetable.generated.tables.Schedules.SCHEDULES.ID.eq(id)
                        .and(com.timetable.generated.tables.Schedules.SCHEDULES.TIMETABLE_ID.eq(timetableId)))
                .fetchOneInto(Schedules.class);
    }

    public boolean existsByIdAndTimetableId(Long id, Long timetableId) {
        return dsl.fetchExists(
            dsl.selectFrom(com.timetable.generated.tables.Schedules.SCHEDULES)
                .where(com.timetable.generated.tables.Schedules.SCHEDULES.ID.eq(id)
                        .and(com.timetable.generated.tables.Schedules.SCHEDULES.TIMETABLE_ID.eq(timetableId)))
        );
    }

    public List<Schedules> findByTimetableIdIn(List<Long> timetableIds) {
        return dsl.selectFrom(com.timetable.generated.tables.Schedules.SCHEDULES)
                .where(com.timetable.generated.tables.Schedules.SCHEDULES.TIMETABLE_ID.in(timetableIds))
                .fetchInto(Schedules.class);
    }

    public void deleteByTimetableId(Long timetableId) {
        dsl.deleteFrom(com.timetable.generated.tables.Schedules.SCHEDULES)
                .where(com.timetable.generated.tables.Schedules.SCHEDULES.TIMETABLE_ID.eq(timetableId))
                .execute();
    }

    /**
     * 按条件批量删除排课
     */
    public int deleteByCondition(Long timetableId, com.timetable.dto.ScheduleRequest request) {
        com.timetable.generated.tables.Schedules dslTable = com.timetable.generated.tables.Schedules.SCHEDULES;
        org.jooq.Condition condition = dslTable.TIMETABLE_ID.eq(timetableId);
        if (request.getStudentName() != null && !request.getStudentName().isEmpty()) {
            condition = condition.and(dslTable.STUDENT_NAME.eq(request.getStudentName()));
        }
        if (request.getDayOfWeek() != null) {
            condition = condition.and(dslTable.DAY_OF_WEEK.eq(request.getDayOfWeek().name()));
        }
        if (request.getStartTime() != null) {
            condition = condition.and(dslTable.START_TIME.eq(request.getStartTime()));
        }
        if (request.getEndTime() != null) {
            condition = condition.and(dslTable.END_TIME.eq(request.getEndTime()));
        }
        if (request.getWeekNumber() != null) {
            condition = condition.and(dslTable.WEEK_NUMBER.eq(request.getWeekNumber()));
        }
        if (request.getScheduleDate() != null) {
            condition = condition.and(dslTable.SCHEDULE_DATE.eq(request.getScheduleDate()));
        }
        return dsl.deleteFrom(dslTable).where(condition).execute();
    }

    // 可根据业务扩展更多jOOQ查询
} 