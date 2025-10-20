package com.marketinghub.product;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.marketinghub.ads.InstagramAccount;
import com.marketinghub.memberarea.MemberArea;
import com.marketinghub.niche.MarketNiche;

import java.time.Instant;
import java.util.List;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instagram_account_id")
    private InstagramAccount instagramAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "market_niche_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private MarketNiche marketNiche;

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
    private String funnel;
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String creativeVolume;
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String storytelling;

    private java.math.BigDecimal aiCost;

    @OneToMany(mappedBy = "product")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<MemberArea> memberAreas;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
