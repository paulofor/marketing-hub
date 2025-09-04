package com.marketinghub.worker.creative;

import com.marketinghub.creative.Creative;
import com.marketinghub.creative.dto.CreateCreativeRequest;
import com.marketinghub.creative.service.CreativeService;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.hypothesis.Hypothesis;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExperimentCreativeServiceTest {
    @Mock
    ExperimentRepository experimentRepository;
    @Mock
    CreativeChatGptClient chatGptClient;
    @Mock
    CreativeImageClient imageClient;
    @Mock
    CreativeService creativeService;
    @InjectMocks
    ExperimentCreativeService service;

    Experiment experiment;

    @BeforeEach
    void setup() {
        experiment = new Experiment();
        experiment.setId(1L);
        experiment.setCreativesToGenerate(1);
        Hypothesis h = new Hypothesis();
        h.setTitle("title");
        experiment.setHypothesisRef(h);
    }

    @Test
    void generateCreatesCreativesAndResetsFlag() {
        when(experimentRepository.findAllToGenerateCreatives()).thenReturn(List.of(experiment));
        CreateCreativeRequest req = new CreateCreativeRequest();
        req.setHeadline("h1");
        req.setPrimaryText("p1");
        when(chatGptClient.generateCreatives(experiment, 1)).thenReturn(List.of(req));
        when(imageClient.generateImage("h1")).thenReturn("img");
        Creative saved = new Creative();
        when(creativeService.create(1L, req)).thenReturn(saved);

        Map<Long, List<Creative>> result = service.generate();

        verify(imageClient).generateImage("h1");
        assertThat(req.getImageUrl()).isEqualTo("img");
        verify(creativeService).create(1L, req);
        verify(experimentRepository).save(experiment);
        assertThat(experiment.getCreativesToGenerate()).isZero();
        assertThat(result.get(1L)).containsExactly(saved);
    }

    @Test
    void truncateLongTextsBeforeSaving() {
        when(experimentRepository.findAllToGenerateCreatives()).thenReturn(List.of(experiment));
        String longText = "a".repeat(300);
        CreateCreativeRequest req = new CreateCreativeRequest();
        req.setHeadline(longText);
        req.setPrimaryText(longText);
        when(chatGptClient.generateCreatives(experiment, 1)).thenReturn(List.of(req));
        when(imageClient.generateImage(anyString())).thenReturn("img");
        Creative saved = new Creative();
        when(creativeService.create(eq(1L), any(CreateCreativeRequest.class))).thenReturn(saved);

        service.generate();

        ArgumentCaptor<CreateCreativeRequest> captor = ArgumentCaptor.forClass(CreateCreativeRequest.class);
        verify(creativeService).create(eq(1L), captor.capture());
        CreateCreativeRequest captured = captor.getValue();
        assertThat(captured.getHeadline().length()).isEqualTo(255);
        assertThat(captured.getPrimaryText().length()).isEqualTo(255);
    }
}
