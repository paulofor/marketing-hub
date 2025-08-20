package com.marketinghub.successproduct;

import com.marketinghub.ads.InstagramAccount;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Entity representing a successful product template.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuccessProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String description;

    private String name;

    /** Flag to indicate newly created entries. */
    @Builder.Default
    private boolean novo = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SuccessProductPlatform platform = SuccessProductPlatform.COFRE;

    private String niche;
    private String avatar;

    private String audienceType;

    private String salesPageUrl;
    private String instagramUrl;
    private String facebookUrl;
    private String youtubeUrl;

    @ManyToOne
    @JoinColumn(name = "instagram_account_id")
    private InstagramAccount instagramAccount;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String explicitPain;
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String promise;
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String uniqueMechanism;
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String tripwire;
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String riskReversal;
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String socialProof;
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String checkoutMonetization;
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "sales_funnel")
    private String salesFunnel;
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String creativeVolume;
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String storytelling;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
