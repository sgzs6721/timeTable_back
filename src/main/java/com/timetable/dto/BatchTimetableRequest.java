package com.timetable.dto;

import javax.validation.constraints.NotEmpty;
import java.util.List;

public class BatchTimetableRequest {

    @NotEmpty
    private List<Long> ids;

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }
} 