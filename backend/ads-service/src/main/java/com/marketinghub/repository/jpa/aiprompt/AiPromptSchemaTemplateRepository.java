package com.marketinghub.repository.jpa.aiprompt;

import com.marketinghub.aiprompt.AiPromptSchemaTemplate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: consultar templates ativos de prompt e schema dos pipelines de IA. */
public interface AiPromptSchemaTemplateRepository
        extends JpaRepository<AiPromptSchemaTemplate, String> {
    /** Busca o template ativo da etapa para o pipeline informado. */
    Optional<AiPromptSchemaTemplate> findFirstByPipelineCodeAndStageCodeAndActiveTrueOrderByVersionDesc(
            String pipelineCode, String stageCode);
}
