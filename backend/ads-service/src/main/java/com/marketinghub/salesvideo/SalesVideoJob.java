package com.marketinghub.salesvideo;

import com.marketinghub.media.Asset;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Job canônico que representa qualquer etapa do fluxo de vídeos.
 */
@Entity
@Table(name = "sales_video_job")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesVideoJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    @ToString.Exclude
    private SalesVideoProfile profile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "script_id")
    @ToString.Exclude
    private SalesVideoScript script;

    @Builder.Default
    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId = "default";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retry_of_job_id")
    @ToString.Exclude
    private SalesVideoJob retryOfJob;

    @Builder.Default
    @Column(name = "retry_attempt", nullable = false)
    private Integer retryAttempt = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "retry_reason", length = 64)
    private SalesVideoRetryReason retryReason;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String retryNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_family", nullable = false, length = 32)
    private SalesVideoProviderFamily providerFamily;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "execution_mode", nullable = false, length = 16)
    private SalesVideoExecutionMode executionMode = SalesVideoExecutionMode.TEST;

    private String providerName;
    private String providerJobId;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 32)
    private SalesVideoJobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private SalesVideoStatus status;

    @Builder.Default
    private Integer progressPercent = 0;

    private String failureCode;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String failureDetail;

    private String requestedBy;
    private Instant requestedAt;
    private Instant startedAt;
    private Instant finishedAt;
    private Instant expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    @ToString.Exclude
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poster_asset_id")
    @ToString.Exclude
    private Asset posterAsset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vtt_asset_id")
    @ToString.Exclude
    private Asset vttAsset;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String metadataJson;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String auditSnapshotJson;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<SalesVideoJobEvent> events = new ArrayList<>();

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
