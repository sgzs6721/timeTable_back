package com.timetable.repository;

import org.jooq.DSLContext;
import com.timetable.generated.tables.daos.SchedulesDao;
import com.timetable.generated.tables.pojos.Schedules;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.time.LocalDate;

import static com.timetable.generated.Tables.SCHEDULES;

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

    /**
     * 只获取模板数据（scheduleDate为null的记录）
     */
    public List<Schedules> findTemplateSchedulesByTimetableId(Long timetableId) {
        return dsl.selectFrom(com.timetable.generated.tables.Schedules.SCHEDULES)
                .where(com.timetable.generated.tables.Schedules.SCHEDULES.TIMETABLE_ID.eq(timetableId))
                .and(com.timetable.generated.tables.Schedules.SCHEDULES.SCHEDULE_DATE.isNull())
                .fetchInto(Schedules.class);
    }

    public List<Schedules> findByTimetableIdAndWeekNumber(Long timetableId, Integer weekNumber) {
        return dsl.selectFrom(SCHEDULES)
                .where(SCHEDULES.TIMETABLE_ID.eq(timetableId).and(SCHEDULES.WEEK_NUMBER.eq(weekNumber)))
                .fetchInto(Schedules.class);
    }

    public List<Schedules> findByTimetableIdAndDateRange(Long timetableId, LocalDate startDate, LocalDate endDate) {
        return dsl.selectFrom(com.timetable.generated.tables.Schedules.SCHEDULES)
                .where(com.timetable.generated.tables.Schedules.SCHEDULES.TIMETABLE_ID.eq(timetableId)
                        .and(com.timetable.generated.tables.Schedules.SCHEDULES.SCHEDULE_DATE.between(startDate, endDate)))
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

    public List<Schedules> findByTimetableIdAndScheduleDateBetween(Long timetableId, LocalDate startDate, LocalDate endDate) {
        return dsl.selectFrom(SCHEDULES)
                .where(SCHEDULES.TIMETABLE_ID.eq(timetableId))
                .and(SCHEDULES.SCHEDULE_DATE.between(startDate, endDate))
                .orderBy(SCHEDULES.SCHEDULE_DATE, SCHEDULES.START_TIME)
                .fetchInto(Schedules.class);
    }

    /**
     * 根据课表ID和学生姓名查询课程安排
     */
    public List<Schedules> findByTimetableIdAndStudentName(Long timetableId, String studentName) {
        return dsl.selectFrom(SCHEDULES)
                .where(SCHEDULES.TIMETABLE_ID.eq(timetableId))
                .and(SCHEDULES.STUDENT_NAME.eq(studentName))
                .orderBy(SCHEDULES.SCHEDULE_DATE, SCHEDULES.START_TIME, SCHEDULES.DAY_OF_WEEK)
                .fetchInto(Schedules.class);
    }

    /**
     * 根据课表ID、学生姓名和周数查询课程安排
     */
    public List<Schedules> findByTimetableIdAndStudentNameAndWeek(Long timetableId, String studentName, Integer week) {
        return dsl.selectFrom(SCHEDULES)
                .where(SCHEDULES.TIMETABLE_ID.eq(timetableId))
                .and(SCHEDULES.STUDENT_NAME.eq(studentName))
                .and(SCHEDULES.WEEK_NUMBER.eq(week))
                .orderBy(SCHEDULES.DAY_OF_WEEK, SCHEDULES.START_TIME)
                .fetchInto(Schedules.class);
    }

    /**
     * 根据课表ID和星期几查找排课
     */
    public List<Schedules> findByTimetableIdAndDayOfWeek(Long timetableId, String dayOfWeek) {
        return dsl.selectFrom(SCHEDULES)
                .where(SCHEDULES.TIMETABLE_ID.eq(timetableId))
                .and(SCHEDULES.DAY_OF_WEEK.eq(dayOfWeek))
                .orderBy(SCHEDULES.START_TIME)
                .fetchInto(Schedules.class);
    }

    /**
     * 根据课表ID和具体日期查找排课
     */
    public List<Schedules> findByTimetableIdAndScheduleDate(Long timetableId, LocalDate scheduleDate) {
        return dsl.selectFrom(SCHEDULES)
                .where(SCHEDULES.TIMETABLE_ID.eq(timetableId))
                .and(SCHEDULES.SCHEDULE_DATE.eq(scheduleDate))
                .orderBy(SCHEDULES.START_TIME)
                .fetchInto(Schedules.class);
    }

    /**
     * 根据日期范围统计课程数量
     */
    public int countByTimetableIdAndScheduleDateBetween(Long timetableId, LocalDate startDate, LocalDate endDate) {
        return dsl.selectCount()
                .from(SCHEDULES)
                .where(SCHEDULES.TIMETABLE_ID.eq(timetableId))
                .and(SCHEDULES.SCHEDULE_DATE.between(startDate, endDate))
                .fetchOne(0, Integer.class);
    }

    // 可根据业务扩展更多jOOQ查询
}