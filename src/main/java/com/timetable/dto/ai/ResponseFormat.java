package com.timetable.dto.ai;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ResponseFormat {
    private final String type;

    @JsonCreator
    public ResponseFormat(@JsonProperty("type") String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
} 