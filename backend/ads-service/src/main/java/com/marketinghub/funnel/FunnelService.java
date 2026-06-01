package com.marketinghub.funnel;

import com.marketinghub.repository.jpa.funnel.FunnelStepRepository;
import com.marketinghub.repository.jpa.funnel.LeadResponseRepository;
import com.marketinghub.repository.jpa.funnel.SalesFunnelRepository;
import com.marketinghub.repository.jpa.funnel.StepMetricSnapshotRepository;
import com.marketinghub.model.Lead;
import com.marketinghub.model.NurtureStage;
import com.marketinghub.repository.jpa.core.LeadRepository;
import com.marketinghub.funnel.dto.SalesFunnelDto;
import com.marketinghub.funnel.dto.FunnelStepDto;
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

    public List<SalesFunnelDto> list() {
        return funnelRepository.findAll().stream()
                .map(f -> {
                    SalesFunnelDto dto = new SalesFunnelDto();
                    dto.setId(f.getId());
                    dto.setName(f.getName());
                    dto.setObjective(f.getObjective());
                    dto.setExperimentCount(0);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public SalesFunnel create(SalesFunnel funnel) {
        if (funnel.getSteps() != null) {
            funnel.getSteps().forEach(step -> step.setFunnel(funnel));
        }
        return funnelRepository.save(funnel);
    }

    public SalesFunnel get(UUID id) {
        return funnelRepository.findWithStepsById(id).orElseThrow();
    }

    public SalesFunnel update(UUID id, SalesFunnel funnel) {
        funnel.setId(id);
        if (funnel.getSteps() != null) {
            funnel.getSteps().forEach(step -> step.setFunnel(funnel));
        }
        return funnelRepository.save(funnel);
    }

    public FunnelStepDto addStep(UUID funnelId, FunnelStepDto stepDto) {
        SalesFunnel funnel = funnelRepository.findById(funnelId).orElseThrow();
        FunnelStep step = toEntity(stepDto);
        step.setFunnel(funnel);
        FunnelStep saved = stepRepository.save(step);
        return toDto(saved);
    }

    private FunnelStep toEntity(FunnelStepDto dto) {
        if (dto == null) {
            return null;
        }
        return FunnelStep.builder()
                .id(dto.getId())
                .orderIdx(dto.getOrderIdx())
                .stimulusType(dto.getStimulusType())
                .channel(dto.getChannel())
                .templateId(dto.getTemplateId())
                .note(dto.getNote())
                .expectedAction(dto.getExpectedAction())
                .scoreInc(dto.getScoreInc())
                .revenueTarget(dto.getRevenueTarget())
                .isActive(dto.getIsActive())
                .build();
    }

    private FunnelStepDto toDto(FunnelStep step) {
        if (step == null) {
            return null;
        }
        FunnelStepDto dto = new FunnelStepDto();
        dto.setId(step.getId());
        dto.setOrderIdx(step.getOrderIdx());
        dto.setStimulusType(step.getStimulusType());
        dto.setChannel(step.getChannel());
        dto.setTemplateId(step.getTemplateId());
        dto.setNote(step.getNote());
        dto.setExpectedAction(step.getExpectedAction());
        dto.setScoreInc(step.getScoreInc());
        dto.setRevenueTarget(step.getRevenueTarget());
        dto.setIsActive(step.getIsActive());
        return dto;
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
