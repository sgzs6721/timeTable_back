package com.timetable.dto;

import com.timetable.generated.tables.pojos.Schedules;

import java.util.List;

public class SchedulesOverviewResponse {
    private List<Schedules> today;
    private List<Schedules> tomorrow;
    private List<Schedules> thisWeek;
    private List<Schedules> template;

    public List<Schedules> getToday() {
        return today;
    }

    public void setToday(List<Schedules> today) {
        this.today = today;
    }

    public List<Schedules> getTomorrow() {
        return tomorrow;
    }

    public void setTomorrow(List<Schedules> tomorrow) {
        this.tomorrow = tomorrow;
    }

    public List<Schedules> getThisWeek() {
        return thisWeek;
    }

    public void setThisWeek(List<Schedules> thisWeek) {
        this.thisWeek = thisWeek;
    }

    public List<Schedules> getTemplate() {
        return template;
    }

    public void setTemplate(List<Schedules> template) {
        this.template = template;
    }
}


