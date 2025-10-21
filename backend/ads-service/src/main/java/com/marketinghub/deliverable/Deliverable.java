package com.marketinghub.deliverable;

import com.marketinghub.niche.MarketNiche;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Deliverable generated for a specific {@link MarketNiche}.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Deliverable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "market_niche_id", nullable = false)
    @ToString.Exclude
    private MarketNiche niche;

    @Column(nullable = false)
    private String title;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String description;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String content;

    @Column(length = 255)
    private String model;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String prompt;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
