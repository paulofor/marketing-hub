package com.marketinghub.emailservice.leadportal.integration;

import java.math.BigDecimal;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class LeadPortalPaymentsClient {

    private static final Logger log = LoggerFactory.getLogger(LeadPortalPaymentsClient.class);

    private final RestTemplate restTemplate;
    private final LeadPortalPaymentsIntegrationProperties properties;

    public LeadPortalPaymentsClient(RestTemplateBuilder restTemplateBuilder,
                                    LeadPortalPaymentsIntegrationProperties properties) {
        this.properties = properties;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(properties.getConnectTimeout())
                .setReadTimeout(properties.getReadTimeout())
                .build();
    }

    public PaymentCheckoutResponse ensureCheckout(long packageId, String buyerEmail, String buyerName) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("Integração com lead-portal-payments está desabilitada");
        }
        if (!StringUtils.hasText(properties.getBaseUrl())) {
            throw new IllegalStateException("Base URL do lead-portal-payments não configurada");
        }
        HttpHeaders headers = buildHeaders();
        PaymentCheckoutRequest request = new PaymentCheckoutRequest(packageId, buyerEmail, buyerName);
        var uri = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                .path("/api/v1/payments/checkout")
                .build()
                .toUri();

        log.info("Solicitando checkout para o pacote {} no lead-portal-payments (destino: {}, autenticação: {} token)",
                packageId,
                uri,
                headers.getFirst(HttpHeaders.AUTHORIZATION) != null ? "com" : "sem");
        try {
            ResponseEntity<PaymentCheckoutResponse> response = restTemplate.exchange(
                    uri,
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    PaymentCheckoutResponse.class);
            PaymentCheckoutResponse body = response.getBody();
            if (body == null || !StringUtils.hasText(body.checkoutUrl())) {
                log.error("Resposta inválida ao solicitar checkout para o pacote {}: status={}, body={}",
                        packageId, response.getStatusCode(), body);
                throw new IllegalStateException("Serviço de pagamentos retornou payload inválido para o pacote " + packageId);
            }
            log.info("Checkout criado com sucesso para o pacote {}: purchaseId={}, checkoutUrl={} (status HTTP {})",
                    packageId, body.purchaseId(), body.checkoutUrl(), response.getStatusCode());
            return body;
        } catch (RestClientResponseException ex) {
            log.error("Falha HTTP ao solicitar checkout para o pacote {}: {}", packageId, ex.getResponseBodyAsString(), ex);
            throw new IllegalStateException("Falha ao solicitar link de pagamento: " + ex.getStatusText(), ex);
        } catch (RestClientException ex) {
            log.error("Erro ao solicitar checkout para o pacote {}", packageId, ex);
            throw new IllegalStateException("Erro ao solicitar link de pagamento", ex);
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.hasText(properties.getAuthToken())) {
            headers.setBearerAuth(properties.getAuthToken());
        }
        return headers;
    }

    public record PaymentCheckoutRequest(Long packageId, String buyerEmail, String buyerName) {
    }

    public record PaymentCheckoutResponse(
            Long purchaseId,
            Long packageId,
            String preferenceId,
            String checkoutUrl,
            String status,
            BigDecimal amount,
            String currency,
            Instant expiresAt,
            String statementDescriptor
    ) {
    }
}
