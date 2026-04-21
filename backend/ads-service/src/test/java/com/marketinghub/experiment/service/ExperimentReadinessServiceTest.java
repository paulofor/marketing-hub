package com.marketinghub.experiment.service;

import com.marketinghub.creative.repository.CreativeRepository;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.dto.ExperimentReadinessIssueDto;
import com.marketinghub.experiment.dto.ExperimentReadinessIssueType;
import com.marketinghub.experiment.dto.ExperimentReadinessSummaryDto;
import com.marketinghub.facebookads.playbook.ExperimentAdSetSpec;
import com.marketinghub.facebookads.playbook.ExperimentAdSetSpecSlot;
import com.marketinghub.facebookads.playbook.ExperimentAdSetWorkflow;
import com.marketinghub.facebookads.playbook.ExperimentAdSetWorkflowStatus;
import com.marketinghub.facebookads.playbook.repository.ExperimentAdSetSpecRepository;
import com.marketinghub.facebookads.playbook.repository.ExperimentAdSetWorkflowRepository;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.targeting.TargetingCandidateType;
import com.marketinghub.targeting.TargetingElementType;
import com.marketinghub.experiment.repository.ExperimentTargetingSelectionRepository;
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
    private ExperimentAdSetWorkflowRepository adSetWorkflowRepository;
    @Mock
    private ExperimentAdSetSpecRepository adSetSpecRepository;

    @InjectMocks
    private ExperimentReadinessService service;

    @Test
    void shouldReportAllIssuesWhenNothingIsReady() {
        Long experimentId = 7L;
        Experiment experiment = buildExperiment(experimentId, 16L);

        when(experimentService.get(experimentId)).thenReturn(experiment);
        when(creativeRepository.countByExperimentId(experimentId)).thenReturn(0L);
        when(adSetWorkflowRepository.findByExperimentId(experimentId)).thenReturn(Optional.empty());
        when(targetingSelectionRepository.countByExperimentIdAndCandidateType(experimentId, TargetingCandidateType.WORK_POSITION))
                .thenReturn(0L);

        ExperimentReadinessSummaryDto summary = service.summarize(experimentId);

        assertThat(summary.hasCreatives()).isFalse();
        assertThat(summary.hasLeadPortalFlow()).isFalse();
        assertThat(summary.hasCompleteTargeting()).isFalse();
        assertThat(summary.missingTargetingTypes()).containsExactly(TargetingElementType.JOB_TITLE);
        assertThat(summary.issues()).hasSize(3);
        assertThat(summary.issues()).extracting(ExperimentReadinessIssueDto::type)
                .containsExactlyInAnyOrder(
                        ExperimentReadinessIssueType.CREATIVE,
                        ExperimentReadinessIssueType.LEAD_PORTAL_FLOW,
                        ExperimentReadinessIssueType.TARGETING
                );
    }

    @Test
    void shouldReturnNoIssuesWhenEverythingIsReady() {
        Long experimentId = 8L;
        Experiment experiment = buildExperiment(experimentId, 18L);
        experiment.setLeadPortalFlow(new LeadPortalFlow());

        when(experimentService.get(experimentId)).thenReturn(experiment);
        when(creativeRepository.countByExperimentId(experimentId)).thenReturn(2L);
        when(targetingSelectionRepository.countByExperimentIdAndCandidateType(experimentId, TargetingCandidateType.WORK_POSITION))
                .thenReturn(1L);

        ExperimentReadinessSummaryDto summary = service.summarize(experimentId);

        assertThat(summary.hasCreatives()).isTrue();
        assertThat(summary.hasLeadPortalFlow()).isTrue();
        assertThat(summary.hasCompleteTargeting()).isTrue();
        assertThat(summary.missingTargetingTypes()).isEmpty();
        assertThat(summary.issues()).isEmpty();
    }

    @Test
    void shouldReportTargetingIssueWhenThereIsNoApprovedJobTitleSelection() {
        Long experimentId = 11L;
        Experiment experiment = buildExperiment(experimentId, 19L);
        experiment.setLeadPortalFlow(new LeadPortalFlow());

        when(experimentService.get(experimentId)).thenReturn(experiment);
        when(creativeRepository.countByExperimentId(experimentId)).thenReturn(1L);
        when(adSetWorkflowRepository.findByExperimentId(experimentId)).thenReturn(Optional.empty());
        when(targetingSelectionRepository.countByExperimentIdAndCandidateType(experimentId, TargetingCandidateType.WORK_POSITION))
                .thenReturn(0L);

        ExperimentReadinessSummaryDto summary = service.summarize(experimentId);

        assertThat(summary.hasCompleteTargeting()).isFalse();
        assertThat(summary.missingTargetingTypes()).containsExactly(TargetingElementType.JOB_TITLE);
        assertThat(summary.issues()).extracting(ExperimentReadinessIssueDto::type)
                .contains(ExperimentReadinessIssueType.TARGETING);
    }

    @Test
    void shouldTreatReadyAdSetSpecsAsCompleteTargeting() {
        Long experimentId = 9L;
        Experiment experiment = buildExperiment(experimentId, 20L);
        experiment.setLeadPortalFlow(new LeadPortalFlow());

        when(experimentService.get(experimentId)).thenReturn(experiment);
        when(creativeRepository.countByExperimentId(experimentId)).thenReturn(1L);

        ExperimentAdSetWorkflow workflow = ExperimentAdSetWorkflow.builder()
                .id(42L)
                .status(ExperimentAdSetWorkflowStatus.COMPLETED)
                .build();
        when(adSetWorkflowRepository.findByExperimentId(experimentId)).thenReturn(Optional.of(workflow));

        List<ExperimentAdSetSpec> specs = List.of(
                buildSpec(ExperimentAdSetSpecSlot.DESIGNERS),
                buildSpec(ExperimentAdSetSpecSlot.MARKETING),
                buildSpec(ExperimentAdSetSpecSlot.SMB)
        );
        when(adSetSpecRepository.findByWorkflowId(workflow.getId())).thenReturn(specs);

        ExperimentReadinessSummaryDto summary = service.summarize(experimentId);

        assertThat(summary.hasCompleteTargeting()).isTrue();
        assertThat(summary.missingTargetingTypes()).isEmpty();
        assertThat(summary.issues()).extracting(ExperimentReadinessIssueDto::type)
                .doesNotContain(ExperimentReadinessIssueType.TARGETING);
    }

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
        return experiment;
    }

    private ExperimentAdSetSpec buildSpec(ExperimentAdSetSpecSlot slot) {
        ExperimentAdSetSpec spec = new ExperimentAdSetSpec();
        spec.setSlot(slot);
        spec.setReachStatus("READY");
        spec.setValidationStatus("VALID");
        return spec;
    }
}
