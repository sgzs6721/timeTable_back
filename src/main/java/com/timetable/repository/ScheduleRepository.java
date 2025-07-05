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
     * 软删除课表下的所有排课（临时实现，使用note字段标记）
     */
    public void softDeleteByTimetableId(Long timetableId) {
        List<Schedules> schedules = findByTimetableId(timetableId);
        String deletedTimestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        
        for (Schedules schedule : schedules) {
            // 检查是否已经软删除
            if (schedule.getNote() != null && schedule.getNote().contains("[DELETED_")) {
                continue; // 已经删除了，跳过
            }
            
            // 在note字段中添加删除标记
            String currentNote = schedule.getNote() != null ? schedule.getNote() : "";
            String deletedNote = "[DELETED_" + deletedTimestamp + "] " + currentNote;
            schedule.setNote(deletedNote);
            schedule.setUpdatedAt(java.time.LocalDateTime.now());
            
            // 更新排课
            update(schedule);
        }
    }

    /**
     * 获取课表下的有效排课（过滤掉已软删除的排课）
     */
    public List<Schedules> findActiveTimetableSchedules(Long timetableId) {
        List<Schedules> allSchedules = findByTimetableId(timetableId);
        return allSchedules.stream()
                .filter(s -> s.getNote() == null || !s.getNote().contains("[DELETED_"))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 获取课表下指定周的有效排课（过滤掉已软删除的排课）
     */
    public List<Schedules> findActiveTimetableSchedulesByWeek(Long timetableId, Integer weekNumber) {
        List<Schedules> allSchedules = findByTimetableIdAndWeekNumber(timetableId, weekNumber);
        return allSchedules.stream()
                .filter(s -> s.getNote() == null || !s.getNote().contains("[DELETED_"))
                .collect(java.util.stream.Collectors.toList());
    }

    // 可根据业务扩展更多jOOQ查询
} 