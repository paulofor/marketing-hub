package com.marketinghub.informationsource;

import com.marketinghub.niche.MarketNiche;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Represents a research information source linked to a market niche.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InformationSource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "market_niche_id", nullable = false)
    @ToString.Exclude
    private MarketNiche niche;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 1024)
    private String url;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
