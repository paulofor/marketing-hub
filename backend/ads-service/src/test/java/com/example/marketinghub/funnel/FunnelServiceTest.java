package com.example.marketinghub.funnel;

import com.example.marketinghub.model.Lead;
import com.example.marketinghub.model.NurtureStage;
import com.example.marketinghub.repository.LeadRepository;
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
}
