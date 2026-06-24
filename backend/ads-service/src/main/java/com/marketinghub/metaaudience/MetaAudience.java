package com.marketinghub.metaaudience;

import com.marketinghub.niche.MarketNiche;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Representa uma audiência Meta Ads controlada pelo Marketing Hub e vinculada a um nicho. */
@Entity
@Table(name = "meta_audience")
@Getter
@Setter
public class MetaAudience {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "market_niche_id")
    private MarketNiche marketNiche;
    @Column(name = "source_cnae_code", nullable = false, length = 7)
    private String sourceCnaeCode;
    @Column(name = "audience_name", nullable = false)
    private String audienceName;
    @Column(name = "facebook_ad_account_id", nullable = false, length = 64)
    private String facebookAdAccountId;
    @Column(name = "facebook_audience_id", length = 64)
    private String facebookAudienceId;
    @Column(name = "audience_type", nullable = false, length = 32)
    private String audienceType;
    @Column(name = "source_type", nullable = false, length = 32)
    private String sourceType;
    @Lob @Column(name = "filter_strategy")
    private String filterStrategy;
    @Column(name = "eligibility_status", nullable = false, length = 32)
    private String eligibilityStatus;
    @Column(name = "total_contacts", nullable = false)
    private long totalContacts;
    @Column(name = "unique_emails", nullable = false)
    private long uniqueEmails;
    @Column(name = "synced_contacts", nullable = false)
    private long syncedContacts;
    @Column(name = "last_sync_at")
    private Instant lastSyncAt;
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
