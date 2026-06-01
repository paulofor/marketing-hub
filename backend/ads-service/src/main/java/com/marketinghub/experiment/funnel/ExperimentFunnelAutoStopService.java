package com.marketinghub.experiment.funnel;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelDiagnosticsResponseDto;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelStageDiagnosticDto;
import com.marketinghub.experiment.funnel.dto.FunnelThresholdCheckDto;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsCampaignRepository;
import com.marketinghub.facebookads.FacebookCampaignStopReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class ExperimentFunnelAutoStopService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentFunnelAutoStopService.class);
    private static final double THREE_PERCENT = 0.03d;

    private final ExperimentFunnelDiagnosticService diagnosticService;
    private final FacebookAdsCampaignRepository campaignRepository;

    public ExperimentFunnelAutoStopService(ExperimentFunnelDiagnosticService diagnosticService,
                                           FacebookAdsCampaignRepository campaignRepository) {
        this.diagnosticService = diagnosticService;
        this.campaignRepository = campaignRepository;
    }

    /**
     * Evaluates the form submission step and, when the rule-of-three threshold at 3% fails,
     * invalidates the experiment and requests an automatic stop for all linked Facebook campaigns.
     *
     * @return {@code true} when the experiment was stopped automatically, {@code false} otherwise.
     */
    public boolean stopIfFormSubmissionZeroConversions(Experiment experiment) {
        if (experiment == null || experiment.getStatus() != ExperimentStatus.RUNNING) {
            return false;
        }
        ExperimentFunnelDiagnosticsResponseDto diagnostics = diagnosticService.diagnose(experiment.getId());
        ExperimentFunnelStageDiagnosticDto submissionStage = diagnostics.diagnostics().stream()
                .filter(dto -> dto.stageKey() == ExperimentFunnelStage.ENVIO_FORM)
                .findFirst()
                .orElse(null);
        if (submissionStage == null || submissionStage.thresholdChecks() == null) {
            return false;
        }
        boolean threePercentFailed = submissionStage.thresholdChecks().stream()
                .filter(Objects::nonNull)
                .anyMatch(this::isThreePercentFailure);
        if (!threePercentFailed) {
            return false;
        }
        LOGGER.warn(
                "Automatic stop triggered for experiment {} due to zero conversions after reaching the 3%% rule-of-three threshold.",
                experiment.getId()
        );
        experiment.setStatus(ExperimentStatus.INVALIDATED);
        requestFacebookCampaignStops(experiment.getId());
        return true;
    }

    private boolean isThreePercentFailure(FunnelThresholdCheckDto check) {
        if (check == null || check.minAcceptableRate() == null) {
            return false;
        }
        return Math.abs(check.minAcceptableRate() - THREE_PERCENT) < 1e-9 && check.statisticallyFailed();
    }

    private void requestFacebookCampaignStops(Long experimentId) {
        List<FacebookAdsCampaign> campaigns = campaignRepository.findByExperimentId(experimentId);
        if (campaigns == null || campaigns.isEmpty()) {
            LOGGER.info(
                    "Experiment {} has no Facebook campaigns registered; status updated but no stop request was necessary.",
                    experimentId
            );
            return;
        }
        Instant now = Instant.now();
        campaigns.stream()
                .filter(campaign -> campaign.getStopCompletedAt() == null)
                .forEach(campaign -> {
                    if (campaign.getStopRequestedAt() == null) {
                        campaign.setStopRequestedAt(now);
                    }
                    campaign.setStopReason(FacebookCampaignStopReason.FORM_ZERO_CONVERSION_RULE_OF_THREE);
                    campaign.setStopLastError(null);
                });
    }
}
