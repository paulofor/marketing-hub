package com.marketinghub.salesvideo;

import com.marketinghub.experiment.LandingPage;
import com.marketinghub.product.Product;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Perfil canônico que descreve um vídeo de venda para um produto.
 */
@Entity
@Table(name = "sales_video_profile")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesVideoProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @ToString.Exclude
    private Product product;

    @Builder.Default
    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId = "default";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landing_page_id")
    @ToString.Exclude
    private LandingPage landingPage;

    @Column(name = "created_by")
    private String createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "video_kind", nullable = false, length = 32)
    private SalesVideoKind videoKind;

    @Column(nullable = false)
    private String title;

    private String personaName;
    private String personaStyle;
    private String voiceStyle;
    private String language;
    private Integer targetDurationSeconds;

    @Builder.Default
    @Column(name = "requires_consent", nullable = false)
    private boolean requiresConsent = false;

    @Column(name = "consent_recorded_by")
    private String consentRecordedBy;

    @Column(name = "consent_recorded_at")
    private Instant consentRecordedAt;

    @Column(name = "consent_evidence_url")
    private String consentEvidenceUrl;

    @Column(name = "human_review_approved_by")
    private String humanReviewApprovedBy;

    @Column(name = "human_review_approved_at")
    private Instant humanReviewApprovedAt;

    @Lob
    private String complianceNotes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private SalesVideoStatus status;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<SalesVideoScript> scripts = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<SalesVideoJob> jobs = new ArrayList<>();

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
