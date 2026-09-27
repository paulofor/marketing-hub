package com.marketinghub.pde.service.promotion;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.util.StringUtils;

/** Responsabilidade: bloquear a publicação PDE genérica quando oferta, vídeo e versão divergem. */
public final class PdeCommercialPublicationPolicy {
  private static final String MIRA_PRODUCT_SLUG = "pde-planejado-36";
  private static final String MIRA_EXPERIENCE_VERSION = "mira-commercial-v1";
  private static final String MIRA_LAYOUT_KEY = "mira-routine-v1";

  /** Impede instanciação da política comercial sem estado. */
  private PdeCommercialPublicationPolicy() {}

  /** Limita o gate novo à versão comercial de Mira sem alterar contratos PDE legados. */
  public static boolean appliesTo(PdeProductionSlot slot) {
    return slot != null
        && MIRA_PRODUCT_SLUG.equals(slot.getProductSlug())
        && MIRA_EXPERIENCE_VERSION.equals(slot.getExperienceVersion())
        && MIRA_LAYOUT_KEY.equals(slot.getLayoutKey());
  }

  /** Lista todas as divergências comerciais da candidata sem alterar seu estado. */
  public static List<String> blockers(
      PdeProductionSlot slot,
      Experiment experiment,
      List<ExperimentVideoAsset> videos,
      JsonNode contract,
      boolean requireHomologation) {
    List<String> blockers = new ArrayList<>();
    validateContractIdentity(slot, contract, blockers);
    validateExperiment(slot, experiment, contract, blockers);
    validateVideo(slot, videos, contract, blockers);
    if (!"OK".equals(slot.getValidationStatus())) {
      blockers.add("Validar a URL e a jornada pública da própria versão.");
    }
    if (requireHomologation
        && slot.getStatus() != PdeProductionSlotStatus.READY
        && slot.getStatus() != PdeProductionSlotStatus.ACTIVE) {
      blockers.add("Concluir a homologação comercial.");
    }
    return blockers.stream().distinct().toList();
  }

  /** Confere se o contrato pertence exatamente ao produto, à versão e ao layout do slot. */
  private static void validateContractIdentity(
      PdeProductionSlot slot, JsonNode contract, List<String> blockers) {
    if (contract == null
        || !contract.isObject()
        || !Objects.equals(slot.getProductSlug(), text(contract, "slug"))
        || !Objects.equals(slot.getExperienceVersion(), text(contract, "experienceVersion"))
        || !Objects.equals(slot.getLayoutKey(), text(contract, "layoutKey"))) {
      blockers.add("Usar o contrato da própria versão e do próprio produto.");
    }
  }

  /** Confere experimento, CTA, preço e checkout contra as fontes persistidas da oferta. */
  private static void validateExperiment(
      PdeProductionSlot slot, Experiment experiment, JsonNode contract, List<String> blockers) {
    JsonNode binding = contract == null ? null : contract.path("commercialBinding");
    JsonNode checkout = contract == null ? null : contract.path("commercialCheckout");
    boolean sameExperiment =
        experiment != null
            && slot.getSourceExperimentId() != null
            && Objects.equals(slot.getSourceExperimentId(), experiment.getId())
            && experiment.getProduct() != null
            && Objects.equals(slot.getProductSlug(), experiment.getProduct().getSlug())
            && binding != null
            && binding.path("experimentId").canConvertToLong()
            && Objects.equals(experiment.getId(), binding.path("experimentId").longValue());
    if (!sameExperiment) {
      blockers.add("Vincular contrato, slot e oferta ao mesmo experimento.");
      return;
    }
    BigDecimal bindingPrice = decimal(binding, "priceBrl");
    BigDecimal checkoutPrice = decimal(checkout, "priceBrl");
    boolean aligned =
        experiment.getUnitPrice() != null
            && experiment
                    .getUnitPrice()
                    .compareTo(bindingPrice == null ? BigDecimal.ZERO : bindingPrice)
                == 0
            && experiment
                    .getUnitPrice()
                    .compareTo(checkoutPrice == null ? BigDecimal.ZERO : checkoutPrice)
                == 0
            && Objects.equals(experiment.getPrimaryCta(), text(binding, "primaryCta"))
            && StringUtils.hasText(experiment.getCommercialCheckoutUrl())
            && Objects.equals(experiment.getCommercialCheckoutUrl(), text(checkout, "checkoutUrl"))
            && "ONE_TIME".equals(text(binding, "billingModel"))
            && "ONE_TIME".equals(text(checkout, "billingModel"))
            && "BRL".equals(text(checkout, "currency"))
            && StringUtils.hasText(text(checkout, "provider"));
    if (!aligned) {
      blockers.add("Alinhar preço, CTA e checkout entre o experimento e a versão.");
    }
  }

  /** Exige que o vídeo declarado no hero seja o ativo aprovado do mesmo experimento. */
  private static void validateVideo(
      PdeProductionSlot slot,
      List<ExperimentVideoAsset> videos,
      JsonNode contract,
      List<String> blockers) {
    JsonNode heroVideos = contract == null ? null : contract.path("heroVideos");
    if (heroVideos == null || !heroVideos.isArray() || heroVideos.isEmpty()) {
      blockers.add("Vincular ao contrato um vídeo aprovado da própria oferta.");
      return;
    }
    List<ExperimentVideoAsset> safeVideos = videos == null ? List.of() : videos;
    for (JsonNode hero : heroVideos) {
      if (!hero.path("experimentVideoAssetId").canConvertToLong()) continue;
      long id = hero.path("experimentVideoAssetId").longValue();
      ExperimentVideoAsset approved =
          safeVideos.stream()
              .filter(video -> Objects.equals(video.getId(), id))
              .filter(video -> video.getStatus() == ExperimentVideoStatus.READY)
              .filter(video -> video.getReviewStatus() == ExperimentVideoReviewStatus.APPROVED)
              .findFirst()
              .orElse(null);
      if (approved == null) continue;
      boolean identityMatches =
          Objects.equals(slot.getExperienceVersion(), text(hero, "experienceVersion"))
              && Objects.equals(approved.getAssetUrl(), text(hero, "playbackUrl"))
              && Objects.equals(approved.getHlsPlaybackUrl(), text(hero, "hlsPlaybackUrl"));
      if (identityMatches) return;
    }
    blockers.add("Vincular ao contrato o vídeo aprovado exato da própria oferta.");
  }

  /** Lê texto não vazio sem converter números ou objetos em identidade comercial. */
  private static String text(JsonNode node, String field) {
    if (node == null || !node.path(field).isTextual()) return null;
    String value = node.path(field).asText().trim();
    return value.isEmpty() ? null : value;
  }

  /** Lê preço decimal somente quando o contrato fornece um número válido. */
  private static BigDecimal decimal(JsonNode node, String field) {
    return node != null && node.path(field).isNumber() ? node.path(field).decimalValue() : null;
  }
}
