package com.timetable.dto.ai;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ChatRequest {
    private final String model;
    private final List<ChatMessage> messages;

    @JsonCreator
    public ChatRequest(@JsonProperty("model") String model,
                       @JsonProperty("messages") List<ChatMessage> messages) {
        this.model = model;
        this.messages = messages;
    }

    public String getModel() {
        return model;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }
} 