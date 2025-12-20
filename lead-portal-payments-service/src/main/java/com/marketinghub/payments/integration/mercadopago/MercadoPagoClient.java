package com.marketinghub.payments.integration.mercadopago;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.HttpStatusCodeException;

@Component
public class MercadoPagoClient {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public MercadoPagoClient(RestClient mercadoPagoRestClient, ObjectMapper objectMapper) {
        this.restClient = mercadoPagoRestClient;
        this.objectMapper = objectMapper;
    }

    public MercadoPagoPreferenceResponse createPreference(MercadoPagoPreferenceRequest request) {
        try {
            ResponseEntity<MercadoPagoPreferenceResponse> response = restClient.post()
                    .uri("/checkout/preferences")
                    .body(request)
                    .retrieve()
                    .toEntity(MercadoPagoPreferenceResponse.class);
            return response.getBody();
        } catch (RestClientException ex) {
            log.error("Falha ao criar preferência no Mercado Pago", ex);
            throw new IllegalStateException("Erro ao criar preferência de pagamento", ex);
        }
    }

    public MercadoPagoPaymentDetails fetchPayment(String paymentId) {
        try {
            ResponseEntity<JsonNode> response = restClient.get()
                    .uri("/v1/payments/{id}", paymentId)
                    .retrieve()
                    .toEntity(JsonNode.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parsePayment(response.getBody());
            }
            throw new IllegalStateException("Pagamento não encontrado no Mercado Pago");
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new IllegalStateException("Pagamento não encontrado no Mercado Pago");
            }
            throw new IllegalStateException("Erro ao consultar pagamento no Mercado Pago", ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Erro ao consultar pagamento no Mercado Pago", ex);
        }
    }

    private MercadoPagoPaymentDetails parsePayment(JsonNode node) {
        String id = node.path("id").asText(null);
        String status = node.path("status").asText(null);
        BigDecimal amount = node.hasNonNull("transaction_amount")
                ? node.get("transaction_amount").decimalValue()
                : null;
        String currency = node.path("currency_id").asText(null);
        String description = node.path("description").asText(null);
        String email = node.path("payer").path("email").asText(null);
        Instant approvedAt = parseInstant(node.path("date_approved").asText(null));
        Map<String, Object> metadata = toMap(node.path("metadata"));
        return new MercadoPagoPaymentDetails(id, status, amount, currency, description, email, approvedAt, metadata);
    }

    private Map<String, Object> toMap(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(objectMapper.writeValueAsBytes(node), new TypeReference<Map<String, Object>>() {
            });
        } catch (IOException ex) {
            log.debug("Falha ao converter metadata do pagamento", ex);
            return new HashMap<>();
        }
    }

    private Instant parseInstant(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(raw, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant();
        } catch (Exception ex) {
            return null;
        }
    }
}
