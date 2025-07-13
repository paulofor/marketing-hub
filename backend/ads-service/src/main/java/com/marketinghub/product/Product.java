package com.marketinghub.product;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.marketinghub.ads.InstagramAccount;

import java.time.Instant;

/**
 * Entity representing a digital product following marketing principles.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String niche;
    private String avatar;

    @ManyToOne
    @JoinColumn(name = "instagram_account_id")
    private InstagramAccount instagramAccount;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String explicitPain;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String promise;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String uniqueMechanism;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String tripwire;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String riskReversal;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String socialProof;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String checkoutMonetization;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String funnel;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String creativeVolume;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String storytelling;

    private java.math.BigDecimal aiCost;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
