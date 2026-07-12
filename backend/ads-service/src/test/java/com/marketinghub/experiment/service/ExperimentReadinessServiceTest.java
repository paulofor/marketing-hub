package com.marketinghub.experiment.service;

import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignObjective;
import com.marketinghub.experiment.ExperimentTargetingSelection;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.experiment.dto.ExperimentReadinessIssueDto;
import com.marketinghub.experiment.dto.ExperimentReadinessIssueType;
import com.marketinghub.experiment.dto.ExperimentReadinessSummaryDto;
import com.marketinghub.experiment.video.service.ExperimentVideoAssetService;
import com.marketinghub.experiment.salespageab.service.ExperimentSalesPageAbTestService;
import com.marketinghub.gerasalespage.v1.GeraSalesPagePublicationAudit;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.gerasalespage.v1.GeraSalesPageStageCode;
import com.marketinghub.gerasalespage.v1.GeraSalesPageStageExecution;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.LeadPortalFlowQuestion;
import com.marketinghub.leadportal.LeadPortalQuestionType;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.productai.ProductAiSubtype;
import com.marketinghub.targeting.TargetingCandidateType;
import com.marketinghub.targeting.TargetingElement;
import com.marketinghub.targeting.TargetingElementStatus;
import com.marketinghub.targeting.TargetingElementType;
import com.marketinghub.repository.jpa.experiment.ExperimentTargetingSelectionRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPagePublicationAuditRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPageStageExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExperimentReadinessServiceTest {

    @Mock
    private ExperimentService experimentService;
    @Mock
    private CreativeRepository creativeRepository;
    @Mock
    private ExperimentTargetingSelectionRepository targetingSelectionRepository;
    @Mock
    private GeraLandingStageExecutionRepository geraLandingStageExecutionRepository;
    @Mock
    private GeraSalesPageStageExecutionRepository geraSalesPageStageExecutionRepository;
    @Mock
    private GeraSalesPagePublicationAuditRepository geraSalesPagePublicationAuditRepository;
    @Mock
    private ExperimentVideoAssetService experimentVideoAssetService;
    @Mock
    private ExperimentSalesPageAbTestService salesPageAbTestService;

    private ExperimentReadinessService service;

    @BeforeEach
    void setUp() {
        ExperimentCampaignDestinationPolicy campaignDestinationPolicy = new ExperimentCampaignDestinationPolicy(
                geraSalesPageStageExecutionRepository,
                geraSalesPagePublicationAuditRepository);
        service = new ExperimentReadinessService(
                experimentService,
                creativeRepository,
                targetingSelectionRepository,
                geraLandingStageExecutionRepository,
                campaignDestinationPolicy,
                experimentVideoAssetService,
                salesPageAbTestService);
        lenient().when(salesPageAbTestService.hasReadyActiveTest(org.mockito.ArgumentMatchers.anyLong())).thenReturn(true);
    }

    @Test
    void shouldReportAllIssuesWhenNothingIsReady() {
        Long experimentId = 7L;
        Experiment experiment = buildExperiment(experimentId, 16L);

        when(experimentService.get(experimentId)).thenReturn(experiment);
        when(creativeRepository.countByExperimentIdAndStatus(experimentId, CreativeStatus.READY)).thenReturn(0L);
        ExperimentReadinessSummaryDto summary = service.summarize(experimentId);

        assertThat(summary.hasCreatives()).isFalse();
        assertThat(summary.hasLeadPortalFlow()).isFalse();
        assertThat(summary.hasCompleteTargeting()).isFalse();
        assertThat(summary.hasGeraLandingPipeline()).isFalse();
        assertThat(summary.geraLandingCompletedStageCount()).isZero();
        assertThat(summary.missingTargetingTypes()).containsExactly(
                TargetingElementType.INTEREST,
                TargetingElementType.JOB_TITLE,
                TargetingElementType.BEHAVIOR);
        assertThat(summary.issues()).hasSize(4);
        assertThat(summary.issues()).extracting(ExperimentReadinessIssueDto::type)
                .containsExactlyInAnyOrder(
                        ExperimentReadinessIssueType.CREATIVE,
                        ExperimentReadinessIssueType.LEAD_PORTAL_FLOW,
                        ExperimentReadinessIssueType.TARGETING,
                        ExperimentReadinessIssueType.GERA_LANDING
                );
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
        when(creativeRepository.countByExperimentIdAndStatus(experimentId, CreativeStatus.READY)).thenReturn(2L);
        mockPublishableSelection(experimentId, TargetingCandidateType.INTEREST, TargetingElementType.INTEREST);
        mockCompletedGeraLandingStages(experimentId);
        ExperimentReadinessSummaryDto summary = service.summarize(experimentId);

        assertThat(summary.hasCreatives()).isTrue();
        assertThat(summary.hasLeadPortalFlow()).isTrue();
        assertThat(summary.hasCompleteTargeting()).isTrue();
        assertThat(summary.hasGeraLandingPipeline()).isTrue();
        assertThat(summary.geraLandingCompletedStageCount()).isEqualTo(summary.geraLandingRequiredStageCount());
        assertThat(summary.missingTargetingTypes()).isEmpty();
        assertThat(summary.issues()).isEmpty();
    }

    @Test
    void shouldUseReadyCreativeAsApprovalSourceEvenWhenExperimentFlagIsStale() {
        Long experimentId = 22L;
        Experiment experiment = buildExperiment(experimentId, 32L);
        experiment.setCreativeApproved(false);
        experiment.setFollowUpActionUrl("https://example.com/landing/22");

        when(creativeRepository.existsByExperimentIdAndStatus(experimentId, CreativeStatus.READY)).thenReturn(true);
        mockPublishableSelection(experimentId, TargetingCandidateType.BEHAVIOR, TargetingElementType.BEHAVIOR);

        assertThat(service.computeMissingConfiguration(experiment)).isEmpty();
        assertThat(service.isReadyForCampaign(experiment)).isTrue();
    }

    /** Garante que vídeo obrigatório sem aprovação bloqueia a campanha. */
    @Test
    void shouldBlockCampaignWhenRequiredVideoIsNotReadyAndApproved() {
        Experiment experiment = buildExperiment(39L, 49L);
        experiment.setFollowUpActionUrl("https://example.com/landing/39");

        when(creativeRepository.existsByExperimentIdAndStatus(39L, CreativeStatus.READY)).thenReturn(true);
        when(experimentVideoAssetService.hasRequiredVideoBlockingRelease(39L)).thenReturn(true);
        mockPublishableSelection(39L, TargetingCandidateType.INTEREST, TargetingElementType.INTEREST);

        assertThat(service.computeMissingConfiguration(experiment))
                .containsExactly("experimentVideoAsset");
        assertThat(service.isReadyForCampaign(experiment)).isFalse();
    }

    /** Garante que teste A/B ativo incompleto bloqueia a liberacao para campanha. */
    @Test
    void shouldBlockCampaignWhenActiveSalesPageAbTestIsIncomplete() {
        Experiment experiment = buildExperiment(44L, 54L);
        experiment.setCampaignObjective(ExperimentCampaignObjective.SALES);
        experiment.setFollowUpActionUrl("https://example.com/landing/44");
        completeCommercialContract(experiment);

        when(creativeRepository.existsByExperimentIdAndStatus(44L, CreativeStatus.READY)).thenReturn(true);
        when(salesPageAbTestService.hasReadyActiveTest(44L)).thenReturn(false);
        mockPublishableSelection(44L, TargetingCandidateType.INTEREST, TargetingElementType.INTEREST);
        mockCompletedGeraSalesPagePublication(44L);
        mockSalesPageAudit(
                44L,
                "https://example.com/landing/44",
                "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=exp44",
                trackedSalesPageHtml());

        assertThat(service.computeMissingConfiguration(experiment))
                .containsExactly("salesPageAbTest");
        assertThat(service.isReadyForCampaign(experiment)).isFalse();
    }

    /** Garante que teste A/B de venda incompleto nao bloqueia campanha de captura de leads. */
    @Test
    void shouldNotRequireSalesPageAbTestForLeadGenerationNicheTest() {
        Experiment experiment = buildExperiment(45L, 55L);
        experiment.setExperimentType(ExperimentType.NICHE_TEST);
        experiment.setCampaignObjective(ExperimentCampaignObjective.LEADS);
        experiment.setFollowUpActionUrl("https://example.com/landing/45");

        when(creativeRepository.existsByExperimentIdAndStatus(45L, CreativeStatus.READY)).thenReturn(true);
        mockPublishableSelection(45L, TargetingCandidateType.INTEREST, TargetingElementType.INTEREST);

        assertThat(service.computeMissingConfiguration(experiment)).isEmpty();
        assertThat(service.isReadyForCampaign(experiment)).isTrue();
    }

    @Test
    void shouldRequireAdDestinationToPointToSalesPageForLowTicketCampaign() {
        Experiment experiment = buildExperiment(56L, 66L);
        experiment.setExperimentType(ExperimentType.LOW_TICKET_PRODUCT);
        experiment.setFollowUpActionUrl("https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=abc");
        experiment.getNiche().setFacebookPixelId("pixel-exp56");
        completeCommercialContract(experiment);

        when(creativeRepository.existsByExperimentIdAndStatus(56L, CreativeStatus.READY)).thenReturn(true);
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
        experiment.setFollowUpActionUrl("https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=sales-direct");
        experiment.getNiche().setFacebookPixelId("pixel-exp60");
        completeCommercialContract(experiment);

        when(creativeRepository.existsByExperimentIdAndStatus(60L, CreativeStatus.READY)).thenReturn(true);
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
        experiment.setLeadPortalFlow(productAiFlow(
                "nome",
                "email",
                "whatsapp",
                "negocio_projeto",
                "contexto_atual",
                "objetivo_visual",
                "dados_personalizacao"));
        experiment.getLeadPortalFlow().setSlug("decoraia-express-exp-57");
        experiment.setFollowUpActionUrl("https://oportunidadebrasil.shop/flows/decoraia-express-exp-57");
        experiment.getNiche().setFacebookPixelId("pixel-exp57");
        completeCommercialContract(experiment);

        when(creativeRepository.existsByExperimentIdAndStatus(57L, CreativeStatus.READY)).thenReturn(true);
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

    @Test
    void shouldAllowLowTicketCampaignOnlyAfterGeraSalesPagePublicationPackage() {
        Experiment experiment = buildExperiment(53L, 63L);
        experiment.setExperimentType(ExperimentType.LOW_TICKET_PRODUCT);
        experiment.setFollowUpActionUrl("https://pagamentopalf.site/sales-page-exp53.html");
        experiment.getNiche().setFacebookPixelId("pixel-exp53");
        completeCommercialContract(experiment);

        when(creativeRepository.existsByExperimentIdAndStatus(53L, CreativeStatus.READY)).thenReturn(true);
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

    @Test
    void shouldBlockLowTicketCampaignWhenSalesPageHasAnalyticsWithoutTrackableSections() {
        Experiment experiment = buildExperiment(61L, 71L);
        experiment.setExperimentType(ExperimentType.LOW_TICKET_PRODUCT);
        experiment.setFollowUpActionUrl("https://pagamentopalf.site/sales-page-exp61.html");
        experiment.getNiche().setFacebookPixelId("pixel-exp61");
        completeCommercialContract(experiment);

        when(creativeRepository.existsByExperimentIdAndStatus(61L, CreativeStatus.READY)).thenReturn(true);
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
        experiment.setFollowUpActionUrl("https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=abc");
        experiment.getNiche().setFacebookPixelId("pixel-exp56");

        when(creativeRepository.existsByExperimentIdAndStatus(56L, CreativeStatus.READY)).thenReturn(true);
        mockPublishableSelection(56L, TargetingCandidateType.INTEREST, TargetingElementType.INTEREST);

        assertThat(service.computeMissingConfiguration(experiment))
                .containsExactly("commercialContract");
        assertThat(service.isReadyForCampaign(experiment)).isFalse();
    }

    /** Simula uma seleção de público aprovada e identificável pela Meta. */
    private void mockPublishableSelection(Long experimentId, TargetingCandidateType candidateType, TargetingElementType elementType) {
        when(targetingSelectionRepository.findByExperimentIdWithTargetingElement(experimentId))
                .thenReturn(List.of(selection(candidateType, targetingElement(elementType, "meta-" + experimentId))));
    }

    /** Cria uma seleção de público para os testes de prontidão. */
    private ExperimentTargetingSelection selection(TargetingCandidateType candidateType, TargetingElement element) {
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
                "landing-page-deliverables"
        ).forEach(stageCode -> when(geraLandingStageExecutionRepository
                .findTopByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(experimentId, stageCode))
                .thenReturn(Optional.of(geraLandingExecution(stageCode, "CONCLUIDO"))));
    }

    /** Cria uma execução de etapa do GeraLanding para testes de prontidão. */
    private GeraLandingStageExecution geraLandingExecution(String stageCode, String status) {
        return GeraLandingStageExecution.builder()
                .stageCode(stageCode)
                .status(status)
                .build();
    }

    /** Simula a etapa final do GeraSalesPage como concluída para low-ticket. */
    private void mockCompletedGeraSalesPagePublication(Long experimentId) {
        when(geraSalesPageStageExecutionRepository.findTopByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(
                experimentId, GeraSalesPageStageCode.PUBLICATION_PACKAGE.code()))
                .thenReturn(Optional.of(GeraSalesPageStageExecution.builder()
                        .experimentId(experimentId)
                        .stageCode(GeraSalesPageStageCode.PUBLICATION_PACKAGE.code())
                        .status("CONCLUIDO")
                        .build()));
    }

    /** Simula o snapshot auditado da página de venda publicada. */
    private void mockSalesPageAudit(Long experimentId, String salesPageUrl, String checkoutUrl, String html) {
        when(geraSalesPagePublicationAuditRepository.findTopByExperimentIdOrderByPublishedAtDesc(experimentId))
                .thenReturn(Optional.of(GeraSalesPagePublicationAudit.builder()
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
            flow.getQuestions().add(LeadPortalFlowQuestion.builder()
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
        return experiment;
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
