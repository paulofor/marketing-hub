package com.marketinghub.planning.service;

import com.marketinghub.geralanding.publiclanding.service.ApprovedLandingProductEvidenceGate;
import com.marketinghub.planning.CommercialPlanVisualAsset;
import com.marketinghub.planning.CommercialPlanVisualAssetStatus;
import com.marketinghub.planning.imagestudio.v1.CommercialPlanVisualAssetReviewStatus;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanVisualAssetRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Responsabilidade: fornecer e validar as provas visuais aprovadas que uma landing deve reutilizar.
 */
@Service
public class CommercialPlanLandingAssetService implements ApprovedLandingProductEvidenceGate {
  private static final int MAX_REQUIRED_REFERENCES = 4;

  private final CommercialPlanRepository commercialPlanRepository;
  private final CommercialPlanVisualAssetRepository visualAssetRepository;

  /** Inicializa o serviço com as fontes canônicas do plano e de sua biblioteca audiovisual. */
  public CommercialPlanLandingAssetService(
      CommercialPlanRepository commercialPlanRepository,
      CommercialPlanVisualAssetRepository visualAssetRepository) {
    this.commercialPlanRepository = commercialPlanRepository;
    this.visualAssetRepository = visualAssetRepository;
  }

  /** Lista as imagens aprovadas para landing do plano vigente que governa o experimento. */
  @Transactional(readOnly = true)
  public List<LandingAssetReference> referencesForExperiment(Long experimentId) {
    if (experimentId == null) {
      return List.of();
    }
    return commercialPlanRepository.findByExperimentReference(experimentId).stream()
        .findFirst()
        .map(
            plan ->
                visualAssetRepository
                    .findByCommercialPlanIdAndStatusOrderByCreatedAtAsc(
                        plan.getId(), CommercialPlanVisualAssetStatus.APPROVED)
                    .stream()
                    .filter(this::isIndependentlyApprovedForLanding)
                    .filter(asset -> StringUtils.hasText(asset.getAssetUrl()))
                    .map(this::toReference)
                    .toList())
        .orElseGet(List::of);
  }

  /** Monta o contrato simples que Dédalo e GeraSalesPage recebem antes de produzir o HTML. */
  @Transactional(readOnly = true)
  public List<Map<String, Object>> payloadForExperiment(Long experimentId) {
    return referencesForExperiment(experimentId).stream()
        .map(
            reference -> {
              Map<String, Object> payload = new LinkedHashMap<>();
              payload.put("assetId", reference.assetId());
              payload.put("assetUrl", reference.assetUrl());
              payload.put("label", reference.label());
              payload.put("version", reference.version());
              payload.put("status", "APPROVED");
              payload.put("agentReviewStatus", "APPROVED");
              payload.put("requiredUsage", "PRESERVE_EXACT_FILE_NO_REDRAW");
              return Map.copyOf(payload);
            })
        .toList();
  }

  /** Informa quantas provas distintas precisam aparecer na página do experimento. */
  @Transactional(readOnly = true)
  public int requiredReferenceCount(Long experimentId) {
    return Math.min(MAX_REQUIRED_REFERENCES, referencesForExperiment(experimentId).size());
  }

  /** Confirma se o HTML contém a quantidade mínima de arquivos aprovados sem reconstrução. */
  @Transactional(readOnly = true)
  public boolean hasRequiredApprovedAssetReferences(Long experimentId, String html) {
    List<LandingAssetReference> references = referencesForExperiment(experimentId);
    if (references.isEmpty()) {
      return true;
    }
    if (!StringUtils.hasText(html)) {
      return false;
    }
    Set<String> approvedUrls =
        references.stream().map(LandingAssetReference::assetUrl).collect(Collectors.toSet());
    long referenced =
        Jsoup.parse(html).select("img[src]").stream()
            .map(element -> element.attr("src").trim())
            .filter(approvedUrls::contains)
            .distinct()
            .count();
    return referenced >= Math.min(MAX_REQUIRED_REFERENCES, references.size());
  }

  /** Bloqueia publicação que substitua a entrega real por placeholders ou reconstruções. */
  @Override
  @Transactional(readOnly = true)
  public void validateApprovedAssetReferences(Long experimentId, String html) {
    int required = requiredReferenceCount(experimentId);
    if (required > 0 && !hasRequiredApprovedAssetReferences(experimentId, html)) {
      throw new IllegalArgumentException(
          "Landing deve exibir ao menos %d arquivos APPROVED da Biblioteca Audiovisual sem redesenho"
              .formatted(required));
    }
  }

  /** Confirma finalidade LANDING e revisão independente aprovada no mesmo arquivo. */
  private boolean isIndependentlyApprovedForLanding(CommercialPlanVisualAsset asset) {
    return asset.getAgentReviewStatus() == CommercialPlanVisualAssetReviewStatus.APPROVED
        && ("LANDING".equalsIgnoreCase(asset.getPurpose())
            || containsLandingPurpose(asset.getPurposesJson()));
  }

  /** Reconhece a finalidade no JSON persistido sem aceitar palavras parciais. */
  private boolean containsLandingPurpose(String purposesJson) {
    if (!StringUtils.hasText(purposesJson)) {
      return false;
    }
    return purposesJson.toUpperCase(Locale.ROOT).contains("\"LANDING\"");
  }

  /** Reduz a entidade JPA ao contrato imutável necessário para compor a landing. */
  private LandingAssetReference toReference(CommercialPlanVisualAsset asset) {
    return new LandingAssetReference(
        asset.getId(), asset.getAssetUrl(), asset.getLabel(), asset.getVersionNumber());
  }

  /** Responsabilidade: transportar a identidade e a versão de uma prova visual aprovada. */
  public record LandingAssetReference(
      Long assetId, String assetUrl, String label, Integer version) {}
}
