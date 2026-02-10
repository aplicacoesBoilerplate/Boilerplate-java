package com.java.boilerplate.dto;

import com.java.boilerplate.model.ChatContacts;

public record DTOChatContactResponse(
        Long idUser,
        Long idContact,
        String username,
        String avatarUrl
) {
    public DTOChatContactResponse(ChatContacts contact) {
        this(
                contact.getIdChatContact(),
                contact.getContact().getIdUser(),
                contact.getContact().getUserUsername(),
                contact.getContact().getAvatarUrl()
        );
    }
}
