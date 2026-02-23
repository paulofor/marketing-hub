package com.marketinghub.experiment.service;

import com.marketinghub.creative.repository.CreativeRepository;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.dto.ExperimentReadinessSummaryDto;
import com.marketinghub.experiment.dto.ExperimentReadinessIssueType;
import com.marketinghub.leadportal.repository.LeadPortalFlowRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.targeting.TargetingElementType;
import com.marketinghub.targeting.repository.TargetingElementRepository;
import com.marketinghub.hypothesis.Hypothesis;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private LeadPortalFlowRepository leadPortalFlowRepository;
    @Mock
    private TargetingElementRepository targetingElementRepository;

    @InjectMocks
    private ExperimentReadinessService service;

    @Test
    void shouldReportAllIssuesWhenNothingIsReady() {
        Long experimentId = 7L;
        Experiment experiment = buildExperiment(16L);

        when(experimentService.get(experimentId)).thenReturn(experiment);
        when(creativeRepository.countByExperimentId(experimentId)).thenReturn(0L);
        when(leadPortalFlowRepository.countByExperimentId(experimentId)).thenReturn(0L);
        for (TargetingElementType type : TargetingElementType.values()) {
            when(targetingElementRepository.existsApprovedForExperiment(experiment.getNiche().getId(), type,
                    experiment.getHypothesisRef().getId())).thenReturn(false);
        }

        ExperimentReadinessSummaryDto summary = service.summarize(experimentId);

        assertThat(summary.hasCreatives()).isFalse();
        assertThat(summary.hasLeadPortalFlow()).isFalse();
        assertThat(summary.hasCompleteTargeting()).isFalse();
        assertThat(summary.missingTargetingTypes()).containsExactlyInAnyOrder(TargetingElementType.values());
        assertThat(summary.issues()).hasSize(3);
        assertThat(summary.issues()).extracting(i -> i.type())
                .containsExactlyInAnyOrder(
                        ExperimentReadinessIssueType.CREATIVE,
                        ExperimentReadinessIssueType.LEAD_PORTAL_FLOW,
                        ExperimentReadinessIssueType.TARGETING
                );
    }

    @Test
    void shouldReturnNoIssuesWhenEverythingIsReady() {
        Long experimentId = 8L;
        Experiment experiment = buildExperiment(18L);

        when(experimentService.get(experimentId)).thenReturn(experiment);
        when(creativeRepository.countByExperimentId(experimentId)).thenReturn(2L);
        when(leadPortalFlowRepository.countByExperimentId(experimentId)).thenReturn(1L);
        for (TargetingElementType type : TargetingElementType.values()) {
            when(targetingElementRepository.existsApprovedForExperiment(experiment.getNiche().getId(), type,
                    experiment.getHypothesisRef().getId())).thenReturn(true);
        }

        ExperimentReadinessSummaryDto summary = service.summarize(experimentId);

        assertThat(summary.hasCreatives()).isTrue();
        assertThat(summary.hasLeadPortalFlow()).isTrue();
        assertThat(summary.hasCompleteTargeting()).isTrue();
        assertThat(summary.missingTargetingTypes()).isEmpty();
        assertThat(summary.issues()).isEmpty();
    }

    private Experiment buildExperiment(Long nicheId) {
        MarketNiche niche = new MarketNiche();
        niche.setId(nicheId);

        Hypothesis hypothesis = new Hypothesis();
        hypothesis.setId(UUID.randomUUID());
        hypothesis.setMarketNiche(niche);

        Experiment experiment = new Experiment();
        experiment.setId(0L);
        experiment.setNiche(niche);
        experiment.setHypothesisRef(hypothesis);
        return experiment;
    }
}
