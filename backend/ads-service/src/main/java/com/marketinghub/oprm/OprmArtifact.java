package com.marketinghub.oprm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OprmArtifact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private OprmJob job;

    @Column(name = "artifact_id", nullable = false, length = 191)
    private String artifactId;

    @Column(name = "artifact_type", nullable = false, length = 128)
    private String artifactType;

    @Column(name = "artifact_version", nullable = false, length = 32)
    private String artifactVersion;

    @Column(name = "module_name", nullable = false, length = 64)
    private String moduleName;

    @Column(name = "producer", nullable = false, length = 191)
    private String producer;

    @Column(name = "artifact_created_at", nullable = false)
    private Instant artifactCreatedAt;

    @Column(name = "correlation_id", nullable = false, length = 191)
    private String correlationId;

    @Column(name = "occupation_seed_ref", nullable = false, length = 191)
    private String occupationSeedRef;

    @Column(name = "trace_id", nullable = false, length = 191)
    private String traceId;

    @Column(name = "source_refs_json", nullable = false, columnDefinition = "LONGTEXT")
    private String sourceRefsJson;

    @Column(name = "input_refs_json", nullable = false, columnDefinition = "LONGTEXT")
    private String inputRefsJson;

    @Column(name = "payload_json", nullable = false, columnDefinition = "LONGTEXT")
    private String payloadJson;

    @Column(name = "lineage_json", nullable = false, columnDefinition = "LONGTEXT")
    private String lineageJson;

    @Column(name = "metadata_json", nullable = false, columnDefinition = "LONGTEXT")
    private String metadataJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "artifact_status", nullable = false, length = 32)
    private OprmArtifactStatus artifactStatus;

    @Column(name = "confidence_score", precision = 5, scale = 4)
    private BigDecimal confidenceScore;

    @Column(name = "idempotency_key", nullable = false, length = 191)
    private String idempotencyKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
