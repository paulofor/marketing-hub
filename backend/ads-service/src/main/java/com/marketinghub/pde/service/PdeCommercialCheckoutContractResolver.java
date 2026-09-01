package com.marketinghub.pde.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.product.Product;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Responsabilidade: resolver e validar o checkout versionado declarado pelo contrato PDE. */
@Service
public class PdeCommercialCheckoutContractResolver {
  private static final Logger log =
      LoggerFactory.getLogger(PdeCommercialCheckoutContractResolver.class);
  private final ObjectMapper objectMapper;

  /** Inicializa a leitura do contrato canônico persistido no produto. */
  public PdeCommercialCheckoutContractResolver(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /** Retorna o checkout versionado quando o produto o declara e rejeita contrato parcial. */
  public Optional<CanonicalCheckout> resolve(Product product) {
    if (product == null || !StringUtils.hasText(product.getPdeExperienceJson())) {
      return Optional.empty();
    }
    try {
      JsonNode root = objectMapper.readTree(product.getPdeExperienceJson());
      if (root == null || !root.isObject()) {
        throw invalid(product, "contrato PDE precisa ser um objeto JSON");
      }
      JsonNode checkout = root.path("commercialCheckout");
      if (checkout.isMissingNode() || checkout.isNull()) return Optional.empty();
      if (!checkout.isObject()) {
        throw invalid(product, "commercialCheckout precisa ser um objeto");
      }
      String provider = requiredText(product, checkout, "provider");
      String checkoutUrl = requiredText(product, checkout, "checkoutUrl");
      String offerReference = requiredText(product, checkout, "offerReference");
      String currency = requiredText(product, checkout, "currency");
      String billingModel = requiredText(product, checkout, "billingModel");
      if (!checkout.path("priceBrl").isNumber()) {
        throw invalid(product, "priceBrl precisa ser numérico");
      }
      BigDecimal priceBrl = checkout.path("priceBrl").decimalValue();
      if (!isSecurePublicUrl(checkoutUrl)) {
        throw invalid(product, "checkoutUrl precisa usar HTTPS");
      }
      if (!"BRL".equals(currency) || !"ONE_TIME".equals(billingModel) || priceBrl.signum() <= 0) {
        throw invalid(product, "moeda, cobrança ou preço do checkout são inválidos");
      }
      return Optional.of(
          new CanonicalCheckout(
              provider, checkoutUrl, offerReference, priceBrl, currency, billingModel));
    } catch (JsonProcessingException ex) {
      log.error(
          "Falha ao ler checkout do contrato PDE. productId={} productSlug={}",
          product.getId(),
          product.getSlug(),
          ex);
      throw invalid(product, "JSON do contrato PDE é inválido", ex);
    }
  }

  /** Confirma protocolo HTTPS e host explícito antes de publicar o destino de pagamento. */
  private boolean isSecurePublicUrl(String checkoutUrl) {
    try {
      URI uri = new URI(checkoutUrl);
      return "https".equalsIgnoreCase(uri.getScheme())
          && StringUtils.hasText(uri.getHost())
          && uri.getUserInfo() == null;
    } catch (URISyntaxException ex) {
      return false;
    }
  }

  /** Lê texto obrigatório sem aceitar aliases ou valor vazio. */
  private String requiredText(Product product, JsonNode checkout, String field) {
    JsonNode value = checkout.path(field);
    if (!value.isTextual() || !StringUtils.hasText(value.asText())) {
      throw invalid(product, "campo obrigatório ausente: " + field);
    }
    return value.asText().trim();
  }

  /** Cria falha contextualizada para impedir oferta ou tarefa com contrato comercial parcial. */
  private IllegalStateException invalid(Product product, String reason) {
    return invalid(product, reason, null);
  }

  /** Preserva a causa original quando o JSON do contrato não pode ser interpretado. */
  private IllegalStateException invalid(Product product, String reason, Throwable cause) {
    return new IllegalStateException(
        "Checkout canônico PDE inválido para o produto " + product.getId() + ": " + reason, cause);
  }

  /** Representa a identidade comercial imutável que acompanha a versão da experiência. */
  public record CanonicalCheckout(
      String provider,
      String checkoutUrl,
      String offerReference,
      BigDecimal priceBrl,
      String currency,
      String billingModel) {}
}
