
package com.marketinghub.leadportal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Valida leitura, persistência e contabilização de acessos dos fluxos. */
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

    /** Monta o serviço com dependências isoladas antes de cada cenário. */
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
                simpleFormStyleDefaults);
        when(simpleFlowCatalog.find(anyString())).thenReturn(Optional.empty());
    }

    /** Garante que a rota pública não migra ativos nem grava alterações durante a leitura. */
    @Test
    void shouldNotOptimizeAssetsWhenServingFlow() {
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

        when(flowRepository.findById("legacy-flow")).thenReturn(Optional.of(entity));

        Flow result = flowService.getAndTrackAccess("legacy-flow", null);

        assertThat(result.questions()).containsExactly(legacyQuestion);
        verify(flowAssetService, never()).optimizeAssets(any(Flow.class));
        verify(flowRepository, never()).save(any(FlowEntity.class));
        verify(flowRepository).incrementAccessCount("legacy-flow");
    }

    /** Garante que fluxos já otimizados também permanecem somente leitura ao serem servidos. */
    @Test
    void shouldSkipPersistenceWhenAssetsAlreadyOptimized() {
        FlowEntity entity = new FlowEntity();
        entity.setSlug("optimized-flow");
        entity.setName("Flow");
        entity.setQuestions(List.of());
        entity.setAccessCount(2L);

        when(flowRepository.findById("optimized-flow")).thenReturn(Optional.of(entity));
        flowService.getAndTrackAccess("optimized-flow", null);

        verify(flowAssetService, never()).optimizeAssets(any(Flow.class));
        verify(flowRepository, never()).save(any(FlowEntity.class));
    }
}
