package com.marketinghub.pipeline.service;

import com.marketinghub.pipeline.Pipeline;
import com.marketinghub.pipeline.PipelineDefinitionEntity;
import com.marketinghub.pipeline.PipelineStage;
import com.marketinghub.pipeline.PipelineStageConfig;
import com.marketinghub.pipeline.PipelineStageDefinitionEntity;
import com.marketinghub.pipeline.definition.PipelineDefinitionRegistry.PipelineDefinition;
import com.marketinghub.pipeline.definition.PipelineDefinitionRegistry.PipelineStageDefinition;
import com.marketinghub.repository.jpa.pipeline.PipelineDefinitionEntityRepository;
import com.marketinghub.repository.jpa.pipeline.PipelineStageConfigRepository;
import com.marketinghub.repository.jpa.pipeline.PipelineStageDefinitionEntityRepository;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Sincroniza a separação persistente entre definição canônica de pipeline e configuração operacional editável.
 */
@Component
public class PipelinePersistentContractSynchronizer {
    private final PipelineDefinitionEntityRepository definitionRepository;
    private final PipelineStageDefinitionEntityRepository stageDefinitionRepository;
    private final PipelineStageConfigRepository configRepository;

    /**
     * Inicializa o sincronizador persistente com repositórios centralizados de definição e configuração.
     */
    public PipelinePersistentContractSynchronizer(
            PipelineDefinitionEntityRepository definitionRepository,
            PipelineStageDefinitionEntityRepository stageDefinitionRepository,
            PipelineStageConfigRepository configRepository) {
        this.definitionRepository = definitionRepository;
        this.stageDefinitionRepository = stageDefinitionRepository;
        this.configRepository = configRepository;
    }

    /**
     * Sincroniza uma definição oficial persistente e cria configurações operacionais sem sobrescrever escolhas do usuário.
     */
    public List<String> sync(PipelineDefinition definition, Pipeline pipeline) {
        PipelineDefinitionEntity persistedDefinition = definitionRepository
                .findByCodeAndCanonicalVersion(definition.code(), definition.canonicalVersion())
                .orElseGet(() -> createPipelineDefinition(definition));
        boolean changed = applyPipelineDefinitionFields(persistedDefinition, definition);
        if (changed) {
            definitionRepository.save(persistedDefinition);
        }
        for (PipelineStageDefinition stageDefinition : definition.stages()) {
            syncStageDefinition(persistedDefinition, stageDefinition, pipeline);
        }
        return List.of("Definição persistente e configurações operacionais sincronizadas sem sobrescrever campos editáveis.");
    }

    /**
     * Cria a definição persistente mínima para o pipeline oficial informado pelo registry.
     */
    private PipelineDefinitionEntity createPipelineDefinition(PipelineDefinition definition) {
        PipelineDefinitionEntity entity = PipelineDefinitionEntity.builder()
                .module(definition.module())
                .code(definition.code())
                .name(definition.name())
                .canonicalVersion(definition.canonicalVersion())
                .active(true)
                .build();
        return definitionRepository.save(entity);
    }

    /**
     * Atualiza somente campos estruturais da definição persistente quando o registry oficial mudar.
     */
    private boolean applyPipelineDefinitionFields(
            PipelineDefinitionEntity entity, PipelineDefinition definition) {
        boolean changed = false;
        if (!definition.module().equals(entity.getModule())) {
            entity.setModule(definition.module());
            changed = true;
        }
        if (!definition.name().equals(entity.getName())) {
            entity.setName(definition.name());
            changed = true;
        }
        if (!entity.isActive()) {
            entity.setActive(true);
            changed = true;
        }
        return changed;
    }

    /**
     * Sincroniza definição persistente de etapa e garante uma configuração operacional editável para ela.
     */
    private void syncStageDefinition(
            PipelineDefinitionEntity pipelineDefinition,
            PipelineStageDefinition stageDefinition,
            Pipeline pipeline) {
        PipelineStageDefinitionEntity entity = stageDefinitionRepository
                .findByPipelineDefinitionIdAndCanonicalCode(pipelineDefinition.getId(), stageDefinition.canonicalCode())
                .orElseGet(() -> createStageDefinition(pipelineDefinition, stageDefinition));
        boolean changed = applyStageDefinitionFields(entity, stageDefinition);
        if (changed) {
            stageDefinitionRepository.save(entity);
        }
        PipelineStage configuredStage = findConfiguredStage(pipeline, stageDefinition);
        syncStageConfig(entity, configuredStage);
    }

    /**
     * Cria a definição persistente mínima para uma etapa oficial do registry.
     */
    private PipelineStageDefinitionEntity createStageDefinition(
            PipelineDefinitionEntity pipelineDefinition, PipelineStageDefinition stageDefinition) {
        PipelineStageDefinitionEntity entity = PipelineStageDefinitionEntity.builder()
                .pipelineDefinition(pipelineDefinition)
                .canonicalCode(stageDefinition.canonicalCode())
                .displayName(stageDefinition.name())
                .position(stageDefinition.position())
                .required(stageDefinition.required())
                .implementedStageEnum(stageDefinition.canonicalCode())
                .executionModule(stageDefinition.executionModule())
                .rootPackage(stageDefinition.rootPackage())
                .requiresOpenAiModel(true)
                .configurable(stageDefinition.configurable())
                .build();
        return stageDefinitionRepository.save(entity);
    }

    /**
     * Atualiza campos estruturais persistentes da etapa, preservando configuração operacional em tabela separada.
     */
    private boolean applyStageDefinitionFields(
            PipelineStageDefinitionEntity entity, PipelineStageDefinition stageDefinition) {
        boolean changed = false;
        if (!stageDefinition.name().equals(entity.getDisplayName())) {
            entity.setDisplayName(stageDefinition.name());
            changed = true;
        }
        if (stageDefinition.position() != entity.getPosition()) {
            entity.setPosition(stageDefinition.position());
            changed = true;
        }
        if (stageDefinition.required() != entity.isRequired()) {
            entity.setRequired(stageDefinition.required());
            changed = true;
        }
        if (!stageDefinition.canonicalCode().equals(entity.getImplementedStageEnum())) {
            entity.setImplementedStageEnum(stageDefinition.canonicalCode());
            changed = true;
        }
        if (!java.util.Objects.equals(stageDefinition.executionModule(), entity.getExecutionModule())) {
            entity.setExecutionModule(stageDefinition.executionModule());
            changed = true;
        }
        if (!java.util.Objects.equals(stageDefinition.rootPackage(), entity.getRootPackage())) {
            entity.setRootPackage(stageDefinition.rootPackage());
            changed = true;
        }
        if (stageDefinition.configurable() != entity.isConfigurable()) {
            entity.setConfigurable(stageDefinition.configurable());
            changed = true;
        }
        return changed;
    }

    /**
     * Cria ou mantém configuração operacional, copiando valores legados apenas quando ainda não existe configuração.
     */
    private void syncStageConfig(PipelineStageDefinitionEntity stageDefinition, PipelineStage configuredStage) {
        configRepository.findByPipelineStageDefinitionId(stageDefinition.getId()).orElseGet(() -> {
            PipelineStageConfig config = PipelineStageConfig.builder()
                    .pipelineStageDefinition(stageDefinition)
                    .active(configuredStage == null || configuredStage.isActive())
                    .openAiModel(configuredStage == null ? null : configuredStage.getOpenAiModel())
                    .descriptionOverride(configuredStage == null ? null : configuredStage.getDescription())
                    .updatedBy("pipeline-sync")
                    .build();
            return configRepository.save(config);
        });
    }

    /**
     * Localiza a etapa operacional legada equivalente para migrar configuração sem tocar nos campos estruturais.
     */
    private PipelineStage findConfiguredStage(Pipeline pipeline, PipelineStageDefinition stageDefinition) {
        return pipeline.getStages().stream()
                .filter(stage -> stageDefinition.operationalCode().equals(stage.getCode())
                        || stageDefinition.aliases().contains(stage.getCode())
                        || stageDefinition.canonicalCode().equals(stage.getCode()))
                .findFirst()
                .orElse(null);
    }
}
