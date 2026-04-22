package com.marketinghub.mois;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "mois_discovery_request")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoisDiscoveryRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, unique = true, length = 64)
    private String requestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private MoisDiscoveryRequestStatus status;

    @Column(name = "niche_name", nullable = false, length = 191)
    private String nicheName;

    @Column(name = "market_theme", nullable = false, length = 191)
    private String marketTheme;

    @Column(name = "pain_or_outcome_focus", columnDefinition = "LONGTEXT")
    private String painOrOutcomeFocus;

    @Column(name = "seed_queries_json", nullable = false, columnDefinition = "LONGTEXT")
    private String seedQueriesJson;

    @Column(name = "seed_urls_json", nullable = false, columnDefinition = "LONGTEXT")
    private String seedUrlsJson;

    @Column(name = "channels_json", nullable = false, columnDefinition = "LONGTEXT")
    private String channelsJson;

    @Column(name = "country", length = 64)
    private String country;

    @Column(name = "language", length = 32)
    private String language;

    @Column(name = "discovery_policy_json", nullable = false, columnDefinition = "LONGTEXT")
    private String discoveryPolicyJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
