package com.marketinghub.pipeline.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.pipeline.Pipeline;
import com.marketinghub.pipeline.PipelineStage;
import com.marketinghub.pipeline.dto.PipelineStageRequest;
import com.marketinghub.repository.jpa.pipeline.PipelineRepository;
import com.marketinghub.repository.jpa.pipeline.PipelineStageRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Testa as regras de manutenção de pipelines e etapas configuráveis.
 */
@ExtendWith(MockitoExtension.class)
class PipelineServiceTest {
    @Mock
    private PipelineRepository pipelineRepository;

    @Mock
    private PipelineStageRepository stageRepository;

    @InjectMocks
    private PipelineService service;

    /**
     * Garante que a listagem devolve pipelines e etapas em ordem operacional previsível.
     */
    @Test
    void shouldListPipelinesWithOrderedStages() {
        Pipeline first = Pipeline.builder()
                .id(1L)
                .module("EXPERIMENT")
                .name("Pipeline B")
                .code("pipeline-b")
                .stages(new ArrayList<>(List.of(stage(2L, 2), stage(1L, 1))))
                .build();
        Pipeline second = Pipeline.builder()
                .id(2L)
                .module("EXPERIMENT")
                .name("Pipeline A")
                .code("pipeline-a")
                .stages(new ArrayList<>())
                .build();
        when(pipelineRepository.findAll()).thenReturn(List.of(first, second));

        List<Pipeline> result = service.list();

        assertThat(result).extracting(Pipeline::getName).containsExactly("Pipeline A", "Pipeline B");
        assertThat(first.getStages()).extracting(PipelineStage::getPosition).containsExactly(1, 2);
    }

    /**
     * Garante que a criação de etapa vincula a etapa ao pipeline informado na rota.
     */
    @Test
    void shouldCreateStageLinkedToPipeline() {
        Pipeline pipeline = Pipeline.builder().id(10L).name("Pipeline").code("pipeline").module("EXPERIMENT").build();
        PipelineStageRequest request = new PipelineStageRequest();
        request.setPosition(1);
        request.setName("Campaign Angle");
        request.setCode("campaign-angle");
        request.setDescription("Ângulo da campanha");
        when(pipelineRepository.findById(10L)).thenReturn(Optional.of(pipeline));
        when(stageRepository.save(any(PipelineStage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PipelineStage created = service.createStage(10L, request);

        assertThat(created.getPipeline()).isEqualTo(pipeline);
        assertThat(created.getPosition()).isEqualTo(1);
        assertThat(created.getName()).isEqualTo("Campaign Angle");
        assertThat(created.isRequired()).isTrue();
        ArgumentCaptor<PipelineStage> captor = ArgumentCaptor.forClass(PipelineStage.class);
        verify(stageRepository).save(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("campaign-angle");
    }

    /**
     * Cria uma etapa sintética para validar ordenação no serviço.
     */
    private PipelineStage stage(Long id, Integer position) {
        return PipelineStage.builder()
                .id(id)
                .position(position)
                .name("Etapa " + position)
                .code("stage-" + position)
                .build();
    }

}
