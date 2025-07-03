package com.timetable.validation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.timetable.validation.DateRangeValidationGroup;
import com.timetable.validation.WeeklyValidationGroup;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class CreateScheduleRequest {
    @NotBlank(message = "学生姓名不能为空")
    @Size(max = 100, message = "学生姓名不能超过100个字符")
    @JsonProperty("studentName")
    private String studentName;

    @NotBlank(message = "上课时间不能为空")
    @JsonProperty("time")
    private String time;

    @NotNull(message = "周课表的星期几不能为空", groups = WeeklyValidationGroup.class)
    @JsonProperty("dayOfWeek")
    private String dayOfWeek;

    @NotNull(message = "日期课表的日期不能为空", groups = DateRangeValidationGroup.class)
    @JsonProperty("date")
    private String date;

    // Getters and Setters
    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
} 