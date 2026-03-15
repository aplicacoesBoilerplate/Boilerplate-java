package com.java.boilerplate.dto;

import java.util.Map;

public record DTOParamsSendingEmail(
    String to,
    String subject,
    String templateName,
    Map<String, Object> variables
) {}
