package com.java.boilerplate.dto.chatMessages;

import com.java.boilerplate.model.ChatMessages;

import java.time.LocalDateTime;

public record DTOChatMessages(
        Long idMessage,
        Long senderId,
        Long receiverId,
        String content,
        String fileUrl,
        LocalDateTime timestamp,
        Boolean read
) {
    public static DTOChatMessages fromEntity(ChatMessages chatMessages) {
        return new DTOChatMessages(
                chatMessages.getIdMessage(),
                chatMessages.getSender().getIdUser(),
                chatMessages.getReceiver().getIdUser(),
                chatMessages.getContent(),
                chatMessages.getFileUrl(),
                chatMessages.getTimestamp(),
                chatMessages.getRead()
        );
    }
}
