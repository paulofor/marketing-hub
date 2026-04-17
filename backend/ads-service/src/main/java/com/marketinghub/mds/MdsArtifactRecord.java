package com.marketinghub.mds;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "artifact_record")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MdsArtifactRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "artifact_type", nullable = false, length = 128)
    private String artifactType;

    @Column(name = "schema_version", nullable = false, length = 32)
    private String schemaVersion;

    @Column(name = "version", nullable = false, length = 32)
    private String version;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private MdsArtifactStatus status;

    @Column(name = "producer_module", nullable = false, length = 64)
    private String producerModule;

    @Column(name = "owner_module", nullable = false, length = 64)
    private String ownerModule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private MdsRequest request;

    @Column(name = "content_json", nullable = false, columnDefinition = "LONGTEXT")
    private String contentJson;

    @Column(name = "hash", nullable = false, length = 64)
    private String hash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
