package com.marketinghub.repository.jpa.gerasalespage.v1;

import com.marketinghub.gerasalespage.v1.GeraSalesPagePromptSchemaTemplate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: consultar templates ativos de prompt e schema do GeraSalesPage v1. */
public interface GeraSalesPagePromptSchemaTemplateRepository
        extends JpaRepository<GeraSalesPagePromptSchemaTemplate, String> {
    /** Busca o template ativo da etapa para o pipeline informado. */
    Optional<GeraSalesPagePromptSchemaTemplate> findFirstByPipelineCodeAndStageCodeAndActiveTrueOrderByVersionDesc(
            String pipelineCode, String stageCode);
}
