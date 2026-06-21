package com.java.boilerplate.dto.googleOAuth;

import com.java.boilerplate.enums.GenderUser;
import jakarta.validation.constraints.NotBlank;

public record DTOGoogleComplete(
        @NotBlank(message = "Token temporário é obrigatório") String tempToken,
        @NotBlank(message = "O campo do username é obrigatório") String username,
        @NotBlank(message = "O campo da senha é obrigatório") String password,
        @NotBlank(message = "O campo de telefone é obrigatório") String phoneNumber,
        GenderUser userGender,
        @NotBlank(message = "O campo de nome é obrigatório") String name,
        String pictureUrl
) {}
