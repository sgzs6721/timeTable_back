package com.example.timetable.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParseResultDto {
    private List<ScheduleCreateDto> schedulesToConfirm;
    private List<ConflictDto> conflicts;
} 