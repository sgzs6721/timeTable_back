package com.timetable.service;

import com.timetable.dto.ScheduleRequest;
import com.timetable.model.Schedule;
import com.timetable.repository.ScheduleRepository;
import com.timetable.repository.TimetableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 排课服务
 */
@Service
public class ScheduleService {
    
    @Autowired
    private ScheduleRepository scheduleRepository;
    
    @Autowired
    private TimetableRepository timetableRepository;
    
    /**
     * 获取课表的排课列表
     */
    public List<Schedule> getTimetableSchedules(Long timetableId, Integer week) {
        if (week != null) {
            return scheduleRepository.findByTimetableIdAndWeekNumber(timetableId, week);
        }
        return scheduleRepository.findByTimetableId(timetableId);
    }
    
    /**
     * 创建新排课
     */
    public Schedule createSchedule(Long timetableId, ScheduleRequest request) {
        Schedule schedule = new Schedule();
        schedule.setTimetableId(timetableId);
        schedule.setStudentName(request.getStudentName());
        schedule.setSubject(request.getSubject());
        schedule.setDayOfWeek(request.getDayOfWeek());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setWeekNumber(request.getWeekNumber());
        schedule.setScheduleDate(request.getScheduleDate());
        schedule.setNote(request.getNote());
        
        return scheduleRepository.save(schedule);
    }
    
    /**
     * 更新排课
     */
    public Schedule updateSchedule(Long timetableId, Long scheduleId, ScheduleRequest request) {
        Schedule schedule = scheduleRepository.findByIdAndTimetableId(scheduleId, timetableId);
        if (schedule == null) {
            return null;
        }
        
        schedule.setStudentName(request.getStudentName());
        schedule.setSubject(request.getSubject());
        schedule.setDayOfWeek(request.getDayOfWeek());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setWeekNumber(request.getWeekNumber());
        schedule.setScheduleDate(request.getScheduleDate());
        schedule.setNote(request.getNote());
        schedule.setUpdatedAt(LocalDateTime.now());
        
        return scheduleRepository.save(schedule);
    }
    
    /**
     * 删除排课
     */
    public boolean deleteSchedule(Long timetableId, Long scheduleId) {
        if (!scheduleRepository.existsByIdAndTimetableId(scheduleId, timetableId)) {
            return false;
        }
        
        scheduleRepository.deleteById(scheduleId);
        return true;
    }
    
    /**
     * 通过语音输入创建排课
     */
    public Schedule createScheduleByVoice(Long timetableId, byte[] audioData) {
        // TODO: 实现语音识别逻辑
        // 这里先返回一个示例排课，实际应该调用AI服务进行语音识别和解析
        
        Schedule schedule = new Schedule();
        schedule.setTimetableId(timetableId);
        schedule.setStudentName("语音识别学生");
        schedule.setSubject("语音识别课程");
        schedule.setDayOfWeek(Schedule.DayOfWeek.MONDAY);
        schedule.setStartTime(java.time.LocalTime.of(9, 0));
        schedule.setEndTime(java.time.LocalTime.of(10, 0));
        schedule.setNote("通过语音输入创建");
        
        return scheduleRepository.save(schedule);
    }
    
    /**
     * 通过文本输入创建排课
     */
    public Schedule createScheduleByText(Long timetableId, String text) {
        // TODO: 实现自然语言处理逻辑
        // 这里先返回一个示例排课，实际应该调用AI服务进行文本解析
        
        Schedule schedule = new Schedule();
        schedule.setTimetableId(timetableId);
        schedule.setStudentName("文本解析学生");
        schedule.setSubject("文本解析课程");
        schedule.setDayOfWeek(Schedule.DayOfWeek.TUESDAY);
        schedule.setStartTime(java.time.LocalTime.of(14, 0));
        schedule.setEndTime(java.time.LocalTime.of(15, 0));
        schedule.setNote("通过文本输入创建: " + text);
        
        return scheduleRepository.save(schedule);
    }
    
    /**
     * 检查排课是否属于指定课表
     */
    public boolean isScheduleInTimetable(Long scheduleId, Long timetableId) {
        return scheduleRepository.existsByIdAndTimetableId(scheduleId, timetableId);
    }
} 