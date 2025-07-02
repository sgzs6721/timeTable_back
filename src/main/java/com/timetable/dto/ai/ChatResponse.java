package com.timetable.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatResponse(List<ChatResponseChoice> choices) {
    public String getFirstChoiceContent() {
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        ChatResponseChoice firstChoice = choices.get(0);
        if (firstChoice == null || firstChoice.message() == null) {
            return null;
        }
        return firstChoice.message().content();
    }
} 