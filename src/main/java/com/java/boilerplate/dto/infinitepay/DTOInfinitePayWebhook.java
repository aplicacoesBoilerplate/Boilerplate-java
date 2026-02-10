package com.java.boilerplate.dto.infinitepay;

import java.util.Map;

public record DTOInfinitePayWebhook(
        String type,
        Data data
) {
    public record Data(
            String id,
            String status,
            Map<String, String> metadata
    ) {}
}