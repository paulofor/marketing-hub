package com.marketinghub.payments.integration.mercadopago;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record MercadoPagoPreferenceDetails(
        String id,
        String status,
        @JsonProperty("init_point") String initPoint,
        @JsonProperty("expiration_date_to") Instant expirationDateTo
) {
}
