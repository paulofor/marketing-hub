package com.marketinghub.payments.integration.mercadopago;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record MercadoPagoPreferenceRequest(
        List<Item> items,
        Payer payer,
        BackUrls backUrls,
        Map<String, Object> metadata,
        String notificationUrl,
        String statementDescriptor
) {

    public record Item(
            String title,
            Integer quantity,
            @JsonProperty("unit_price") BigDecimal unitPrice,
            @JsonProperty("currency_id") String currencyId) {}

    public record Payer(String name, String email) {}

    public record BackUrls(String success, String failure, String pending) {}
}
