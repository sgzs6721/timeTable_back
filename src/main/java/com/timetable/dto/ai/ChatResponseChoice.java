package com.timetable.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Ignoring unknown properties to be more robust
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatResponseChoice(ChatMessage message) {
} 