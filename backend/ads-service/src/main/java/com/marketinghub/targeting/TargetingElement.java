package com.marketinghub.targeting;

import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.niche.MarketNiche;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Elemento individual de segmentação (interesse, cargo ou comportamento).
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TargetingElement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "market_niche_id", nullable = false)
    private MarketNiche niche;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hypothesis_id")
    private Hypothesis hypothesis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TargetingElementType type;

    @Column(name = "term", nullable = false, length = 255)
    private String term;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String description;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String prompt;

    @Column(length = 191)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private TargetingElementSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private TargetingElementStatus status = TargetingElementStatus.DRAFT;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String notes;

    @Column(length = 191)
    private String lastReviewedBy;

    @Column(length = 100)
    private String metaId;

    @Column(length = 191)
    private String metaKey;

    @Column(precision = 10, scale = 4)
    private BigDecimal confidence;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
