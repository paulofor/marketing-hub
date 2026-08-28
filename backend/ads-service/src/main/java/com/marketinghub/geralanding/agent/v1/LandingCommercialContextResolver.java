package com.marketinghub.geralanding.agent.v1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.product.Product;
import com.marketinghub.product.service.commercialoffer.PublicProductCommercialOfferResponse;
import com.marketinghub.product.service.commercialoffer.PublicProductCommercialOfferService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: montar o contexto comercial e de confiança que limita a criação de uma landing.
 */
@Service
public class LandingCommercialContextResolver {
  private static final Logger log = LoggerFactory.getLogger(LandingCommercialContextResolver.class);

  private final ObjectMapper objectMapper;
  private final PublicProductCommercialOfferService publicOfferService;

  /** Inicializa o resolvedor com o contrato PDE e a oferta pública canônica. */
  public LandingCommercialContextResolver(
      ObjectMapper objectMapper, PublicProductCommercialOfferService publicOfferService) {
    this.objectMapper = objectMapper;
    this.publicOfferService = publicOfferService;
  }

  /**
   * Expõe somente fatos comerciais persistidos e identifica quando o contrato público não existe.
   */
  public Map<String, Object> resolve(Experiment experiment) {
    Product product = experiment.getProduct();
    if (product == null) return Map.of();

    Map<String, Object> context = new LinkedHashMap<>();
    putWhenPresent(context, "targetAudience", product.getTargetAudience());
    putWhenPresent(context, "productFormat", product.getProductFormat());
    putWhenPresent(context, "deliveryMode", product.getDeliveryMode());
    putWhenPresent(context, "valueUnit", product.getValueUnit());
    putWhenPresent(context, "uniqueMechanism", product.getUniqueMechanism());
    putWhenPresent(context, "riskReversal", product.getRiskReversal());
    addExperienceContract(context, experiment, product);
    addPublicTrustContract(context, experiment, product);
    return Map.copyOf(context);
  }

  /** Extrai escopo, processo e provas sem serializar JSON novamente dentro do snapshot. */
  private void addExperienceContract(
      Map<String, Object> context, Experiment experiment, Product product) {
    if (!StringUtils.hasText(product.getPdeExperienceJson())) return;
    try {
      JsonNode contract = objectMapper.readTree(product.getPdeExperienceJson());
      Map<String, Object> experience = new LinkedHashMap<>();
      putNodeWhenPresent(experience, "serviceScope", contract.path("serviceScope"));
      putNodeWhenPresent(experience, "commercialProcess", contract.path("commercialProcess"));
      putNodeWhenPresent(experience, "publicProofs", contract.path("publicProofs"));
      if (!experience.isEmpty()) context.put("serviceExperienceContract", Map.copyOf(experience));
    } catch (JsonProcessingException ex) {
      log.error(
          "Falha ao ler contrato PDE para geração de landing. experimentId={} productId={}",
          experiment.getId(),
          product.getId(),
          ex);
      throw new IllegalStateException("Contrato PDE do produto é inválido", ex);
    }
  }

  /** Acrescenta a identidade pública mínima e as políticas do mesmo experimento. */
  private void addPublicTrustContract(
      Map<String, Object> context, Experiment experiment, Product product) {
    if (!StringUtils.hasText(product.getSlug())) {
      context.put(
          "commercialTrustContract",
          Map.of("status", "UNAVAILABLE", "blockReason", "Produto sem slug público."));
      return;
    }
    try {
      PublicProductCommercialOfferResponse offer = publicOfferService.getOffer(product.getSlug());
      if (!Objects.equals(experiment.getId(), offer.experimentId())) {
        context.put(
            "commercialTrustContract",
            Map.of(
                "status",
                "UNAVAILABLE",
                "blockReason",
                "A oferta pública atual pertence a outro experimento."));
        return;
      }
      context.put(
          "commercialTrustContract",
          Map.of(
              "status",
              "AVAILABLE",
              "salesPageUrl",
              offer.salesPageUrl(),
              "supplier",
              Map.of(
                  "displayName", offer.supplierDisplayName(),
                  "registrationNumber", offer.supplierRegistrationNumber(),
                  "supportEmail", offer.supportEmail()),
              "policies",
              Map.of(
                  "termsUrl", offer.termsUrl(),
                  "privacyUrl", offer.privacyUrl(),
                  "refundPolicyUrl", offer.refundPolicyUrl())));
    } catch (ResponseStatusException ex) {
      log.warn(
          "Oferta pública indisponível no contexto de landing. experimentId={} productSlug={}",
          experiment.getId(),
          product.getSlug(),
          ex);
      context.put(
          "commercialTrustContract",
          Map.of(
              "status",
              "UNAVAILABLE",
              "blockReason",
              ex.getReason() == null ? "Oferta pública indisponível." : ex.getReason()));
    }
  }

  /** Inclui texto opcional sem propagar valores vazios ao agente. */
  private void putWhenPresent(Map<String, Object> target, String key, String value) {
    if (StringUtils.hasText(value)) target.put(key, value.trim());
  }

  /** Inclui somente seções JSON materializadas e com conteúdo. */
  private void putNodeWhenPresent(Map<String, Object> target, String key, JsonNode value) {
    if (!value.isMissingNode() && !value.isNull() && !value.isEmpty()) target.put(key, value);
  }
}
