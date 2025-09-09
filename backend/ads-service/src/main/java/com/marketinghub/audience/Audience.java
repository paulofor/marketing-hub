package com.marketinghub.audience;

import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.niche.MarketNiche;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Represents a Facebook Ads audience that can be linked to either a market niche or a hypothesis.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Audience {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Name of the audience for identification. */
    private String name;

    /** Optional description or notes about the audience. */
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String description;

    /** Associated market niche, if this is a generic audience. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "market_niche_id")
    private MarketNiche niche;

    /** Associated hypothesis, if this audience is specific to a hypothesis. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hypothesis_id")
    private Hypothesis hypothesis;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}

