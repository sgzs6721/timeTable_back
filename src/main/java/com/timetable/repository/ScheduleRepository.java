package com.timetable.repository;

import com.timetable.model.Schedule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 课程安排仓储
 */
@Repository
public class ScheduleRepository {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private final RowMapper<Schedule> scheduleRowMapper = new RowMapper<Schedule>() {
        @Override
        public Schedule mapRow(ResultSet rs, int rowNum) throws SQLException {
            Schedule schedule = new Schedule();
            schedule.setId(rs.getLong("id"));
            schedule.setTimetableId(rs.getLong("timetable_id"));
            schedule.setStudentName(rs.getString("student_name"));
            schedule.setSubject(rs.getString("subject"));
            schedule.setDayOfWeek(Schedule.DayOfWeek.valueOf(rs.getString("day_of_week")));
            schedule.setStartTime(rs.getTime("start_time").toLocalTime());
            schedule.setEndTime(rs.getTime("end_time").toLocalTime());
            schedule.setWeekNumber(rs.getObject("week_number", Integer.class));
            if (rs.getDate("schedule_date") != null) {
                schedule.setScheduleDate(rs.getDate("schedule_date").toLocalDate());
            }
            schedule.setNote(rs.getString("note"));
            schedule.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            schedule.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            return schedule;
        }
    };
    
    public List<Schedule> findByTimetableId(Long timetableId) {
        String sql = "SELECT * FROM schedules WHERE timetable_id = ? ORDER BY day_of_week, start_time";
        return jdbcTemplate.query(sql, scheduleRowMapper, timetableId);
    }
    
    public List<Schedule> findByTimetableIdAndWeekNumber(Long timetableId, Integer weekNumber) {
        String sql = "SELECT * FROM schedules WHERE timetable_id = ? AND week_number = ? ORDER BY day_of_week, start_time";
        return jdbcTemplate.query(sql, scheduleRowMapper, timetableId, weekNumber);
    }
    
    public Schedule findById(Long id) {
        String sql = "SELECT * FROM schedules WHERE id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, scheduleRowMapper, id);
        } catch (Exception e) {
            return null;
        }
    }
    
    public List<Schedule> saveAll(List<Schedule> schedules) {
        for (Schedule schedule : schedules) {
            save(schedule);
        }
        return schedules;
    }
    
    public Schedule save(Schedule schedule) {
        if (schedule.getId() == null) {
            return insert(schedule);
        } else {
            return update(schedule);
        }
    }
    
    public void delete(Long id) {
        String sql = "DELETE FROM schedules WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
    
    private Schedule insert(Schedule schedule) {
        String sql = "INSERT INTO schedules (timetable_id, student_name, subject, day_of_week, start_time, end_time, week_number, schedule_date, note, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        LocalDateTime now = LocalDateTime.now();
        
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, schedule.getTimetableId());
            ps.setString(2, schedule.getStudentName());
            ps.setString(3, schedule.getSubject());
            ps.setString(4, schedule.getDayOfWeek().name());
            ps.setTime(5, java.sql.Time.valueOf(schedule.getStartTime()));
            ps.setTime(6, java.sql.Time.valueOf(schedule.getEndTime()));
            ps.setObject(7, schedule.getWeekNumber());
            ps.setDate(8, schedule.getScheduleDate() != null ? java.sql.Date.valueOf(schedule.getScheduleDate()) : null);
            ps.setString(9, schedule.getNote());
            ps.setObject(10, now);
            ps.setObject(11, now);
            return ps;
        }, keyHolder);
        
        schedule.setId(keyHolder.getKey().longValue());
        schedule.setCreatedAt(now);
        schedule.setUpdatedAt(now);
        
        return schedule;
    }
    
    private Schedule update(Schedule schedule) {
        String sql = "UPDATE schedules SET timetable_id = ?, student_name = ?, subject = ?, day_of_week = ?, start_time = ?, end_time = ?, week_number = ?, schedule_date = ?, note = ?, updated_at = ? WHERE id = ?";
        
        LocalDateTime now = LocalDateTime.now();
        
        jdbcTemplate.update(sql,
                schedule.getTimetableId(),
                schedule.getStudentName(),
                schedule.getSubject(),
                schedule.getDayOfWeek().name(),
                java.sql.Time.valueOf(schedule.getStartTime()),
                java.sql.Time.valueOf(schedule.getEndTime()),
                schedule.getWeekNumber(),
                schedule.getScheduleDate() != null ? java.sql.Date.valueOf(schedule.getScheduleDate()) : null,
                schedule.getNote(),
                now,
                schedule.getId());
        
        schedule.setUpdatedAt(now);
        return schedule;
    }
} 