package com.marketinghub.experiment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignObjective;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.productai.ProductAiSubtype;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPagePublicationAuditRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPageStageExecutionRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o destino de compra sem duplicar páginas já auditadas. */
class ExperimentCampaignDestinationPolicyTest {
  private final GeraSalesPageStageExecutionRepository executions =
      mock(GeraSalesPageStageExecutionRepository.class);
  private final GeraSalesPagePublicationAuditRepository publications =
      mock(GeraSalesPagePublicationAuditRepository.class);
  private final PublishedPdePreflightEvidenceService publishedSurface =
      mock(PublishedPdePreflightEvidenceService.class);
  private final ExperimentCampaignDestinationPolicy policy =
      new ExperimentCampaignDestinationPolicy(executions, publications, publishedSurface);

  /** Aceita a superfície própria da entrega paga somente quando seu preflight está vigente. */
  @Test
  void acceptsAuditedPersonalizedPaidDeliverySurface() {
    Experiment experiment = paidDelivery();
    when(publishedSurface.isReady(experiment)).thenReturn(true);

    assertThat(policy.missingConfiguration(experiment)).isEmpty();
  }

  /** Mantém o GeraSalesPage obrigatório quando a superfície paga não possui preflight vigente. */
  @Test
  void rejectsPersonalizedPaidDeliveryWithoutPublishedPreflight() {
    Experiment experiment = paidDelivery();

    assertThat(policy.missingConfiguration(experiment)).containsExactly("geraSalesPagePipeline");
  }

  /** Não transforma uma prova de slot simulada em exceção para low-ticket genérico. */
  @Test
  void keepsTraditionalSalesPageForGenericLowTicket() {
    Experiment experiment = paidDelivery();
    experiment.setProductAiSubtype(null);
    when(publishedSurface.isReady(experiment)).thenReturn(true);

    assertThat(policy.missingConfiguration(experiment)).containsExactly("geraSalesPagePipeline");
  }

  /** Monta o contrato comercial mínimo da entrega personalizada vendida antes da coleta. */
  private Experiment paidDelivery() {
    Experiment experiment = new Experiment();
    experiment.setId(93L);
    experiment.setExperimentType(ExperimentType.LOW_TICKET_PRODUCT);
    experiment.setCampaignObjective(ExperimentCampaignObjective.SALES);
    experiment.setProductAiSubtype(ProductAiSubtype.AI_PERSONALIZED_PAID_DELIVERY);
    experiment.setSinglePain("Rotina confusa");
    experiment.setFreeReward("Demonstração limitada do produto real");
    experiment.setFunnelPromise("Organizar os produtos já disponíveis");
    experiment.setPrimaryCta("Organizar minha rotina");
    experiment.setUnitPrice(new BigDecimal("49.00"));
    return experiment;
  }
}
