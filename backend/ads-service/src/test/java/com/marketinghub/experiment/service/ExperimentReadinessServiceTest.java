package com.marketinghub.experiment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignObjective;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentTargetingSelection;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.experiment.dto.ExperimentReadinessIssueDto;
import com.marketinghub.experiment.dto.ExperimentReadinessIssueType;
import com.marketinghub.experiment.dto.ExperimentReadinessSummaryDto;
import com.marketinghub.experiment.dto.ExperimentRunningGateRequirementDto;
import com.marketinghub.experiment.salespageab.service.ExperimentSalesPageAbTestService;
import com.marketinghub.experiment.video.service.ExperimentVideoAssetService;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.gerasalespage.v1.GeraSalesPagePublicationAudit;
import com.marketinghub.gerasalespage.v1.GeraSalesPageStageCode;
import com.marketinghub.gerasalespage.v1.GeraSalesPageStageExecution;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.LeadPortalFlowQuestion;
import com.marketinghub.leadportal.LeadPortalQuestionType;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.planning.service.CommercialPlanLandingAssetService;
import com.marketinghub.product.Product;
import com.marketinghub.productai.ProductAiSubtype;
import com.marketinghub.producttype.ProductTypeDefinition;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentTargetingSelectionRepository;
import com.marketinghub.repository.jpa.experiment.salespagetype.ExperimentSalesPageTypeSelectionRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPagePublicationAuditRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPageStageExecutionRepository;
import com.marketinghub.targeting.TargetingCandidateType;
import com.marketinghub.targeting.TargetingElement;
import com.marketinghub.targeting.TargetingElementStatus;
import com.marketinghub.targeting.TargetingElementType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Valida as regras de prontidão comercial dos experimentos antes de liberar campanha paga. */
@ExtendWith(MockitoExtension.class)
class ExperimentReadinessServiceTest {

  @Mock private ExperimentService experimentService;
  @Mock private CreativeRepository creativeRepository;
  @Mock private ExperimentTargetingSelectionRepository targetingSelectionRepository;
  @Mock private GeraLandingStageExecutionRepository geraLandingStageExecutionRepository;
  @Mock private GeraSalesPageStageExecutionRepository geraSalesPageStageExecutionRepository;
  @Mock private GeraSalesPagePublicationAuditRepository geraSalesPagePublicationAuditRepository;
  @Mock private ExperimentVideoAssetService experimentVideoAssetService;
  @Mock private ExperimentSalesPageAbTestService salesPageAbTestService;
  @Mock private ExperimentSalesPageTypeSelectionRepository salesPageTypeSelectionRepository;
  @Mock private CommercialPlanLandingAssetService landingAssetService;
  @Mock private ExperimentDirectPdeActivationService directPdeActivationService;
  @Mock private IntegratedPdeJourneyEvidenceService integratedPdeJourneyEvidenceService;

  private ExperimentReadinessService service;

  @BeforeEach
  void setUp() {
    ExperimentCampaignDestinationPolicy campaignDestinationPolicy =
        new ExperimentCampaignDestinationPolicy(
            geraSalesPageStageExecutionRepository, geraSalesPagePublicationAuditRepository);
    service =
        new ExperimentReadinessService(
            experimentService,
            creativeRepository,
            targetingSelectionRepository,
            geraLandingStageExecutionRepository,
            campaignDestinationPolicy,
            experimentVideoAssetService,
            salesPageAbTestService,
            salesPageTypeSelectionRepository,
            landingAssetService,
            directPdeActivationService,
            integratedPdeJourneyEvidenceService);
    lenient()
        .when(
            landingAssetService.hasRequiredApprovedAssetReferences(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.nullable(String.class)))
        .thenReturn(true);
    lenient()
        .when(salesPageAbTestService.hasReadyActiveTest(org.mockito.ArgumentMatchers.anyLong()))
        .thenReturn(true);
    lenient()
        .when(
            directPdeActivationService.appliesTo(
                org.mockito.ArgumentMatchers.any(Experiment.class)))
        .thenReturn(false);
    lenient()
        .when(
            integratedPdeJourneyEvidenceService.appliesTo(
                org.mockito.ArgumentMatchers.any(Experiment.class)))
        .thenReturn(false);
  }

  @Test
  void shouldReportAllIssuesWhenNothingIsReady() {
    Long experimentId = 7L;
    Experiment experiment = buildExperiment(experimentId, 16L);

    when(experimentService.get(experimentId)).thenReturn(experiment);
    when(creativeRepository.countByExperimentIdAndStatusAndUsableImage(
            experimentId, CreativeStatus.READY))
        .thenReturn(0L);
    ExperimentReadinessSummaryDto summary = service.summarize(experimentId);

    assertThat(summary.hasCreatives()).isFalse();
    assertThat(summary.hasLeadPortalFlow()).isFalse();
    assertThat(summary.hasCompleteTargeting()).isFalse();
    assertThat(summary.hasGeraLandingPipeline()).isFalse();
    assertThat(summary.geraLandingCompletedStageCount()).isZero();
    assertThat(summary.missingTargetingTypes())
        .containsExactly(
            TargetingElementType.INTEREST,
            TargetingElementType.JOB_TITLE,
            TargetingElementType.BEHAVIOR);
    assertThat(summary.issues()).hasSize(4);
    assertThat(summary.eligibleForRunning()).isFalse();
    assertThat(summary.runningGateRequirements())
        .filteredOn(requirement -> !requirement.ready())
        .extracting(requirement -> requirement.code())
        .contains("LANDING_APPROVED", "CREATIVE_APPROVED", "TARGETING_READY", "NO_BLOCKING_STAGES");
    assertThat(summary.issues())
        .extracting(ExperimentReadinessIssueDto::type)
        .containsExactlyInAnyOrder(
            ExperimentReadinessIssueType.CREATIVE,
            ExperimentReadinessIssueType.LEAD_PORTAL_FLOW,
            ExperimentReadinessIssueType.TARGETING,
            ExperimentReadinessIssueType.GERA_LANDING);
  }

  @Test
  void shouldReturnNoIssuesWhenEverythingIsReady() {
    Long experimentId = 8L;
    Experiment experiment = buildExperiment(experimentId, 18L);
    experiment.setCreativeApproved(false);
    LeadPortalFlow flow = new LeadPortalFlow();
    flow.setApproved(true);
    experiment.setLeadPortalFlow(flow);

    when(experimentService.get(experimentId)).thenReturn(experiment);
    when(creativeRepository.countByExperimentIdAndStatusAndUsableImage(
            experimentId, CreativeStatus.READY))
        .thenReturn(2L);
    when(creativeRepository.existsByExperimentIdAndStatusAndUsableImage(
            experimentId, CreativeStatus.READY))
        .thenReturn(true);
    mockPublishableSelection(
        experimentId, TargetingCandidateType.INTEREST, TargetingElementType.INTEREST);
    mockCompletedGeraLandingStages(experimentId);
    ExperimentReadinessSummaryDto summary = service.summarize(experimentId);

    assertThat(summary.hasCreatives()).isTrue();
    assertThat(summary.hasLeadPortalFlow()).isTrue();
    assertThat(summary.hasCompleteTargeting()).isTrue();
    assertThat(summary.hasGeraLandingPipeline()).isTrue();
    assertThat(summary.geraLandingCompletedStageCount())
        .isEqualTo(summary.geraLandingRequiredStageCount());
    assertThat(summary.missingTargetingTypes()).isEmpty();
    assertThat(summary.issues()).isEmpty();
    assertThat(summary.eligibleForRunning()).isTrue();
    assertThat(summary.runningGateRequirements()).allMatch(requirement -> requirement.ready());
  }

  /** Bloqueia a publicação Meta quando o teto total não acompanha o orçamento diário. */
  @Test
  void shouldBlockFacebookCampaignWithoutAuthorizedMediaSpendLimit() {
    Long experimentId = 801L;
    Experiment experiment = buildExperiment(experimentId, 811L);
    experiment.setMediaSpendLimit(null);

    when(experimentService.get(experimentId)).thenReturn(experiment);

    ExperimentReadinessSummaryDto summary = service.summarize(experimentId);

    assertThat(summary.issues())
        .extracting(ExperimentReadinessIssueDto::type)
        .contains(ExperimentReadinessIssueType.BUDGET);
    assertThat(summary.runningGateRequirements())
        .filteredOn(requirement -> requirement.code().equals("MEDIA_BUDGET_READY"))
        .singleElement()
        .satisfies(requirement -> assertThat(requirement.ready()).isFalse());
    assertThat(service.computeMissingConfiguration(experiment)).contains("mediaSpendLimit");
  }

  /** Garante que abordagem individual preserve gates comerciais sem exigir segmentação Meta. */
  @Test
  void shouldTreatMetaTargetingAsNotApplicableForDirectOneToOneChannel() {
    Long experimentId = 81L;
    Experiment experiment = buildExperiment(experimentId, 91L);
    experiment.setPlatform(ExperimentPlatform.DIRECT_ONE_TO_ONE);
    clearMediaSpendPlan(experiment);
    LeadPortalFlow flow = new LeadPortalFlow();
    flow.setApproved(true);
    experiment.setLeadPortalFlow(flow);

    when(experimentService.get(experimentId)).thenReturn(experiment);
    when(creativeRepository.countByExperimentIdAndStatusAndUsableImage(
            experimentId, CreativeStatus.READY))
        .thenReturn(1L);
    when(creativeRepository.existsByExperimentIdAndStatusAndUsableImage(
            experimentId, CreativeStatus.READY))
        .thenReturn(true);
    mockCompletedGeraLandingStages(experimentId);

    ExperimentReadinessSummaryDto summary = service.summarize(experimentId);

    assertThat(summary.hasCompleteTargeting()).isTrue();
    assertThat(summary.missingTargetingTypes()).isEmpty();
    assertThat(summary.issues())
        .extracting(ExperimentReadinessIssueDto::type)
        .doesNotContain(ExperimentReadinessIssueType.TARGETING);
    assertThat(summary.runningGateRequirements())
        .filteredOn(requirement -> requirement.code().equals("TARGETING_READY"))
        .allMatch(requirement -> requirement.ready())
        .extracting(requirement -> requirement.title())
        .containsExactly("Canal individual pronto");
  }

  @Test
  void shouldUseReadyCreativeAsApprovalSourceEvenWhenExperimentFlagIsStale() {
    Long experimentId = 22L;
    Experiment experiment = buildExperiment(experimentId, 32L);
    experiment.setCreativeApproved(false);
    experiment.setFollowUpActionUrl("https://example.com/landing/22");

    when(creativeRepository.existsByExperimentIdAndStatusAndUsableImage(
            experimentId, CreativeStatus.READY))
        .thenReturn(true);
    mockPublishableSelection(
        experimentId, TargetingCandidateType.BEHAVIOR, TargetingElementType.BEHAVIOR);

    assertThat(service.computeMissingConfiguration(experiment)).isEmpty();
    assertThat(service.isReadyForCampaign(experiment)).isTrue();
  }

  /** Garante que criativo de vídeo sem áudio aprovado não libera publicação Meta. */
  @Test
  void shouldBlockVideoCreativeWithoutAudibleApprovedVideo() {
    Long experimentId = 72L;
    Experiment experiment = buildExperiment(experimentId, 82L);
    experiment.setFollowUpActionUrl("https://example.com/landing/72");
    completeCommercialContract(experiment);
    Creative videoCreative =
        Creative.builder()
            .id(501L)
            .experiment(experiment)
            .format("VIDEO")
            .headline("Cliente some depois da manutencao")
            .primaryText("Enxergar riscos e encaixes em 7 dias antes do cliente sumir")
            .cta("SHOP_NOW")
            .videoUrl("https://cdn.example/video-sem-som.mp4")
            .status(CreativeStatus.READY)
            .build();

    when(creativeRepository.existsByExperimentIdAndStatusAndUsableImage(
            experimentId, CreativeStatus.READY))
        .thenReturn(true);
    when(creativeRepository.findByExperimentId(experimentId)).thenReturn(List.of(videoCreative));
    when(experimentVideoAssetService.hasReadyApprovedAudibleVideoForPublication(
            experimentId, null, "https://cdn.example/video-sem-som.mp4"))
        .thenReturn(false);
    mockPublishableSelection(
        experimentId, TargetingCandidateType.INTEREST, TargetingElementType.INTEREST);

    assertThat(service.computeMissingConfiguration(experiment)).containsExactly("creativeApproval");
    assertThat(service.isReadyForCampaign(experiment)).isFalse();
  }

  /** Garante que copy sem conexão mínima com dor, promessa, recompensa ou CTA bloqueia campanha. */
  @Test
  void shouldBlockCreativeWithMisalignedCopy() {
    Long experimentId = 73L;
    Experiment experiment = buildExperiment(experimentId, 83L);
    experiment.setFollowUpActionUrl("https://example.com/landing/73");
    completeCommercialContract(experiment);
    Creative imageCreative =
        Creative.builder()
            .id(502L)
            .experiment(experiment)
            .format("IMAGE")
            .headline("Oferta imperdivel")
            .primaryText("Clique e veja novidades para mudar sua rotina")
            .cta("LEARN_MORE")
            .imageUrl("https://cdn.example/creative.png")
            .status(CreativeStatus.READY)
            .build();

    when(creativeRepository.existsByExperimentIdAndStatusAndUsableImage(
            experimentId, CreativeStatus.READY))
        .thenReturn(true);
    when(creativeRepository.findByExperimentId(experimentId)).thenReturn(List.of(imageCreative));
    mockPublishableSelection(
        experimentId, TargetingCandidateType.INTEREST, TargetingElementType.INTEREST);

    assertThat(service.computeMissingConfiguration(experiment)).containsExactly("creativeApproval");
    assertThat(service.isReadyForCampaign(experiment)).isFalse();
  }

  /** Garante que vídeo obrigatório sem aprovação bloqueia a campanha. */
  @Test
  void shouldBlockCampaignWhenRequiredVideoIsNotReadyAndApproved() {
    Experiment experiment = buildExperiment(39L, 49L);
    experiment.setFollowUpActionUrl("https://example.com/landing/39");

    when(creativeRepository.existsByExperimentIdAndStatusAndUsableImage(39L, CreativeStatus.READY))
        .thenReturn(true);
    when(experimentVideoAssetService.hasRequiredVideoBlockingRelease(39L)).thenReturn(true);
    mockPublishableSelection(39L, TargetingCandidateType.INTEREST, TargetingElementType.INTEREST);

    assertThat(service.computeMissingConfiguration(experiment))
        .containsExactly("experimentVideoAsset");
    assertThat(service.isReadyForCampaign(experiment)).isFalse();
  }

  /** Garante que pagina de venda com video humano bloqueia campanha sem video pronto. */
  @Test
  void shouldBlockCampaignWhenHumanVideoSalesPageTypeHasNoReadyApprovedVideo() {
    Experiment experiment = buildExperiment(46L, 56L);
    experiment.setCampaignObjective(ExperimentCampaignObjective.SALES);
    experiment.setFollowUpActionUrl("https://example.com/sales/46");
    completeCommercialContract(experiment);

    when(creativeRepository.existsByExperimentIdAndStatusAndUsableImage(46L, CreativeStatus.READY))
        .thenReturn(true);
    when(salesPageTypeSelectionRepository.existsByExperimentIdAndSalesPageTypeCodeAndActiveTrue(
            46L, "HUMAN_VIDEO_SALES_PAGE"))
        .thenReturn(true);
    when(experimentVideoAssetService.hasReadyApprovedVideo(46L)).thenReturn(false);
    mockPublishableSelection(46L, TargetingCandidateType.INTEREST, TargetingElementType.INTEREST);
    mockCompletedGeraSalesPagePublication(46L);
    mockSalesPageAudit(
        46L,
        "https://example.com/sales/46",
        "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=exp46",
        trackedSalesPageHtml());

    assertThat(service.computeMissingConfiguration(experiment))
        .containsExactly("experimentVideoAsset");
    assertThat(service.isReadyForCampaign(experiment)).isFalse();
  }

  /** Garante que pagina de venda com video humano libera somente com video pronto e aprovado. */
  @Test
  void shouldAllowHumanVideoSalesPageTypeWithReadyApprovedVideo() {
    Experiment experiment = buildExperiment(47L, 57L);
    experiment.setCampaignObjective(ExperimentCampaignObjective.SALES);
    experiment.setFollowUpActionUrl("https://example.com/sales/47");
    completeCommercialContract(experiment);

    when(creativeRepository.existsByExperimentIdAndStatusAndUsableImage(47L, CreativeStatus.READY))
        .thenReturn(true);
    when(salesPageTypeSelectionRepository.existsByExperimentIdAndSalesPageTypeCodeAndActiveTrue(
            47L, "HUMAN_VIDEO_SALES_PAGE"))
        .thenReturn(true);
    when(experimentVideoAssetService.hasReadyApprovedVideo(47L)).thenReturn(true);
    mockPublishableSelection(47L, TargetingCandidateType.INTEREST, TargetingElementType.INTEREST);
    mockCompletedGeraSalesPagePublication(47L);
    mockSalesPageAudit(
        47L,
        "https://example.com/sales/47",
        "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=exp47",
        trackedSalesPageHtml());

    assertThat(service.computeMissingConfiguration(experiment)).isEmpty();
    assertThat(service.isReadyForCampaign(experiment)).isTrue();
  }

  /** Garante que teste A/B ativo incompleto bloqueia a liberacao para campanha. */
  @Test
  void shouldBlockCampaignWhenActiveSalesPageAbTestIsIncomplete() {
    Experiment experiment = buildExperiment(44L, 54L);
    experiment.setCampaignObjective(ExperimentCampaignObjective.SALES);
    experiment.setFollowUpActionUrl("https://example.com/landing/44");
    completeCommercialContract(experiment);

    when(creativeRepository.existsByExperimentIdAndStatusAndUsableImage(44L, CreativeStatus.READY))
        .thenReturn(true);
    when(salesPageAbTestService.hasReadyActiveTest(44L)).thenReturn(false);
    mockPublishableSelection(44L, TargetingCandidateType.INTEREST, TargetingElementType.INTEREST);
    mockCompletedGeraSalesPagePublication(44L);
    mockSalesPageAudit(
        44L,
        "https://example.com/landing/44",
        "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=exp44",
        trackedSalesPageHtml());

    assertThat(service.computeMissingConfiguration(experiment)).containsExactly("salesPageAbTest");
    assertThat(service.isReadyForCampaign(experiment)).isFalse();
  }

  /** Garante que teste A/B de venda incompleto nao bloqueia campanha de captura de leads. */
  @Test
  void shouldNotRequireSalesPageAbTestForLeadGenerationNicheTest() {
    Experiment experiment = buildExperiment(45L, 55L);
    experiment.setExperimentType(ExperimentType.NICHE_TEST);
    experiment.setCampaignObjective(ExperimentCampaignObjective.LEADS);
    experiment.setFollowUpActionUrl("https://example.com/landing/45");

    when(creativeRepository.existsByExperimentIdAndStatusAndUsableImage(45L, CreativeStatus.READY))
        .thenReturn(true);
    mockPublishableSelection(45L, TargetingCandidateType.INTEREST, TargetingElementType.INTEREST);

    assertThat(service.computeMissingConfiguration(experiment)).isEmpty();
    assertThat(service.isReadyForCampaign(experiment)).isTrue();
  }

  @Test
  void shouldRequireAdDestinationToPointToSalesPageForLowTicketCampaign() {
    Experiment experiment = buildExperiment(56L, 66L);
    experiment.setExperimentType(ExperimentType.LOW_TICKET_PRODUCT);
    experiment.setFollowUpActionUrl(
        "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=abc");
    experiment.getNiche().setFacebookPixelId("pixel-exp56");
    completeCommercialContract(experiment);

    when(creativeRepository.existsByExperimentIdAndStatusAndUsableImage(56L, CreativeStatus.READY))
        .thenReturn(true);
    mockPublishableSelection(56L, TargetingCandidateType.INTEREST, TargetingElementType.INTEREST);
    mockCompletedGeraSalesPagePublication(56L);
    mockSalesPageAudit(
        56L,
        "https://pagamentopalf.site/sales-page-exp56.html",
        "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=abc",
        trackedSalesPageHtml());

    assertThat(service.computeMissingConfiguration(experiment))
        .containsExactly("salesPageAdDestination");
    assertThat(service.isReadyForCampaign(experiment)).isFalse();
  }

  @Test
  void shouldRequireSalesPageDestinationForAnySalesCampaignObjective() {
    Experiment experiment = buildExperiment(60L, 70L);
    experiment.setCampaignObjective(ExperimentCampaignObjective.SALES);
    experiment.setFollowUpActionUrl(
        "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=sales-direct");
    experiment.getNiche().setFacebookPixelId("pixel-exp60");
    completeCommercialContract(experiment);

    when(creativeRepository.existsByExperimentIdAndStatusAndUsableImage(60L, CreativeStatus.READY))
        .thenReturn(true);
    mockPublishableSelection(60L, TargetingCandidateType.INTEREST, TargetingElementType.INTEREST);
    mockCompletedGeraSalesPagePublication(60L);
    mockSalesPageAudit(
        60L,
        "https://pagamentopalf.site/sales-page-exp60.html",
        "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=sales-direct",
        trackedSalesPageHtml());

    assertThat(service.computeMissingConfiguration(experiment))
        .containsExactly("salesPageAdDestination");
    assertThat(service.isReadyForCampaign(experiment)).isFalse();
  }

  @Test
  void shouldAllowPersonalizedSampleProductAiCampaignAfterGeraSalesPagePublishesLeadPortalFunnel() {
    Experiment experiment = buildExperiment(57L, 30L);
    experiment.setExperimentType(ExperimentType.LOW_TICKET_PRODUCT);
    experiment.setCampaignObjective(ExperimentCampaignObjective.SALES);
    experiment.setProductAiSubtype(ProductAiSubtype.AI_PERSONALIZED_SAMPLE);
    experiment.setLeadPortalFlow(
        productAiFlow(
            "email",
            "negocio_projeto",
            "contexto_atual",
            "objetivo_visual",
            "dados_personalizacao"));
    experiment.getLeadPortalFlow().setSlug("decoraia-express-exp-57");
    experiment.setFollowUpActionUrl(
        "https://oportunidadebrasil.shop/flows/decoraia-express-exp-57");
    experiment.getNiche().setFacebookPixelId("pixel-exp57");
    completeCommercialContract(experiment);

    when(creativeRepository.existsByExperimentIdAndStatusAndUsableImage(57L, CreativeStatus.READY))
        .thenReturn(true);
    mockPublishableSelection(57L, TargetingCandidateType.INTEREST, TargetingElementType.INTEREST);
    mockCompletedGeraSalesPagePublication(57L);
    mockSalesPageAudit(
        57L,
        "https://oportunidadebrasil.shop/flows/decoraia-express-exp-57",
        null,
        trackedSalesPageHtml());

    assertThat(service.computeMissingConfiguration(experiment)).isEmpty();
    assertThat(service.isReadyForCampaign(experiment)).isTrue();
  }

  /** Garante que a microamostra social curta atende a prontidão sem campos do template genérico. */
  @Test
  void shouldAllowPersonalizedSampleProductAiWithSocialMediaMicroSampleContract() {
    Experiment experiment = buildExperiment(58L, 30L);
    experiment.setProductAiSubtype(ProductAiSubtype.AI_PERSONALIZED_SAMPLE);
    experiment.setLeadPortalFlow(
        productAiFlow("nome_profissional", "servico_divulgado", "estilo_visual", "email"));
    experiment.setFollowUpActionUrl(
        "https://oportunidadebrasil.shop/flows/product-ai-exp-58-personalized-sample");

    when(creativeRepository.existsByExperimentIdAndStatusAndUsableImage(58L, CreativeStatus.READY))
        .thenReturn(true);
    mockPublishableSelection(58L, TargetingCandidateType.INTEREST, TargetingElementType.INTEREST);

    assertThat(service.computeMissingConfiguration(experiment)).isEmpty();
    assertThat(service.isReadyForCampaign(experiment)).isTrue();
  }

  /** Garante que a microamostra auditada pelo GeraSalesPage não exige o GeraLanding paralelo. */
  @Test
  void shouldSummarizePersonalizedSampleWithAuditedGeraSalesPage() {
    Long experimentId = 70L;
    Experiment experiment = buildExperiment(experimentId, 30L);
    experiment.setProductAiSubtype(ProductAiSubtype.AI_PERSONALIZED_SAMPLE);
    experiment.setLeadPortalFlow(
        productAiFlow("nome_profissional", "servico_divulgado", "estilo_visual", "email"));
    experiment.getLeadPortalFlow().setSlug("product-ai-exp-70-personalized-sample");
    experiment.setFollowUpActionUrl(
        "https://oportunidadebrasil.shop/flows/product-ai-exp-70-personalized-sample");

    when(experimentService.get(experimentId)).thenReturn(experiment);
    when(creativeRepository.countByExperimentIdAndStatusAndUsableImage(
            experimentId, CreativeStatus.READY))
        .thenReturn(1L);
    when(creativeRepository.existsByExperimentIdAndStatusAndUsableImage(
            experimentId, CreativeStatus.READY))
        .thenReturn(true);
    mockPublishableSelection(
        experimentId, TargetingCandidateType.INTEREST, TargetingElementType.INTEREST);
    mockCompletedGeraSalesPagePublication(experimentId);
    mockSalesPageAudit(
        experimentId, experiment.getFollowUpActionUrl(), null, trackedSalesPageHtml());

    ExperimentReadinessSummaryDto summary = service.summarize(experimentId);

    assertThat(summary.issues()).isEmpty();
    assertThat(summary.hasGeraLandingPipeline()).isFalse();
  }

  /** Garante que a microamostra sem publicação auditada não seja declarada pronta. */
  @Test
  void shouldBlockPersonalizedSampleWithoutAuditedGeraSalesPage() {
    Long experimentId = 71L;
    Experiment experiment = buildExperiment(experimentId, 30L);
    experiment.setProductAiSubtype(ProductAiSubtype.AI_PERSONALIZED_SAMPLE);
    experiment.setLeadPortalFlow(
        productAiFlow("nome_profissional", "servico_divulgado", "estilo_visual", "email"));
    experiment.setFollowUpActionUrl(
        "https://oportunidadebrasil.shop/flows/product-ai-exp-71-personalized-sample");

    when(experimentService.get(experimentId)).thenReturn(experiment);
    when(creativeRepository.countByExperimentIdAndStatusAndUsableImage(
            experimentId, CreativeStatus.READY))
        .thenReturn(1L);
    when(creativeRepository.existsByExperimentIdAndStatusAndUsableImage(
            experimentId, CreativeStatus.READY))
        .thenReturn(true);
    mockPublishableSelection(
        experimentId, TargetingCandidateType.INTEREST, TargetingElementType.INTEREST);

    ExperimentReadinessSummaryDto summary = service.summarize(experimentId);

    assertThat(summary.issues())
        .extracting(ExperimentReadinessIssueDto::type)
        .containsExactly(ExperimentReadinessIssueType.GERA_SALES_PAGE);
  }

  /** Garante que a regra por template não libera uma microamostra social incompleta. */
  @Test
  void shouldBlockPersonalizedSampleProductAiWithIncompleteSocialMediaContract() {
    Experiment experiment = buildExperiment(59L, 30L);
    experiment.setProductAiSubtype(ProductAiSubtype.AI_PERSONALIZED_SAMPLE);
    experiment.setLeadPortalFlow(productAiFlow("nome_profissional", "servico_divulgado", "email"));
    experiment.setFollowUpActionUrl(
        "https://oportunidadebrasil.shop/flows/product-ai-exp-59-personalized-sample");

    when(creativeRepository.existsByExperimentIdAndStatusAndUsableImage(59L, CreativeStatus.READY))
        .thenReturn(true);
    mockPublishableSelection(59L, TargetingCandidateType.INTEREST, TargetingElementType.INTEREST);

    assertThat(service.computeMissingConfiguration(experiment))
        .containsExactly("productAiPersonalizedSampleFunnel");
    assertThat(service.isReadyForCampaign(experiment)).isFalse();
  }

  @Test
  void shouldAllowLowTicketCampaignOnlyAfterGeraSalesPagePublicationPackage() {
    Experiment experiment = buildExperiment(53L, 63L);
    experiment.setExperimentType(ExperimentType.LOW_TICKET_PRODUCT);
    experiment.setFollowUpActionUrl("https://pagamentopalf.site/sales-page-exp53.html");
    experiment.getNiche().setFacebookPixelId("pixel-exp53");
    completeCommercialContract(experiment);

    when(creativeRepository.existsByExperimentIdAndStatusAndUsableImage(53L, CreativeStatus.READY))
        .thenReturn(true);
    mockPublishableSelection(53L, TargetingCandidateType.BEHAVIOR, TargetingElementType.BEHAVIOR);
    mockCompletedGeraSalesPagePublication(53L);
    mockSalesPageAudit(
        53L,
        "https://pagamentopalf.site/sales-page-exp53.html",
        "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=exp53",
        trackedSalesPageHtml());

    assertThat(service.computeMissingConfiguration(experiment)).isEmpty();
    assertThat(service.isReadyForCampaign(experiment)).isTrue();
  }

  /** Garante que pagina sem arquivos reais aprovados nao possa liberar campanha. */
  @Test
  void shouldBlockLowTicketCampaignWithoutApprovedProductEvidenceOnLanding() {
    Experiment experiment = buildExperiment(54L, 64L);
    experiment.setExperimentType(ExperimentType.LOW_TICKET_PRODUCT);
    experiment.setFollowUpActionUrl("https://pagamentopalf.site/sales-page-exp54.html");
    experiment.getNiche().setFacebookPixelId("pixel-exp54");
    completeCommercialContract(experiment);

    when(creativeRepository.existsByExperimentIdAndStatusAndUsableImage(54L, CreativeStatus.READY))
        .thenReturn(true);
    mockPublishableSelection(54L, TargetingCandidateType.BEHAVIOR, TargetingElementType.BEHAVIOR);
    mockCompletedGeraSalesPagePublication(54L);
    mockSalesPageAudit(
        54L,
        "https://pagamentopalf.site/sales-page-exp54.html",
        "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=exp54",
        trackedSalesPageHtml());
    when(landingAssetService.hasRequiredApprovedAssetReferences(
            org.mockito.ArgumentMatchers.eq(54L), org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(false);

    assertThat(service.computeMissingConfiguration(experiment))
        .containsExactly("landingApprovedProductEvidence");
    assertThat(service.isReadyForCampaign(experiment)).isFalse();
  }

  /** Garante que o funil PDE MUSA bloqueia domínio raiz sem versão explícita. */
  @Test
  void shouldBlockPdeMembershipCampaignWithRootMusaLoginDestination() {
    Experiment experiment = buildExperiment(67L, 77L);
    experiment.setExperimentType(ExperimentType.PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL);
    experiment.setCampaignObjective(ExperimentCampaignObjective.SALES);
    experiment.setFollowUpActionUrl("https://clubemusa.com.br");
    completeCommercialContract(experiment);

    when(creativeRepository.existsByExperimentIdAndStatusAndUsableImage(67L, CreativeStatus.READY))
        .thenReturn(true);
    mockPublishableSelection(67L, TargetingCandidateType.INTEREST, TargetingElementType.INTEREST);

    assertThat(service.computeMissingConfiguration(experiment))
        .contains("pdeMembershipDestination");
    assertThat(service.isReadyForCampaign(experiment)).isFalse();
  }

  /** Garante que o funil PDE MUSA aceita subdomínio de slot produtivo versionado. */
  @Test
  void shouldAllowPdeMembershipCampaignWithVersionedMusaSlotDestination() {
    Experiment experiment = buildExperiment(71L, 77L);
    experiment.setExperimentType(ExperimentType.PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL);
    experiment.setCampaignObjective(ExperimentCampaignObjective.SALES);
    experiment.setFollowUpActionUrl("https://v5.clubemusa.com.br");
    completeCommercialContract(experiment);

    when(creativeRepository.existsByExperimentIdAndStatusAndUsableImage(71L, CreativeStatus.READY))
        .thenReturn(true);
    mockPublishableSelection(71L, TargetingCandidateType.INTEREST, TargetingElementType.INTEREST);

    assertThat(service.computeMissingConfiguration(experiment)).isEmpty();
    assertThat(service.isReadyForCampaign(experiment)).isTrue();
  }

  /** Garante que o funil PDE MUSA bloqueia destino que nao seja a entrada do Clube MUSA. */
  @Test
  void shouldBlockPdeMembershipCampaignWhenDestinationDoesNotPointToMusaLogin() {
    Experiment experiment = buildExperiment(68L, 78L);
    experiment.setExperimentType(ExperimentType.PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL);
    experiment.setCampaignObjective(ExperimentCampaignObjective.SALES);
    experiment.setFollowUpActionUrl(
        "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=pde");
    completeCommercialContract(experiment);

    when(creativeRepository.existsByExperimentIdAndStatusAndUsableImage(68L, CreativeStatus.READY))
        .thenReturn(true);
    mockPublishableSelection(68L, TargetingCandidateType.INTEREST, TargetingElementType.INTEREST);

    assertThat(service.computeMissingConfiguration(experiment))
        .containsExactly("pdeMembershipDestination");
    assertThat(service.isReadyForCampaign(experiment)).isFalse();
  }

  /** Garante que o resumo do PDE MUSA nao exige GeraLanding, Lead Portal nem GeraSalesPage. */
  @Test
  void shouldSummarizePdeMembershipWithoutTraditionalLandingPipelines() {
    Long experimentId = 69L;
    Experiment experiment = buildExperiment(experimentId, 79L);
    experiment.setExperimentType(ExperimentType.PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL);
    experiment.setCampaignObjective(ExperimentCampaignObjective.SALES);
    experiment.setFollowUpActionUrl("https://v5.clubemusa.com.br");
    completeCommercialContract(experiment);

    when(experimentService.get(experimentId)).thenReturn(experiment);
    when(creativeRepository.countByExperimentIdAndStatusAndUsableImage(
            experimentId, CreativeStatus.READY))
        .thenReturn(1L);
    when(creativeRepository.existsByExperimentIdAndStatusAndUsableImage(
            experimentId, CreativeStatus.READY))
        .thenReturn(true);
    mockPublishableSelection(
        experimentId, TargetingCandidateType.INTEREST, TargetingElementType.INTEREST);

    ExperimentReadinessSummaryDto summary = service.summarize(experimentId);

    assertThat(summary.issues()).isEmpty();
    assertThat(summary.hasCreatives()).isTrue();
    assertThat(summary.hasCompleteTargeting()).isTrue();
  }

  /** Reutiliza a homologação da superfície PDE no sucessor Facebook sem herdar seu criativo. */
  @Test
  void shouldReuseAuditedPdeDestinationForFacebookSuccessor() {
    Long experimentId = 91L;
    Product product = Product.builder().id(4L).build();
    Experiment source = buildExperiment(90L, 79L);
    source.setProduct(product);
    source.setExperimentType(ExperimentType.PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL);
    source.setPlatform(ExperimentPlatform.DIRECT_ONE_TO_ONE);
    source.setFollowUpActionUrl("https://v7.clubemusa.com.br");
    source.setCommercialCheckoutUrl("https://go.pepper.com.br/owm6x");

    Experiment successor = buildExperiment(experimentId, 79L);
    successor.setProduct(product);
    successor.setSourceExperiment(source);
    successor.setExperimentType(ExperimentType.PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL);
    successor.setCampaignObjective(ExperimentCampaignObjective.SALES);
    successor.setFollowUpActionUrl("https://v7.clubemusa.com.br");
    successor.setCommercialCheckoutUrl("https://go.pepper.com.br/owm6x");
    completeCommercialContract(successor);

    when(experimentService.get(experimentId)).thenReturn(successor);
    when(creativeRepository.countByExperimentIdAndStatusAndUsableImage(
            experimentId, CreativeStatus.READY))
        .thenReturn(1L);
    when(creativeRepository.existsByExperimentIdAndStatusAndUsableImage(
            experimentId, CreativeStatus.READY))
        .thenReturn(true);
    when(directPdeActivationService.isReadyForActivation(
            org.mockito.ArgumentMatchers.any(Experiment.class)))
        .thenAnswer(invocation -> invocation.getArgument(0) == source);
    mockPublishableSelection(
        experimentId, TargetingCandidateType.INTEREST, TargetingElementType.INTEREST);

    ExperimentReadinessSummaryDto summary = service.summarize(experimentId);

    assertThat(summary.issues()).isEmpty();
    assertThat(summary.eligibleForRunning()).isTrue();
    assertThat(service.computeMissingConfiguration(successor)).isEmpty();
  }

  /** Mantém o bloqueio quando o sucessor altera o checkout homologado pelo experimento anterior. */
  @Test
  void shouldRejectPdeSuccessorDestinationReuseWhenCheckoutChanges() {
    Product product = Product.builder().id(4L).build();
    Experiment source = buildExperiment(90L, 79L);
    source.setProduct(product);
    source.setExperimentType(ExperimentType.PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL);
    source.setPlatform(ExperimentPlatform.DIRECT_ONE_TO_ONE);
    source.setFollowUpActionUrl("https://v7.clubemusa.com.br");
    source.setCommercialCheckoutUrl("https://go.pepper.com.br/owm6x");

    Experiment successor = buildExperiment(91L, 79L);
    successor.setProduct(product);
    successor.setSourceExperiment(source);
    successor.setExperimentType(ExperimentType.PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL);
    successor.setCampaignObjective(ExperimentCampaignObjective.SALES);
    successor.setFollowUpActionUrl("https://v7.clubemusa.com.br");
    successor.setCommercialCheckoutUrl("https://checkout.example/outro");
    completeCommercialContract(successor);

    when(creativeRepository.existsByExperimentIdAndStatusAndUsableImage(91L, CreativeStatus.READY))
        .thenReturn(true);
    when(directPdeActivationService.isReadyForActivation(
            org.mockito.ArgumentMatchers.any(Experiment.class)))
        .thenAnswer(invocation -> invocation.getArgument(0) == source);
    when(landingAssetService.hasRequiredApprovedAssetReferences(91L, null)).thenReturn(false);
    mockPublishableSelection(91L, TargetingCandidateType.INTEREST, TargetingElementType.INTEREST);

    assertThat(service.computeMissingConfiguration(successor))
        .contains("landingApprovedProductEvidence");
  }

  /** Libera abordagem PDE direta pelo run homologado sem inventar um criativo de mídia paga. */
  @Test
  void shouldUseReadyProductionRunForDirectPdeReadiness() {
    Long experimentId = 90L;
    Experiment experiment = buildExperiment(experimentId, 79L);
    experiment.setExperimentType(ExperimentType.PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL);
    experiment.setPlatform(ExperimentPlatform.DIRECT_ONE_TO_ONE);
    clearMediaSpendPlan(experiment);
    experiment.setCampaignObjective(ExperimentCampaignObjective.SALES);
    experiment.setFollowUpActionUrl("https://v7.clubemusa.com.br");
    experiment.setProduct(Product.builder().id(4L).build());
    completeCommercialContract(experiment);

    when(experimentService.get(experimentId)).thenReturn(experiment);
    when(creativeRepository.countByExperimentIdAndStatusAndUsableImage(
            experimentId, CreativeStatus.READY))
        .thenReturn(0L);
    when(directPdeActivationService.appliesTo(experiment)).thenReturn(true);
    when(directPdeActivationService.isReadyForActivation(experiment)).thenReturn(true);

    ExperimentReadinessSummaryDto summary = service.summarize(experimentId);

    assertThat(summary.hasCreatives()).isFalse();
    assertThat(summary.issues()).isEmpty();
    assertThat(summary.eligibleForRunning()).isTrue();
    assertThat(summary.runningGateRequirements())
        .filteredOn(requirement -> requirement.code().equals("CREATIVE_APPROVED"))
        .singleElement()
        .satisfies(
            requirement -> {
              assertThat(requirement.ready()).isTrue();
              assertThat(requirement.title()).isEqualTo("Material da abordagem pronto");
            });
  }

  /** Libera o Rigel pelo gate BPM integrado sem exigir o pipeline legado de página de venda. */
  @Test
  void shouldUseIntegratedPdeJourneyForRigelReadiness() {
    Long experimentId = 89L;
    Experiment experiment = buildExperiment(experimentId, 79L);
    experiment.setExperimentType(ExperimentType.LOW_TICKET_PRODUCT);
    experiment.setPlatform(ExperimentPlatform.DIRECT_ONE_TO_ONE);
    clearMediaSpendPlan(experiment);
    experiment.setCampaignObjective(ExperimentCampaignObjective.SALES);
    experiment.setProduct(Product.builder().id(9L).slug("kit-whatsapp-pronto").build());

    when(experimentService.get(experimentId)).thenReturn(experiment);
    when(creativeRepository.countByExperimentIdAndStatusAndUsableImage(
            experimentId, CreativeStatus.READY))
        .thenReturn(0L);
    when(integratedPdeJourneyEvidenceService.appliesTo(experiment)).thenReturn(true);
    when(integratedPdeJourneyEvidenceService.isReady(experiment)).thenReturn(true);

    ExperimentReadinessSummaryDto summary = service.summarize(experimentId);

    assertThat(summary.hasCreatives()).isFalse();
    assertThat(summary.issues()).isEmpty();
    assertThat(summary.eligibleForRunning()).isTrue();
    assertThat(summary.runningGateRequirements())
        .allMatch(ExperimentRunningGateRequirementDto::ready)
        .filteredOn(requirement -> requirement.code().equals("CREATIVE_APPROVED"))
        .singleElement()
        .satisfies(
            requirement -> assertThat(requirement.title()).isEqualTo("Jornada PDE integrada"));
    assertThat(service.computeMissingConfiguration(experiment)).isEmpty();
  }

  /** Libera o formulário do Rigel quando o run produtivo já auditou a jornada completa. */
  @Test
  void shouldUseAuditedDirectPreflightForLowTicketPdeReadiness() {
    Long experimentId = 89L;
    Experiment experiment = buildExperiment(experimentId, 79L);
    experiment.setExperimentType(ExperimentType.LOW_TICKET_PRODUCT);
    experiment.setPlatform(ExperimentPlatform.DIRECT_ONE_TO_ONE);
    clearMediaSpendPlan(experiment);
    experiment.setCampaignObjective(ExperimentCampaignObjective.SALES);
    experiment.setProduct(
        Product.builder()
            .id(9L)
            .slug("kit-whatsapp-pronto")
            .productTypeDefinition(ProductTypeDefinition.builder().code("PDE").build())
            .build());

    when(experimentService.get(experimentId)).thenReturn(experiment);
    when(creativeRepository.countByExperimentIdAndStatusAndUsableImage(
            experimentId, CreativeStatus.READY))
        .thenReturn(0L);
    when(directPdeActivationService.appliesTo(experiment)).thenReturn(true);
    when(directPdeActivationService.isReadyForActivation(experiment)).thenReturn(true);

    ExperimentReadinessSummaryDto summary = service.summarize(experimentId);

    assertThat(summary.hasCreatives()).isFalse();
    assertThat(summary.issues()).isEmpty();
    assertThat(summary.eligibleForRunning()).isTrue();
    assertThat(summary.runningGateRequirements())
        .allMatch(ExperimentRunningGateRequirementDto::ready)
        .extracting(ExperimentRunningGateRequirementDto::detail)
        .anyMatch(detail -> detail.contains("desktop e mobile"))
        .anyMatch(detail -> detail.contains("pagamento de teste"))
        .anyMatch(detail -> detail.contains("eventos segregados"))
        .anyMatch(detail -> detail.contains("ausência de gasto"));
    assertThat(service.computeMissingConfiguration(experiment)).isEmpty();
  }

  @Test
  void shouldBlockLowTicketCampaignWhenSalesPageHasAnalyticsWithoutTrackableSections() {
    Experiment experiment = buildExperiment(61L, 71L);
    experiment.setExperimentType(ExperimentType.LOW_TICKET_PRODUCT);
    experiment.setFollowUpActionUrl("https://pagamentopalf.site/sales-page-exp61.html");
    experiment.getNiche().setFacebookPixelId("pixel-exp61");
    completeCommercialContract(experiment);

    when(creativeRepository.existsByExperimentIdAndStatusAndUsableImage(61L, CreativeStatus.READY))
        .thenReturn(true);
    mockPublishableSelection(61L, TargetingCandidateType.INTEREST, TargetingElementType.INTEREST);
    mockCompletedGeraSalesPagePublication(61L);
    mockSalesPageAudit(
        61L,
        "https://pagamentopalf.site/sales-page-exp61.html",
        "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=exp61",
        salesPageHtmlWithoutTrackableSections());

    assertThat(service.computeMissingConfiguration(experiment))
        .containsExactly("salesPageAnalyticsCollectors");
    assertThat(service.isReadyForCampaign(experiment)).isFalse();
  }

  @Test
  void shouldRequireCommercialContractBeforeSalesPageForPurchaseIntent() {
    Experiment experiment = buildExperiment(56L, 66L);
    experiment.setExperimentType(ExperimentType.LOW_TICKET_PRODUCT);
    experiment.setFollowUpActionUrl(
        "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=abc");
    experiment.getNiche().setFacebookPixelId("pixel-exp56");

    when(creativeRepository.existsByExperimentIdAndStatusAndUsableImage(56L, CreativeStatus.READY))
        .thenReturn(true);
    mockPublishableSelection(56L, TargetingCandidateType.INTEREST, TargetingElementType.INTEREST);

    assertThat(service.computeMissingConfiguration(experiment))
        .containsExactly("commercialContract");
    assertThat(service.isReadyForCampaign(experiment)).isFalse();
  }

  /** Simula uma seleção de público aprovada e identificável pela Meta. */
  private void mockPublishableSelection(
      Long experimentId, TargetingCandidateType candidateType, TargetingElementType elementType) {
    when(targetingSelectionRepository.findByExperimentIdWithTargetingElement(experimentId))
        .thenReturn(
            List.of(
                selection(candidateType, targetingElement(elementType, "meta-" + experimentId))));
  }

  /** Cria uma seleção de público para os testes de prontidão. */
  private ExperimentTargetingSelection selection(
      TargetingCandidateType candidateType, TargetingElement element) {
    return ExperimentTargetingSelection.builder()
        .candidateType(candidateType)
        .term(element.getTerm())
        .targetingElement(element)
        .build();
  }

  /** Cria um elemento de público aprovado para os testes de prontidão. */
  private TargetingElement targetingElement(TargetingElementType type, String metaId) {
    return TargetingElement.builder()
        .type(type)
        .term(type.name())
        .status(TargetingElementStatus.APPROVED)
        .metaId(metaId)
        .build();
  }

  /** Simula todas as etapas obrigatórias do GeraLanding como concluídas. */
  private void mockCompletedGeraLandingStages(Long experimentId) {
    List.of(
            "landing-page-wireframe",
            "landing-page-copy",
            "landing-page-image-planning",
            "landing-page-image-generation",
            "landing-page-design-preset",
            "landing-page-quality-review",
            "landing-page-deliverables")
        .forEach(
            stageCode ->
                when(geraLandingStageExecutionRepository
                        .findTopByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(
                            experimentId, stageCode))
                    .thenReturn(Optional.of(geraLandingExecution(stageCode, "CONCLUIDO"))));
  }

  /** Cria uma execução de etapa do GeraLanding para testes de prontidão. */
  private GeraLandingStageExecution geraLandingExecution(String stageCode, String status) {
    return GeraLandingStageExecution.builder().stageCode(stageCode).status(status).build();
  }

  /** Simula a etapa final do GeraSalesPage como concluída para low-ticket. */
  private void mockCompletedGeraSalesPagePublication(Long experimentId) {
    when(geraSalesPageStageExecutionRepository
            .findTopByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(
                experimentId, GeraSalesPageStageCode.PUBLICATION_PACKAGE.code()))
        .thenReturn(
            Optional.of(
                GeraSalesPageStageExecution.builder()
                    .experimentId(experimentId)
                    .stageCode(GeraSalesPageStageCode.PUBLICATION_PACKAGE.code())
                    .status("CONCLUIDO")
                    .build()));
  }

  /** Simula o snapshot auditado da página de venda publicada. */
  private void mockSalesPageAudit(
      Long experimentId, String salesPageUrl, String checkoutUrl, String html) {
    when(geraSalesPagePublicationAuditRepository.findTopByExperimentIdOrderByPublishedAtDesc(
            experimentId))
        .thenReturn(
            Optional.of(
                GeraSalesPagePublicationAudit.builder()
                    .experimentId(experimentId)
                    .salesPageUrl(salesPageUrl)
                    .checkoutUrl(checkoutUrl)
                    .html(html)
                    .build()));
  }

  /** Retorna HTML com todos os coletores mínimos esperados no funil low-ticket. */
  private String trackedSalesPageHtml() {
    return """
                <html><body>
                <section data-track-section="oferta">Oferta</section>
                <script data-mh-sales-page-analytics="true">
                sendEvent('page_view');
                sendEvent('page_load_metric');
                sendEvent('section_view_time');
                sendEvent('checkout_click');
                </script>
                </body></html>
                """;
  }

  /** Retorna HTML com script antigo sem marcacao de secao, regressao que zerava tempo por secao. */
  private String salesPageHtmlWithoutTrackableSections() {
    return """
                <html><body>
                <section>Oferta sem rastreamento</section>
                <script data-mh-sales-page-analytics="true">
                sendEvent('page_view');
                sendEvent('page_load_metric');
                sendEvent('section_view_time');
                sendEvent('checkout_click');
                </script>
                </body></html>
                """;
  }

  /** Cria um funil aprovado de Produto IA com as chaves informadas. */
  private LeadPortalFlow productAiFlow(String... dataKeys) {
    LeadPortalFlow flow = new LeadPortalFlow();
    flow.setApproved(true);
    for (int i = 0; i < dataKeys.length; i++) {
      flow.getQuestions()
          .add(
              LeadPortalFlowQuestion.builder()
                  .flow(flow)
                  .title("Pergunta " + i)
                  .dataKey(dataKeys[i])
                  .type(LeadPortalQuestionType.TEXT)
                  .required(true)
                  .position(i)
                  .build());
    }
    return flow;
  }

  /** Cria um experimento base para os testes de prontidão. */
  private Experiment buildExperiment(Long experimentId, Long nicheId) {
    MarketNiche niche = new MarketNiche();
    niche.setId(nicheId);

    Hypothesis hypothesis = new Hypothesis();
    hypothesis.setId(UUID.randomUUID());
    hypothesis.setMarketNiche(niche);

    Experiment experiment = new Experiment();
    experiment.setId(experimentId);
    experiment.setNiche(niche);
    experiment.setHypothesisRef(hypothesis);
    experiment.setCreativeApproved(true);
    experiment.setPlatform(ExperimentPlatform.FACEBOOK);
    experiment.setDailyBudget(new BigDecimal("20.00"));
    experiment.setMediaSpendLimit(new BigDecimal("100.00"));
    experiment.setStartDate(LocalDate.of(2026, 9, 1));
    experiment.setEndDate(LocalDate.of(2026, 9, 5));
    return experiment;
  }

  /** Remove qualquer verba Meta do cenário que usa abordagem individual consentida. */
  private void clearMediaSpendPlan(Experiment experiment) {
    experiment.setDailyBudget(null);
    experiment.setMediaSpendLimit(null);
    experiment.setStartDate(null);
    experiment.setEndDate(null);
  }

  /** Preenche o contrato comercial mínimo gerado pela etapa Oferta. */
  private void completeCommercialContract(Experiment experiment) {
    experiment.setSinglePain("Cliente some depois da manutenção");
    experiment.setFreeReward("Preview visual da agenda preenchida");
    experiment.setFunnelPromise("Enxergar riscos e encaixes em 7 dias");
    experiment.setPrimaryCta("Comprar o Mapa 7D");
    experiment.setUnitPrice(BigDecimal.valueOf(29.90));
  }
}
