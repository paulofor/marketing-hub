package com.marketinghub.targeting;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa uma opção válida retornada pela Graph API para um candidato.
 */
@Entity
@Table(name = "targeting_option")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "candidate")
@EqualsAndHashCode(exclude = "candidate")
public class TargetingOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private TargetingCandidate candidate;

    @Column(name = "facebook_id", length = 100, nullable = false)
    private String facebookId;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 32, nullable = false)
    private TargetingCandidateType type;

    @Column(name = "audience_size")
    private Long audienceSize;

    @Column(name = "match_score", precision = 5, scale = 4)
    private BigDecimal matchScore;

    @ElementCollection
    @CollectionTable(name = "targeting_option_path", joinColumns = @JoinColumn(name = "option_id"))
    @Column(name = "path_entry", length = 255)
    @Builder.Default
    private List<String> path = new ArrayList<>();

    @Column(name = "search_locale", length = 10)
    private String searchLocale;

    @Column(name = "search_country", length = 5)
    private String searchCountry;

    @Column(name = "search_term", length = 255)
    private String searchTerm;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
