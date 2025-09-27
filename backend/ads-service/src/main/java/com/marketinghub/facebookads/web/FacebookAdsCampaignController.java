package com.marketinghub.facebookads.web;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.service.ExperimentService;
import com.marketinghub.facebookads.BudgetMode;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.facebookads.FacebookAdsCampaignRepository;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/facebook-campaigns")
public class FacebookAdsCampaignController {
    private final ExperimentService experimentService;
    private final FacebookAdsCampaignRepository campaignRepository;

    public FacebookAdsCampaignController(ExperimentService experimentService,
                                         FacebookAdsCampaignRepository campaignRepository) {
        this.experimentService = experimentService;
        this.campaignRepository = campaignRepository;
    }

    @GetMapping("/experiments-ready")
    public List<ExperimentSummary> experimentsReady() {
        return experimentService.listReadyForCampaign().stream()
                .map(this::toSummary)
                .toList();
    }

    @GetMapping("/experiments")
    public List<ExperimentSummary> experiments(@RequestParam("status")
            com.marketinghub.experiment.ExperimentStatus status) {
        return experimentService
                .listByStatusAndPlatform(status, com.marketinghub.experiment.ExperimentPlatform.FACEBOOK)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @PostMapping
    public FacebookAdsCampaign create(@RequestBody CreateCampaignRequest req) {
        FacebookAdsCampaign campaign = new FacebookAdsCampaign();
        campaign.setId(req.id());
        campaign.setAdAccountId(req.adAccountId());
        campaign.setName(req.name());
        campaign.setObjective(req.objective());
        campaign.setBudgetMode(req.budgetMode());
        return campaignRepository.save(campaign);
    }

    private ExperimentSummary toSummary(Experiment experiment) {
        return new ExperimentSummary(
                experiment.getId(),
                experiment.getName(),
                experiment.getHypothesis(),
                experiment.getKpiTargetCpl(),
                experiment.getStartDate(),
                experiment.getEndDate(),
                experiment.getNiche() != null ? experiment.getNiche().getName() : null,
                experiment.getHypothesisRef() != null ? experiment.getHypothesisRef().getTitle() : null,
                computeMissingConfiguration(experiment));
    }

    private List<String> computeMissingConfiguration(Experiment experiment) {
        List<String> missing = new ArrayList<>();
        if (!experiment.isCreativeApproved()) {
            missing.add("creativeApproval");
        }
        if (experiment.getKpiTargetCpl() == null) {
            missing.add("kpiTargetCpl");
        }
        if (experiment.getStopLossCpl() == null) {
            missing.add("stopLossCpl");
        }
        if (experiment.getSampleSize() == null) {
            missing.add("sampleSize");
        }
        if (experiment.getStartDate() == null) {
            missing.add("startDate");
        }
        if (experiment.getEndDate() == null) {
            missing.add("endDate");
        }
        if (experiment.getSalesFunnel() == null) {
            missing.add("salesFunnel");
        }
        return missing;
    }

    public record ExperimentSummary(
            Long id,
            String name,
            String hypothesis,
            BigDecimal kpiTargetCpl,
            LocalDate startDate,
            LocalDate endDate,
            String nicheName,
            String hypothesisTitle,
            List<String> missingConfiguration) {}

    public record CreateCampaignRequest(
            String id,
            String adAccountId,
            String name,
            String objective,
            BudgetMode budgetMode) {}
}
