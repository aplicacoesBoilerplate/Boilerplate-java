package com.java.boilerplate.dto.common;

import java.util.Map;

public record RParamsSendingEmail(
        String to,
        String subject,
        String templateName,
        Map<String, Object> variables
) {
}
