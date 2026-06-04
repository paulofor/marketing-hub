package com.marketinghub.pipeline.service;

import com.marketinghub.pipeline.Pipeline;
import com.marketinghub.pipeline.PipelineStage;
import com.marketinghub.pipeline.definition.PipelineDefinitionRegistry;
import com.marketinghub.pipeline.definition.PipelineDefinitionRegistry.PipelineDefinition;
import com.marketinghub.pipeline.definition.PipelineDefinitionRegistry.PipelineStageDefinition;
import com.marketinghub.pipeline.dto.PipelineDiagnosticsDto;
import com.marketinghub.pipeline.dto.PipelineDiagnosticsIssueDto;
import com.marketinghub.pipeline.dto.PipelineSyncResultDto;
import com.marketinghub.repository.jpa.pipeline.PipelineRepository;
import com.marketinghub.repository.jpa.pipeline.PipelineStageRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sincronizador idempotente que repara divergências simples entre contrato oficial e banco operacional.
 */
@Component
public class PipelineDefinitionSynchronizer {
    private final PipelineRepository pipelineRepository;
    private final PipelineStageRepository stageRepository;
    private final PipelineDefinitionRegistry definitionRegistry;
    private final PipelineService pipelineService;

    /**
     * Inicializa o sincronizador com persistência centralizada, registry canônico e diagnóstico do serviço.
     */
    public PipelineDefinitionSynchronizer(
            PipelineRepository pipelineRepository,
            PipelineStageRepository stageRepository,
            PipelineDefinitionRegistry definitionRegistry,
            PipelineService pipelineService) {
        this.pipelineRepository = pipelineRepository;
        this.stageRepository = stageRepository;
        this.definitionRegistry = definitionRegistry;
        this.pipelineService = pipelineService;
    }

    /**
     * Sincroniza com segurança um pipeline já existente, preservando campos operacionais configuráveis.
     */
    @Transactional
    public PipelineSyncResultDto sync(Long pipelineId) {
        Pipeline pipeline = pipelineService.get(pipelineId);
        PipelineDefinition definition = definitionRegistry.findByPipelineCode(pipeline.getCode()).orElse(null);
        if (definition == null) {
            PipelineDiagnosticsDto diagnostics = pipelineService.diagnostics(pipelineId);
            return blockedResult(diagnostics, List.of());
        }

        List<String> appliedActions = new ArrayList<>();
        List<PipelineDiagnosticsIssueDto> blockingIssues = destructiveIssues(pipeline, definition);
        if (!blockingIssues.isEmpty()) {
            PipelineDiagnosticsDto diagnostics = pipelineService.diagnostics(pipelineId);
            return PipelineSyncResultDto.builder()
                    .status("BLOQUEADO")
                    .synchronizedSafely(false)
                    .pipelineId(diagnostics.pipelineId())
                    .pipelineCode(diagnostics.pipelineCode())
                    .canonicalPipelineCode(diagnostics.canonicalPipelineCode())
                    .expectedStages(diagnostics.expectedStages())
                    .configuredStages(diagnostics.configuredStages())
                    .appliedActions(appliedActions)
                    .issues(blockingIssues)
                    .build();
        }

        synchronizePipelineFields(pipeline, definition, appliedActions);
        for (PipelineStageDefinition stageDefinition : definition.stages()) {
            synchronizeStage(pipeline, definition, stageDefinition, appliedActions);
        }
        pipelineRepository.save(pipeline);

        PipelineDiagnosticsDto diagnostics = pipelineService.diagnostics(pipelineId);
        return PipelineSyncResultDto.builder()
                .status(diagnostics.status())
                .synchronizedSafely("OK".equals(diagnostics.status()))
                .pipelineId(diagnostics.pipelineId())
                .pipelineCode(diagnostics.pipelineCode())
                .canonicalPipelineCode(diagnostics.canonicalPipelineCode())
                .expectedStages(diagnostics.expectedStages())
                .configuredStages(diagnostics.configuredStages())
                .appliedActions(appliedActions)
                .issues(diagnostics.issues())
                .build();
    }

    /**
     * Cria o pipeline oficial ausente pelo código canônico sem sobrescrever configuração existente.
     */
    @Transactional
    public PipelineSyncResultDto syncOfficialByCode(String code) {
        PipelineDefinition definition = definitionRegistry.findByPipelineCode(code).orElse(null);
        if (definition == null) {
            return PipelineSyncResultDto.builder()
                    .status("BLOQUEADO")
                    .synchronizedSafely(false)
                    .pipelineCode(code)
                    .canonicalPipelineCode(null)
                    .expectedStages(0)
                    .configuredStages(0)
                    .appliedActions(List.of())
                    .issues(List.of(issue("ERROR", null, null,
                            "Pipeline informado não possui definição oficial no backend.",
                            "A tela solicitou sincronização de código fora do contrato canônico.",
                            "Selecionar um pipeline oficial conhecido em /api/pipelines/metadata.")))
                    .build();
        }
        Pipeline pipeline = findExistingOfficialPipeline(code, definition);
        return sync(pipeline.getId());
    }

    /**
     * Localiza pipeline oficial já salvo pelo código canônico ou alias recebido antes de criar novo registro.
     */
    private Pipeline findExistingOfficialPipeline(String requestedCode, PipelineDefinition definition) {
        return pipelineRepository.findByCode(definition.code())
                .or(() -> pipelineRepository.findByCode(requestedCode))
                .orElseGet(() -> createPipeline(definition));
    }

    /**
     * Corrige campos estruturais do pipeline quando a política oficial permite reparo automático.
     */
    private void synchronizePipelineFields(Pipeline pipeline, PipelineDefinition definition, List<String> appliedActions) {
        if (definition.pipelineFieldPolicy().nameStructural() && !definition.name().equals(pipeline.getName())) {
            pipeline.setName(definition.name());
            appliedActions.add("Nome do pipeline corrigido para o contrato canônico.");
        }
    }

    /**
     * Cria ou corrige uma etapa oficial sem alterar modelo OpenAI, descrição operacional ou ativo configurado.
     */
    private void synchronizeStage(
            Pipeline pipeline,
            PipelineDefinition definition,
            PipelineStageDefinition stageDefinition,
            List<String> appliedActions) {
        PipelineStage stage = findConfiguredStage(pipeline, definition, stageDefinition);
        if (stage == null) {
            stage = createStage(pipeline, stageDefinition);
            appliedActions.add("Etapa oficial ausente criada: " + stageDefinition.operationalCode());
            return;
        }
        if (definition.stageFieldPolicy().nameStructural() && !stageDefinition.name().equals(stage.getName())) {
            stage.setName(stageDefinition.name());
            appliedActions.add("Nome da etapa corrigido: " + stageDefinition.operationalCode());
        }
        if (definition.stageFieldPolicy().positionStructural() && !stage.getPosition().equals(stageDefinition.position())) {
            stage.setPosition(stageDefinition.position());
            appliedActions.add("Posição canônica da etapa corrigida: " + stageDefinition.operationalCode());
        }
        if (definition.stageFieldPolicy().requiredStructural() && stage.isRequired() != stageDefinition.required()) {
            stage.setRequired(stageDefinition.required());
            appliedActions.add("Obrigatoriedade estrutural da etapa corrigida: " + stageDefinition.operationalCode());
        }
        stageRepository.save(stage);
    }

    /**
     * Localiza no banco a etapa correspondente ao código operacional ou aliases oficiais.
     */
    private PipelineStage findConfiguredStage(
            Pipeline pipeline, PipelineDefinition definition, PipelineStageDefinition stageDefinition) {
        return pipeline.getStages().stream()
                .filter(stage -> definitionRegistry.findStage(definition, stage.getCode())
                        .map(stageDefinition::equals)
                        .orElse(false))
                .findFirst()
                .orElse(null);
    }

    /**
     * Cria uma etapa oficial mínima, deixando campos operacionais sem configuração específica do usuário.
     */
    private PipelineStage createStage(Pipeline pipeline, PipelineStageDefinition stageDefinition) {
        PipelineStage stage = PipelineStage.builder()
                .pipeline(pipeline)
                .position(stageDefinition.position())
                .name(stageDefinition.name())
                .code(stageDefinition.operationalCode())
                .required(stageDefinition.required())
                .active(true)
                .build();
        pipeline.getStages().add(stage);
        return stageRepository.save(stage);
    }

    /**
     * Cria pipeline oficial ausente com todos os campos estruturais canônicos básicos.
     */
    private Pipeline createPipeline(PipelineDefinition definition) {
        Pipeline pipeline = Pipeline.builder()
                .name(definition.name())
                .code(definition.code())
                .module(definition.module())
                .description("Pipeline oficial sincronizado a partir de " + definition.canonicalVersion())
                .active(true)
                .stages(new ArrayList<>())
                .build();
        return pipelineRepository.save(pipeline);
    }

    /**
     * Detecta divergências que poderiam causar perda de histórico ou sobrescrita operacional perigosa.
     */
    private List<PipelineDiagnosticsIssueDto> destructiveIssues(Pipeline pipeline, PipelineDefinition definition) {
        List<PipelineDiagnosticsIssueDto> issues = new ArrayList<>();
        Set<String> codes = new HashSet<>();
        Set<Integer> positions = new HashSet<>();
        for (PipelineStage stage : pipeline.getStages()) {
            PipelineStageDefinition stageDefinition = definitionRegistry.findStage(definition, stage.getCode()).orElse(null);
            if (!codes.add(definitionRegistry.normalize(stage.getCode()))) {
                issues.add(issue("ERROR", stage.getCode(), stageDefinition == null ? null : stageDefinition.canonicalCode(),
                        "Código operacional duplicado impede sincronização segura.",
                        "Há duas etapas gravadas com o mesmo código dentro do pipeline oficial.",
                        "Resolver a duplicidade manualmente antes de sincronizar."));
            }
            if (!positions.add(stage.getPosition())) {
                issues.add(issue("ERROR", stage.getCode(), stageDefinition == null ? null : stageDefinition.canonicalCode(),
                        "Posição operacional duplicada impede sincronização segura.",
                        "Há duas etapas gravadas com a mesma posição dentro do pipeline oficial.",
                        "Resolver a duplicidade manualmente antes de sincronizar."));
            }
            if (stageDefinition == null) {
                issues.add(issue("ERROR", stage.getCode(), null,
                        "Etapa extra com possível histórico impede sincronização destrutiva.",
                        "Banco contém etapa que não existe no contrato canônico atual.",
                        "Analisar histórico da etapa e decidir manualmente se ela deve ser removida ou canonizada."));
            }
        }
        return issues;
    }

    /**
     * Constrói resultado bloqueado a partir do diagnóstico já calculado pelo serviço principal.
     */
    private PipelineSyncResultDto blockedResult(PipelineDiagnosticsDto diagnostics, List<String> appliedActions) {
        return PipelineSyncResultDto.builder()
                .status("BLOQUEADO")
                .synchronizedSafely(false)
                .pipelineId(diagnostics.pipelineId())
                .pipelineCode(diagnostics.pipelineCode())
                .canonicalPipelineCode(diagnostics.canonicalPipelineCode())
                .expectedStages(diagnostics.expectedStages())
                .configuredStages(diagnostics.configuredStages())
                .appliedActions(appliedActions)
                .issues(diagnostics.issues())
                .build();
    }

    /**
     * Cria uma divergência padronizada com causa-raiz e ação recomendada.
     */
    private PipelineDiagnosticsIssueDto issue(
            String severity,
            String stageCode,
            String canonicalCode,
            String message,
            String rootCause,
            String recommendedAction) {
        return PipelineDiagnosticsIssueDto.builder()
                .severity(severity)
                .stageCode(stageCode)
                .canonicalCode(canonicalCode)
                .message(message)
                .rootCause(rootCause)
                .recommendedAction(recommendedAction)
                .build();
    }
}
