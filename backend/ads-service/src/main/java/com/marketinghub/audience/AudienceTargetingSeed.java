package com.marketinghub.audience;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Seeds utilizados para compor a segmentação estruturada.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudienceTargetingSeed {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audience_id", nullable = false)
    private Audience audience;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TargetingSeedType type;

    /** Nome exibido do seed (texto vindo do prompt ou do analista). */
    @Column(nullable = false)
    private String value;

    /** Identificador retornado pelo Targeting Search (id numérico). */
    private String metaId;

    /** Chave/slug retornada pelo Targeting Search (key). */
    private String key;

    /** Confiança ou score atribuído pela IA para priorizar revisão. */
    private BigDecimal confidence;

    /** Estado atual da resolução do seed. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TargetingSeedStatus status = TargetingSeedStatus.DRAFT;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
