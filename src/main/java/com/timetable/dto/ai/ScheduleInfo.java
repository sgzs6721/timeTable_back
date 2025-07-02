package com.timetable.dto.ai;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ScheduleInfo {
    private final String studentName;
    private final String time;
    private final String dayOfWeek;
    private final String date;
    private final String errorMessage;

    @JsonCreator
    public ScheduleInfo(@JsonProperty("studentName") String studentName,
                        @JsonProperty("time") String time,
                        @JsonProperty("dayOfWeek") String dayOfWeek,
                        @JsonProperty("date") String date,
                        @JsonProperty("errorMessage") String errorMessage) {
        this.studentName = studentName;
        this.time = time;
        this.dayOfWeek = dayOfWeek;
        this.date = date;
        this.errorMessage = errorMessage;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getTime() {
        return time;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public String getDate() {
        return date;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
} 