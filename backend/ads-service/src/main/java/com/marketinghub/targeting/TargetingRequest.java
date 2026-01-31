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
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Solicitação inicial de targeting feita pelo cliente, que será atendida pelo AI Worker.
 */
@Entity
@Table(name = "targeting_request")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "candidates")
@EqualsAndHashCode(exclude = "candidates")
public class TargetingRequest {
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    /** Descrição livre enviada pelo cliente (nicho/hipótese). */
    @Column(nullable = false, length = 500)
    private String descricao;

    /** Locale preferencial para geração e consulta na Graph API. */
    @Column(length = 10)
    @Builder.Default
    private String locale = "pt_BR";

    /** País alvo (ISO alpha-2). */
    @Column(length = 5)
    @Builder.Default
    private String country = "BR";

    /** Tipo de público desejado (ex.: prospect, remarketing). */
    @Enumerated(EnumType.STRING)
    @Column(name = "audience_type", length = 32)
    @Builder.Default
    private TargetingAudienceType audienceType = TargetingAudienceType.PROSPECT;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    @Builder.Default
    private TargetingRequestStatus status = TargetingRequestStatus.PENDING_AI;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    @Builder.Default
    private TargetingRequestOrigin origin = TargetingRequestOrigin.CLIENT;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TargetingCandidate> candidates = new ArrayList<>();
}
