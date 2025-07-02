package com.timetable.dto.ai;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Ignoring unknown properties to be more robust
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ChatResponseChoice {
    private final ChatMessage message;

    @JsonCreator
    public ChatResponseChoice(@JsonProperty("message") ChatMessage message) {
        this.message = message;
    }

    public ChatMessage getMessage() {
        return message;
    }
} 