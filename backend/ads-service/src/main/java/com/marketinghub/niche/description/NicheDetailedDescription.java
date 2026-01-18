package com.marketinghub.niche.description;

import com.marketinghub.niche.MarketNiche;
import com.marketinghub.prompt.Prompt;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Descrição detalhada de um nicho, gerada pelo Worker de IA.
 */
@Entity
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NicheDetailedDescription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "market_niche_id", nullable = false)
    @ToString.Exclude
    private MarketNiche marketNiche;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prompt_id")
    @ToString.Exclude
    private Prompt promptTemplate;

    @Column(length = 255)
    private String title;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String description;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String pains;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String desires;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String needs;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String prompt;

    @Column(length = 191)
    private String model;

    @Column(name = "cost_usd", precision = 10, scale = 4)
    private BigDecimal costUsd;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    private Integer inputTokens;

    private Integer outputTokens;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
