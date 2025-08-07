package com.example.marketinghub.funnel;

import com.example.marketinghub.model.Lead;
import com.example.marketinghub.model.NurtureStage;
import com.example.marketinghub.repository.LeadRepository;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service layer for funnel operations.
 */
@Service
@RequiredArgsConstructor
public class FunnelService {
    private final SalesFunnelRepository funnelRepository;
    private final FunnelStepRepository stepRepository;
    private final LeadRepository leadRepository;
    private final LeadResponseRepository responseRepository;
    private final StepMetricSnapshotRepository snapshotRepository;
    private final ExperimentRepository experimentRepository;

    public List<StepMetricSnapshot> getSnapshots(UUID funnelId) {
        return stepRepository.findByFunnelId(funnelId).stream()
                .flatMap(step -> snapshotRepository.findByFunnelStepIdOrderByCapturedAtDesc(step.getId()).stream().limit(1))
                .collect(Collectors.toList());
    }

    public List<SalesFunnel> findBest(String metric) {
        return funnelRepository.findAll().stream()
                .sorted((a, b) -> totalRevenue(b.getId()).compareTo(totalRevenue(a.getId())))
                .collect(Collectors.toList());
    }

    private java.math.BigDecimal totalRevenue(UUID funnelId) {
        return stepRepository.findByFunnelId(funnelId).stream()
                .flatMap(step -> snapshotRepository.findByFunnelStepIdOrderByCapturedAtDesc(step.getId()).stream())
                .map(s -> s.getRevenue() != null ? s.getRevenue() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }

    public List<SalesFunnel> findByExperiment(Long experimentId) {
        return funnelRepository.findByExperimentId(experimentId);
    }

    public SalesFunnel create(Long experimentId, SalesFunnel funnel) {
        Experiment experiment = experimentRepository.findById(experimentId).orElseThrow();
        funnel.setExperiment(experiment);
        if (funnel.getSteps() != null) {
            funnel.getSteps().forEach(step -> step.setFunnel(funnel));
        }
        return funnelRepository.save(funnel);
    }

    public FunnelStep addStep(UUID funnelId, FunnelStep step) {
        SalesFunnel funnel = funnelRepository.findById(funnelId).orElseThrow();
        step.setFunnel(funnel);
        return stepRepository.save(step);
    }

    @Transactional
    public void registerResponse(UUID leadId, UUID stepId, ActionType action, BigDecimal revenue) {
        Lead lead = leadRepository.findById(leadId).orElseThrow();
        FunnelStep step = stepRepository.findById(stepId).orElseThrow();

        LeadResponse response = LeadResponse.builder()
                .lead(lead)
                .funnelStep(step)
                .action(action)
                .revenue(revenue)
                .occurredAt(Instant.now())
                .build();
        responseRepository.save(response);

        int newScore = lead.getLeadScore() + (step.getScoreInc() != null ? step.getScoreInc() : 0);
        lead.setLeadScore(newScore);
        if (newScore >= 100) {
            lead.setNurtureStage(NurtureStage.CLIENTE);
        } else if (newScore >= 30) {
            lead.setNurtureStage(NurtureStage.HOT);
        } else if (newScore > 0) {
            lead.setNurtureStage(NurtureStage.WARM);
        }
        leadRepository.save(lead);
    }
}
