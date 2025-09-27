package com.timetable.dto;

import com.timetable.generated.tables.pojos.Schedules;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 包含教练信息的课程DTO
 */
public class ScheduleWithCoachDTO extends Schedules {
    private String coachName;

    public ScheduleWithCoachDTO() {
        super();
    }

    public ScheduleWithCoachDTO(Schedules schedule, String coachName) {
        super(schedule);
        this.coachName = coachName;
    }

    public String getCoachName() {
        return coachName;
    }

    public void setCoachName(String coachName) {
        this.coachName = coachName;
    }
}
