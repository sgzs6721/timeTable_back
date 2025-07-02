package com.timetable.dto.ai;

import java.util.List;

public record ChatRequest(String model, List<ChatMessage> messages) {
} 