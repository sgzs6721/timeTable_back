package com.timetable.repository;

import org.jooq.DSLContext;
import com.timetable.generated.tables.daos.SchedulesDao;
import com.timetable.generated.tables.pojos.Schedules;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalTime;

import static com.timetable.generated.Tables.SCHEDULES;
import static com.timetable.generated.Tables.USERS;
import static com.timetable.generated.Tables.WEEKLY_INSTANCE_SCHEDULES;
import static org.jooq.impl.DSL.*;

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
                .orderBy(SCHEDULES.SCHEDULE_DATE.desc(), SCHEDULES.START_TIME.desc(), SCHEDULES.DAY_OF_WEEK.desc())
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
     * 根据课表ID和星期几查找模板排课（只获取scheduleDate为null的记录）
     */
    public List<Schedules> findTemplateSchedulesByTimetableIdAndDayOfWeek(Long timetableId, String dayOfWeek) {
        return dsl.selectFrom(SCHEDULES)
                .where(SCHEDULES.TIMETABLE_ID.eq(timetableId))
                .and(SCHEDULES.DAY_OF_WEEK.eq(dayOfWeek))
                .and(SCHEDULES.SCHEDULE_DATE.isNull())
                .orderBy(SCHEDULES.START_TIME)
                .fetchInto(Schedules.class);
    }

    /**
     * 根据课表ID和星期几（整数）查找模板排课
     */
    public List<Schedules> findByTimetableAndDayOfWeek(Long timetableId, int dayOfWeek) {
        // 将整数转换为DayOfWeek枚举名称 (1=MONDAY, 2=TUESDAY, ..., 7=SUNDAY)
        String dayOfWeekStr = java.time.DayOfWeek.of(dayOfWeek).name();
        return dsl.selectFrom(SCHEDULES)
                .where(SCHEDULES.TIMETABLE_ID.eq(timetableId))
                .and(SCHEDULES.DAY_OF_WEEK.eq(dayOfWeekStr))
                .and(SCHEDULES.SCHEDULE_DATE.isNull()) // 只查询模板课程
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

    /**
     * 查询所有教练（普通用户且职位为教练，或管理员，状态为已批准且未删除的用户）
     */
    public List<com.timetable.generated.tables.pojos.Users> findAllCoaches() {
        return dsl.selectFrom(USERS)
                .where(
                    USERS.ROLE.eq("ADMIN")
                    .or(USERS.ROLE.eq("USER").and(USERS.POSITION.eq("COACH")))
                )
                .and(USERS.STATUS.eq("APPROVED"))
                .and(USERS.IS_DELETED.eq((byte) 0))
                .fetchInto(com.timetable.generated.tables.pojos.Users.class);
    }

    /**
     * 查询周实例在指定日期和时间段的课程
     */
    public List<Schedules> findByInstanceAndDateTime(Long instanceId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        // 查询weekly_instance_schedules表，排除已取消/请假的课程
        List<com.timetable.generated.tables.pojos.WeeklyInstanceSchedules> instanceSchedules = dsl.selectFrom(WEEKLY_INSTANCE_SCHEDULES)
                .where(WEEKLY_INSTANCE_SCHEDULES.WEEKLY_INSTANCE_ID.eq(instanceId))
                .and(WEEKLY_INSTANCE_SCHEDULES.SCHEDULE_DATE.eq(date))
                .and(WEEKLY_INSTANCE_SCHEDULES.IS_CANCELLED.isFalse())
                .and(
                    // 时间段有重叠
                    WEEKLY_INSTANCE_SCHEDULES.START_TIME.lessThan(endTime)
                    .and(WEEKLY_INSTANCE_SCHEDULES.END_TIME.greaterThan(startTime))
                )
                .fetchInto(com.timetable.generated.tables.pojos.WeeklyInstanceSchedules.class);
        
        // 转换为Schedules对象返回
        return instanceSchedules.stream().map(is -> {
            Schedules s = new Schedules();
            s.setId(is.getId());
            s.setStudentName(is.getStudentName());
            s.setDayOfWeek(is.getDayOfWeek());
            s.setScheduleDate(is.getScheduleDate());
            s.setStartTime(is.getStartTime());
            s.setEndTime(is.getEndTime());
            s.setIsTrial(is.getIsTrial()); // 关键：设置体验课标志
            s.setCustomerId(is.getCustomerId()); // 设置客户ID
            return s;
        }).collect(java.util.stream.Collectors.toList());
    }

    /**
     * 查询课表在指定日期和时间段的课程
     */
    public List<Schedules> findByTimetableAndDateTime(Long timetableId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        return dsl.selectFrom(SCHEDULES)
                .where(SCHEDULES.TIMETABLE_ID.eq(timetableId))
                .and(SCHEDULES.SCHEDULE_DATE.eq(date))
                .and(
                    // 时间段有重叠
                    SCHEDULES.START_TIME.lessThan(endTime)
                    .and(SCHEDULES.END_TIME.greaterThan(startTime))
                )
                .fetchInto(Schedules.class);
    }

    /**
     * 插入课程到周实例（带is_trial标志和customer_id）
     * @return 插入的课程ID
     */
    public Long insertInstanceSchedule(com.timetable.generated.tables.pojos.WeeklyInstanceSchedules schedule, boolean isTrial, Long customerId) {
        // 在插入时直接设置is_trial和customer_id字段
        dsl.insertInto(WEEKLY_INSTANCE_SCHEDULES)
                .set(WEEKLY_INSTANCE_SCHEDULES.WEEKLY_INSTANCE_ID, schedule.getWeeklyInstanceId())
                .set(WEEKLY_INSTANCE_SCHEDULES.STUDENT_NAME, schedule.getStudentName())
                .set(WEEKLY_INSTANCE_SCHEDULES.DAY_OF_WEEK, schedule.getDayOfWeek())
                .set(WEEKLY_INSTANCE_SCHEDULES.SCHEDULE_DATE, schedule.getScheduleDate())
                .set(WEEKLY_INSTANCE_SCHEDULES.START_TIME, schedule.getStartTime())
                .set(WEEKLY_INSTANCE_SCHEDULES.END_TIME, schedule.getEndTime())
                .set(WEEKLY_INSTANCE_SCHEDULES.NOTE, schedule.getNote())
                .set(WEEKLY_INSTANCE_SCHEDULES.CREATED_AT, schedule.getCreatedAt())
                .set(WEEKLY_INSTANCE_SCHEDULES.UPDATED_AT, schedule.getUpdatedAt())
                .set(WEEKLY_INSTANCE_SCHEDULES.IS_TRIAL, isTrial ? (byte) 1 : null)
                .set(WEEKLY_INSTANCE_SCHEDULES.CUSTOMER_ID, customerId)
                .execute();
        
        // 获取插入的ID
        Long scheduleId = dsl.select(field("LAST_INSERT_ID()", Long.class)).fetchOne(0, Long.class);
        
        System.out.println("插入周实例课程成功，ID: " + scheduleId + ", isTrial: " + isTrial + ", customerId: " + customerId);
        
        return scheduleId;
    }

    /**
     * 插入课程到课表（带is_trial标志和customer_id）
     * @return 插入的课程ID
     */
    public Long insertSchedule(Schedules schedule, boolean isTrial, Long customerId) {
        // 在插入时直接设置is_trial和customer_id字段
        dsl.insertInto(SCHEDULES)
                .set(SCHEDULES.TIMETABLE_ID, schedule.getTimetableId())
                .set(SCHEDULES.STUDENT_NAME, schedule.getStudentName())
                .set(SCHEDULES.DAY_OF_WEEK, schedule.getDayOfWeek())
                .set(SCHEDULES.SCHEDULE_DATE, schedule.getScheduleDate())
                .set(SCHEDULES.START_TIME, schedule.getStartTime())
                .set(SCHEDULES.END_TIME, schedule.getEndTime())
                .set(SCHEDULES.NOTE, schedule.getNote())
                .set(SCHEDULES.CREATED_AT, schedule.getCreatedAt())
                .set(SCHEDULES.UPDATED_AT, schedule.getUpdatedAt())
                .set(SCHEDULES.IS_TRIAL, isTrial ? (byte) 1 : null)
                .set(SCHEDULES.CUSTOMER_ID, customerId)
                .execute();
        
        // 获取插入的ID
        Long scheduleId = dsl.select(field("LAST_INSERT_ID()", Long.class)).fetchOne(0, Long.class);
        
        System.out.println("插入课表课程成功，ID: " + scheduleId + ", isTrial: " + isTrial + ", customerId: " + customerId);
        
        return scheduleId;
    }

    /**
     * 查询学生的体验课程
     */
    public List<java.util.Map<String, Object>> findTrialSchedulesByStudentName(String studentName) {
        String sql = "SELECT s.*, t.user_id as coach_id " +
                    "FROM schedules s " +
                    "LEFT JOIN timetables t ON s.timetable_id = t.id " +
                    "WHERE s.student_name = ? AND s.is_trial = 1 AND s.schedule_date IS NOT NULL " +
                    "ORDER BY s.schedule_date DESC, s.start_time DESC " +
                    "LIMIT 10";
        return dsl.fetch(sql, studentName).intoMaps();
    }

    /**
     * 查询周实例中学生的体验课程
     */
    public List<java.util.Map<String, Object>> findTrialSchedulesInInstancesByStudentName(String studentName) {
        String sql = "SELECT wis.*, wi.timetable_id, t.user_id as coach_id " +
                    "FROM weekly_instance_schedules wis " +
                    "LEFT JOIN weekly_instances wi ON wis.weekly_instance_id = wi.id " +
                    "LEFT JOIN timetables t ON wi.timetable_id = t.id " +
                    "WHERE wis.student_name = ? AND wis.is_trial = 1 AND wis.schedule_date IS NOT NULL " +
                    "ORDER BY wis.schedule_date DESC, wis.start_time DESC " +
                    "LIMIT 10";
        return dsl.fetch(sql, studentName).intoMaps();
    }

    /**
     * 执行原生SQL查询并返回Map列表
     */
    public List<java.util.Map<String, Object>> queryForMaps(String sql, Object... params) {
        return dsl.fetch(sql, params).intoMaps();
    }

    // 可根据业务扩展更多jOOQ查询
}