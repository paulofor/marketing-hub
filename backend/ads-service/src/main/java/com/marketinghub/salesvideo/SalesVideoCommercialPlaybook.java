package com.marketinghub.salesvideo;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Playbook comercial por perfil para orientar variações de objeção/CTA.
 */
@Entity
@Table(name = "sales_video_commercial_playbook")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesVideoCommercialPlaybook {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    @ToString.Exclude
    private SalesVideoProfile profile;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "niche_key", nullable = false, length = 120)
    private String nicheKey;

    @Column(name = "variant_key", nullable = false, length = 120)
    private String variantKey;

    @Lob
    @Column(name = "objection_text", nullable = false)
    private String objectionText;

    @Lob
    @Column(name = "cta_text", nullable = false)
    private String ctaText;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_by")
    private String createdBy;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
