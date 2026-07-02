package com.marketinghub.experiment.service;

import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentTargetingSelection;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.experiment.dto.ExperimentReadinessIssueDto;
import com.marketinghub.experiment.dto.ExperimentReadinessIssueType;
import com.marketinghub.experiment.dto.ExperimentReadinessSummaryDto;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.gerasalespage.v1.GeraSalesPageStageCode;
import com.marketinghub.gerasalespage.v1.GeraSalesPageStageExecution;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.targeting.TargetingCandidateType;
import com.marketinghub.targeting.TargetingElement;
import com.marketinghub.targeting.TargetingElementStatus;
import com.marketinghub.targeting.TargetingElementType;
import com.marketinghub.repository.jpa.experiment.ExperimentTargetingSelectionRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
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
    // Verifica que low-ticket sem pixel não entra na fila de campanha de venda.
    void shouldRequireFacebookPixelForLowTicketCampaign() {
        Experiment experiment = buildExperiment(54L, 64L);
        experiment.setExperimentType(ExperimentType.LOW_TICKET_PRODUCT);
        experiment.setFollowUpActionUrl("https://pagamentopalf.site/sales-page-exp54.html");

        when(creativeRepository.existsByExperimentIdAndStatus(54L, CreativeStatus.READY)).thenReturn(true);
        mockPublishableSelection(54L, TargetingCandidateType.INTEREST, TargetingElementType.INTEREST);
        mockCompletedGeraSalesPagePublication(54L);

        assertThat(service.computeMissingConfiguration(experiment))
                .containsExactly("facebookPixel");
        assertThat(service.isReadyForCampaign(experiment)).isFalse();
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
