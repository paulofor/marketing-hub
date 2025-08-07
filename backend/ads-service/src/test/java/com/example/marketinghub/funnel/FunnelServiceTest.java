package com.example.marketinghub.funnel;

import com.example.marketinghub.model.Lead;
import com.example.marketinghub.model.NurtureStage;
import com.example.marketinghub.repository.LeadRepository;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FunnelServiceTest {
    @InjectMocks
    private FunnelService service;
    @Mock private SalesFunnelRepository funnelRepository;
    @Mock private FunnelStepRepository stepRepository;
    @Mock private LeadRepository leadRepository;
    @Mock private LeadResponseRepository responseRepository;
    @Mock private StepMetricSnapshotRepository snapshotRepository;
    @Mock private ExperimentRepository experimentRepository;

    @Test
    void registerResponseUpdatesScoreAndStage() {
        UUID leadId = UUID.randomUUID();
        UUID stepId = UUID.randomUUID();
        Lead lead = Lead.builder().id(leadId).leadScore(0).nurtureStage(NurtureStage.NEW).build();
        FunnelStep step = FunnelStep.builder().id(stepId).scoreInc(40).build();
        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(stepRepository.findById(stepId)).thenReturn(Optional.of(step));

        service.registerResponse(leadId, stepId, ActionType.OPEN, null);

        assertEquals(40, lead.getLeadScore());
        assertEquals(NurtureStage.HOT, lead.getNurtureStage());
        verify(responseRepository).save(any());
        verify(leadRepository).save(lead);
    }

    @Test
    void createSetsBackReferenceOnSteps() {
        FunnelStep step = FunnelStep.builder().build();
        SalesFunnel funnel = SalesFunnel.builder().name("test").steps(java.util.List.of(step)).build();
        Long expId = 1L;
        Experiment experiment = new Experiment();
        when(experimentRepository.findById(expId)).thenReturn(java.util.Optional.of(experiment));
        when(funnelRepository.save(funnel)).thenReturn(funnel);

        SalesFunnel saved = service.create(expId, funnel);

        assertSame(saved, step.getFunnel());
        assertSame(experiment, saved.getExperiment());
        verify(funnelRepository).save(funnel);
    }
}
