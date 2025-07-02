package com.timetable.dto.ai;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class ChatResponse {
    private final List<ChatResponseChoice> choices;

    @JsonCreator
    public ChatResponse(@JsonProperty("choices") List<ChatResponseChoice> choices) {
        this.choices = choices;
    }

    public List<ChatResponseChoice> getChoices() {
        return choices;
    }

    public String getFirstChoiceContent() {
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        ChatResponseChoice firstChoice = choices.get(0);
        if (firstChoice == null || firstChoice.getMessage() == null) {
            return null;
        }
        return firstChoice.getMessage().getContent();
    }
} 