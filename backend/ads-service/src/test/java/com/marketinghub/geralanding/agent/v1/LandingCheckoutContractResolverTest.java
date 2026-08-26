package com.marketinghub.geralanding.agent.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.gerasalespage.v1.GeraSalesPagePublicationAudit;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPagePublicationAuditRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a fonte de verdade do checkout usado antes da primeira publicação. */
class LandingCheckoutContractResolverTest {

  /** Deve permitir a primeira landing usando o checkout comercial já congelado no experimento. */
  @Test
  void shouldResolveCommercialCheckoutBeforeFirstPublication() {
    GeraSalesPagePublicationAuditRepository publications =
        mock(GeraSalesPagePublicationAuditRepository.class);
    Experiment experiment = new Experiment();
    experiment.setId(89L);
    experiment.setCommercialCheckoutUrl(" https://checkout.example/rigel ");

    String checkout = new LandingCheckoutContractResolver(publications).resolve(experiment);

    assertThat(checkout).isEqualTo("https://checkout.example/rigel");
    verifyNoInteractions(publications);
  }

  /** Deve preservar o checkout auditado de experimentos legados sem contrato comercial próprio. */
  @Test
  void shouldFallbackToLatestPublishedCheckoutForLegacyExperiment() {
    GeraSalesPagePublicationAuditRepository publications =
        mock(GeraSalesPagePublicationAuditRepository.class);
    Experiment experiment = new Experiment();
    experiment.setId(88L);
    GeraSalesPagePublicationAudit publication = new GeraSalesPagePublicationAudit();
    publication.setCheckoutUrl("https://checkout.example/legacy");
    when(publications.findTopByExperimentIdOrderByPublishedAtDesc(88L))
        .thenReturn(Optional.of(publication));

    String checkout = new LandingCheckoutContractResolver(publications).resolve(experiment);

    assertThat(checkout).isEqualTo("https://checkout.example/legacy");
  }
}
