package com.marketinghub.facebookads.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.facebookads.FacebookCampaignMetricSnapshot;
import com.marketinghub.facebookads.FacebookCampaignMetricSnapshotRepository;
import com.marketinghub.facebookads.dto.CampaignMetricRequest;
import com.marketinghub.facebookads.dto.ExperimentPerformanceDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class FacebookCampaignMetricService {
    private final FacebookCampaignMetricSnapshotRepository snapshotRepository;

    public FacebookCampaignMetricService(FacebookCampaignMetricSnapshotRepository snapshotRepository) {
        this.snapshotRepository = snapshotRepository;
    }

    @Transactional
    public FacebookCampaignMetricSnapshot recordSnapshot(FacebookAdsCampaign campaign, CampaignMetricRequest request) {
        FacebookCampaignMetricSnapshot snapshot = new FacebookCampaignMetricSnapshot();
        snapshot.setCampaign(campaign);
        snapshot.setExperiment(campaign.getExperiment());
        snapshot.setAccountId(request.accountId());
        snapshot.setCurrency(request.currency());
        snapshot.setDateStart(parseDate(request.dateStart()));
        snapshot.setDateStop(parseDate(request.dateStop()));
        snapshot.setSpend(request.spend());
        snapshot.setImpressions(request.impressions());
        snapshot.setReach(request.reach());
        snapshot.setClicks(request.clicks());
        snapshot.setCtr(request.ctr());
        snapshot.setCpc(request.cpc());
        snapshot.setCpm(request.cpm());
        snapshot.setLeads(request.leads());
        snapshot.setRawResponse(extractRaw(request.rawInsights()));
        return snapshotRepository.save(snapshot);
    }

    public List<ExperimentPerformanceDto> latestPerformance(Long experimentId) {
        List<FacebookCampaignMetricSnapshot> snapshots = experimentId == null
                ? snapshotRepository.findLatestPerExperiment()
                : snapshotRepository.findTop30ByExperimentIdOrderByCapturedAtDesc(experimentId);
        return snapshots.stream()
                .map(this::toDto)
                .toList();
    }

    private ExperimentPerformanceDto toDto(FacebookCampaignMetricSnapshot snapshot) {
        BigDecimal cpl = null;
        if (snapshot.getLeads() != null && snapshot.getLeads() > 0 && snapshot.getSpend() != null) {
            cpl = snapshot.getSpend().divide(BigDecimal.valueOf(snapshot.getLeads()), 4, RoundingMode.HALF_UP);
        }
        return new ExperimentPerformanceDto(
                snapshot.getExperiment().getId(),
                snapshot.getExperiment().getName(),
                snapshot.getCampaign().getId(),
                snapshot.getCapturedAt(),
                snapshot.getDateStart(),
                snapshot.getDateStop(),
                snapshot.getSpend(),
                snapshot.getImpressions(),
                snapshot.getReach(),
                snapshot.getClicks(),
                snapshot.getLeads(),
                snapshot.getCtr(),
                snapshot.getCpc(),
                snapshot.getCpm(),
                cpl,
                snapshot.getCurrency()
        );
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return LocalDate.parse(raw);
    }

    private String extractRaw(JsonNode rawInsights) {
        return rawInsights == null ? null : rawInsights.toString();
    }
}
