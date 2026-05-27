package com.university.campustix.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private Long eventId;
    private String message;
}
