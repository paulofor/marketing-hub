package com.marketinghub.experiment.service;

import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentTargetingSelection;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.experiment.dto.ExperimentReadinessIssueDto;
import com.marketinghub.experiment.dto.ExperimentReadinessIssueType;
import com.marketinghub.experiment.dto.ExperimentReadinessSummaryDto;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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

    @InjectMocks
    private ExperimentReadinessService service;

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

    @Test
    void shouldReportTargetingIssueWhenThereIsNoPublishableSelection() {
        Long experimentId = 11L;
        Experiment experiment = buildExperiment(experimentId, 19L);
        LeadPortalFlow flow = new LeadPortalFlow();
        flow.setApproved(true);
        experiment.setLeadPortalFlow(flow);

        when(experimentService.get(experimentId)).thenReturn(experiment);
        when(creativeRepository.countByExperimentIdAndStatus(experimentId, CreativeStatus.READY)).thenReturn(1L);
        ExperimentReadinessSummaryDto summary = service.summarize(experimentId);

        assertThat(summary.hasCompleteTargeting()).isFalse();
        assertThat(summary.missingTargetingTypes()).containsExactly(
                TargetingElementType.INTEREST,
                TargetingElementType.JOB_TITLE,
                TargetingElementType.BEHAVIOR);
        assertThat(summary.issues()).extracting(ExperimentReadinessIssueDto::type)
                .contains(ExperimentReadinessIssueType.TARGETING);
    }

    @Test
    void shouldTreatSavedInterestSelectionAsReady() {
        Long experimentId = 15L;
        Experiment experiment = buildExperiment(experimentId, 21L);
        LeadPortalFlow flow = new LeadPortalFlow();
        flow.setApproved(true);
        experiment.setLeadPortalFlow(flow);

        when(experimentService.get(experimentId)).thenReturn(experiment);
        when(creativeRepository.countByExperimentIdAndStatus(experimentId, CreativeStatus.READY)).thenReturn(1L);
        mockPublishableSelection(experimentId, TargetingCandidateType.INTEREST, TargetingElementType.INTEREST);
        ExperimentReadinessSummaryDto summary = service.summarize(experimentId);

        assertThat(summary.hasCompleteTargeting()).isTrue();
        assertThat(summary.missingTargetingTypes()).isEmpty();
        assertThat(summary.issues()).extracting(ExperimentReadinessIssueDto::type)
                .doesNotContain(ExperimentReadinessIssueType.TARGETING);
    }

    @Test
    void shouldKeepGeraLandingPendingWhenLatestRequiredStageFailed() {
        Long experimentId = 48L;
        Experiment experiment = buildExperiment(experimentId, 22L);
        LeadPortalFlow flow = new LeadPortalFlow();
        flow.setApproved(true);
        experiment.setLeadPortalFlow(flow);

        when(experimentService.get(experimentId)).thenReturn(experiment);
        when(creativeRepository.countByExperimentIdAndStatus(experimentId, CreativeStatus.READY)).thenReturn(1L);
        when(geraLandingStageExecutionRepository.findTopByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(
                experimentId, "landing-page-wireframe"))
                .thenReturn(Optional.of(geraLandingExecution("landing-page-wireframe", "CONCLUIDO")));
        when(geraLandingStageExecutionRepository.findTopByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(
                experimentId, "landing-page-copy"))
                .thenReturn(Optional.of(geraLandingExecution("landing-page-copy", "FALHA")));

        ExperimentReadinessSummaryDto summary = service.summarize(experimentId);

        assertThat(summary.hasGeraLandingPipeline()).isFalse();
        assertThat(summary.geraLandingCompletedStageCount()).isEqualTo(1);
        assertThat(summary.issues()).extracting(ExperimentReadinessIssueDto::type)
                .contains(ExperimentReadinessIssueType.GERA_LANDING);
    }

    @Test
    void shouldRequireLandingDestinationInCampaignPendingChecklist() {
        Experiment experiment = buildExperiment(20L, 30L);
        experiment.setFollowUpActionUrl(null);

        when(creativeRepository.existsByExperimentIdAndStatus(20L, CreativeStatus.READY)).thenReturn(true);
        mockPublishableSelection(20L, TargetingCandidateType.WORK_POSITION, TargetingElementType.JOB_TITLE);

        assertThat(service.computeMissingConfiguration(experiment))
                .containsExactly("landingDestination");
    }

    @Test
    void shouldBeReadyForCampaignWhenCreativeAndLandingAreReady() {
        Experiment experiment = buildExperiment(21L, 31L);
        experiment.setFollowUpActionUrl("https://example.com/landing/21");

        when(creativeRepository.existsByExperimentIdAndStatus(21L, CreativeStatus.READY)).thenReturn(true);
        mockPublishableSelection(21L, TargetingCandidateType.BEHAVIOR, TargetingElementType.BEHAVIOR);

        assertThat(service.computeMissingConfiguration(experiment)).isEmpty();
        assertThat(service.isReadyForCampaign(experiment)).isTrue();
    }

    @Test
    void shouldBlockCampaignWhenSelectionDoesNotHaveOfficialMetaId() {
        Experiment experiment = buildExperiment(23L, 33L);
        experiment.setFollowUpActionUrl("https://example.com/landing/23");

        when(creativeRepository.existsByExperimentIdAndStatus(23L, CreativeStatus.READY)).thenReturn(true);
        when(targetingSelectionRepository.findByExperimentIdWithTargetingElement(23L))
                .thenReturn(List.of(selection(
                        TargetingCandidateType.INTEREST,
                        targetingElement(TargetingElementType.INTEREST, ""))));

        assertThat(service.computeMissingConfiguration(experiment)).containsExactly("approvedTargetingPackage");
        assertThat(service.isReadyForCampaign(experiment)).isFalse();
    }

    @Test
    void shouldRequireGeraSalesPagePipelineForLowTicketCampaign() {
        Experiment experiment = buildExperiment(52L, 62L);
        experiment.setExperimentType(ExperimentType.LOW_TICKET_PRODUCT);
        experiment.setFollowUpActionUrl("https://pagamentopalf.site/sales-page-exp52.html");
        experiment.getNiche().setFacebookPixelId("pixel-exp52");

        when(creativeRepository.existsByExperimentIdAndStatus(52L, CreativeStatus.READY)).thenReturn(true);
        mockPublishableSelection(52L, TargetingCandidateType.INTEREST, TargetingElementType.INTEREST);

        assertThat(service.computeMissingConfiguration(experiment))
                .containsExactly("geraSalesPagePipeline");
        assertThat(service.isReadyForCampaign(experiment)).isFalse();
    }

    @Test
    // Verifica que low-ticket com pipeline concluído ainda precisa de auditoria da página publicada.
    void shouldRequirePublishedSalesPageAuditForLowTicketCampaign() {
        Experiment experiment = buildExperiment(55L, 65L);
        experiment.setExperimentType(ExperimentType.LOW_TICKET_PRODUCT);
        experiment.setFollowUpActionUrl("https://pagamentopalf.site/sales-page-exp55.html");
        experiment.getNiche().setFacebookPixelId("pixel-exp55");

        when(creativeRepository.existsByExperimentIdAndStatus(55L, CreativeStatus.READY)).thenReturn(true);
        mockPublishableSelection(55L, TargetingCandidateType.INTEREST, TargetingElementType.INTEREST);
        mockCompletedGeraSalesPagePublication(55L);

        assertThat(service.computeMissingConfiguration(experiment))
                .containsExactly("geraSalesPagePipeline");
        assertThat(service.isReadyForCampaign(experiment)).isFalse();
    }

    @Test
    // Verifica que low-ticket não pode enviar o clique do anúncio direto para checkout.
    void shouldRequireAdDestinationToPointToSalesPageForLowTicketCampaign() {
        Experiment experiment = buildExperiment(56L, 66L);
        experiment.setExperimentType(ExperimentType.LOW_TICKET_PRODUCT);
        experiment.setFollowUpActionUrl("https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=abc");
        experiment.getNiche().setFacebookPixelId("pixel-exp56");

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
    // Verifica que low-ticket não publica página antiga sem coletores mínimos de funil.
    void shouldRequireSalesPageAnalyticsCollectorsForLowTicketCampaign() {
        Experiment experiment = buildExperiment(57L, 67L);
        experiment.setExperimentType(ExperimentType.LOW_TICKET_PRODUCT);
        experiment.setFollowUpActionUrl("https://pagamentopalf.site/sales-page-exp57.html");
        experiment.getNiche().setFacebookPixelId("pixel-exp57");

        when(creativeRepository.existsByExperimentIdAndStatus(57L, CreativeStatus.READY)).thenReturn(true);
        mockPublishableSelection(57L, TargetingCandidateType.INTEREST, TargetingElementType.INTEREST);
        mockCompletedGeraSalesPagePublication(57L);
        mockSalesPageAudit(
                57L,
                "https://pagamentopalf.site/sales-page-exp57.html",
                "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=def",
                "<html><body><a href=\"https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=def\">Comprar</a></body></html>");

        assertThat(service.computeMissingConfiguration(experiment))
                .containsExactly("salesPageAnalyticsCollectors");
        assertThat(service.isReadyForCampaign(experiment)).isFalse();
    }

    @Test
    // Verifica que low-ticket sem pixel não entra na fila de campanha de venda.
    void shouldRequireFacebookPixelForLowTicketCampaign() {
        Experiment experiment = buildExperiment(54L, 64L);
        experiment.setExperimentType(ExperimentType.LOW_TICKET_PRODUCT);
        experiment.setFollowUpActionUrl("https://pagamentopalf.site/sales-page-exp54.html");

        when(creativeRepository.existsByExperimentIdAndStatus(54L, CreativeStatus.READY)).thenReturn(true);
        mockPublishableSelection(54L, TargetingCandidateType.INTEREST, TargetingElementType.INTEREST);
        mockCompletedGeraSalesPagePublication(54L);
        mockSalesPageAudit(
                54L,
                "https://pagamentopalf.site/sales-page-exp54.html",
                "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=exp54",
                trackedSalesPageHtml());

        assertThat(service.computeMissingConfiguration(experiment))
                .containsExactly("facebookPixel");
        assertThat(service.isReadyForCampaign(experiment)).isFalse();
    }

    @Test
    // Verifica que Produto IA personalizado não publica sem coleta prévia dos dados do lead.
    void shouldRequirePersonalizedSampleFunnelForProductAiCampaign() {
        Experiment experiment = buildExperiment(58L, 68L);
        experiment.setExperimentType(ExperimentType.LOW_TICKET_PRODUCT);
        experiment.setProductAiSubtype(ProductAiSubtype.AI_PERSONALIZED_SAMPLE);
        experiment.setFollowUpActionUrl("https://pagamentopalf.site/sales-page-exp58.html");
        experiment.getNiche().setFacebookPixelId("pixel-exp58");

        when(creativeRepository.existsByExperimentIdAndStatus(58L, CreativeStatus.READY)).thenReturn(true);
        mockPublishableSelection(58L, TargetingCandidateType.INTEREST, TargetingElementType.INTEREST);
        mockCompletedGeraSalesPagePublication(58L);
        mockSalesPageAudit(
                58L,
                "https://pagamentopalf.site/sales-page-exp58.html",
                "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=exp58",
                trackedSalesPageHtml());

        assertThat(service.computeMissingConfiguration(experiment))
                .containsExactly("productAiPersonalizedSampleFunnel");
        assertThat(service.isReadyForCampaign(experiment)).isFalse();
    }

    @Test
    // Verifica que Produto IA personalizado completo exige funil aprovado com campos mínimos de personalização.
    void shouldAllowPersonalizedSampleCampaignWhenFunnelCollectsRequiredData() {
        Experiment experiment = buildExperiment(59L, 69L);
        experiment.setExperimentType(ExperimentType.LOW_TICKET_PRODUCT);
        experiment.setProductAiSubtype(ProductAiSubtype.AI_PERSONALIZED_SAMPLE);
        experiment.setFollowUpActionUrl("https://pagamentopalf.site/sales-page-exp59.html");
        experiment.getNiche().setFacebookPixelId("pixel-exp59");
        experiment.setLeadPortalFlow(productAiFlow(
                "nome",
                "email",
                "whatsapp",
                "negocio_projeto",
                "contexto_atual",
                "objetivo_visual",
                "dados_personalizacao",
                "preferencias_visuais"));

        when(creativeRepository.existsByExperimentIdAndStatus(59L, CreativeStatus.READY)).thenReturn(true);
        mockPublishableSelection(59L, TargetingCandidateType.INTEREST, TargetingElementType.INTEREST);
        mockCompletedGeraSalesPagePublication(59L);
        mockSalesPageAudit(
                59L,
                "https://pagamentopalf.site/sales-page-exp59.html",
                "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=exp59",
                trackedSalesPageHtml());

        assertThat(service.computeMissingConfiguration(experiment)).isEmpty();
        assertThat(service.isReadyForCampaign(experiment)).isTrue();
    }

    @Test
    // Verifica que low-ticket completo com página e pixel pode ser publicado.
    void shouldAllowLowTicketCampaignOnlyAfterGeraSalesPagePublicationPackage() {
        Experiment experiment = buildExperiment(53L, 63L);
        experiment.setExperimentType(ExperimentType.LOW_TICKET_PRODUCT);
        experiment.setFollowUpActionUrl("https://pagamentopalf.site/sales-page-exp53.html");
        experiment.getNiche().setFacebookPixelId("pixel-exp53");

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

}
