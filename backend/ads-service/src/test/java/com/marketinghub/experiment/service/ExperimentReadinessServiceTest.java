package com.marketinghub.experiment.service;

import com.marketinghub.ads.FacebookInstantForm;
import com.marketinghub.ads.FacebookPage;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCaptureDestinationType;
import com.marketinghub.experiment.dto.ExperimentReadinessIssueDto;
import com.marketinghub.experiment.dto.ExperimentReadinessIssueType;
import com.marketinghub.experiment.dto.ExperimentReadinessSummaryDto;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.targeting.TargetingCandidateType;
import com.marketinghub.targeting.TargetingElement;
import com.marketinghub.targeting.TargetingElementType;
import com.marketinghub.repository.jpa.experiment.ExperimentTargetingSelectionRepository;
import com.marketinghub.repository.jpa.targeting.TargetingElementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
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

    @InjectMocks
    private ExperimentReadinessService service;

    @Test
    void shouldReportAllIssuesWhenNothingIsReady() {
        Long experimentId = 7L;
        Experiment experiment = buildExperiment(experimentId, 16L);

        when(experimentService.get(experimentId)).thenReturn(experiment);
        when(creativeRepository.countByExperimentId(experimentId)).thenReturn(0L);
        when(creativeRepository.existsByExperimentIdAndStatus(experimentId, CreativeStatus.READY)).thenReturn(false);
        ExperimentReadinessSummaryDto summary = service.summarize(experimentId);

        assertThat(summary.hasCreatives()).isFalse();
        assertThat(summary.hasLeadPortalFlow()).isFalse();
        assertThat(summary.hasCompleteTargeting()).isTrue();
        assertThat(summary.missingTargetingTypes()).isEmpty();
        assertThat(summary.issues()).hasSize(2);
        assertThat(summary.issues()).extracting(ExperimentReadinessIssueDto::type)
                .containsExactlyInAnyOrder(
                        ExperimentReadinessIssueType.CREATIVE,
                        ExperimentReadinessIssueType.LEAD_PORTAL_FLOW
                );
    }

    @Test
    void shouldReturnNoIssuesWhenEverythingIsReady() {
        Long experimentId = 8L;
        Experiment experiment = buildExperiment(experimentId, 18L);
        LeadPortalFlow flow = new LeadPortalFlow();
        flow.setApproved(true);
        experiment.setLeadPortalFlow(flow);
        experiment.setFollowUpActionUrl("https://example.com/landing/8");

        when(experimentService.get(experimentId)).thenReturn(experiment);
        when(creativeRepository.countByExperimentId(experimentId)).thenReturn(2L);
        when(creativeRepository.existsByExperimentIdAndStatus(experimentId, CreativeStatus.READY)).thenReturn(true);
        ExperimentReadinessSummaryDto summary = service.summarize(experimentId);

        assertThat(summary.hasCreatives()).isTrue();
        assertThat(summary.hasLeadPortalFlow()).isTrue();
        assertThat(summary.hasCompleteTargeting()).isTrue();
        assertThat(summary.missingTargetingTypes()).isEmpty();
        assertThat(summary.issues()).isEmpty();
    }

    @Test
    void shouldNotReportTargetingIssueWhenThereIsNoApprovedJobTitleSelection() {
        Long experimentId = 11L;
        Experiment experiment = buildExperiment(experimentId, 19L);
        LeadPortalFlow flow = new LeadPortalFlow();
        flow.setApproved(true);
        experiment.setLeadPortalFlow(flow);
        experiment.setFollowUpActionUrl("https://example.com/landing/11");

        when(experimentService.get(experimentId)).thenReturn(experiment);
        when(creativeRepository.countByExperimentId(experimentId)).thenReturn(1L);
        when(creativeRepository.existsByExperimentIdAndStatus(experimentId, CreativeStatus.READY)).thenReturn(true);
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
        experiment.setFollowUpActionUrl("https://example.com/landing/15");

        when(experimentService.get(experimentId)).thenReturn(experiment);
        when(creativeRepository.countByExperimentId(experimentId)).thenReturn(1L);
        when(creativeRepository.existsByExperimentIdAndStatus(experimentId, CreativeStatus.READY)).thenReturn(true);
        ExperimentReadinessSummaryDto summary = service.summarize(experimentId);

        assertThat(summary.hasCompleteTargeting()).isTrue();
        assertThat(summary.missingTargetingTypes()).isEmpty();
        assertThat(summary.issues()).extracting(ExperimentReadinessIssueDto::type)
                .doesNotContain(ExperimentReadinessIssueType.TARGETING);
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
    @Test
    void shouldRequireInstantFormWhenCaptureDestinationIsMetaInstantForm() {
        Experiment experiment = buildExperiment(22L, 32L);
        experiment.setCaptureDestinationType(ExperimentCaptureDestinationType.META_INSTANT_FORM);
        experiment.setFollowUpActionUrl("https://example.com/landing/22");

        when(creativeRepository.existsByExperimentIdAndStatus(22L, CreativeStatus.READY)).thenReturn(true);

        assertThat(service.computeMissingConfiguration(experiment))
                .containsExactly("instantFormDestination");
    }

    @Test
    void shouldBeReadyForCampaignWhenMetaInstantFormIsApprovedAndAddressable() {
        Experiment experiment = buildExperiment(23L, 33L);
        experiment.setCaptureDestinationType(ExperimentCaptureDestinationType.META_INSTANT_FORM);
        experiment.setFacebookInstantForm(FacebookInstantForm.builder()
                .approved(true)
                .formId("123456789")
                .page(FacebookPage.builder().id(1L).pageId("84").name("Página").build())
                .build());

        when(creativeRepository.existsByExperimentIdAndStatus(23L, CreativeStatus.READY)).thenReturn(true);

        assertThat(service.computeMissingConfiguration(experiment)).isEmpty();
        assertThat(service.isReadyForCampaign(experiment)).isTrue();
    }

    @Test
    void shouldSummarizeInstantFormReadinessForMetaDestination() {
        Long experimentId = 24L;
        Experiment experiment = buildExperiment(experimentId, 34L);
        experiment.setCaptureDestinationType(ExperimentCaptureDestinationType.META_INSTANT_FORM);
        experiment.setFacebookInstantForm(FacebookInstantForm.builder()
                .approved(true)
                .shareLink("https://www.facebook.com/ads/leadgen/?id=2468")
                .page(FacebookPage.builder().id(2L).pageId("84").name("Página").build())
                .build());

        when(experimentService.get(experimentId)).thenReturn(experiment);
        when(creativeRepository.countByExperimentId(experimentId)).thenReturn(1L);
        when(creativeRepository.existsByExperimentIdAndStatus(experimentId, CreativeStatus.READY)).thenReturn(true);

        ExperimentReadinessSummaryDto summary = service.summarize(experimentId);

        assertThat(summary.captureDestinationType()).isEqualTo(ExperimentCaptureDestinationType.META_INSTANT_FORM);
        assertThat(summary.hasInstantForm()).isTrue();
        assertThat(summary.instantFormCount()).isEqualTo(1L);
        assertThat(summary.issues()).isEmpty();
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
        experiment.setCreativeApproved(true);
        return experiment;
    }

}
