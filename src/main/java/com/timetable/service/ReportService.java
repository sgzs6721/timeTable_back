package com.timetable.service;

import com.timetable.generated.tables.pojos.Schedules;
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
        List<Schedules> list = reportRepository.querySchedulesByUserPaged(userId, start, end, page, size);
        long total = reportRepository.countSchedulesByUser(userId, start, end);
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);
        return data;
    }
}


