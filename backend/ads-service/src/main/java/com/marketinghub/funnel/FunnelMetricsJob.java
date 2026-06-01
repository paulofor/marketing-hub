package com.marketinghub.funnel;

import com.marketinghub.repository.jpa.funnel.FunnelStepRepository;
import com.marketinghub.repository.jpa.funnel.LeadResponseRepository;
import com.marketinghub.repository.jpa.funnel.StepMetricSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Aggregates responses into snapshots every five minutes.
 */
@Component
@RequiredArgsConstructor
public class FunnelMetricsJob {
    private final LeadResponseRepository responseRepository;
    private final FunnelStepRepository stepRepository;
    private final StepMetricSnapshotRepository snapshotRepository;

    @Scheduled(fixedDelay = 300000)
    public void aggregate() {
        List<LeadResponse> responses = responseRepository.findAll();
        Map<UUID, List<LeadResponse>> grouped = responses.stream()
                .collect(Collectors.groupingBy(r -> r.getFunnelStep().getId()));
        for (UUID stepId : grouped.keySet()) {
            List<LeadResponse> stepResponses = grouped.get(stepId);
            long resp = stepResponses.size();
            long conv = stepResponses.stream().filter(r -> r.getAction() == ActionType.PURCHASE).count();
            BigDecimal revenue = stepResponses.stream()
                    .map(r -> r.getRevenue() != null ? r.getRevenue() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal cvr = resp == 0 ? BigDecimal.ZERO : new BigDecimal(conv).divide(new BigDecimal(resp), 4, RoundingMode.HALF_UP);
            StepMetricSnapshot snapshot = StepMetricSnapshot.builder()
                    .funnelStep(stepRepository.findById(stepId).orElse(null))
                    .responses(resp)
                    .conversions(conv)
                    .revenue(revenue)
                    .cvr(cvr)
                    .capturedAt(Instant.now())
                    .build();
            snapshotRepository.save(snapshot);
        }
    }
}
