package com.marketinghub.payments.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/** Valida a minimização de dados pessoais na auditoria financeira persistida. */
class PaymentAuditPayloadSanitizerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PaymentAuditPayloadSanitizer sanitizer = new PaymentAuditPayloadSanitizer(objectMapper);

    /** Remove e-mails aninhados sem apagar os identificadores técnicos do pagamento. */
    @Test
    void redactsEveryEmailFieldAndPreservesTechnicalEvidence() throws Exception {
        String rawPayload = """
                {
                  "id": "payment-123",
                  "status": "approved",
                  "payer": {"email": "payer@example.com"},
                  "metadata": {
                    "submission_email": "submission@example.com",
                    "buyerEmail": "buyer@example.com",
                    "experimentId": 89
                  },
                  "recipients": [{"recipient-email": "recipient@example.com"}]
                }
                """;

        String minimized = sanitizer.minimize(rawPayload);
        JsonNode result = objectMapper.readTree(minimized);

        assertThat(result.path("id").asText()).isEqualTo("payment-123");
        assertThat(result.path("status").asText()).isEqualTo("approved");
        assertThat(result.path("metadata").path("experimentId").asInt()).isEqualTo(89);
        assertThat(minimized)
                .contains("[REDACTED]")
                .doesNotContain(
                        "payer@example.com",
                        "submission@example.com",
                        "buyer@example.com",
                        "recipient@example.com");
    }

    /** Substitui conteúdo ilegível por hash para nunca persistir e-mail fora de um JSON válido. */
    @Test
    void replacesMalformedPayloadWithIntegrityHashOnly() {
        String minimized = sanitizer.minimize("payment=123&email=buyer@example.com");

        assertThat(minimized)
                .contains("UNPARSEABLE_REDACTED", "sha256")
                .doesNotContain("buyer@example.com", "payment=123");
    }
}
