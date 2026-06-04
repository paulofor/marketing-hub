package com.marketinghub.pipeline;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Entidade que persiste regras estruturais de uma etapa implementada no contrato canônico de pipeline.
 */
@Entity
@Table(
        name = "pipeline_stage_definition",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_pipeline_stage_definition_code",
                    columnNames = {"pipeline_definition_id", "canonical_code"}),
            @UniqueConstraint(
                    name = "uk_pipeline_stage_definition_position",
                    columnNames = {"pipeline_definition_id", "position"})
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineStageDefinitionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pipeline_definition_id", nullable = false)
    private PipelineDefinitionEntity pipelineDefinition;

    @Column(nullable = false, length = 80)
    private String canonicalCode;

    @Column(nullable = false, length = 120)
    private String displayName;

    @Column(nullable = false)
    private Integer position;

    @Column(nullable = false)
    @Builder.Default
    private boolean required = true;

    @Column(nullable = false, length = 80)
    private String implementedStageEnum;

    @Column(nullable = false)
    @Builder.Default
    private boolean requiresOpenAiModel = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean configurable = true;

    @OneToOne(mappedBy = "pipelineStageDefinition", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private PipelineStageConfig config;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
