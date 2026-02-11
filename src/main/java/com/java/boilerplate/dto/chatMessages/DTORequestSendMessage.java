package com.java.boilerplate.dto.chatMessages;

import jakarta.validation.constraints.NotBlank;

public record DTORequestSendMessage(
        Long receiverId,
        @NotBlank(message = "Message content cannot be empty")
        String content
) { }
