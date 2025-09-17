package com.marketinghub.worker.adset;

import com.marketinghub.audience.Audience;
import com.marketinghub.audience.repository.AudienceRepository;
import com.marketinghub.experiment.AdSet;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.dto.CreateAdSetRequest;
import com.marketinghub.experiment.repository.AdSetRepository;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.experiment.service.AdSetService;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.niche.MarketNiche;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AudienceAdSetServiceTest {
    @Mock
    ExperimentRepository experimentRepository;
    @Mock
    AudienceRepository audienceRepository;
    @Mock
    AdSetRepository adSetRepository;
    @Mock
    AdSetService adSetService;
    @Mock
    AudienceAdSetChatGptClient chatGptClient;
    @InjectMocks
    AudienceAdSetService service;

    private Experiment experiment;
    private Audience approvedAudience;
    private Audience pendingAudience;

    @BeforeEach
    void setUp() {
        MarketNiche niche = new MarketNiche();
        niche.setId(5L);

        Hypothesis hypothesis = new Hypothesis();
        hypothesis.setId(UUID.randomUUID());
        hypothesis.setMarketNiche(niche);

        experiment = new Experiment();
        experiment.setId(10L);
        experiment.setNiche(niche);
        experiment.setHypothesisRef(hypothesis);
        experiment.setStatus(ExperimentStatus.PLANNED);
        experiment.setPlatform(ExperimentPlatform.FACEBOOK);
        experiment.setCreativeApproved(true);

        approvedAudience = new Audience();
        approvedAudience.setId(1L);
        approvedAudience.setApproved(true);
        approvedAudience.setNiche(niche);
        approvedAudience.setHypothesis(hypothesis);
        approvedAudience.setName("Approved audience");

        pendingAudience = new Audience();
        pendingAudience.setId(2L);
        pendingAudience.setApproved(false);
        pendingAudience.setNiche(niche);
        pendingAudience.setHypothesis(hypothesis);
        pendingAudience.setName("Pending audience");
    }

    @Test
    void generateSkipsUnapprovedAudiences() {
        when(experimentRepository.findAllReadyForAdSets(eq(ExperimentPlatform.FACEBOOK), anyList()))
                .thenReturn(List.of(experiment));
        when(adSetRepository.countByExperimentId(10L)).thenReturn(0L);
        when(audienceRepository.findDetailedByNicheId(5L))
                .thenReturn(List.of(approvedAudience, pendingAudience));
        AdSetPlan plan = new AdSetPlan("BR", List.of("Interest"), List.of(), BigDecimal.TEN, 7, "{}", "prompt", "gpt-4");
        when(chatGptClient.planAdSet(experiment, approvedAudience)).thenReturn(plan);
        AdSet saved = new AdSet();
        when(adSetService.create(any(CreateAdSetRequest.class))).thenReturn(saved);

        Map<Long, List<AdSet>> result = service.generate();

        verify(chatGptClient).planAdSet(experiment, approvedAudience);
        verify(chatGptClient, never()).planAdSet(any(), eq(pendingAudience));
        verify(adSetService).create(any(CreateAdSetRequest.class));
        assertThat(result).containsEntry(10L, List.of(saved));
    }
}
