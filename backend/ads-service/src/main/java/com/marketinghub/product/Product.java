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
    private String explicitPain;
    @Lob
    private String promise;
    @Lob
    private String uniqueMechanism;
    @Lob
    private String tripwire;
    @Lob
    private String riskReversal;
    @Lob
    private String socialProof;
    @Lob
    private String checkoutMonetization;
    @Lob
    private String funnel;
    @Lob
    private String creativeVolume;
    @Lob
    private String storytelling;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
