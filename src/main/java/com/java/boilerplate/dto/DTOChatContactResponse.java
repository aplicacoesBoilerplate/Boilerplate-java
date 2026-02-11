package com.java.boilerplate.dto;

import com.java.boilerplate.model.ChatContacts;

import java.time.LocalDateTime;

public record DTOChatContactResponse(
        Long idChatContact,
        Long idContact,
        String username,
        String avatarUrl,
        String lastMessage,
        LocalDateTime lastMessageTime,
        Long unreadMessages,
        Boolean isOnline
) {
    public DTOChatContactResponse(ChatContacts contact, String lastMessage, LocalDateTime lastMessageTime, Long unreadMessages, Boolean isOnline) {
        this(
                contact.getIdChatContact(),
                contact.getContact().getIdUser(),
                contact.getContact().getUserUsername(),
                contact.getContact().getAvatarUrl(),
                lastMessage,
                lastMessageTime,
                unreadMessages,
                isOnline
        );
    }
}
