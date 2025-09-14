package com.marketinghub.facebookads.web;

import com.marketinghub.experiment.service.ExperimentService;
import com.marketinghub.facebookads.BudgetMode;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.facebookads.FacebookAdsCampaignRepository;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
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
                .map(e -> new ExperimentSummary(
                        e.getId(),
                        e.getName(),
                        e.getHypothesis(),
                        e.getKpiTargetCpl(),
                        e.getStartDate(),
                        e.getEndDate()))
                .toList();
    }

    @GetMapping("/experiments")
    public List<ExperimentSummary> experiments(@RequestParam("status")
            com.marketinghub.experiment.ExperimentStatus status) {
        return experimentService
                .listByStatusAndPlatform(status, com.marketinghub.experiment.ExperimentPlatform.FACEBOOK)
                .stream()
                .map(e -> new ExperimentSummary(
                        e.getId(),
                        e.getName(),
                        e.getHypothesis(),
                        e.getKpiTargetCpl(),
                        e.getStartDate(),
                        e.getEndDate()))
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

    public record ExperimentSummary(
            Long id,
            String name,
            String hypothesis,
            BigDecimal kpiTargetCpl,
            LocalDate startDate,
            LocalDate endDate) {}

    public record CreateCampaignRequest(
            String id,
            String adAccountId,
            String name,
            String objective,
            BudgetMode budgetMode) {}
}
