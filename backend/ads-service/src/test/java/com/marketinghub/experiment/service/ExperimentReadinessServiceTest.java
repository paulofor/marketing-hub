package com.marketinghub.experiment.service;

import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.dto.ExperimentReadinessIssueDto;
import com.marketinghub.experiment.dto.ExperimentReadinessIssueType;
import com.marketinghub.experiment.dto.ExperimentReadinessSummaryDto;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.targeting.TargetingCandidateType;
import com.marketinghub.targeting.TargetingElement;
import com.marketinghub.targeting.TargetingElementType;
import com.marketinghub.repository.jpa.experiment.ExperimentTargetingSelectionRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import com.marketinghub.repository.jpa.targeting.TargetingElementRepository;
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
    private TargetingElementRepository targetingElementRepository;
    @Mock
    private GeraLandingStageExecutionRepository geraLandingStageExecutionRepository;

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
        assertThat(summary.hasCompleteTargeting()).isTrue();
        assertThat(summary.hasGeraLandingPipeline()).isFalse();
        assertThat(summary.geraLandingCompletedStageCount()).isZero();
        assertThat(summary.missingTargetingTypes()).isEmpty();
        assertThat(summary.issues()).hasSize(3);
        assertThat(summary.issues()).extracting(ExperimentReadinessIssueDto::type)
                .containsExactlyInAnyOrder(
                        ExperimentReadinessIssueType.CREATIVE,
                        ExperimentReadinessIssueType.LEAD_PORTAL_FLOW,
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

        assertThat(service.computeMissingConfiguration(experiment)).isEmpty();
        assertThat(service.isReadyForCampaign(experiment)).isTrue();
    }

    @Test
    void shouldNotReportTargetingIssueWhenThereIsNoApprovedJobTitleSelection() {
        Long experimentId = 11L;
        Experiment experiment = buildExperiment(experimentId, 19L);
        LeadPortalFlow flow = new LeadPortalFlow();
        flow.setApproved(true);
        experiment.setLeadPortalFlow(flow);

        when(experimentService.get(experimentId)).thenReturn(experiment);
        when(creativeRepository.countByExperimentIdAndStatus(experimentId, CreativeStatus.READY)).thenReturn(1L);
        ExperimentReadinessSummaryDto summary = service.summarize(experimentId);

        assertThat(summary.hasCompleteTargeting()).isTrue();
        assertThat(summary.missingTargetingTypes()).isEmpty();
        assertThat(summary.issues()).extracting(ExperimentReadinessIssueDto::type)
                .doesNotContain(ExperimentReadinessIssueType.TARGETING);
    }

    @Test
    void shouldTreatApprovedTargetingPackageAsReadyEvenWithoutSelection() {
        Long experimentId = 15L;
        Experiment experiment = buildExperiment(experimentId, 21L);
        LeadPortalFlow flow = new LeadPortalFlow();
        flow.setApproved(true);
        experiment.setLeadPortalFlow(flow);

        when(experimentService.get(experimentId)).thenReturn(experiment);
        when(creativeRepository.countByExperimentIdAndStatus(experimentId, CreativeStatus.READY)).thenReturn(1L);
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

        assertThat(service.computeMissingConfiguration(experiment))
                .containsExactly("landingDestination");
    }

    @Test
    void shouldBeReadyForCampaignWhenCreativeAndLandingAreReady() {
        Experiment experiment = buildExperiment(21L, 31L);
        experiment.setFollowUpActionUrl("https://example.com/landing/21");

        when(creativeRepository.existsByExperimentIdAndStatus(21L, CreativeStatus.READY)).thenReturn(true);

        assertThat(service.computeMissingConfiguration(experiment)).isEmpty();
        assertThat(service.isReadyForCampaign(experiment)).isTrue();
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
