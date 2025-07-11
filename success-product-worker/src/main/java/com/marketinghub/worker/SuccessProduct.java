package com.marketinghub.worker;

import com.marketinghub.ads.InstagramAccount;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

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
    @Column(columnDefinition = "LONGTEXT")
    private String description;

    private String name;

    @Builder.Default
    private boolean novo = true;

    private String niche;
    private String avatar;

    private String salesPageUrl;
    private String instagramUrl;
    private String facebookUrl;
    private String youtubeUrl;

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

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
