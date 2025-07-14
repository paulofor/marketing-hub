package com.marketinghub.niche;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Entity representing a market niche that can be tested manually or via AI.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketNiche {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    /** Optional description or notes about this niche. */
    @Lob
    private String description;

    /** Results of demand volume tests. */
    @Lob
    private String demandVolume;

    /** Promises validated for this niche. */
    @Lob
    private String promises;

    /** Offers validated for this niche. */
    @Lob
    private String offers;

    /** Base segmentation for the Brazilian market. */
    @Lob
    private String baseSegmentation;

    /** Main interests or behaviors for this niche. */
    @Lob
    private String interests;

    /** Demographic filters and job roles. */
    @Lob
    private String demographicFilters;

    /** Extra tips for advertising this niche. */
    @Lob
    private String extraTips;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
