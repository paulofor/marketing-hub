package com.marketinghub.quartzo.commercial.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeAgentReviewStatus;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.experiment.*;
import com.marketinghub.experiment.service.*;
import com.marketinghub.financialplan.v1.service.FinancialPlanService;
import com.marketinghub.gerasalespage.v1.GeraSalesPagePublicationAudit;
import com.marketinghub.planning.service.CommercialPlanLandingAssetService;
import com.marketinghub.product.Product;
import com.marketinghub.producttype.ProductTypeDefinition;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Responsabilidade: testar identidade, fontes e invalidação da preparação low-ticket sem slots. */
class QuartzoCommercialContextTest {
  final ExperimentRepository experiments = mock(ExperimentRepository.class);
  final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  final ExperimentCampaignDestinationPolicy destinations =
      mock(ExperimentCampaignDestinationPolicy.class);
  final CreativeRepository creatives = mock(CreativeRepository.class);
  final QuartzoCommercialContext context =
      new QuartzoCommercialContext(
          experiments,
          cycles,
          destinations,
          creatives,
          mock(CommercialPlanLandingAssetService.class),
          mock(ExperimentTargetingSelectionService.class),
          mock(FinancialPlanService.class),
          mock(CommercialPlanRepository.class),
          new ObjectMapper().findAndRegisterModules());
  Product product;
  Experiment experiment;
  GeraSalesPagePublicationAudit publication;

  /** Monta um produto sintético com a forma de entrega e a versão efetivamente persistidas. */
  @BeforeEach
  void setup() {
    product =
        Product.builder()
            .id(7L)
            .slug("synthetic-kit")
            .validationDefinitionVersion("v1")
            .productTypeDefinition(
                ProductTypeDefinition.builder().code(QuartzoCommercialContext.TYPE).build())
            .currentPriceBrl(new BigDecimal("67"))
            .pdeExperienceJson("{\"offerType\":\"kit\"}")
            .build();
    experiment = new Experiment();
    experiment.setId(88L);
    experiment.setProduct(product);
    experiment.setExperimentType(ExperimentType.LOW_TICKET_PRODUCT);
    experiment.setStatus(ExperimentStatus.USER_STOPPED);
    experiment.setUnitPrice(new BigDecimal("67"));
    experiment.setPrimaryCta("Comprar o kit por R$ 67");
    when(experiments.findById(88L)).thenReturn(Optional.of(experiment));
    publication =
        GeraSalesPagePublicationAudit.builder()
            .id(27L)
            .experimentId(88L)
            .salesPageUrl("https://example.test/kit")
            .checkoutUrl("https://example.test/checkout")
            .html("<main>Kit</main>")
            .build();
    when(destinations.auditedSalesPagePublication(experiment)).thenReturn(Optional.of(publication));
  }

  /** Um produto interrompido e sem ciclo pode ser preparado sem reativação ou slot Opala. */
  @Test
  void acceptsExactLowTicketExperimentWithoutCycleAndPreservesStop() {
    var scope = context.scope("experiment:88", 7L, true);
    assertThat(scope.cycleId()).isNull();
    assertThat(scope.productVersion()).isEqualTo("v1");
    assertThat(context.snapshot("experiment:88").path("destinationUrl").asText())
        .isEqualTo(publication.getSalesPageUrl());
    assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.USER_STOPPED);
    verify(experiments, never()).save(any());
  }

  /** Janela vencida bloqueia até candidata planejada, antes de consultar ativos ou finanças. */
  @Test
  void blocksExpiredPlannedReferenceWithoutRevalidatingCurrentSources() {
    experiment.setStatus(ExperimentStatus.PLANNED);
    experiment.setEndDate(java.time.LocalDate.now(java.time.ZoneOffset.UTC).minusDays(1));
    product.setValidationDefinitionVersion(null);
    assertThat(context.historicalBlockReason("experiment:88", 7L))
        .contains("janela terminou", "não renove Plutus");
    assertThatThrownBy(() -> context.scope("experiment:88", 7L, true))
        .hasMessageContaining("não renove Plutus");
    verifyNoInteractions(destinations, creatives);
    verify(experiments, never()).save(any());
  }

  /** Estados terminais são históricos mesmo com data futura; data final de hoje segue válida. */
  @Test
  void distinguishesTerminalStateFromOpenWindowAndStoppedCandidate() {
    var today = java.time.LocalDate.now(java.time.ZoneOffset.UTC);
    experiment.setEndDate(today.plusDays(1));
    for (var status : List.of(ExperimentStatus.INVALIDATED, ExperimentStatus.FINISHED)) {
      experiment.setStatus(status);
      assertThat(context.historicalBlockReason("experiment:88", 7L)).contains("encerrado");
    }
    for (var status :
        List.of(ExperimentStatus.PLANNED, ExperimentStatus.PAUSED, ExperimentStatus.USER_STOPPED)) {
      experiment.setStatus(status);
      experiment.setEndDate(today);
      assertThat(context.historicalBlockReason("experiment:88", 7L)).isNull();
      assertThat(context.scope("experiment:88", 7L, true)).isNotNull();
    }
  }

  /** Ciclo fechado é resolvido pela referência, sem aceitar produto ou ciclo de outra origem. */
  @Test
  void blocksClosedCycleAndRejectsForeignIdentityBeforeCurrentSources() {
    var cycle = new com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle();
    cycle.setId(98321L);
    cycle.setProductId(product.getId());
    cycle.setStatus("ADJUSTED");
    when(cycles.findByExperimentId(experiment.getId())).thenReturn(Optional.of(cycle));
    assertThat(context.historicalBlockReason("experiment:88", 7L)).contains("#98321", "encerrado");
    assertThatThrownBy(() -> context.scope("experiment:88", 7L, true))
        .hasMessageContaining("encerrado");
    assertThatThrownBy(() -> context.historicalBlockReason("experiment:88", 98399L))
        .hasMessageContaining("outro produto");
    cycle.setProductId(98399L);
    assertThatThrownBy(() -> context.historicalBlockReason("experiment:88", 7L))
        .hasMessageContaining("outro produto");
    verifyNoInteractions(destinations, creatives);
  }

  /**
   * A tela recompõe a fotografia uma única vez por transação e nunca compartilha objeto mutável.
   */
  @Test
  void reusesSnapshotOnlyInsideCurrentTransaction() {
    TransactionSynchronizationManager.initSynchronization();
    try {
      assertThat(context.historicalBlockReason("experiment:88", 7L)).isNull();
      context.scope("experiment:88", 7L, true);
      var first = context.snapshot("experiment:88");
      first.put("destinationUrl", "https://mutated.test");
      var second = context.snapshot("experiment:88");

      assertThat(second.path("destinationUrl").asText()).isEqualTo(publication.getSalesPageUrl());
      verify(experiments, times(1)).findById(88L);
      verify(cycles, times(1)).findByExperimentId(88L);
      verify(destinations, times(1)).auditedSalesPagePublication(experiment);
    } finally {
      TransactionSynchronizationManager.getSynchronizations()
          .forEach(
              synchronization ->
                  synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
      TransactionSynchronizationManager.clearSynchronization();
    }

    context.snapshot("experiment:88");
    verify(experiments, times(2)).findById(88L);
  }

  /** Não permite usar outra identidade nem modificar uma campanha que está em operação. */
  @Test
  void rejectsOtherProductTypeAndRunningMutation() {
    assertThatThrownBy(() -> context.scope("experiment:88", 4L, true))
        .hasMessageContaining("outro produto");
    experiment.setStatus(ExperimentStatus.RUNNING);
    assertThatThrownBy(() -> context.scope("experiment:88", 7L, true))
        .hasMessageContaining("operação");
    assertThat(context.scope("experiment:88", 7L, false)).isNotNull();
    product.getProductTypeDefinition().setCode("PDE");
    assertThatThrownBy(() -> context.scope("experiment:88", 7L, false))
        .hasMessageContaining("Quartzo");
  }

  /** Rejeita uma publicação de outro experimento mesmo quando a URL parece válida. */
  @Test
  void rejectsPublicationFromAnotherExperiment() {
    publication.setExperimentId(92L);
    assertThatThrownBy(() -> context.snapshot("experiment:88"))
        .hasMessageContaining("outro experimento");
  }

  /** Separa a mensagem do sucessor do CTA pertencente à página auditada da origem. */
  @Test
  void acceptsAuditedPublicationFromExplicitLowTicketSource() {
    Experiment source = new Experiment();
    source.setId(88L);
    source.setPrimaryCta("Comprar o kit por R$ 67");
    experiment.setId(94L);
    experiment.setPrimaryCta("Ver amostras do kit e, se fizer sentido, comprar por R$ 67");
    experiment.setSourceExperiment(source);
    publication.setExperimentId(88L);
    when(experiments.findById(94L)).thenReturn(Optional.of(experiment));

    var snapshot = context.snapshot("experiment:94");

    assertThat(snapshot.path("experimentId").asLong()).isEqualTo(94L);
    assertThat(snapshot.path("publicationId").asLong()).isEqualTo(27L);
    assertThat(snapshot.path("landingExperimentId").asLong()).isEqualTo(88L);
    assertThat(snapshot.path("primaryCta").asText())
        .isEqualTo("Ver amostras do kit e, se fizer sentido, comprar por R$ 67");
    assertThat(snapshot.path("landingPrimaryCta").asText()).isEqualTo("Comprar o kit por R$ 67");
    assertThat(snapshot.path("destinationUrl").asText()).isEqualTo(publication.getSalesPageUrl());
  }

  /** Uma nova consulta é estável; mudança em página, promessa ou preço invalida o conjunto. */
  @Test
  void fingerprintsMaterialChangesWithoutUsingConsultationTime() {
    String initial = context.snapshot("experiment:88").path("fingerprint").asText();
    assertThat(context.snapshot("experiment:88").path("fingerprint").asText()).isEqualTo(initial);
    publication.setHtml("<main>Outra oferta</main>");
    assertThat(context.snapshot("experiment:88").path("fingerprint").asText())
        .isNotEqualTo(initial);
    String changed = context.snapshot("experiment:88").path("fingerprint").asText();
    experiment.setFunnelPromise("Outra promessa");
    assertThat(context.snapshot("experiment:88").path("fingerprint").asText())
        .isNotEqualTo(changed);
  }

  /** O retorno do MySQL e a ordem de chaves JSON não criam alterações comerciais fictícias. */
  @Test
  void keepsFingerprintForEquivalentDecimalsAndJsonPropertyOrder() {
    product.setPdeExperienceJson("{\"price\":67.00,\"format\":\"kit\"}");
    String initial = context.snapshot("experiment:88").path("fingerprint").asText();
    product.setPdeExperienceJson("{\"format\":\"kit\",\"price\":67}");
    experiment.setUnitPrice(new BigDecimal("67.00"));
    product.setCurrentPriceBrl(new BigDecimal("67.0000"));
    assertThat(context.snapshot("experiment:88").path("fingerprint").asText()).isEqualTo(initial);
  }

  /**
   * Isola a validade de cada prova para que finanças não invalidem página, criativo ou checkout.
   */
  @Test
  void fingerprintsEachPreparationActivityByItsOwnSources() {
    var initial = context.snapshot("experiment:88");
    var financialChange = initial.deepCopy();
    financialChange.putObject("financialPlan").put("revision", 2);
    assertThat(QuartzoCommercialContext.activityFingerprint("entry", financialChange))
        .isEqualTo(QuartzoCommercialContext.activityFingerprint("entry", initial));
    assertThat(QuartzoCommercialContext.activityFingerprint("economics", financialChange))
        .isNotEqualTo(QuartzoCommercialContext.activityFingerprint("economics", initial));

    var pageChange = initial.deepCopy();
    pageChange.put("destinationUrl", "https://example.test/other-kit");
    assertThat(QuartzoCommercialContext.activityFingerprint("entry", pageChange))
        .isNotEqualTo(QuartzoCommercialContext.activityFingerprint("entry", initial));
    assertThat(QuartzoCommercialContext.activityFingerprint("economics", pageChange))
        .isEqualTo(QuartzoCommercialContext.activityFingerprint("economics", initial));

    var landingCtaChange = initial.deepCopy();
    landingCtaChange.put("landingPrimaryCta", "Comprar outra página");
    assertThat(QuartzoCommercialContext.activityFingerprint("entry", landingCtaChange))
        .isNotEqualTo(QuartzoCommercialContext.activityFingerprint("entry", initial));
  }

  /** Apenas o anúncio final aprovado participa da revisão, preservando a separação de linhagens. */
  @Test
  void onlyIncludesApprovedLeafCreatives() {
    var old = new Creative();
    old.setId(1L);
    old.setStatus(CreativeStatus.READY);
    old.setAgentReviewStatus(CreativeAgentReviewStatus.APPROVED);
    var current = new Creative();
    current.setId(2L);
    current.setStatus(CreativeStatus.READY);
    current.setAgentReviewStatus(CreativeAgentReviewStatus.APPROVED);
    current.setSourceCreative(old);
    var draft = new Creative();
    draft.setId(3L);
    draft.setStatus(CreativeStatus.DRAFT);
    when(creatives.findByExperimentId(88L)).thenReturn(List.of(old, current, draft));
    var result = context.snapshot("experiment:88").path("creatives");
    assertThat(result.size()).isEqualTo(1);
    assertThat(result.get(0).path("id").asLong()).isEqualTo(2L);
  }
}
