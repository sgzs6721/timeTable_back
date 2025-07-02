package com.timetable.dto.ai;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ChatRequest {
    private final String model;
    private final List<ChatMessage> messages;
    private final ResponseFormat response_format;

    @JsonCreator
    public ChatRequest(@JsonProperty("model") String model,
                       @JsonProperty("messages") List<ChatMessage> messages,
                       @JsonProperty("response_format") ResponseFormat response_format) {
        this.model = model;
        this.messages = messages;
        this.response_format = response_format;
    }

    public String getModel() {
        return model;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public ResponseFormat getResponse_format() {
        return response_format;
    }
} 