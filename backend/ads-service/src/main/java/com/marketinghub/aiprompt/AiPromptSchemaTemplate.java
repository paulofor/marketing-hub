package com.marketinghub.aiprompt;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Responsabilidade: armazenar no banco o prompt e o schema JSON usados por uma etapa de IA. */
@Entity
@Table(name = "ai_prompt_schema_template")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiPromptSchemaTemplate {
    @Id
    @Column(name = "template_key", nullable = false, length = 191)
    private String templateKey;

    @Column(name = "pipeline_code", nullable = false, length = 100)
    private String pipelineCode;

    @Column(name = "stage_code", nullable = false, length = 100)
    private String stageCode;

    @Column(name = "version", nullable = false, length = 40)
    private String version;

    @Column(name = "openai_model", nullable = false, length = 120)
    private String openAiModel;

    @Column(name = "schema_name", nullable = false, length = 120)
    private String schemaName;

    @Column(name = "prompt_markdown_content", nullable = false, columnDefinition = "LONGTEXT")
    private String promptMarkdownContent;

    @Column(name = "schema_json", nullable = false, columnDefinition = "LONGTEXT")
    private String schemaJson;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
