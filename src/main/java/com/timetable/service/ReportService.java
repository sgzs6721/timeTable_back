package com.timetable.service;

import com.timetable.generated.tables.pojos.Schedules;
import com.timetable.dto.ScheduleWithCoachDTO;
import com.timetable.repository.ReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    @Autowired
    private ReportRepository reportRepository;

    public Map<String, Object> queryHoursPaged(Long userId, LocalDate start, LocalDate end, int page, int size) {
        List<ScheduleWithCoachDTO> list = reportRepository.querySchedulesByUserPaged(userId, start, end, page, size);
        long total = reportRepository.countSchedulesByUser(userId, start, end);
        
        // 计算总课时数（包括半小时课程按0.5计算）
        double totalHours = 0.0;
        for (ScheduleWithCoachDTO schedule : list) {
            if (schedule.getStartTime() != null && schedule.getEndTime() != null) {
                long durationMinutes = java.time.Duration.between(schedule.getStartTime(), schedule.getEndTime()).toMinutes();
                totalHours += durationMinutes / 60.0; // 转换为小时，支持小数
            } else {
                totalHours += 1.0; // 如果没有时间信息，默认1课时
            }
        }
        
        // 计算所有记录的课时总数（用于总计显示）
        List<ScheduleWithCoachDTO> allSchedules = reportRepository.querySchedulesByUserPaged(userId, start, end, 1, Integer.MAX_VALUE);
        double grandTotalHours = 0.0;
        for (ScheduleWithCoachDTO schedule : allSchedules) {
            if (schedule.getStartTime() != null && schedule.getEndTime() != null) {
                long durationMinutes = java.time.Duration.between(schedule.getStartTime(), schedule.getEndTime()).toMinutes();
                grandTotalHours += durationMinutes / 60.0;
            } else {
                grandTotalHours += 1.0;
            }
        }
        
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);
        data.put("totalHours", totalHours); // 当前页课时数
        data.put("grandTotalHours", grandTotalHours); // 总计课时数
        return data;
    }
}


