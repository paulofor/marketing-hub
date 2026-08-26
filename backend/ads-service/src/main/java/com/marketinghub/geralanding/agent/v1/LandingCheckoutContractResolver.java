package com.marketinghub.geralanding.agent.v1;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPagePublicationAuditRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Responsabilidade: resolver o checkout canônico protegido usado na criação de uma landing. */
@Service
public class LandingCheckoutContractResolver {
  private final GeraSalesPagePublicationAuditRepository publicationRepository;

  /** Inicializa o resolvedor com o histórico auditável de publicações legadas. */
  public LandingCheckoutContractResolver(
      GeraSalesPagePublicationAuditRepository publicationRepository) {
    this.publicationRepository = publicationRepository;
  }

  /** Prioriza o checkout comercial atual e usa a última publicação apenas como compatibilidade. */
  public String resolve(Experiment experiment) {
    if (StringUtils.hasText(experiment.getCommercialCheckoutUrl())) {
      return experiment.getCommercialCheckoutUrl().trim();
    }
    return publicationRepository
        .findTopByExperimentIdOrderByPublishedAtDesc(experiment.getId())
        .map(publication -> publication.getCheckoutUrl())
        .filter(StringUtils::hasText)
        .map(String::trim)
        .orElse(null);
  }
}
