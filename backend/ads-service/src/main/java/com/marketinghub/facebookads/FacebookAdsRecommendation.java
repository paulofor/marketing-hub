package com.marketinghub.facebookads;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Representa uma sugestão oficial retornada pela Meta para uma campanha ativa.
 */
@Entity
@Table(name = "facebook_ads_recommendation")
@Getter
@Setter
@NoArgsConstructor
public class FacebookAdsRecommendation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false, columnDefinition = "CHAR(36)")
    private FacebookAdsCampaign campaign;

    @Column(name = "recommendation_code")
    private String recommendationCode;

    @Column(name = "title", columnDefinition = "TEXT")
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "importance", length = 50)
    private String importance;

    @Column(name = "confidence", length = 50)
    private String confidence;

    @Column(name = "blame_field", length = 255)
    private String blameField;

    @Column(name = "recommendation_data_json", columnDefinition = "LONGTEXT")
    private String recommendationDataJson;

    @Column(name = "raw_json", nullable = false, columnDefinition = "LONGTEXT")
    private String rawJson;

    @Column(name = "collected_at", nullable = false)
    private Instant collectedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
