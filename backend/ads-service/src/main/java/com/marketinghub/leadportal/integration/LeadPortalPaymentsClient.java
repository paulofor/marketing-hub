package com.marketinghub.leadportal.integration;

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

/** Integra o backend principal ao serviço oficial de pagamentos do Lead Portal. */
@Component
public class LeadPortalPaymentsClient {

  private static final Logger log = LoggerFactory.getLogger(LeadPortalPaymentsClient.class);

  private final RestTemplate restTemplate;
  private final LeadPortalPaymentsIntegrationProperties properties;

  /** Configura o cliente HTTP com tempo limite e credenciais internas. */
  public LeadPortalPaymentsClient(
      RestTemplateBuilder restTemplateBuilder, LeadPortalPaymentsIntegrationProperties properties) {
    this.properties = properties;
    this.restTemplate =
        restTemplateBuilder
            .setConnectTimeout(properties.getConnectTimeout())
            .setReadTimeout(properties.getReadTimeout())
            .build();
  }

  /** Cria ou recupera o checkout comercial de um pacote. */
  public PaymentCheckoutResponse ensureCheckout(
      long packageId, String buyerEmail, String buyerName) {
    if (!properties.isEnabled()) {
      throw new IllegalStateException("Integração com lead-portal-payments está desabilitada");
    }
    if (!StringUtils.hasText(properties.getBaseUrl())) {
      throw new IllegalStateException("Base URL do lead-portal-payments não configurada");
    }
    HttpHeaders headers = buildHeaders();
    PaymentCheckoutRequest request = new PaymentCheckoutRequest(packageId, buyerEmail, buyerName);
    var uri =
        UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
            .path("/api/v1/payments/checkout")
            .build()
            .toUri();

    log.info(
        "Solicitando checkout para o pacote {} no lead-portal-payments (destino: {}, autenticação: {} token)",
        packageId,
        uri,
        headers.getFirst(HttpHeaders.AUTHORIZATION) != null ? "com" : "sem");
    try {
      ResponseEntity<PaymentCheckoutResponse> response =
          restTemplate.exchange(
              uri,
              HttpMethod.POST,
              new HttpEntity<>(request, headers),
              PaymentCheckoutResponse.class);
      PaymentCheckoutResponse body = response.getBody();
      if (body == null || !StringUtils.hasText(body.checkoutUrl())) {
        log.error(
            "Resposta inválida ao solicitar checkout para o pacote {}: status={}, body={}",
            packageId,
            response.getStatusCode(),
            body);
        throw new IllegalStateException(
            "Serviço de pagamentos retornou payload inválido para o pacote " + packageId);
      }
      log.info(
          "Checkout criado com sucesso para o pacote {}: purchaseId={}, checkoutUrl={} (status HTTP {})",
          packageId,
          body.purchaseId(),
          body.checkoutUrl(),
          response.getStatusCode());
      return body;
    } catch (RestClientResponseException ex) {
      log.error(
          "Falha HTTP ao solicitar checkout para o pacote {}: {}",
          packageId,
          ex.getResponseBodyAsString(),
          ex);
      throw new IllegalStateException(
          "Falha ao solicitar link de pagamento: " + ex.getStatusText(), ex);
    } catch (RestClientException ex) {
      log.error("Erro ao solicitar checkout para o pacote {}", packageId, ex);
      throw new IllegalStateException("Erro ao solicitar link de pagamento", ex);
    }
  }

  /** Ativa um checkout temporário por meio do serviço oficial de pagamentos. */
  public TemporaryCheckoutResponse activateTemporaryCheckout(TemporaryCheckoutRequest request) {
    return exchangeTemporary(
        HttpMethod.POST, "/api/v1/payments/temporary", request, request.productKey());
  }

  /** Consulta a fonte de verdade do checkout temporário. */
  public TemporaryCheckoutResponse getTemporaryCheckout(String productKey) {
    return exchangeTemporary(
        HttpMethod.GET, "/api/v1/payments/temporary/" + productKey, null, productKey);
  }

  /** Restaura imediatamente o checkout comercial do produto. */
  public TemporaryCheckoutResponse restoreTemporaryCheckout(String productKey) {
    return exchangeTemporary(
        HttpMethod.POST, "/api/v1/payments/temporary/" + productKey + "/restore", null, productKey);
  }

  /** Solicita ao serviço de pagamentos um checkout comercial para contrato já validado. */
  public CommercialProductCheckoutResponse createCommercialProductCheckout(
      CommercialProductCheckoutRequest request) {
    if (!properties.isEnabled() || !StringUtils.hasText(properties.getBaseUrl())) {
      throw new IllegalStateException("Integração com lead-portal-payments não configurada");
    }
    var uri =
        UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
            .path("/api/v1/payments/products/checkout")
            .build()
            .toUri();
    try {
      log.info(
          "Enviando checkout comercial ao Lead Portal Payments. productKey={}, experimentId={}, amount={}, deliveryPageUrl={}, endpoint={}",
          request.productKey(),
          request.experimentId(),
          request.amount(),
          request.deliveryPageUrl(),
          uri);
      ResponseEntity<CommercialProductCheckoutResponse> response =
          restTemplate.exchange(
              uri,
              HttpMethod.POST,
              new HttpEntity<>(request, buildHeaders()),
              CommercialProductCheckoutResponse.class);
      CommercialProductCheckoutResponse body = response.getBody();
      if (body == null || !StringUtils.hasText(body.checkoutUrl())) {
        throw new IllegalStateException("Serviço de pagamentos não retornou checkout comercial");
      }
      log.info(
          "Checkout comercial recebido do Lead Portal Payments. productKey={}, experimentId={}, preferenceId={}, status={}, endpoint={}",
          body.productKey(),
          body.experimentId(),
          body.preferenceId(),
          response.getStatusCode(),
          uri);
      return body;
    } catch (RestClientResponseException ex) {
      log.error(
          "Falha HTTP ao criar checkout comercial. productKey={}, experimentId={}, endpoint={}, body={}",
          request.productKey(),
          request.experimentId(),
          uri,
          ex.getResponseBodyAsString(),
          ex);
      throw new IllegalStateException("Falha ao criar checkout comercial", ex);
    } catch (RestClientException ex) {
      log.error(
          "Falha de comunicação ao criar checkout comercial. productKey={}, experimentId={}, endpoint={}",
          request.productKey(),
          request.experimentId(),
          uri,
          ex);
      throw new IllegalStateException("Serviço de pagamentos indisponível", ex);
    }
  }

  private TemporaryCheckoutResponse exchangeTemporary(
      HttpMethod method, String path, TemporaryCheckoutRequest request, String productKey) {
    if (!properties.isEnabled() || !StringUtils.hasText(properties.getBaseUrl())) {
      throw new IllegalStateException("Integração com lead-portal-payments não configurada");
    }
    var uri = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl()).path(path).build().toUri();
    try {
      ResponseEntity<TemporaryCheckoutResponse> response =
          restTemplate.exchange(
              uri,
              method,
              new HttpEntity<>(request, buildHeaders()),
              TemporaryCheckoutResponse.class);
      if (response.getBody() == null) {
        throw new IllegalStateException("Serviço de pagamentos não retornou o checkout temporário");
      }
      return response.getBody();
    } catch (RestClientResponseException ex) {
      log.error(
          "Falha HTTP na operação de checkout temporário. productKey={}, method={}, endpoint={}, body={}",
          productKey,
          method,
          uri,
          ex.getResponseBodyAsString(),
          ex);
      throw new IllegalStateException("Falha ao administrar checkout temporário", ex);
    } catch (RestClientException ex) {
      log.error(
          "Falha de comunicação no checkout temporário. productKey={}, method={}, endpoint={}",
          productKey,
          method,
          uri,
          ex);
      throw new IllegalStateException("Serviço de pagamentos indisponível", ex);
    }
  }

  /** Monta os cabeçalhos internos exigidos pelo serviço de pagamentos. */
  private HttpHeaders buildHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (StringUtils.hasText(properties.getAuthToken())) {
      headers.setBearerAuth(properties.getAuthToken());
    }
    return headers;
  }

  public record PaymentCheckoutRequest(Long packageId, String buyerEmail, String buyerName) {}

  public record PaymentCheckoutResponse(
      Long purchaseId,
      Long packageId,
      String preferenceId,
      String checkoutUrl,
      String status,
      BigDecimal amount,
      String currency,
      Instant expiresAt,
      String statementDescriptor) {}

  public record TemporaryCheckoutRequest(
      String productKey,
      String productName,
      BigDecimal testAmount,
      String commercialCheckoutUrl,
      Integer durationMinutes) {}

  public record TemporaryCheckoutResponse(
      String productKey,
      String productName,
      String redirectUrl,
      String temporaryCheckoutUrl,
      String commercialCheckoutUrl,
      BigDecimal testAmount,
      String status,
      Instant activatedAt,
      Instant expiresAt) {}

  /** Contrato interno enviado para criar checkout de produto digital. */
  public record CommercialProductCheckoutRequest(
      String productKey,
      String productName,
      Long productId,
      Long experimentId,
      BigDecimal amount,
      String deliveryPageUrl) {}

  /** Checkout comercial retornado pelo serviço oficial de pagamentos. */
  public record CommercialProductCheckoutResponse(
      String productKey,
      Long productId,
      Long experimentId,
      String preferenceId,
      String checkoutUrl,
      BigDecimal amount,
      String currency,
      String deliveryPageUrl) {}
}
