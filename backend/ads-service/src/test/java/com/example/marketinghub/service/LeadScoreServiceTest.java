package com.example.marketinghub.service;

import com.example.marketinghub.model.*;
import com.example.marketinghub.repository.FunnelEventRepository;
import com.example.marketinghub.repository.LeadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeadScoreServiceTest {

    @Mock
    LeadRepository leadRepository;
    @Mock
    FunnelEventRepository funnelEventRepository;

    LeadScoreService leadScoreService;

    @BeforeEach
    void setUp() {
        leadScoreService = new LeadScoreService(leadRepository, funnelEventRepository);
    }

    @Test
    void recordEventUpdatesScoreAndStage() {
        Lead lead = Lead.builder().id(UUID.randomUUID()).score(0).nurtureStage(NurtureStage.NEW).build();
        when(leadRepository.save(any())).thenReturn(lead);

        leadScoreService.recordEvent(lead, FunnelStimulus.CHECKOUT_VIEW);

        assertEquals(2, lead.getScore());
        assertEquals(NurtureStage.WARM, lead.getNurtureStage());
        verify(funnelEventRepository).save(any());
        verify(leadRepository).save(lead);
    }
}
