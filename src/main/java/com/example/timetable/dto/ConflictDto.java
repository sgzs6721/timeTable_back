package com.example.timetable.dto;

import com.example.timetable.entity.Schedule;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConflictDto {
    private ScheduleCreateDto newSchedule;
    private Schedule existingSchedule;
} 