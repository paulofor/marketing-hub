package com.marketinghub.appidea;

import com.marketinghub.niche.MarketNiche;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Entity that stores ideation details for new applications tailored to market niches.
 */
@Entity
@Table(name = "app_idea")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppIdea {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "market_niche_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private MarketNiche niche;

    @Column(name = "target_audience")
    private String targetAudience;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "problem_to_solve")
    private String problemToSolve;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "value_proposition")
    private String valueProposition;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "core_features")
    private String coreFeatures;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String differentiator;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String monetization;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "go_to_market")
    private String goToMarket;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "technology_stack")
    private String technologyStack;

    private String model;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String prompt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
