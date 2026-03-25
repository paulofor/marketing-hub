package com.marketinghub.salesvideo;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Versão editorial do script utilizado na geração do vídeo.
 */
@Entity
@Table(name = "sales_video_script")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesVideoScript {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    @ToString.Exclude
    private SalesVideoProfile profile;

    @Column(nullable = false)
    private Integer version;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String scriptText;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String hookText;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String ctaText;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String captionText;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String storyboardJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SalesVideoScriptSource source;

    private String model;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String prompt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SalesVideoScriptStatus status;

    private String approvedBy;
    private Instant approvedAt;

    @CreationTimestamp
    private Instant createdAt;
}
