package com.marketinghub.pde.service.promotion;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Responsabilidade: governar a candidata com continuidade de sete dias e materiais por compra
 * única.
 */
public final class PdeOneTimeAccessPublicationPolicy {
  /** Impede instanciação desta política sem estado. */
  private PdeOneTimeAccessPublicationPolicy() {}

  /** Reconhece a capacidade declarada, preservando a política específica das versões históricas. */
  public static boolean appliesTo(PdeProductionSlot slot, JsonNode contract) {
    return slot != null
        && !PdeV12PublicationPolicy.appliesTo(slot)
        && contract != null
        && "espelho-antes-de-sair".equals(slot.getLayoutKey())
        && contract.path("commercialAccess").isObject();
  }

  /** Permite disponibilizar a candidata planejada sem homologar, ativar ou abrir cobrança. */
  public static List<String> preparationBlockers(
      PdeProductionSlot slot, Experiment experiment, JsonNode contract) {
    var blockers = identityBlockers(slot, experiment, contract);
    if (slot == null || contract == null || !contract.isObject()) return List.copyOf(blockers);
    if (slot == null
        || (slot.getStatus() != PdeProductionSlotStatus.CANDIDATE
            && slot.getStatus() != PdeProductionSlotStatus.PLANNED)
        || experiment == null
        || experiment.getStatus() != ExperimentStatus.PLANNED
        || experiment.getFacebookReleaseRequestedAt() != null) {
      blockers.add("Disponibilizar a candidata somente antes da liberação comercial.");
    }
    return List.copyOf(blockers);
  }

  /** Exige vínculos materializados e preflight próprio antes da promoção, sem exigir vídeo. */
  public static List<String> blockers(
      PdeProductionSlot slot,
      Experiment experiment,
      JsonNode contract,
      boolean requireHomologation) {
    var blockers = identityBlockers(slot, experiment, contract);
    if (slot == null || contract == null || !contract.isObject()) return List.copyOf(blockers);
    if (experiment == null
        || !Objects.equals(
            experiment.getCommercialCheckoutUrl(),
            contract.path("commercialCheckout").path("checkoutUrl").asText())
        || !Objects.equals(experiment.getFollowUpActionUrl(), slot.getPublicUrl()))
      blockers.add("Vincular destino e checkout canônicos ao próprio experimento.");
    if (!"OK".equals(slot.getValidationStatus()))
      blockers.add("Validar a URL e a jornada pública da própria versão.");
    if (slot.getStatus() == PdeProductionSlotStatus.ACTIVE
        && (slot.getPublishedExperienceJson() == null
            || slot.getPublishedExperienceJson().isBlank()))
      blockers.add("Publicar o contrato da própria versão antes de ativá-la.");
    if (requireHomologation
        && slot.getStatus() != PdeProductionSlotStatus.READY
        && slot.getStatus() != PdeProductionSlotStatus.ACTIVE)
      blockers.add("Concluir a homologação comercial.");
    return List.copyOf(blockers);
  }

  /** Confere produto, versão, compra única e entrega declarada sem conceder parecer econômico. */
  private static ArrayList<String> identityBlockers(
      PdeProductionSlot slot, Experiment experiment, JsonNode contract) {
    var blockers = new ArrayList<String>();
    if (slot == null || contract == null || !contract.isObject()) {
      blockers.add("Usar um contrato candidato íntegro.");
      return blockers;
    }
    var binding = contract.path("commercialBinding");
    var checkout = contract.path("commercialCheckout");
    var access = contract.path("commercialAccess");
    if (experiment != null
        && experiment.getCommercialCheckoutUrl() != null
        && !experiment.getCommercialCheckoutUrl().isBlank()
        && !Objects.equals(
            experiment.getCommercialCheckoutUrl(), checkout.path("checkoutUrl").asText()))
      blockers.add("Preservar e revisar o checkout persistido divergente da candidata.");
    if (!Objects.equals(slot.getProductSlug(), contract.path("slug").asText())
        || !Objects.equals(slot.getExperienceVersion(), contract.path("experienceVersion").asText())
        || !Objects.equals(slot.getLayoutKey(), contract.path("layoutKey").asText())
        || experiment == null
        || experiment.getProduct() == null
        || !Objects.equals(slot.getProductSlug(), experiment.getProduct().getSlug())
        || experiment.getId() == null
        || !Objects.equals(slot.getSourceExperimentId(), experiment.getId())
        || binding.path("experimentId").asLong() != experiment.getId())
      blockers.add("Vincular contrato, produto, versão e experimento exatos.");
    if (experiment == null
        || experiment.getUnitPrice() == null
        || experiment.getUnitPrice().signum() <= 0
        || !binding.path("priceBrl").isNumber()
        || !checkout.path("priceBrl").isNumber()
        || binding.path("priceBrl").decimalValue().compareTo(experiment.getUnitPrice()) != 0
        || checkout.path("priceBrl").decimalValue().compareTo(experiment.getUnitPrice()) != 0
        || !"ONE_TIME".equals(binding.path("billingModel").asText())
        || !"ONE_TIME".equals(checkout.path("billingModel").asText())
        || !"BRL".equals(checkout.path("currency").asText())
        || checkout.path("provider").asText().isBlank()
        || !https(checkout.path("checkoutUrl").asText()))
      blockers.add("Declarar preço, checkout HTTPS e cobrança única canônicos.");
    if (!Objects.equals(slot.getExperienceVersion(), access.path("experienceVersion").asText())
        || !"DAYS_2_TO_7_AND_SUPPORT_MATERIALS".equals(access.path("scope").asText())
        || !access.path("accessDays").isIntegralNumber()
        || access.path("accessDays").asInt() <= 0
        || access.path("renewal").asBoolean(true)
        || !"PAYMENT_APPROVED".equals(access.path("activationTrigger").asText())
        || !contract.path("missions").isArray()
        || contract.path("missions").size() != 7
        || !contract.path("supportMaterials").isArray()
        || contract.path("supportMaterials").isEmpty())
      blockers.add("Declarar sete dias, materiais e acesso pago da mesma versão.");
    return blockers;
  }

  /** Recusa URLs sem TLS ou com credencial embutida sem registrar o valor recebido. */
  private static boolean https(String value) {
    try {
      var uri = URI.create(value);
      return "https".equals(uri.getScheme()) && uri.getHost() != null && uri.getUserInfo() == null;
    } catch (IllegalArgumentException ex) {
      return false;
    }
  }
}
