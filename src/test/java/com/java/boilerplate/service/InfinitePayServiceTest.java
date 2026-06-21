package com.java.boilerplate.service;

import com.java.boilerplate.config.TokensProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InfinitePayServiceTest {

    @Test
    void resolveLinksEndpointAddsLinksPathToBaseEndpoint() {
        TokensProperties tokensProperties = new TokensProperties();
        tokensProperties.setInfinitePayEndpoint("https://api.checkout.infinitepay.io");

        InfinitePayService service = new InfinitePayService(tokensProperties, null, null, null, null);

        assertEquals("https://api.checkout.infinitepay.io/links", service.resolveLinksEndpoint());
    }

    @Test
    void resolveLinksEndpointKeepsFullLinksEndpoint() {
        TokensProperties tokensProperties = new TokensProperties();
        tokensProperties.setInfinitePayEndpoint("https://api.checkout.infinitepay.io/links/");

        InfinitePayService service = new InfinitePayService(tokensProperties, null, null, null, null);

        assertEquals("https://api.checkout.infinitepay.io/links", service.resolveLinksEndpoint());
    }
}
