package com.marketinghub.successproduct;

import com.marketinghub.ads.InstagramAccount;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
    private String description;

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
