
package com.marketinghub.leadportal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.leadportal.catalog.SimpleFlowCatalog;
import com.marketinghub.leadportal.entity.FlowEntity;
import com.marketinghub.leadportal.model.Flow;
import com.marketinghub.leadportal.model.FlowQuestion;
import com.marketinghub.leadportal.model.FlowQuestionType;
import com.marketinghub.leadportal.repository.FlowAccessRepository;
import com.marketinghub.leadportal.repository.FlowRepository;
import com.marketinghub.leadportal.style.SimpleFormStyleDefaults;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FlowServiceTest {

    @Mock
    private FlowRepository flowRepository;

    @Mock
    private FlowAccessRepository flowAccessRepository;

    @Mock
    private SimpleFlowCatalog simpleFlowCatalog;

    @Mock
    private FlowAssetService flowAssetService;

    private FlowService flowService;

    @BeforeEach
    void setUp() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        SimpleFormStyleDefaults simpleFormStyleDefaults = new SimpleFormStyleDefaults();
        flowService = new FlowService(
                flowRepository,
                flowAccessRepository,
                meterRegistry,
                simpleFlowCatalog,
                flowAssetService,
                simpleFormStyleDefaults,
                new CustomFormHtmlResolver(new ObjectMapper()));
        when(simpleFlowCatalog.find(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void shouldPersistOptimizedAssetsWhenServingFlow() {
        FlowEntity entity = new FlowEntity();
        entity.setSlug("legacy-flow");
        entity.setName("Legacy Flow");
        entity.setDescription("desc");
        entity.setAccessCount(5L);
        FlowQuestion legacyQuestion = new FlowQuestion(
                "http://legacy.example/uploads/proof.png",
                "exemplo_real_card_1_imagem_url",
                FlowQuestionType.TEXT,
                false,
                null,
                null,
                List.of());
        entity.setQuestions(List.of(legacyQuestion));

        FlowQuestion optimizedQuestion = new FlowQuestion(
                "https://cdn.example.com/optimized.png",
                "exemplo_real_card_1_imagem_url",
                FlowQuestionType.TEXT,
                false,
                null,
                null,
                List.of());
        Flow optimizedFlow = new Flow(
                entity.getSlug(),
                entity.getName(),
                entity.getDescription(),
                null,
                entity.getModel(),
                entity.getPrompt(),
                entity.getImagePromptModel(),
                entity.getImagePromptTemplate(),
                entity.getImageBatchSize(),
                List.of(optimizedQuestion),
                null);

        when(flowRepository.findById("legacy-flow")).thenReturn(Optional.of(entity));
        when(flowAssetService.optimizeAssets(any(Flow.class))).thenReturn(optimizedFlow);
        when(flowRepository.save(any(FlowEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Flow result = flowService.getAndTrackAccess("legacy-flow", null);

        assertThat(result).isEqualTo(optimizedFlow);

        ArgumentCaptor<FlowEntity> captor = ArgumentCaptor.forClass(FlowEntity.class);
        verify(flowRepository).save(captor.capture());
        FlowEntity saved = captor.getValue();
        assertThat(saved.getQuestions()).containsExactlyElementsOf(optimizedFlow.questions());
        assertThat(saved.getAccessCount()).isEqualTo(entity.getAccessCount());
    }

    @Test
    void shouldSkipPersistenceWhenAssetsAlreadyOptimized() {
        FlowEntity entity = new FlowEntity();
        entity.setSlug("optimized-flow");
        entity.setName("Flow");
        entity.setQuestions(List.of());
        entity.setAccessCount(2L);

        when(flowRepository.findById("optimized-flow")).thenReturn(Optional.of(entity));
        when(flowAssetService.optimizeAssets(any(Flow.class))).thenAnswer(invocation -> invocation.getArgument(0));

        flowService.getAndTrackAccess("optimized-flow", null);

        verify(flowRepository, never()).save(any(FlowEntity.class));
    }
}
