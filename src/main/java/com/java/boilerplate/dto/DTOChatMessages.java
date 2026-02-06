package com.java.boilerplate.dto;

import com.fasterxml.jackson.annotation.JsonView;
import com.java.boilerplate.dto.users.DTOUser;
import com.java.boilerplate.dto.users.UserViews;

import java.time.LocalDateTime;

public record DTOChatMessages(
        Long idMessage,

        @JsonView(UserViews.Public.class)
        DTOUser sender,

        @JsonView(UserViews.Public.class)
        DTOUser receiver,

        String content,
        LocalDateTime timestamp,
        Boolean read
) { }
