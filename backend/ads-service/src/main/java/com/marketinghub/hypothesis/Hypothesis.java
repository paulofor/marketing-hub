package com.marketinghub.hypothesis;

import com.marketinghub.niche.MarketNiche;
import com.marketinghub.creative.label.Angle;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
public class Hypothesis {
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "market_niche_id")
    private MarketNiche marketNiche;

    @Column(nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "premise_angle_id")
    private Angle premiseAngle;

    /** Promessa de valor em até 140 caracteres. */
    @Column(length = 140)
    private String promise;

    /** Problema ou insight do cliente em uma frase. */
    @Column
    private String problem;

    /** Persona alvo dentro do nicho. */
    @Column
    private String persona;

    /** Mecanismo que sustenta a promessa. */
    @Lob
    private String mechanism;

    /** Mecanismo único que sustenta a promessa. */
    @Lob
    private String uniqueMechanism;

    /** Prompt usado quando a hipótese é gerada por IA. */
    @Lob
    private String prompt;

    /** Modelo de IA responsável pela geração desta hipótese. */
    private String model;

    /** Regra de sucesso que define se a hipótese será validada. */
    @Lob
    private String successRule;

    @Enumerated(EnumType.STRING)
    @Column
    private OfferType offerType;

    @Column(precision = 6, scale = 2)
    private BigDecimal price;
    @Column(precision = 7, scale = 2)
    private BigDecimal kpiTargetCpl;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column
    private HypothesisStatus status = HypothesisStatus.BACKLOG;

    /** Data em que a hipótese foi gerada pela IA. */
    private Instant generatedAt;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
