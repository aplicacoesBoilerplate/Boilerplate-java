package com.java.boilerplate.dto.infinitepay;

public record DTOInfinitePayCustomerLinkRequest(
        String name,
        String email,
        String phone_number
) { }
