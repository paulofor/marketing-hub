package com.marketinghub.pipeline;

import com.marketinghub.openai.OpenAiModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Entidade que guarda somente a configuração operacional editável de uma etapa definida no contrato canônico.
 */
@Entity
@Table(name = "pipeline_stage_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineStageConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pipeline_stage_definition_id", nullable = false, unique = true)
    private PipelineStageDefinitionEntity pipelineStageDefinition;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "openai_model_id")
    private OpenAiModel openAiModel;

    @Column(columnDefinition = "TEXT")
    private String descriptionOverride;

    @Column(length = 120)
    private String updatedBy;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
