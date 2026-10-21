package com.marketinghub.pde.paidvisual.v1.service;

import static com.marketinghub.pde.visualpersonalization.v1.service.VisualPreparationService.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.productai.ProductAiSubtype;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: resolver o contrato pago publicado sem herdar provas ou permissões privadas.
 */
@Component
@Slf4j
public class PaidVisualPolicy {
  public static final String VERSION = "PDE_PAID_VISUAL_V1";
  private final ExperimentRepository experiments;
  private final PdeProductionSlotRepository slots;
  private final ObjectMapper json;
  private final com.marketinghub.repository.jpa.planning.CommercialPlanRepository plans;

  /** Recebe fontes canônicas de oferta e versão. */
  public PaidVisualPolicy(
      ExperimentRepository experiments,
      PdeProductionSlotRepository slots,
      ObjectMapper json,
      com.marketinghub.repository.jpa.planning.CommercialPlanRepository plans) {
    this.experiments = experiments;
    this.slots = slots;
    this.json = json;
    this.plans = plans;
  }

  /**
   * Carrega uma única versão publicada da oferta e exige liberação comercial para novas compras.
   */
  public ObjectNode resolve(Long experimentId, boolean requireSelling) {
    return resolve(experimentId, requireSelling, null);
  }

  /** Preserva a entrega de uma compra confirmada depois de a campanha encerrar ou pausar. */
  public ObjectNode resolveSettlement(Long experimentId, String experienceVersion) {
    require(experienceVersion != null && !experienceVersion.isBlank(), "Versão da compra ausente.");
    return resolve(experimentId, false, experienceVersion);
  }

  /** Separa a autorização de novas compras da conciliação da versão já vendida. */
  private ObjectNode resolve(Long experimentId, boolean requireSelling, String settlementVersion) {
    Experiment exp =
        experiments.findById(experimentId).orElseThrow(() -> fail(404, "Oferta não encontrada."));
    require(
        exp.getProduct() != null
            && exp.getProductAiSubtype() == ProductAiSubtype.AI_PERSONALIZED_PAID_DELIVERY,
        "A oferta não pertence à entrega personalizada após compra.");
    List<PdeProductionSlot> candidates =
        slots.findByProductSlugOrderBySlotCodeAsc(exp.getProduct().getSlug()).stream()
            .filter(s -> experimentId.equals(s.getSourceExperimentId()))
            .filter(s -> s.getPublishedExperienceJson() != null)
            .filter(
                s ->
                    settlementVersion == null || settlementVersion.equals(s.getExperienceVersion()))
            .filter(
                s ->
                    settlementVersion != null
                        || List.of(
                                PdeProductionSlotStatus.CANDIDATE,
                                PdeProductionSlotStatus.READY,
                                PdeProductionSlotStatus.ACTIVE)
                            .contains(s.getStatus()))
            .filter(
                s ->
                    read(s.getPublishedExperienceJson())
                        .path("paidVisualDelivery")
                        .path("contractVersion")
                        .asText()
                        .equals(VERSION))
            .toList();
    require(
        candidates.size() == 1,
        "Prepare e revise uma única versão de entrega visual da própria oferta.");
    var slot = candidates.getFirst();
    var full = read(slot.getPublishedExperienceJson());
    if (settlementVersion != null) {
      require(
          exp.getStatus() != ExperimentStatus.PLANNED
              && List.of("OK", "DELIVERY_READY").contains(slot.getValidationStatus()),
          "Não há versão comercial homologada para esta confirmação financeira.");
    }
    var result = (ObjectNode) full.path("paidVisualDelivery").deepCopy();
    require(
        slot.getExperienceVersion().equals(full.path("experienceVersion").asText())
            && slot.getProductSlug().equals(full.path("slug").asText())
            && full.path("commercialBinding").path("experimentId").asLong() == experimentId,
        "Contrato comercial de outra versão ou experimento.");
    require(
        result.path("accessDays").asInt() == 7
            && result.path("maxRegenerations").asInt() == 1
            && result.path("maxGenerationUsd").isNumber()
            && result.path("maxGenerationUsd").decimalValue().signum() > 0
            && result.path("maxGenerationUsd").decimalValue().compareTo(new BigDecimal("2")) <= 0
            && result.path("commercialPlanId").asLong() > 0
            && result.path("model").isTextual()
            && !result.path("model").asText().isBlank(),
        "Contrato de entrega precisa de prazo, limite de regeneração e custo máximo por geração revisados.");
    require(
        full.path("commercialBinding").path("priceBrl").isNumber()
            && full.path("commercialBinding").path("priceBrl").decimalValue().signum() > 0
            && (settlementVersion != null
                || (exp.getUnitPrice() != null
                    && exp.getUnitPrice().signum() > 0
                    && exp.getUnitPrice()
                            .compareTo(
                                full.path("commercialBinding").path("priceBrl").decimalValue())
                        == 0)),
        "O preço da versão diverge da oferta.");
    var plan =
        plans
            .findById(result.path("commercialPlanId").asLong())
            .orElseThrow(() -> fail(409, "Plano da entrega não encontrado."));
    require(
        plan.getExperiment() != null && experimentId.equals(plan.getExperiment().getId()),
        "Plano de outra oferta.");
    URI delivery = URI.create(slot.getPublicUrl());
    require(
        "https".equals(delivery.getScheme())
            && delivery.getHost() != null
            && delivery.getUserInfo() == null
            && delivery.getFragment() == null
            && delivery.getPath().endsWith("/paid-visual.html"),
        "A entrega exige URL HTTPS própria, sem credencial.");
    boolean selling =
        exp.getStatus() == ExperimentStatus.RUNNING
            && slot.getStatus() == PdeProductionSlotStatus.ACTIVE
            && "OK".equals(slot.getValidationStatus())
            && !Boolean.FALSE.equals(exp.getProduct().getAutomaticExecutionEnabled())
            && exp.getCommercialCheckoutUrl() != null
            && exp.getCommercialCheckoutUrl().startsWith("https://");
    boolean customerInformation =
        result.path("supportEmail").asText().contains("@")
            && List.of("termsUrl", "privacyUrl", "refundPolicyUrl").stream()
                .allMatch(key -> result.path(key).asText().startsWith("https://"));
    selling = selling && customerInformation;
    if (requireSelling)
      require(selling, "A oferta ainda não está homologada e em execução comercial.");
    result.put("productId", exp.getProduct().getId());
    result.put("experimentId", experimentId);
    result.put("productSlug", exp.getProduct().getSlug());
    result.put("productName", exp.getProduct().getName());
    result.put("productVersion", slot.getExperienceVersion());
    result.set("priceBrl", full.path("commercialBinding").path("priceBrl"));
    result.put("deliveryUrl", slot.getPublicUrl());
    result.put("selling", selling);
    result.put(
        "checkoutPrepared",
        exp.getCommercialCheckoutUrl() != null
            && exp.getCommercialCheckoutUrl().startsWith("https://"));
    if (selling) result.put("checkoutUrl", exp.getCommercialCheckoutUrl());
    return result;
  }

  /** Lê apenas contratos persistidos, preservando diagnóstico de corrupção. */
  private JsonNode read(String value) {
    try {
      return json.readTree(value);
    } catch (Exception ex) {
      log.error("Contrato da entrega visual paga ilegível", ex);
      throw fail(409, "Contrato comercial ilegível.");
    }
  }
}
