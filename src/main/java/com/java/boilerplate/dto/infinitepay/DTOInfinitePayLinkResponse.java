package com.java.boilerplate.dto.infinitepay;

public record DTOInfinitePayLinkResponse(
        String id,
        String url,
        String status,
        String created_at
) {}
