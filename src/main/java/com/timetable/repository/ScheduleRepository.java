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

    // 可根据业务扩展更多jOOQ查询
} 