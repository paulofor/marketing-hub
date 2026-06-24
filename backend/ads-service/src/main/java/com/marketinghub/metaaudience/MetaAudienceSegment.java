package com.marketinghub.metaaudience;

import com.marketinghub.niche.MarketNiche;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Representa uma parcela funcional do nicho associada a uma audiência Meta Ads. */
@Entity
@Table(name = "meta_audience_segment")
@Getter
@Setter
public class MetaAudienceSegment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "meta_audience_id")
    private MetaAudience metaAudience;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "market_niche_id")
    private MarketNiche marketNiche;
    @Column(name = "segment_name", nullable = false)
    private String segmentName;
    @Lob @Column(name = "segment_description") private String segmentDescription;
    @Lob @Column(name = "pain_focus") private String painFocus;
    @Lob @Column(name = "desired_outcome_focus") private String desiredOutcomeFocus;
    @Lob @Column(name = "offer_angle") private String offerAngle;
    @Lob @Column(name = "selection_rule") private String selectionRule;
    @Column(name = "estimated_contacts", nullable = false) private long estimatedContacts;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
}
