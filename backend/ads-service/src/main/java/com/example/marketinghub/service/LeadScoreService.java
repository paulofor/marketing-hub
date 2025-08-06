package com.example.marketinghub.service;

import com.example.marketinghub.model.*;
import com.example.marketinghub.repository.FunnelEventRepository;
import com.example.marketinghub.repository.LeadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

/**
 * Applies scoring rules based on funnel stimuli.
 */
@Service
@RequiredArgsConstructor
public class LeadScoreService {

    private final LeadRepository leadRepository;
    private final FunnelEventRepository funnelEventRepository;

    private static final Map<FunnelStimulus, Integer> WEIGHTS = Map.of(
            FunnelStimulus.LEAD_CAPTURED, 1,
            FunnelStimulus.LANDING_VIEW, 1,
            FunnelStimulus.CHECKOUT_VIEW, 2,
            FunnelStimulus.CHATBOT_INTERACTION, 1,
            FunnelStimulus.PURCHASE, 5
    );

    /**
     * Records a stimulus for the lead and updates its score and nurture stage.
     * @param lead lead to update
     * @param stimulus funnel stimulus emitted
     */
    @Transactional
    public void recordEvent(Lead lead, FunnelStimulus stimulus) {
        int weight = WEIGHTS.getOrDefault(stimulus, 0);
        lead.setScore(lead.getScore() + weight);
        updateStage(lead);
        leadRepository.save(lead);

        FunnelEvent event = FunnelEvent.builder()
                .lead(lead)
                .stimulus(stimulus)
                .createdAt(Instant.now())
                .build();
        funnelEventRepository.save(event);
    }

    private void updateStage(Lead lead) {
        if (lead.getScore() >= 5) {
            lead.setNurtureStage(NurtureStage.HOT);
        } else if (lead.getScore() >= 2) {
            lead.setNurtureStage(NurtureStage.WARM);
        } else {
            lead.setNurtureStage(NurtureStage.NEW);
        }
    }
}
