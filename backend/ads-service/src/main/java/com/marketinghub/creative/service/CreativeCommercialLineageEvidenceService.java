package com.marketinghub.creative.service;

import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeAgentReviewStatus;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.creative.dto.CreativeCommercialLineageEvidenceDto;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.product.Product;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Responsabilidade: comprovar que superfícies comerciais e criativos herdados pertencem à linhagem
 * explícita de um experimento sucessor.
 */
@Service
public class CreativeCommercialLineageEvidenceService {
  static final String CONTRACT_VERSION = "CREATIVE_COMMERCIAL_LINEAGE_V1";
  private static final int MAX_INTERNAL_REVISION_DEPTH = 64;

  /** Resolve a adoção sem inferir parentesco por nomes, URLs ou descrições livres. */
  public CreativeCommercialLineageEvidenceDto resolve(
      Creative creative, String targetDestinationUrl) {
    Experiment target = creative.getExperiment();
    Experiment sourceExperiment = target.getSourceExperiment();
    if (sourceExperiment == null) {
      return null;
    }
    CommercialSourceResolution sourceResolution = resolveCommercialSource(creative, target);
    Creative sourceCreative = sourceResolution.sourceCreative();
    String sourceDestinationUrl =
        firstText(
            sourceCreative == null ? null : sourceCreative.getDestinationUrl(),
            sourceExperiment.getFollowUpActionUrl());
    String targetCheckoutUrl = trimToNull(target.getCommercialCheckoutUrl());
    String sourceCheckoutUrl = trimToNull(sourceExperiment.getCommercialCheckoutUrl());
    boolean sameProduct = sameId(target.getProduct(), sourceExperiment.getProduct());
    var targetHypothesisId = target.getHypothesisRefIdForPending();
    boolean sameHypothesis =
        targetHypothesisId != null
            && Objects.equals(targetHypothesisId, sourceExperiment.getHypothesisRefIdForPending());
    boolean destinationMatched = sameText(targetDestinationUrl, sourceDestinationUrl);
    boolean checkoutMatched = sameText(targetCheckoutUrl, sourceCheckoutUrl);
    boolean reusedCreative = sourceCreative != null;
    boolean sourceCreativeMatches =
        sourceResolution.valid()
            && (!reusedCreative
                || (sourceCreative.getExperiment() != null
                    && Objects.equals(
                        sourceCreative.getExperiment().getId(), sourceExperiment.getId())
                    && sourceCreative.getStatus() == CreativeStatus.READY
                    && sourceCreative.getAgentReviewStatus() == CreativeAgentReviewStatus.APPROVED
                    && sourceCreative.getReviewedAt() != null));
    Boolean mediaMatched =
        reusedCreative
            ? sameText(resolveMediaUrl(creative), resolveMediaUrl(sourceCreative))
            : null;
    boolean verified =
        sameProduct
            && sameHypothesis
            && destinationMatched
            && checkoutMatched
            && sourceCreativeMatches
            && (!reusedCreative || Boolean.TRUE.equals(mediaMatched));
    return new CreativeCommercialLineageEvidenceDto(
        CONTRACT_VERSION,
        verified ? "VERIFIED" : "INCOMPLETE",
        target.getId(),
        sourceExperiment.getId(),
        sourceCreative == null ? null : sourceCreative.getId(),
        sourceCreative == null || sourceCreative.getExperiment() == null
            ? null
            : sourceCreative.getExperiment().getId(),
        reusedCreative,
        sameProduct,
        sameHypothesis,
        destinationMatched,
        checkoutMatched,
        mediaMatched,
        sourceDestinationUrl,
        trimToNull(targetDestinationUrl),
        sourceCheckoutUrl,
        targetCheckoutUrl,
        sourceCreative == null || sourceCreative.getStatus() == null
            ? null
            : sourceCreative.getStatus().name(),
        sourceCreative == null || sourceCreative.getAgentReviewStatus() == null
            ? null
            : sourceCreative.getAgentReviewStatus().name(),
        sourceCreative == null ? null : sourceCreative.getReviewedAt());
  }

  /**
   * Ignora versões internas do experimento-alvo e resolve apenas o primeiro criativo externo que
   * pode representar adoção comercial.
   */
  private CommercialSourceResolution resolveCommercialSource(Creative creative, Experiment target) {
    Creative candidate = creative.getSourceCreative();
    Set<Long> visitedIds = new HashSet<>();
    Set<Creative> visitedInstances = Collections.newSetFromMap(new IdentityHashMap<>());
    int depth = 0;
    while (candidate != null && sameExperiment(candidate.getExperiment(), target)) {
      if (depth++ >= MAX_INTERNAL_REVISION_DEPTH
          || !visitedInstances.add(candidate)
          || (candidate.getId() != null && !visitedIds.add(candidate.getId()))) {
        return new CommercialSourceResolution(candidate, false);
      }
      candidate = candidate.getSourceCreative();
    }
    return new CommercialSourceResolution(candidate, true);
  }

  /** Compara experimentos persistidos sem depender da identidade da instância JPA. */
  private boolean sameExperiment(Experiment left, Experiment right) {
    return left != null
        && right != null
        && left.getId() != null
        && Objects.equals(left.getId(), right.getId());
  }

  /** Compara entidades somente por identificador persistido e não por instância JPA. */
  private boolean sameId(Product left, Product right) {
    return left != null
        && right != null
        && left.getId() != null
        && Objects.equals(left.getId(), right.getId());
  }

  /** Resolve a URL efetivamente revisada de uma imagem ou vídeo. */
  private String resolveMediaUrl(Creative creative) {
    return "VIDEO".equalsIgnoreCase(creative.getFormat())
        ? trimToNull(creative.getVideoUrl())
        : trimToNull(creative.getImageUrl());
  }

  /** Compara textos obrigatórios após remover apenas espaços externos. */
  private boolean sameText(String left, String right) {
    String normalizedLeft = trimToNull(left);
    return normalizedLeft != null && normalizedLeft.equals(trimToNull(right));
  }

  /** Retorna o primeiro texto não vazio sem fabricar valor substituto. */
  private String firstText(String... values) {
    for (String value : values) {
      String normalized = trimToNull(value);
      if (normalized != null) {
        return normalized;
      }
    }
    return null;
  }

  /** Normaliza texto opcional preservando seu conteúdo. */
  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  /** Representa a origem comercial resolvida e a integridade da cadeia de revisões internas. */
  private record CommercialSourceResolution(Creative sourceCreative, boolean valid) {}
}
