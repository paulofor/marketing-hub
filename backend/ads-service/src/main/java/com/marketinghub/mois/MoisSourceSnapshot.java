package com.marketinghub.mois;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "mois_source_snapshot")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoisSourceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_pk", nullable = false)
    private MoisDiscoveryRequest request;

    @Column(name = "artifact_id", nullable = false, unique = true, length = 64)
    private String artifactId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private MoisArtifactStatus status;

    @Column(name = "source_url", nullable = false, length = 1024)
    private String sourceUrl;

    @Column(name = "source_title", length = 255)
    private String sourceTitle;

    @Column(name = "source_kind", length = 64)
    private String sourceKind;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "content_hash", length = 128)
    private String contentHash;

    @Column(name = "raw_excerpt", columnDefinition = "LONGTEXT")
    private String rawExcerpt;

    @Column(name = "normalized_text_ref", length = 255)
    private String normalizedTextRef;

    @Column(name = "capture_notes", columnDefinition = "LONGTEXT")
    private String captureNotes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
