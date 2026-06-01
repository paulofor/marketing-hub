package com.marketinghub.experiment.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.experiment.funnel.ExperimentFunnelAutoStopService;
import java.math.BigDecimal;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Periodically evaluates running experiments using metrics. */
@Component
public class ExperimentEngine {
    private final ExperimentRepository repository;
    private final ExperimentInsightsFetcher fetcher;
    private final ExperimentFunnelAutoStopService funnelAutoStopService;

    public ExperimentEngine(ExperimentRepository repository, ExperimentInsightsFetcher fetcher,
                            ExperimentFunnelAutoStopService funnelAutoStopService) {
        this.repository = repository;
        this.fetcher = fetcher;
        this.funnelAutoStopService = funnelAutoStopService;
    }

    @Scheduled(fixedDelay = 60 * 60 * 1000)
    @Transactional
    public void evaluate() {
        for (Experiment exp : repository.findByStatus(ExperimentStatus.RUNNING)) {
            if (funnelAutoStopService.stopIfFormSubmissionZeroConversions(exp)) {
                continue;
            }
            var stats = fetcher.fetch(exp.getId());
            BigDecimal cpl = stats.getCpl();
            if (stats.clicks() >= 300 && cpl.compareTo(exp.getStopLossCpl()) > 0) {
                exp.setStatus(ExperimentStatus.PAUSED);
                continue;
            }
            if (exp.getSampleSize() != null && stats.clicks() >= exp.getSampleSize()) {
                var ci = StatsUtils.ci95(stats.leads(), stats.clicks());
                double target = exp.getTargetCvr().doubleValue() / 100.0;
                double baseline = exp.getBaselineCvr().doubleValue() / 100.0;
                if (ci.lower() >= target) {
                    exp.setStatus(ExperimentStatus.VALIDATED);
                } else if (ci.upper() <= baseline) {
                    exp.setStatus(ExperimentStatus.INVALIDATED);
                } else {
                    exp.setStatus(ExperimentStatus.INCONCLUSIVE);
                }
            }
        }
    }
}
