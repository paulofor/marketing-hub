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
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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

    public Optional<MercadoPagoPaymentDetails> fetchPayment(String paymentId) {
        try {
            ResponseEntity<JsonNode> response = restClient.get()
                    .uri("/v1/payments/{id}", paymentId)
                    .retrieve()
                    .toEntity(JsonNode.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Optional.of(parsePayment(response.getBody()));
            }
            log.warn("Pagamento {} não retornou corpo válido no Mercado Pago", paymentId);
            return Optional.empty();
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("Pagamento {} não encontrado no Mercado Pago", paymentId);
                return Optional.empty();
            }
            throw new IllegalStateException("Erro ao consultar pagamento no Mercado Pago", ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Erro ao consultar pagamento no Mercado Pago", ex);
        }
    }

    public Optional<MercadoPagoPreferenceDetails> fetchPreference(String preferenceId) {
        try {
            ResponseEntity<JsonNode> response = restClient.get()
                    .uri("/checkout/preferences/{id}", preferenceId)
                    .retrieve()
                    .toEntity(JsonNode.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Optional.of(parsePreference(response.getBody()));
            }
            log.warn("Preferência {} não retornou corpo válido no Mercado Pago", preferenceId);
            return Optional.empty();
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("Preferência {} não encontrada no Mercado Pago", preferenceId);
                return Optional.empty();
            }
            throw new IllegalStateException("Erro ao consultar preferência no Mercado Pago", ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Erro ao consultar preferência no Mercado Pago", ex);
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

    private MercadoPagoPreferenceDetails parsePreference(JsonNode node) {
        String id = node.path("id").asText(null);
        String status = node.path("status").asText(null);
        String initPoint = node.path("init_point").asText(null);
        Instant expiration = parseInstant(node.path("expiration_date_to").asText(null));
        return new MercadoPagoPreferenceDetails(id, status, initPoint, expiration);
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
