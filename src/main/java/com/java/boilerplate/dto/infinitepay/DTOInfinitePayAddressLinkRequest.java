package com.java.boilerplate.dto.infinitepay;

public record DTOInfinitePayAddressLinkRequest(
        String cep,
        String street,
        String neighborhood,
        String number,
        String complement
) {}
