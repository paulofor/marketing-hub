package com.marketinghub.geralanding.agent.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.product.Product;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Responsabilidade: congelar a coerência comercial do checkout entregue aos gates da landing. */
@Service
public class LandingCheckoutEvidenceResolver {
  private static final Logger log = LoggerFactory.getLogger(LandingCheckoutEvidenceResolver.class);
  private static final String VALIDATED = "VALIDATED_FROM_PERSISTED_CANONICAL_BINDING";
  private final LandingCheckoutContractResolver checkoutResolver;
  private final ObjectMapper objectMapper;

  /** Inicializa a evidência com o resolvedor do destino protegido e o leitor do contrato PDE. */
  public LandingCheckoutEvidenceResolver(
      LandingCheckoutContractResolver checkoutResolver, ObjectMapper objectMapper) {
    this.checkoutResolver = checkoutResolver;
    this.objectMapper = objectMapper;
  }

  /**
   * Compara produto, experimento, preço e cobrança e devolve um snapshot auditável sem pagamento.
   */
  public Map<String, Object> resolve(Experiment experiment) {
    Map<String, Object> evidence = new LinkedHashMap<>();
    List<String> blockers = new ArrayList<>();
    Product product = experiment.getProduct();
    String checkoutUrl = checkoutResolver.resolve(experiment);

    evidence.put("evidenceType", "CANONICAL_BACKEND_BINDING");
    evidence.put("experimentId", experiment.getId());
    evidence.put("currency", "BRL");
    evidence.put("externalSideEffects", false);
    evidence.put("liveProviderPreflight", "REQUIRED_IN_CHANNEL_CHECKOUT_ACCESS_EVENTS_INTEGRATION");
    if (StringUtils.hasText(checkoutUrl)) {
      evidence.put("canonicalUrl", checkoutUrl);
      evidence.put("checkoutUrl", checkoutUrl);
    } else {
      blockers.add("Checkout comercial canônico ausente.");
    }
    if (product == null) {
      blockers.add("Experimento sem produto comercial vinculado.");
    } else {
      evidence.put("productId", product.getId());
      putWhenPresent(evidence, "productKey", product.getSlug());
      putWhenPresent(evidence, "productName", product.getName());
      putWhenPresent(evidence, "deliveryPageUrl", product.getPublicUrl());
    }
    if (experiment.getUnitPrice() == null || experiment.getUnitPrice().signum() <= 0) {
      blockers.add("Experimento sem preço comercial positivo.");
    } else {
      evidence.put("amountBrl", experiment.getUnitPrice());
    }
    validatePersistedBinding(experiment, product, evidence, blockers);
    evidence.put("validationStatus", blockers.isEmpty() ? VALIDATED : "BLOCKED");
    evidence.put("blockers", List.copyOf(blockers));
    evidence.put("requiredMarkers", List.of("checkout-cta-primary", "primary-checkout"));
    evidence.put(
        "rule",
        "Todo CTA de compra deve usar literalmente canonicalUrl; são proibidos placeholders e destinos alternativos.");
    return Map.copyOf(evidence);
  }

  /** Valida o binding comercial versionado sem inferir fatos ausentes da preferência externa. */
  private void validatePersistedBinding(
      Experiment experiment, Product product, Map<String, Object> evidence, List<String> blockers) {
    if (product == null || !StringUtils.hasText(product.getPdeExperienceJson())) {
      blockers.add("Produto sem binding comercial PDE persistido.");
      return;
    }
    try {
      JsonNode binding =
          objectMapper.readTree(product.getPdeExperienceJson()).path("commercialBinding");
      if (!binding.isObject()) {
        blockers.add("Contrato PDE sem commercialBinding.");
        return;
      }
      long boundExperimentId = binding.path("experimentId").asLong(-1);
      BigDecimal boundPrice = binding.path("priceBrl").decimalValue();
      String billingModel = binding.path("billingModel").asText();
      evidence.put("billingModel", billingModel);
      evidence.put("bindingSource", "product.pdeExperienceJson.commercialBinding");
      if (experiment.getId() == null || boundExperimentId != experiment.getId()) {
        blockers.add("Binding comercial pertence a outro experimento.");
      }
      if (experiment.getUnitPrice() == null
          || !binding.path("priceBrl").isNumber()
          || experiment.getUnitPrice().compareTo(boundPrice) != 0) {
        blockers.add("Preço do binding comercial diverge do experimento.");
      }
      if (!"ONE_TIME".equals(billingModel)) {
        blockers.add("Modelo de cobrança não comprova pagamento único.");
      }
    } catch (Exception ex) {
      log.error(
          "Falha ao validar binding do checkout da landing. experimentId={} productId={}",
          experiment.getId(),
          product.getId(),
          ex);
      blockers.add("Binding comercial PDE inválido.");
    }
  }

  /** Acrescenta texto persistido somente quando ele possui conteúdo útil. */
  private void putWhenPresent(Map<String, Object> evidence, String key, String value) {
    if (StringUtils.hasText(value)) evidence.put(key, value.trim());
  }

  /** Expõe o status estável aceito pelos revisores de landing. */
  static String validatedStatus() {
    return VALIDATED;
  }
}
