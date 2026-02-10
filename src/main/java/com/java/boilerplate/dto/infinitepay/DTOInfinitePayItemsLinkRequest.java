package com.java.boilerplate.dto.infinitepay;

public record DTOInfinitePayItemsLinkRequest(
        Integer quantity,
        Integer price,
        String description
) { }
