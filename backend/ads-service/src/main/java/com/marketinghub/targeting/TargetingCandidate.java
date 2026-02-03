package com.marketinghub.targeting;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Candidato de targeting enviado pelo AI Worker antes de validação com a Meta.
 */
@Entity
@Table(name = "targeting_candidate")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"request", "options"})
@EqualsAndHashCode(exclude = {"request", "options"})
public class TargetingCandidate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private TargetingRequest request;

    @Column(name = "texto_sugerido", nullable = false, length = 255)
    private String textoSugerido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TargetingCandidateType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private TargetingCandidateStatus status = TargetingCandidateStatus.PENDING_FACEBOOK_MATCH;

    @Column(length = 10)
    private String idioma;

    @Column(length = 5)
    private String country;

    @Column(length = 32)
    private String origem;

    @Column(name = "intent_tag", length = 32)
    private String intentTag;

    @Column(precision = 5, scale = 4)
    private BigDecimal score;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String rationale;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "rejection_reason")
    private String rejectionReason;

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<TargetingOption> options = new LinkedHashSet<>();

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
