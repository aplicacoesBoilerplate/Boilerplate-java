package com.java.boilerplate.dto.infinitepay;

import java.util.List;

public record DTOInfinitePayWebhook(
          String invoice_slug,
          Integer amount,
          Integer paid_amount,
          Integer installments,
          String capture_method,
          String transaction_nsu,
          String order_nsu,
          String receipt_url,
          List<DTOInfinitePayItemsLinkRequest> items
) { }
