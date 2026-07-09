package com.marketinghub.repository.jpa.aiprompt;

import com.marketinghub.aiprompt.AiPromptSchemaTemplate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: consultar templates ativos de prompt e schema dos pipelines de IA. */
public interface AiPromptSchemaTemplateRepository
        extends JpaRepository<AiPromptSchemaTemplate, String> {
    /** Busca o template ativo da etapa para o pipeline informado. */
    Optional<AiPromptSchemaTemplate> findFirstByPipelineCodeAndStageCodeAndActiveTrueOrderByVersionDesc(
            String pipelineCode, String stageCode);

    /** Lista templates de um pipeline ordenando por etapa e versão. */
    List<AiPromptSchemaTemplate> findByPipelineCodeOrderByStageCodeAscVersionDesc(String pipelineCode);

    /** Lista templates de uma etapa ordenando por versão. */
    List<AiPromptSchemaTemplate> findByPipelineCodeAndStageCodeOrderByVersionDesc(String pipelineCode, String stageCode);

    /** Desativa outros templates da mesma etapa para manter um ativo canônico. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update AiPromptSchemaTemplate template
               set template.active = false,
                   template.updatedAt = :updatedAt
             where template.pipelineCode = :pipelineCode
               and template.stageCode = :stageCode
               and template.templateKey <> :templateKey
            """)
    void deactivateOthersForStage(
            @Param("pipelineCode") String pipelineCode,
            @Param("stageCode") String stageCode,
            @Param("templateKey") String templateKey,
            @Param("updatedAt") Instant updatedAt);
}
