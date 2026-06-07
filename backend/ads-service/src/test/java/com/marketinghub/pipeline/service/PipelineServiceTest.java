package com.marketinghub.pipeline.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.openai.OpenAiModel;
import com.marketinghub.pipeline.Pipeline;
import com.marketinghub.pipeline.PipelineDefinitionEntity;
import com.marketinghub.pipeline.PipelineStage;
import com.marketinghub.pipeline.PipelineStageConfig;
import com.marketinghub.pipeline.PipelineStageDefinitionEntity;
import com.marketinghub.pipeline.definition.PipelineDefinitionRegistry;
import com.marketinghub.pipeline.dto.GeraLandingStageModelDto;
import com.marketinghub.pipeline.dto.PipelineDiagnosticsDto;
import com.marketinghub.pipeline.dto.PipelineRequest;
import com.marketinghub.pipeline.dto.PipelineStageRequest;
import com.marketinghub.pipeline.dto.PipelineSyncResultDto;
import com.marketinghub.repository.jpa.openai.OpenAiModelRepository;
import com.marketinghub.repository.jpa.pipeline.PipelineDefinitionEntityRepository;
import com.marketinghub.repository.jpa.pipeline.PipelineRepository;
import com.marketinghub.repository.jpa.pipeline.PipelineStageConfigRepository;
import com.marketinghub.repository.jpa.pipeline.PipelineStageDefinitionEntityRepository;
import com.marketinghub.repository.jpa.pipeline.PipelineStageRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

/**
 * Testa as regras de manutenção de pipelines e etapas configuráveis.
 */
@ExtendWith(MockitoExtension.class)
class PipelineServiceTest {
    @Mock
    private PipelineRepository pipelineRepository;

    @Mock
    private PipelineStageRepository stageRepository;

    @Mock
    private OpenAiModelRepository openAiModelRepository;

    private final PipelineDefinitionRegistry definitionRegistry = new PipelineDefinitionRegistry();

    private PipelineService service;
    private PipelineDefinitionSynchronizer synchronizer;

    /**
     * Inicializa o serviço com registry real para validar regras canônicas.
     */
    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new PipelineService(pipelineRepository, stageRepository, openAiModelRepository, definitionRegistry);
        synchronizer = new PipelineDefinitionSynchronizer(
                pipelineRepository, stageRepository, definitionRegistry, service);
    }

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
     * Garante que a consulta de modelos do Gera Landing usa as etapas ativas persistidas no banco.
     */
    @Test
    void shouldListGeraLandingStageModelsFromPersistedPipelineStages() {
        OpenAiModel model = OpenAiModel.builder()
                .id(77L)
                .name("GPT Vendas")
                .code("gpt-vendas")
                .priceInputBatch(new BigDecimal("0.50"))
                .priceInputCachedBatch(new BigDecimal("0.05"))
                .priceOutputBatch(new BigDecimal("2.00"))
                .build();
        OpenAiModel defaultTextModel = OpenAiModel.builder()
                .id(88L)
                .name("GPT-5.2")
                .code("gpt-5.2")
                .priceInputBatch(new BigDecimal("0.875"))
                .priceInputCachedBatch(new BigDecimal("0.0875"))
                .priceOutputBatch(new BigDecimal("7.00"))
                .build();
        when(openAiModelRepository.findByCode("gpt-5.2")).thenReturn(Optional.of(defaultTextModel));
        PipelineStage wireframe = stage(1L, 1, "LANDING_PAGE_WIREFRAME");
        wireframe.setOpenAiModel(model);
        PipelineStage copyWithoutModel = stage(2L, 2, "landing-page-copy");
        Pipeline pipeline = officialPipelineWith(wireframe, copyWithoutModel);
        when(pipelineRepository.findAll()).thenReturn(List.of(pipeline));

        List<GeraLandingStageModelDto> result = service.listGeraLandingStageModels();

        assertThat(result).hasSize(7);
        GeraLandingStageModelDto wireframeModel = result.stream()
                .filter(stageModel -> stageModel.getStageCode().equals("landing-page-wireframe"))
                .findFirst()
                .orElseThrow();
        assertThat(wireframeModel.getPipelineId()).isEqualTo(10L);
        assertThat(wireframeModel.getPipelineCode()).isEqualTo("experiment-pipeline");
        assertThat(wireframeModel.getPipelineStageId()).isEqualTo(1L);
        assertThat(wireframeModel.getPipelineStageCode()).isEqualTo("LANDING_PAGE_WIREFRAME");
        assertThat(wireframeModel.getOpenAiModelId()).isEqualTo(77L);
        assertThat(wireframeModel.getOpenAiModelName()).isEqualTo("GPT Vendas");
        assertThat(wireframeModel.getOpenAiModelCode()).isEqualTo("gpt-vendas");
        assertThat(wireframeModel.getPricingMode()).isEqualTo("flex");
        assertThat(wireframeModel.getGeneratedAssetType()).isEqualTo("texto");
        assertThat(wireframeModel.getPriceInputFlex()).isEqualByComparingTo("0.50");
        assertThat(wireframeModel.getPriceInputCachedFlex()).isEqualByComparingTo("0.05");
        assertThat(wireframeModel.getPriceOutputFlex()).isEqualByComparingTo("2.00");
        assertThat(wireframeModel.isDefaultModelApplied()).isFalse();
        GeraLandingStageModelDto copyModel = result.stream()
                .filter(stageModel -> stageModel.getStageCode().equals("landing-page-copy"))
                .findFirst()
                .orElseThrow();
        assertThat(copyModel.getOpenAiModelId()).isEqualTo(88L);
        assertThat(copyModel.getOpenAiModelCode()).isEqualTo("gpt-5.2");
        assertThat(copyModel.getPriceInputFlex()).isEqualByComparingTo("0.875");
        assertThat(copyModel.isDefaultModelApplied()).isTrue();
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
        assertThat(created.getExecutionModule()).isNull();
        assertThat(created.getRootPackage()).isNull();
        assertThat(created.isRequired()).isTrue();
        ArgumentCaptor<PipelineStage> captor = ArgumentCaptor.forClass(PipelineStage.class);
        verify(stageRepository).save(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("campaign-angle");
    }


    /**
     * Garante que pipeline oficial não pode ser excluído pela tela administrativa.
     */
    @Test
    void shouldNotDeleteOfficialPipeline() {
        Pipeline pipeline = Pipeline.builder()
                .id(10L)
                .name("Pipeline de Experimento")
                .code("experiment-pipeline")
                .module("EXPERIMENT")
                .stages(new ArrayList<>())
                .build();
        when(pipelineRepository.findById(10L)).thenReturn(Optional.of(pipeline));

        assertThatThrownBy(() -> service.delete(10L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Pipeline oficial não pode ser excluído");
    }

    /**
     * Garante que a atualização de pipeline oficial preserva código e módulo estruturais.
     */
    @Test
    void shouldNotChangeOfficialPipelineCodeOrModule() {
        Pipeline pipeline = Pipeline.builder()
                .id(10L)
                .name("Pipeline de Experimento")
                .code("experiment-pipeline")
                .module("EXPERIMENT")
                .stages(new ArrayList<>())
                .build();
        PipelineRequest request = new PipelineRequest();
        request.setName("Pipeline editado");
        request.setCode("outro-pipeline");
        request.setModule("EXPERIMENT");
        request.setActive(true);
        when(pipelineRepository.findById(10L)).thenReturn(Optional.of(pipeline));

        assertThatThrownBy(() -> service.update(10L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Código de pipeline oficial não pode ser alterado");
    }

    /**
     * Garante que criação de pipeline oficial não permite módulo divergente do contrato.
     */
    @Test
    void shouldNotCreateOfficialPipelineWithDifferentModule() {
        PipelineRequest request = new PipelineRequest();
        request.setName("Pipeline de Experimento");
        request.setCode("experiment-pipeline");
        request.setModule("MDS");
        request.setActive(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Pipeline oficial deve usar o módulo canônico EXPERIMENT");
    }

    /**
     * Garante que etapa obrigatória oficial não pode ser removida.
     */
    @Test
    void shouldNotDeleteRequiredOfficialStage() {
        Pipeline pipeline = officialPipelineWith(stage(1L, 1, "campaign-angle"));
        when(stageRepository.findById(1L)).thenReturn(Optional.of(pipeline.getStages().getFirst()));

        assertThatThrownBy(() -> service.deleteStage(10L, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Etapa obrigatória oficial não pode ser excluída");
    }

    /**
     * Garante que etapa oficial não aceita troca de código estrutural para outra etapa canônica.
     */
    @Test
    void shouldNotChangeOfficialStageStructuralCode() {
        Pipeline pipeline = officialPipelineWith(stage(1L, 1, "campaign-angle"));
        PipelineStageRequest request = stageRequest(1, "Ad Copy", "ad-copy");
        when(stageRepository.findById(1L)).thenReturn(Optional.of(pipeline.getStages().getFirst()));

        assertThatThrownBy(() -> service.updateStage(10L, 1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Código estrutural de etapa oficial não pode ser alterado");
    }

    /**
     * Garante que o diagnóstico acusa etapa obrigatória ausente no banco.
     */
    @Test
    void shouldDiagnoseMissingRequiredOfficialStage() {
        Pipeline pipeline = officialPipelineWith(stage(1L, 1, "campaign-angle"));
        when(pipelineRepository.findById(10L)).thenReturn(Optional.of(pipeline));

        PipelineDiagnosticsDto diagnostics = service.diagnostics(10L);

        assertThat(diagnostics.status()).isEqualTo("BLOQUEADO");
        assertThat(diagnostics.expectedStages()).isEqualTo(10);
        assertThat(diagnostics.configuredStages()).isEqualTo(1);
        assertThat(diagnostics.issues())
                .anySatisfy(issue -> {
                    assertThat(issue.severity()).isEqualTo("ERROR");
                    assertThat(issue.message()).isEqualTo("Etapa obrigatória está ausente no banco.");
                });
    }

    /**
     * Garante que o diagnóstico acusa etapa extra sem mapeamento canônico.
     */
    @Test
    void shouldDiagnoseExtraStageWithoutCanonicalMapping() {
        Pipeline pipeline = officialPipelineWith(stage(1L, 1, "unknown-stage"));
        when(pipelineRepository.findById(10L)).thenReturn(Optional.of(pipeline));

        PipelineDiagnosticsDto diagnostics = service.diagnostics(10L);

        assertThat(diagnostics.status()).isEqualTo("BLOQUEADO");
        assertThat(diagnostics.issues())
                .anySatisfy(issue -> assertThat(issue.message())
                        .isEqualTo("Etapa extra não possui mapeamento canônico conhecido."));
    }

    /**
     * Garante que cada etapa oficial possui código operacional e alias canônico reconhecido.
     */
    @Test
    void shouldExposeOfficialStageAliasesAndCanonicalCodes() {
        assertThat(definitionRegistry.officialPipelines()).hasSize(2);
        assertThat(definitionRegistry.findByPipelineCode("experiment-pipeline")).hasValueSatisfying(pipeline -> {
            assertThat(pipeline.code()).isEqualTo("experiment-pipeline");
            assertThat(pipeline.stages()).hasSize(10);
            assertThat(pipeline.stages())
                    .allSatisfy(stage -> {
                        assertThat(stage.canonicalCode()).isNotBlank();
                        assertThat(stage.operationalCode()).isNotBlank();
                        assertThat(stage.executionModule()).isNull();
                        assertThat(stage.rootPackage()).isNotBlank();
                        assertThat(definitionRegistry.findStage(pipeline, stage.canonicalCode())).contains(stage);
                        assertThat(definitionRegistry.findStage(pipeline, stage.operationalCode())).contains(stage);
                    });
            assertThat(pipeline.stages())
                    .filteredOn(stage -> stage.canonicalCode().startsWith("LANDING_PAGE_"))
                    .allSatisfy(stage -> assertThat(stage.rootPackage()).startsWith("com.marketinghub.geralanding."));
            assertThat(pipeline.stages())
                    .filteredOn(stage -> stage.canonicalCode().equals("LANDING_PAGE_IMAGE_GENERATION"))
                    .singleElement()
                    .satisfies(stage -> assertThat(stage.modulePackage())
                            .isEqualTo("com.marketinghub.worker.openai.core.imagegeneration"));
            assertThat(pipeline.stages())
                    .filteredOn(stage -> stage.canonicalCode().equals("LANDING_PAGE_DELIVERABLES"))
                    .singleElement()
                    .satisfies(stage -> assertThat(stage.modulePackage())
                            .isEqualTo("com.marketinghub.worker.geralanding.deliverables"));
        });
    }

    /**
     * Garante que o serviço rejeita modelo OpenAI inexistente na etapa.
     */
    @Test
    void shouldRejectMissingOpenAiModel() {
        Pipeline pipeline = Pipeline.builder().id(10L).name("Pipeline").code("custom").module("EXPERIMENT").stages(new ArrayList<>()).build();
        PipelineStageRequest request = stageRequest(1, "Campaign Angle", "campaign-angle");
        request.setOpenAiModelId(99L);
        when(pipelineRepository.findById(10L)).thenReturn(Optional.of(pipeline));
        when(openAiModelRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createStage(10L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Modelo OpenAI não encontrado");
    }


    /**
     * Garante que a sincronização cria etapas oficiais ausentes de forma idempotente.
     */
    @Test
    void shouldSynchronizeMissingOfficialStage() {
        Pipeline pipeline = officialPipelineWith(stage(1L, 1, "campaign-angle"));
        when(pipelineRepository.findById(10L)).thenReturn(Optional.of(pipeline));
        when(stageRepository.save(any(PipelineStage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pipelineRepository.save(any(Pipeline.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PipelineSyncResultDto result = synchronizer.sync(10L);

        assertThat(result.appliedActions()).anyMatch(action -> action.contains("Etapa oficial ausente criada"));
        assertThat(pipeline.getStages()).hasSize(10);
        assertThat(pipeline.getStages()).extracting(PipelineStage::getCode)
                .contains(
                        "landing-page-image-generation",
                        "landing-page-quality-review",
                        "landing-page-deliverables");
        assertThat(pipeline.getStages())
                .filteredOn(stage -> stage.getCode().startsWith("landing-page-"))
                .allSatisfy(stage -> assertThat(stage.getRootPackage()).startsWith("com.marketinghub.geralanding."));
    }

    /**
     * Garante que a sincronização preserva modelo OpenAI e descrição operacional já configurados.
     */
    @Test
    void shouldPreserveConfiguredOpenAiModelDuringSynchronization() {
        OpenAiModel model = OpenAiModel.builder().id(99L).name("GPT 5.2").code("gpt-5.2").build();
        PipelineStage configured = stage(1L, 2, "campaign-angle");
        configured.setName("Nome antigo");
        configured.setDescription("Descrição operacional do usuário");
        configured.setOpenAiModel(model);
        Pipeline pipeline = officialPipelineWith(configured);
        when(pipelineRepository.findById(10L)).thenReturn(Optional.of(pipeline));
        when(stageRepository.save(any(PipelineStage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pipelineRepository.save(any(Pipeline.class))).thenAnswer(invocation -> invocation.getArgument(0));

        synchronizer.sync(10L);

        assertThat(configured.getPosition()).isEqualTo(1);
        assertThat(configured.getName()).isEqualTo("Campaign Angle");
        assertThat(configured.getDescription()).isEqualTo("Descrição operacional do usuário");
        assertThat(configured.getOpenAiModel()).isEqualTo(model);
        assertThat(configured.getRootPackage()).isEqualTo("com.marketinghub.experiment.pipeline");
    }

    /**
     * Garante que a sincronização bloqueia divergência destrutiva antes de remover etapa extra.
     */
    @Test
    void shouldBlockDestructiveSynchronization() {
        Pipeline pipeline = officialPipelineWith(stage(1L, 1, "unknown-stage"));
        when(pipelineRepository.findById(10L)).thenReturn(Optional.of(pipeline));

        PipelineSyncResultDto result = synchronizer.sync(10L);

        assertThat(result.status()).isEqualTo("BLOQUEADO");
        assertThat(result.synchronizedSafely()).isFalse();
        assertThat(result.issues()).anySatisfy(issue -> {
            assertThat(issue.message()).isEqualTo("Etapa extra com possível histórico impede sincronização destrutiva.");
            assertThat(issue.rootCause()).contains("não existe no contrato canônico");
            assertThat(issue.recommendedAction()).contains("Analisar histórico");
        });
    }

    /**
     * Garante que a recriação explícita remove etapas legadas e recria somente etapas oficiais preservando configuração compatível.
     */
    @Test
    void shouldRebuildOfficialStagesFromScreenAction() {
        OpenAiModel model = OpenAiModel.builder().id(99L).name("GPT 5.2").code("gpt-5.2").build();
        PipelineStage legacyWireframe = stage(3L, 3, "landing-wireframe");
        legacyWireframe.setDescription("Descrição operacional preservada");
        legacyWireframe.setOpenAiModel(model);
        Pipeline pipeline = officialPipelineWith(
                stage(1L, 1, "campaign-angle"),
                stage(2L, 2, "ad-copy"),
                legacyWireframe,
                stage(4L, 4, "landing-copy"),
                stage(5L, 5, "image-planning"),
                stage(6L, 6, "preset-design"),
                stage(7L, 7, "geralanding-html"),
                stage(8L, 8, "landing-html"),
                stage(9L, 9, "landing-page-deliverables"));
        when(pipelineRepository.findById(10L)).thenReturn(Optional.of(pipeline));
        when(stageRepository.save(any(PipelineStage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pipelineRepository.save(any(Pipeline.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PipelineSyncResultDto result = synchronizer.rebuildOfficialStages(10L);

        assertThat(result.status()).isEqualTo("OK");
        assertThat(result.synchronizedSafely()).isTrue();
        assertThat(pipeline.getStages()).hasSize(10);
        assertThat(pipeline.getStages()).extracting(PipelineStage::getCode)
                .containsExactly(
                        "campaign-angle",
                        "ad-copy",
                        "ad-image-briefing",
                        "landing-page-wireframe",
                        "landing-page-copy",
                        "landing-page-image-planning",
                        "landing-page-image-generation",
                        "landing-page-design-preset",
                        "landing-page-quality-review",
                        "landing-page-deliverables");
        assertThat(pipeline.getStages())
                .filteredOn(stage -> stage.getCode().equals("landing-page-wireframe"))
                .singleElement()
                .satisfies(stage -> {
                    assertThat(stage.getDescription()).isEqualTo("Descrição operacional preservada");
                    assertThat(stage.getOpenAiModel()).isEqualTo(model);
                    assertThat(stage.getRootPackage()).isEqualTo("com.marketinghub.geralanding.wireframe");
                });
        assertThat(result.appliedActions()).anyMatch(action -> action.contains("Etapas operacionais antigas removidas"));
        verify(stageRepository).flush();
        verify(stageRepository, never()).deleteAll(any());
    }

    /**
     * Garante que a sincronização cria pipeline oficial ausente pelo código canônico.
     */
    @Test
    void shouldCreateMissingOfficialPipelineByCode() {
        when(pipelineRepository.findByCode("experiment-pipeline")).thenReturn(Optional.empty());
        when(pipelineRepository.save(any(Pipeline.class))).thenAnswer(invocation -> {
            Pipeline saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(10L);
            }
            return saved;
        });
        when(pipelineRepository.findById(10L)).thenAnswer(invocation -> {
            Pipeline pipeline = Pipeline.builder()
                    .id(10L)
                    .name("Pipeline de Experimento")
                    .code("experiment-pipeline")
                    .module("EXPERIMENT")
                    .stages(new ArrayList<>())
                    .build();
            return Optional.of(pipeline);
        });
        when(stageRepository.save(any(PipelineStage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PipelineSyncResultDto result = synchronizer.syncOfficialByCode("experiment-pipeline");

        assertThat(result.appliedActions()).hasSize(10);
        assertThat(result.canonicalPipelineCode()).isEqualTo("experiment-pipeline");
    }

    /**
     * Garante que o registry expõe versão canônica e política explícita de campos estruturais.
     */
    @Test
    void shouldExposeCanonicalVersionAndFieldPolicies() {
        assertThat(definitionRegistry.findByPipelineCode("experiment-pipeline")).hasValueSatisfying(pipeline -> {
            assertThat(pipeline.canonicalVersion()).isEqualTo("procedimento-experimento-canon.v1");
            assertThat(pipeline.pipelineFieldPolicy().codeStructural()).isTrue();
            assertThat(pipeline.stageFieldPolicy().openAiModelOperational()).isTrue();
        });
        assertThat(definitionRegistry.findByPipelineCode("oprm-nicho-cnae-pipeline")).hasValueSatisfying(pipeline -> {
            assertThat(pipeline.canonicalVersion()).isEqualTo("oprm-nichocnae-canon.v1");
            assertThat(pipeline.pipelineFieldPolicy().codeStructural()).isTrue();
            assertThat(pipeline.stageFieldPolicy().openAiModelOperational()).isTrue();
        });
    }


    /**
     * Garante que o pipeline oficial OPRM NichoCNAE reflete as etapas implementadas no backend e no coletor.
     */
    @Test
    void shouldExposeOfficialOprmNichoCnaePipelineFromImplementedStages() {
        assertThat(definitionRegistry.findByPipelineCode("oprm-nicho-cnae-pipeline")).hasValueSatisfying(pipeline -> {
            assertThat(pipeline.module()).isEqualTo("OPRM");
            assertThat(pipeline.name()).isEqualTo("Pipeline Nicho CNAE");
            assertThat(pipeline.stages()).hasSize(9);
            assertThat(pipeline.stages()).extracting(stage -> stage.operationalCode())
                    .containsExactly(
                            "routine-research-orchestrator",
                            "routine-research-cycle",
                            "niche-research-seed-builder",
                            "source-searcher",
                            "source-fetcher",
                            "signal-extractor",
                            "routine-synthesizer",
                            "routine-quality-gate",
                            "enriched-niche-materializer");
            assertThat(pipeline.stages())
                    .allSatisfy(stage -> {
                        assertThat(stage.executionModule()).isEqualTo("oprm-coletor-mei");
                        assertThat(stage.rootPackage()).startsWith("com.marketinghub.oprm.nichocnae.");
                        assertThat(definitionRegistry.findStage(pipeline, stage.canonicalCode())).contains(stage);
                        assertThat(definitionRegistry.findStage(pipeline, stage.operationalCode())).contains(stage);
                    });
            assertThat(pipeline.stages())
                    .filteredOn(stage -> stage.requiresOpenAiModel())
                    .extracting(stage -> stage.operationalCode())
                    .containsExactly("niche-research-seed-builder");
            assertThat(definitionRegistry.findStage(pipeline, "oprmEnrichedNicheMaterializer"))
                    .hasValueSatisfying(stage -> assertThat(stage.operationalCode()).isEqualTo("enriched-niche-materializer"));
        });
    }

    /**
     * Garante que o flag OpenAI do pipeline NichoCNAE vem do código real do coletor OPRM.
     */
    @Test
    void shouldMatchOprmNichoCnaeOpenAiFlagsWithCollectorCode() throws IOException {
        PipelineDefinitionRegistry.PipelineDefinition pipeline = definitionRegistry
                .findByPipelineCode("oprm-nicho-cnae-pipeline")
                .orElseThrow();

        for (PipelineDefinitionRegistry.PipelineStageDefinition stage : pipeline.stages()) {
            boolean collectorCodeUsesOpenAi = collectorStageSourceUsesOpenAi(stage);

            assertThat(stage.requiresOpenAiModel())
                    .as("Etapa %s deve refletir o uso real de OpenAI no código do coletor", stage.operationalCode())
                    .isEqualTo(collectorCodeUsesOpenAi);
        }
    }

    /**
     * Garante que a sincronização oficial cria o pipeline OPRM NichoCNAE ausente pelo código canônico.
     */
    @Test
    void shouldCreateMissingOfficialOprmNichoCnaePipelineByCode() {
        when(pipelineRepository.findByCode("oprm-nicho-cnae-pipeline")).thenReturn(Optional.empty());
        when(pipelineRepository.save(any(Pipeline.class))).thenAnswer(invocation -> {
            Pipeline saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(20L);
            }
            return saved;
        });
        when(pipelineRepository.findById(20L)).thenAnswer(invocation -> {
            Pipeline pipeline = Pipeline.builder()
                    .id(20L)
                    .name("Pipeline Nicho CNAE")
                    .code("oprm-nicho-cnae-pipeline")
                    .module("OPRM")
                    .stages(new ArrayList<>())
                    .build();
            return Optional.of(pipeline);
        });
        when(stageRepository.save(any(PipelineStage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PipelineSyncResultDto result = synchronizer.syncOfficialByCode("oprm-nicho-cnae-pipeline");

        assertThat(result.appliedActions()).hasSize(9);
        assertThat(result.canonicalPipelineCode()).isEqualTo("oprm-nicho-cnae-pipeline");
    }

    /**
     * Garante que a fase 3 persiste definição canônica separada da configuração operacional herdada.
     */
    @Test
    void shouldPersistDefinitionSeparatedFromOperationalConfig() {
        PipelineDefinitionEntityRepository definitionRepository = mock(PipelineDefinitionEntityRepository.class);
        PipelineStageDefinitionEntityRepository stageDefinitionRepository = mock(PipelineStageDefinitionEntityRepository.class);
        PipelineStageConfigRepository configRepository = mock(PipelineStageConfigRepository.class);
        PipelinePersistentContractSynchronizer persistentSynchronizer = new PipelinePersistentContractSynchronizer(
                definitionRepository, stageDefinitionRepository, configRepository);
        OpenAiModel model = OpenAiModel.builder().id(99L).name("GPT 5.2").code("gpt-5.2").build();
        PipelineStage configured = stage(1L, 1, "campaign-angle");
        configured.setDescription("Descrição operacional preservada");
        configured.setOpenAiModel(model);
        Pipeline pipeline = officialPipelineWith(configured);
        PipelineDefinitionEntity persistedDefinition = PipelineDefinitionEntity.builder()
                .id(7L)
                .code("experiment-pipeline")
                .module("EXPERIMENT")
                .name("Pipeline de Experimento")
                .canonicalVersion("procedimento-experimento-canon.v1")
                .active(true)
                .build();
        PipelineStageDefinitionEntity persistedStage = PipelineStageDefinitionEntity.builder()
                .id(8L)
                .pipelineDefinition(persistedDefinition)
                .canonicalCode("CAMPAIGN_ANGLE")
                .displayName("Campaign Angle")
                .position(1)
                .required(true)
                .implementedStageEnum("CAMPAIGN_ANGLE")
                .requiresOpenAiModel(true)
                .configurable(true)
                .build();
        when(definitionRepository.findByCodeAndCanonicalVersion(
                        "experiment-pipeline", "procedimento-experimento-canon.v1"))
                .thenReturn(Optional.of(persistedDefinition));
        when(stageDefinitionRepository.findByPipelineDefinitionIdAndCanonicalCode(7L, "CAMPAIGN_ANGLE"))
                .thenReturn(Optional.of(persistedStage));
        when(stageDefinitionRepository.save(any(PipelineStageDefinitionEntity.class))).thenAnswer(invocation -> {
            PipelineStageDefinitionEntity saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(90L + saved.getPosition());
            }
            return saved;
        });
        when(configRepository.findByPipelineStageDefinitionId(8L)).thenReturn(Optional.empty());
        when(configRepository.save(any(PipelineStageConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<String> actions = persistentSynchronizer.sync(
                definitionRegistry.findByPipelineCode("experiment-pipeline").orElseThrow(), pipeline);

        assertThat(actions)
                .containsExactly(
                        "Definição persistente e configurações operacionais sincronizadas sem sobrescrever campos editáveis.");
        ArgumentCaptor<PipelineStageConfig> configCaptor = ArgumentCaptor.forClass(PipelineStageConfig.class);
        verify(configRepository, atLeastOnce()).save(configCaptor.capture());
        PipelineStageConfig migratedConfig = configCaptor.getAllValues().stream()
                .filter(config -> config.getPipelineStageDefinition().getId().equals(8L))
                .findFirst()
                .orElseThrow();
        assertThat(migratedConfig.getPipelineStageDefinition()).isEqualTo(persistedStage);
        assertThat(migratedConfig.getDescriptionOverride()).isEqualTo("Descrição operacional preservada");
        assertThat(migratedConfig.getOpenAiModel()).isEqualTo(model);
        assertThat(migratedConfig.isActive()).isTrue();
    }


    /**
     * Lê os arquivos Java da etapa no coletor OPRM e identifica uso direto de OpenAI no código-fonte.
     */
    private boolean collectorStageSourceUsesOpenAi(PipelineDefinitionRegistry.PipelineStageDefinition stage)
            throws IOException {
        Path sourceDirectory = repositoryRoot()
                .resolve("oprm-coletor-mei/src/main/java")
                .resolve(stage.modulePackage().replace('.', '/'));
        assertThat(sourceDirectory)
                .as("Pacote da etapa %s deve existir no coletor OPRM", stage.operationalCode())
                .isDirectory();

        try (Stream<Path> javaFiles = Files.walk(sourceDirectory)) {
            return javaFiles
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .map(this::readSourceFile)
                    .anyMatch(source -> source.contains("OpenAi")
                            || source.contains("openai")
                            || source.contains("api.openai.com"));
        }
    }

    /**
     * Lê um arquivo de código-fonte Java e propaga falhas de leitura de forma compatível com streams.
     */
    private String readSourceFile(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao ler código-fonte para validar uso de OpenAI: " + path, ex);
        }
    }

    /**
     * Localiza a raiz do repositório a partir do diretório de execução do Maven.
     */
    private Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("oprm-coletor-mei"))
                    && Files.isDirectory(current.resolve("backend/ads-service"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Raiz do repositório não encontrada para validar o código do coletor OPRM.");
    }

    /**
     * Cria uma etapa sintética para validar ordenação no serviço.
     */
    private PipelineStage stage(Long id, Integer position) {
        return stage(id, position, "stage-" + position);
    }

    /**
     * Cria uma etapa sintética com código explícito para validar contrato oficial.
     */
    private PipelineStage stage(Long id, Integer position, String code) {
        return PipelineStage.builder()
                .id(id)
                .position(position)
                .name("Etapa " + position)
                .code(code)
                .required(true)
                .active(true)
                .build();
    }

    /**
     * Cria payload sintético de etapa para testes de validação.
     */
    private PipelineStageRequest stageRequest(Integer position, String name, String code) {
        PipelineStageRequest request = new PipelineStageRequest();
        request.setPosition(position);
        request.setName(name);
        request.setCode(code);
        request.setRequired(true);
        request.setActive(true);
        return request;
    }

    /**
     * Cria pipeline oficial sintético com as etapas informadas já vinculadas.
     */
    private Pipeline officialPipelineWith(PipelineStage... stages) {
        Pipeline pipeline = Pipeline.builder()
                .id(10L)
                .name("Pipeline de Experimento")
                .code("experiment-pipeline")
                .module("EXPERIMENT")
                .stages(new ArrayList<>(List.of(stages)))
                .build();
        pipeline.getStages().forEach(stage -> stage.setPipeline(pipeline));
        return pipeline;
    }

}
