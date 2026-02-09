package com.java.boilerplate.dto.infinitepay;

import java.util.Map;

public record DTOInfinitePayLinkRequest(
        Integer amount,
        String description,
        Map<String, String> metadata
) {}
