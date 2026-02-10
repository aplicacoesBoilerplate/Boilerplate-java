package com.java.boilerplate.dto.infinitepay;

import java.util.List;

public record DTOInfinitePayLinkRequest(
        String handle,
        List<DTOInfinitePayItemsLinkRequest> items,
        String order_nsu,
        String redirect_url,
        String webhook_url,
        DTOInfinitePayCustomerLinkRequest customer
) { }
