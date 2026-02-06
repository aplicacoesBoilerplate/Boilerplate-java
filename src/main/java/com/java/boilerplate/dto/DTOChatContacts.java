package com.java.boilerplate.dto;

import com.fasterxml.jackson.annotation.JsonView;
import com.java.boilerplate.dto.users.DTOUser;
import com.java.boilerplate.dto.users.UserViews;

public record DTOChatContacts(
        Long idChatContact,

        @JsonView(UserViews.Public.class)
        DTOUser user,

        @JsonView(UserViews.Public.class)
        DTOUser contact,

        Boolean contactBlocked
) { }
