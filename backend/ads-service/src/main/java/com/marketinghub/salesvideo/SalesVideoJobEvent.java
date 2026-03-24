package com.marketinghub.salesvideo;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Registro de auditoria de mudanças em um job de vídeo.
 */
@Entity
@Table(name = "sales_video_job_event")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesVideoJobEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    @ToString.Exclude
    private SalesVideoJob job;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private SalesVideoJobEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 64)
    private SalesVideoStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 64)
    private SalesVideoStatus newStatus;

    private String message;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String detailsJson;

    @CreationTimestamp
    private Instant createdAt;
}
