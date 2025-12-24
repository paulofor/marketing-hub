package com.marketinghub.payments.integration.mercadopago;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MercadoPagoPreferenceResponse(
        String id,
        @JsonProperty("init_point") String initPoint
) {
}
