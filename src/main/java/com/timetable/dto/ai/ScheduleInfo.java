package com.timetable.dto.ai;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ScheduleInfo(
    String type, // "weekly" or "dated"
    String studentName,
    String time,
    String dayOfWeek,
    String date
) {} 