package com.marketinghub.experiment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignObjective;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.gerasalespage.v1.GeraSalesPagePublicationAudit;
import com.marketinghub.gerasalespage.v1.GeraSalesPageStageCode;
import com.marketinghub.gerasalespage.v1.GeraSalesPageStageExecution;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.product.Product;
import com.marketinghub.productai.ProductAiSubtype;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPagePublicationAuditRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPageStageExecutionRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o destino de compra sem duplicar páginas já auditadas. */
class ExperimentCampaignDestinationPolicyTest {
  private final GeraSalesPageStageExecutionRepository executions =
      mock(GeraSalesPageStageExecutionRepository.class);
  private final GeraSalesPagePublicationAuditRepository publications =
      mock(GeraSalesPagePublicationAuditRepository.class);
  private final PublishedPdePreflightEvidenceService publishedSurface =
      mock(PublishedPdePreflightEvidenceService.class);
  private final FacebookSuccessorCommercialContractPolicy successorContract =
      new FacebookSuccessorCommercialContractPolicy();
  private final ExperimentCampaignDestinationPolicy policy =
      new ExperimentCampaignDestinationPolicy(
          executions, publications, publishedSurface, successorContract);

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

  /** Reutiliza a página auditada sem inventar uma execução do GeraSalesPage no sucessor. */
  @Test
  void reusesAuditedSalesPageForEquivalentLowTicketSuccessor() {
    Experiment source = lowTicket(88L);
    Experiment successor = lowTicket(94L);
    successor.setSinglePain("Mensagem específica do novo criativo");
    successor.setFreeReward("Quatro amostras mostradas pelo vídeo");
    successor.setFunnelPromise("Mesma entrega paga apresentada por outro ângulo");
    successor.setPrimaryCta("Ver amostras e decidir");
    successor.setSourceExperiment(source);
    GeraSalesPagePublicationAudit publication = publication(88L);
    completePublicationPipeline(88L);
    when(publications.findTopByExperimentIdOrderByPublishedAtDesc(88L))
        .thenReturn(Optional.of(publication));

    assertThat(policy.auditedSalesPagePublication(successor)).containsSame(publication);
    assertThat(policy.hasCompletedGeraSalesPagePipeline(successor)).isTrue();
    assertThat(policy.missingConfiguration(successor)).isEmpty();
  }

  /** Bloqueia a reutilização quando checkout ou preço deixam de representar a mesma oferta. */
  @Test
  void rejectsInheritedSalesPageAfterCommercialDivergence() {
    Experiment source = lowTicket(88L);
    Experiment successor = lowTicket(94L);
    successor.setSourceExperiment(source);
    successor.setCommercialCheckoutUrl("https://checkout.test/outro");
    successor.setUnitPrice(new BigDecimal("79.00"));
    completePublicationPipeline(88L);
    when(publications.findTopByExperimentIdOrderByPublishedAtDesc(88L))
        .thenReturn(Optional.of(publication(88L)));

    assertThat(policy.auditedSalesPagePublication(successor)).isEmpty();
    assertThat(policy.hasCompletedGeraSalesPagePipeline(successor)).isFalse();
    assertThat(policy.missingConfiguration(successor)).containsExactly("geraSalesPagePipeline");
  }

  /** Uma publicação própria mais nova nunca é mascarada pela versão herdada do antecessor. */
  @Test
  void prefersOwnPublicationAndRequiresItsOwnCompletedPipeline() {
    Experiment source = lowTicket(88L);
    Experiment successor = lowTicket(94L);
    successor.setSourceExperiment(source);
    GeraSalesPagePublicationAudit ownPublication = publication(94L);
    when(publications.findTopByExperimentIdOrderByPublishedAtDesc(94L))
        .thenReturn(Optional.of(ownPublication));
    when(publications.findTopByExperimentIdOrderByPublishedAtDesc(88L))
        .thenReturn(Optional.of(publication(88L)));
    completePublicationPipeline(88L);

    assertThat(policy.auditedSalesPagePublication(successor)).containsSame(ownPublication);
    assertThat(policy.hasCompletedGeraSalesPagePipeline(successor)).isFalse();
    assertThat(policy.missingConfiguration(successor)).containsExactly("geraSalesPagePipeline");
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

  /** Monta origem e sucessor com a mesma identidade comercial usada pela adoção oficial. */
  private Experiment lowTicket(Long id) {
    Product product = Product.builder().id(7L).build();
    MarketNiche niche = new MarketNiche();
    niche.setId(21L);
    Hypothesis hypothesis = new Hypothesis();
    hypothesis.setId(UUID.fromString("ed8b1395-8d98-417f-81e2-0ec57b68cbb7"));
    Experiment experiment = new Experiment();
    experiment.setId(id);
    experiment.setProduct(product);
    experiment.setNiche(niche);
    experiment.setHypothesisRef(hypothesis);
    experiment.setPlatform(ExperimentPlatform.FACEBOOK);
    experiment.setExperimentType(ExperimentType.LOW_TICKET_PRODUCT);
    experiment.setCampaignObjective(ExperimentCampaignObjective.SALES);
    experiment.setDesireTerritoryCode("PROFESSIONAL_PRIDE");
    experiment.setSinglePain("Instagram não comunica o valor do trabalho");
    experiment.setFreeReward("Quatro amostras aprovadas do kit");
    experiment.setFunnelPromise("Kit visual pronto para publicar");
    experiment.setPrimaryCta("Ver amostras e comprar");
    experiment.setUnitPrice(new BigDecimal("67.00"));
    experiment.setFollowUpActionUrl("https://sales.test/capella");
    experiment.setCommercialCheckoutUrl("https://checkout.test/capella");
    return experiment;
  }

  /** Cria a fotografia publicada com destino, checkout e coletores de venda. */
  private GeraSalesPagePublicationAudit publication(Long experimentId) {
    return GeraSalesPagePublicationAudit.builder()
        .experimentId(experimentId)
        .salesPageUrl("https://sales.test/capella")
        .checkoutUrl("https://checkout.test/capella")
        .html(
            """
            <section data-track-section="oferta">Oferta</section>
            <script data-mh-sales-page-analytics="true">
            sendEvent('page_view'); sendEvent('page_load_metric');
            sendEvent('section_view_time'); sendEvent('checkout_click');
            </script>
            """)
        .build();
  }

  /** Marca somente a etapa final auditada da origem, sem criar etapa fictícia no sucessor. */
  private void completePublicationPipeline(Long experimentId) {
    when(executions.findTopByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(
            experimentId, GeraSalesPageStageCode.PUBLICATION_PACKAGE.code()))
        .thenReturn(
            Optional.of(
                GeraSalesPageStageExecution.builder()
                    .experimentId(experimentId)
                    .stageCode(GeraSalesPageStageCode.PUBLICATION_PACKAGE.code())
                    .status("CONCLUIDO")
                    .build()));
  }
}
