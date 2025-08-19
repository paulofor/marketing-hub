package com.marketinghub.niche;

import jakarta.persistence.*;
import lombok.*;
import com.marketinghub.chat.ChatDialog;
import com.marketinghub.experiment.Experiment;
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
    @Column(columnDefinition = "LONGTEXT")
    private String description;

    /** Results of demand volume tests. */
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String demandVolume;

    /** Promises validated for this niche. */
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String promises;

    /** Offers validated for this niche. */
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String offers;

    /** Base segmentation for the Brazilian market. */
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String baseSegmentation;

    /** Main interests or behaviors for this niche. */
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String interests;

    /** Demographic filters and job roles. */
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String demographicFilters;

    /** Extra tips for advertising this niche. */
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String extraTips;

    /** ChatGPT dialog that originated this niche, if any. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_dialog_id")
    @ToString.Exclude
    private ChatDialog chatDialog;

    @OneToMany(mappedBy = "niche")
    private java.util.List<Experiment> experiments;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
