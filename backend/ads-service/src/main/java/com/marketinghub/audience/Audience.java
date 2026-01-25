package com.marketinghub.audience;

import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.niche.MarketNiche;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Facebook Ads audience that can be linked to either a market niche or a hypothesis.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Audience {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Name of the audience for identification. */
    private String name;

    /** Optional description or notes about the audience. */
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String description;

    /** Prompt utilizado para gerar o público via IA. */
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String prompt;

    /** Modelo responsável pela criação automática. */
    private String model;

    /** Indica se o público foi aprovado para uso em mídia. */
    @Column(nullable = false)
    private boolean approved;

    /** JSON com o Targeting Spec compatível com a API do Meta Ads. */
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String targetingSpec;

    /** Status do processo de construção/revisão do targeting estruturado. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TargetingStatus targetingStatus = TargetingStatus.DRAFT;

    /** Observações da pessoa que revisou o targeting. */
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String targetingNotes;

    /** Origem do público (manual vs IA) para auditoria. */
    @Enumerated(EnumType.STRING)
    private AudienceSource source;

    /** Usuário que revisou por último o targeting. */
    private String lastReviewedBy;

    /** Seeds resolvidas/pendentes usadas para montar o targeting. */
    @OneToMany(mappedBy = "audience", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AudienceTargetingSeed> targetingSeeds = new ArrayList<>();

    /** Associated market niche, if this is a generic audience. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "market_niche_id")
    private MarketNiche niche;

    /** Associated hypothesis, if this audience is specific to a hypothesis. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hypothesis_id")
    private Hypothesis hypothesis;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
