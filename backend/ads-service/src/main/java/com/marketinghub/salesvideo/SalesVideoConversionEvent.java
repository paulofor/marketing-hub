package com.marketinghub.salesvideo;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Fato canônico de conversão vinculado ao perfil/job/script para leitura de performance.
 */
@Entity
@Table(name = "sales_video_conversion_event")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesVideoConversionEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    @ToString.Exclude
    private SalesVideoProfile profile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    @ToString.Exclude
    private SalesVideoJob job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "script_id")
    @ToString.Exclude
    private SalesVideoScript script;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private SalesVideoConversionEventType eventType;

    @Column(name = "event_value", precision = 14, scale = 2)
    private BigDecimal eventValue;

    @Column(name = "currency_code", length = 8)
    private String currencyCode;

    @Column(name = "source", length = 80)
    private String source;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "metadata_json")
    private String metadataJson;

    @CreationTimestamp
    private Instant createdAt;
}
