package com.marketinghub.pde.service.promotion;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoSlot;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.util.StringUtils;

/** Responsabilidade: impedir a publicação da Vega v12 com vínculos comerciais divergentes. */
public final class PdeV12PublicationPolicy {

  public static final String EXPERIENCE_VERSION = "musa-pde-entry-v12-primeiro-ajuste-aplicavel";
  private static final String SLOT_CODE = "v8";
  private static final String PUBLIC_URL = "https://v8.clubemusa.com.br";
  private static final String MATERIAL_PREFIX = "/materials/musa-v12/";
  private static final long AD_VIDEO_ID = 41L;
  private static final long HERO_VIDEO_ID = 42L;

  private PdeV12PublicationPolicy() {}

  /** Informa se o slot pertence à candidata Vega v12 protegida por esta política. */
  public static boolean appliesTo(PdeProductionSlot slot) {
    return slot != null && EXPERIENCE_VERSION.equals(slot.getExperienceVersion());
  }

  /** Lista todas as divergências da candidata para corrigi-las em uma única rodada. */
  public static List<String> blockers(
      PdeProductionSlot slot,
      Experiment experiment,
      List<ExperimentVideoAsset> videos,
      JsonNode contract,
      boolean requireHomologation) {
    if (!appliesTo(slot)) return List.of();

    List<String> blockers = new ArrayList<>();
    validateSlotIdentity(slot, blockers);
    validateContractIdentity(slot, contract, blockers);
    validateExperiment(slot, experiment, contract, blockers);
    validateVideos(experiment, videos, contract, blockers);
    validateMaterials(contract, blockers);
    if (!"OK".equals(slot.getValidationStatus())) {
      blockers.add("Validar a URL e a jornada da própria v12.");
    }
    if (requireHomologation
        && slot.getStatus() != PdeProductionSlotStatus.READY
        && slot.getStatus() != PdeProductionSlotStatus.ACTIVE) {
      blockers.add("Concluir a homologação comercial da v12.");
    }
    return blockers.stream().distinct().toList();
  }

  /** Confere o slot comercial reservado para a v12 sem aceitar identidade privada ou legada. */
  private static void validateSlotIdentity(PdeProductionSlot slot, List<String> blockers) {
    if (!SLOT_CODE.equals(slot.getSlotCode())
        || !PUBLIC_URL.equals(withoutTrailingSlash(slot.getPublicUrl()))
        || !"v8.clubemusa.com.br".equals(slot.getDomain())) {
      blockers.add("Vincular a v12 ao destino comercial https://v8.clubemusa.com.br.");
    }
  }

  /** Confere se o rascunho representa a mesma versão e o mesmo produto do slot. */
  private static void validateContractIdentity(
      PdeProductionSlot slot, JsonNode contract, List<String> blockers) {
    if (contract == null
        || !contract.isObject()
        || !Objects.equals(slot.getProductSlug(), text(contract, "slug"))
        || !Objects.equals(EXPERIENCE_VERSION, text(contract, "experienceVersion"))) {
      blockers.add("Usar o contrato imutável da própria v12.");
    }
  }

  /** Confere preço, CTA, checkout e experimento contra a fonte persistida do teste #92. */
  private static void validateExperiment(
      PdeProductionSlot slot, Experiment experiment, JsonNode contract, List<String> blockers) {
    JsonNode binding = contract == null ? null : contract.path("commercialBinding");
    JsonNode checkout = contract == null ? null : contract.path("commercialCheckout");
    boolean validExperiment =
        experiment != null
            && experiment.getProduct() != null
            && Objects.equals(slot.getProductSlug(), experiment.getProduct().getSlug())
            && slot.getSourceExperimentId() != null
            && Objects.equals(slot.getSourceExperimentId(), experiment.getId())
            && binding != null
            && binding.path("experimentId").canConvertToLong()
            && Objects.equals(experiment.getId(), binding.path("experimentId").longValue());
    if (!validExperiment) {
      blockers.add("Vincular contrato, slot e oferta ao mesmo experimento.");
      return;
    }
    BigDecimal contractPrice = decimal(binding, "priceBrl");
    String contractCta = text(binding, "primaryCta");
    String contractCheckout = text(checkout, "checkoutUrl");
    BigDecimal checkoutPrice = decimal(checkout, "priceBrl");
    if (experiment.getUnitPrice() == null
        || contractPrice == null
        || checkoutPrice == null
        || experiment.getUnitPrice().compareTo(contractPrice) != 0
        || experiment.getUnitPrice().compareTo(checkoutPrice) != 0
        || !Objects.equals(experiment.getPrimaryCta(), contractCta)
        || !StringUtils.hasText(experiment.getCommercialCheckoutUrl())
        || !Objects.equals(experiment.getCommercialCheckoutUrl(), contractCheckout)
        || !"ONE_TIME".equals(text(binding, "billingModel"))
        || !"ONE_TIME".equals(text(checkout, "billingModel"))
        || !"BRL".equals(text(checkout, "currency"))
        || !"PEPPER".equals(text(checkout, "provider"))) {
      blockers.add("Alinhar preço, CTA e checkout de R$ 67 entre o experimento e a v12.");
    }
  }

  /** Exige os ativos AD e LANDING_HERO aprovados do experimento e o hero exato no contrato. */
  private static void validateVideos(
      Experiment experiment,
      List<ExperimentVideoAsset> videos,
      JsonNode contract,
      List<String> blockers) {
    if (experiment == null) return;
    List<ExperimentVideoAsset> safeVideos = videos == null ? List.of() : videos;
    ExperimentVideoAsset ad = approved(safeVideos, ExperimentVideoSlot.AD, AD_VIDEO_ID);
    ExperimentVideoAsset hero =
        approved(safeVideos, ExperimentVideoSlot.LANDING_HERO, HERO_VIDEO_ID);
    if (ad == null) {
      blockers.add("Vincular e aprovar o vídeo de anúncio #41 da v12.");
    }
    if (hero == null) {
      blockers.add("Vincular e aprovar o vídeo de apresentação da v12.");
      return;
    }
    JsonNode heroVideos = contract == null ? null : contract.path("heroVideos");
    boolean exactHero = false;
    if (heroVideos != null && heroVideos.isArray()) {
      for (JsonNode item : heroVideos) {
        if (item.path("experimentVideoAssetId").asLong(-1) == hero.getId()
            && EXPERIENCE_VERSION.equals(text(item, "experienceVersion"))
            && "READY".equals(text(item, "status"))
            && "APPROVED".equals(text(item, "reviewStatus"))
            && StringUtils.hasText(text(item, "hlsPlaybackUrl"))) {
          exactHero = true;
          break;
        }
      }
    }
    if (!exactHero) {
      blockers.add("Usar no contrato o vídeo de apresentação aprovado da própria v12.");
    }
  }

  /** Exige um kit pós-compra próprio da v12, sem caminhos identificados como v7. */
  private static void validateMaterials(JsonNode contract, List<String> blockers) {
    JsonNode materials = contract == null ? null : contract.path("supportMaterials");
    if (materials == null || !materials.isArray() || materials.isEmpty()) {
      blockers.add("Vincular o kit pós-compra próprio da v12.");
      return;
    }
    for (JsonNode material : materials) {
      String url = text(material, "url");
      if (url == null || !url.startsWith(MATERIAL_PREFIX)) {
        blockers.add("Vincular o kit pós-compra próprio da v12.");
        return;
      }
    }
  }

  /** Localiza o ativo mais recente que já passou pelos dois gates obrigatórios. */
  private static ExperimentVideoAsset approved(
      List<ExperimentVideoAsset> videos, ExperimentVideoSlot slot, long assetId) {
    return videos.stream()
        .filter(video -> Objects.equals(assetId, video.getId()))
        .filter(video -> video.getSlot() == slot)
        .filter(video -> video.getStatus() == ExperimentVideoStatus.READY)
        .filter(video -> video.getReviewStatus() == ExperimentVideoReviewStatus.APPROVED)
        .findFirst()
        .orElse(null);
  }

  /** Lê texto preenchido sem transformar nós ausentes em string vazia válida. */
  private static String text(JsonNode node, String field) {
    if (node == null || !node.hasNonNull(field) || !node.get(field).isTextual()) return null;
    String value = node.get(field).asText().trim();
    return value.isEmpty() ? null : value;
  }

  /** Lê número decimal do contrato para comparação monetária exata. */
  private static BigDecimal decimal(JsonNode node, String field) {
    if (node == null || !node.hasNonNull(field) || !node.get(field).isNumber()) return null;
    return node.get(field).decimalValue();
  }

  /** Remove somente a barra final para comparar a identidade canônica da URL. */
  private static String withoutTrailingSlash(String value) {
    if (!StringUtils.hasText(value)) return null;
    return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
  }
}
