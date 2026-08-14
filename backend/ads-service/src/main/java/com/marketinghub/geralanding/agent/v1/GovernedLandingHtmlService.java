package com.marketinghub.geralanding.agent.v1;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.geralanding.qualityreview.service.BackendQualityReviewService;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPagePublicationAuditRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Responsabilidade: aplicar HTML integral criado por Dédalo sem permitir alterar contratos
 * comerciais protegidos.
 */
@Service
public class GovernedLandingHtmlService {
  private static final Pattern ANCHOR_TAG =
      Pattern.compile("<a\\b[^>]*>", Pattern.CASE_INSENSITIVE);
  private static final Pattern PRIMARY_CHECKOUT_ID =
      Pattern.compile("\\bid\\s*=\\s*[\"']checkout-cta-primary[\"']", Pattern.CASE_INSENSITIVE);
  private static final Pattern HREF_ATTRIBUTE =
      Pattern.compile("\\bhref\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
  private final ExperimentRepository experimentRepository;
  private final GeraSalesPagePublicationAuditRepository publicationRepository;
  private final BackendQualityReviewService qualityReviewService;

  /** Inicializa o aplicador com a fonte canônica do experimento e o revisor independente. */
  public GovernedLandingHtmlService(
      ExperimentRepository experimentRepository,
      GeraSalesPagePublicationAuditRepository publicationRepository,
      BackendQualityReviewService qualityReviewService) {
    this.experimentRepository = experimentRepository;
    this.publicationRepository = publicationRepository;
    this.qualityReviewService = qualityReviewService;
  }

  /** Valida, persiste como rascunho e envia o documento integral para Têmis. */
  @Transactional
  public void apply(Long experimentId, String generatedHtml) {
    Experiment experiment =
        experimentRepository
            .findById(experimentId)
            .orElseThrow(
                () -> new EntityNotFoundException("Experiment not found: " + experimentId));
    String currentHtml = experiment.getHtmlGeraLanding();
    validateDocument(generatedHtml);
    preserveCommercialContract(experiment, currentHtml, generatedHtml);
    experiment.setHtmlGeraLanding(generatedHtml.trim());
    experimentRepository.save(experiment);
    qualityReviewService.reviewAfterHtmlGeneration(experiment);
  }

  /** Bloqueia documento incompleto ou capacidade executável não autorizada. */
  private void validateDocument(String html) {
    if (!StringUtils.hasText(html)
        || html.length() < 500
        || html.length() > 200_000
        || !html.toLowerCase(Locale.ROOT).contains("<html")
        || !html.toLowerCase(Locale.ROOT).contains("</html>")) {
      throw new IllegalArgumentException("HTML integral de Dédalo está incompleto");
    }
    String normalized = html.toLowerCase(Locale.ROOT);
    if (normalized.contains("<script")
        || normalized.contains("javascript:")
        || Pattern.compile("\son[a-z]+\\s*=", Pattern.CASE_INSENSITIVE).matcher(html).find()) {
      throw new IllegalArgumentException("HTML integral contém execução não autorizada");
    }
  }

  /** Preserva CTA, preço e destino de checkout já aprovados pelo backend. */
  private void preserveCommercialContract(
      Experiment experiment, String currentHtml, String generatedHtml) {
    if (StringUtils.hasText(experiment.getPrimaryCta())
        && !generatedHtml.contains(experiment.getPrimaryCta())) {
      throw new IllegalArgumentException("HTML integral alterou o CTA principal");
    }
    String currentCheckout = checkoutHref(currentHtml);
    String generatedCheckout = checkoutHref(generatedHtml);
    String canonicalCheckout =
        publicationRepository
            .findTopByExperimentIdOrderByPublishedAtDesc(experiment.getId())
            .map(publication -> publication.getCheckoutUrl())
            .filter(StringUtils::hasText)
            .orElse(null);
    if (!StringUtils.hasText(generatedCheckout)
        || (StringUtils.hasText(currentCheckout)
            && !currentCheckout.equals(generatedCheckout)
            && !(currentCheckout.startsWith("#")
                && StringUtils.hasText(canonicalCheckout)
                && canonicalCheckout.equals(generatedCheckout)))) {
      throw new IllegalArgumentException("HTML integral alterou o destino protegido do checkout");
    }
  }

  /** Extrai o destino do CTA de checkout canônico. */
  private String checkoutHref(String html) {
    if (!StringUtils.hasText(html)) return null;
    Matcher anchors = ANCHOR_TAG.matcher(html);
    while (anchors.find()) {
      String anchor = anchors.group();
      if (!PRIMARY_CHECKOUT_ID.matcher(anchor).find()) continue;
      Matcher href = HREF_ATTRIBUTE.matcher(anchor);
      return href.find() ? href.group(1) : null;
    }
    return null;
  }
}
