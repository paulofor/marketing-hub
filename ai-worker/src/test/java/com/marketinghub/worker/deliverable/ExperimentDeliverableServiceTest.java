package com.marketinghub.worker.deliverable;

import com.marketinghub.deliverable.Deliverable;
import com.marketinghub.deliverable.dto.CreateDeliverableRequest;
import com.marketinghub.deliverable.service.DeliverableService;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.worker.experiment.ExperimentGenerationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExperimentDeliverableServiceTest {
    @Mock
    ExperimentGenerationRepository generationRepository;
    @Mock
    DeliverableChatGptClient chatGptClient;
    @Mock
    DeliverableService deliverableService;
    @Mock
    ExperimentRepository experimentRepository;
    @InjectMocks
    ExperimentDeliverableService service;

    Experiment experiment;
    MarketNiche niche;

    @BeforeEach
    void setup() {
        niche = new MarketNiche();
        niche.setId(5L);
        experiment = new Experiment();
        experiment.setId(10L);
        experiment.setNiche(niche);
        experiment.setDeliverablesToGenerate(2);
        Hypothesis hypothesis = new Hypothesis();
        hypothesis.setTitle("Hipótese principal");
        experiment.setHypothesisRef(hypothesis);
    }

    @Test
    void generatePersistsDeliverablesAndResetsFlag() {
        when(generationRepository.findAllToGenerateDeliverables()).thenReturn(List.of(experiment));
        CreateDeliverableRequest first = new CreateDeliverableRequest();
        first.setTitle("Guia de captura");
        first.setPrompt("prompt");
        first.setModel("gpt-4o");
        CreateDeliverableRequest second = new CreateDeliverableRequest();
        second.setTitle("Checklist de follow-up");
        second.setPrompt("prompt");
        second.setModel("gpt-4o");
        when(chatGptClient.generateDeliverables(experiment, 2)).thenReturn(List.of(first, second));

        Deliverable firstDeliverable = new Deliverable();
        firstDeliverable.setId(100L);
        Deliverable secondDeliverable = new Deliverable();
        secondDeliverable.setId(101L);
        when(deliverableService.create(any(CreateDeliverableRequest.class)))
                .thenReturn(firstDeliverable)
                .thenReturn(secondDeliverable);

        Map<Long, List<Deliverable>> generated = service.generate();

        ArgumentCaptor<CreateDeliverableRequest> captor = ArgumentCaptor.forClass(CreateDeliverableRequest.class);
        verify(deliverableService, times(2)).create(captor.capture());
        List<CreateDeliverableRequest> savedRequests = captor.getAllValues();
        assertThat(savedRequests).allMatch(req -> req.getMarketNicheId().equals(niche.getId()));
        assertThat(experiment.getDeliverablesToGenerate()).isZero();
        verify(experimentRepository).save(experiment);
        assertThat(generated).containsKey(experiment.getId());
        assertThat(generated.get(experiment.getId())).containsExactly(firstDeliverable, secondDeliverable);
    }

    @Test
    void skipRequestsWithoutTitle() {
        when(generationRepository.findAllToGenerateDeliverables()).thenReturn(List.of(experiment));
        CreateDeliverableRequest missingTitle = new CreateDeliverableRequest();
        missingTitle.setDescription("Sem título");
        when(chatGptClient.generateDeliverables(experiment, 2)).thenReturn(List.of(missingTitle));

        Map<Long, List<Deliverable>> generated = service.generate();

        verify(deliverableService, never()).create(any());
        assertThat(experiment.getDeliverablesToGenerate()).isZero();
        verify(experimentRepository).save(experiment);
        assertThat(generated).containsEntry(experiment.getId(), List.of());
    }
}
