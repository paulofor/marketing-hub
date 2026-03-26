package com.marketinghub.hypothesis;

import com.marketinghub.niche.MarketNiche;
import com.marketinghub.creative.label.Angle;
import com.marketinghub.prompt.PromptAttributeDescription;
import com.marketinghub.ads.FacebookInstantForm;
import com.marketinghub.targeting.TargetingElement;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
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

    /** Promessa de valor com espaço para textos completos gerados pela IA. */
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String promise;

    /** Problema ou insight do cliente em uma frase. */
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String problem;

    /** Persona alvo dentro do nicho. */
    @Column
    private String persona;

    /** Mecanismo que sustenta a promessa. */
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String mechanism;

    /** Mecanismo único que sustenta a promessa. */
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String uniqueMechanism;

    /** Entrega ou deliverable associado à hipótese. */
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String entrega;

    /** Prompt usado quando a hipótese é gerada por IA. */
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String prompt;

    /** Snapshot em JSON do framework Dor → Resultado → Mecanismo → Prova → Oferta. */
    @Lob
    @Column(name = "framework_json", columnDefinition = "LONGTEXT")
    private String frameworkJson;

    /** Modelo de IA responsável pela geração desta hipótese. */
    @Column(length = 191)
    private String model;

    /** Custo estimado em USD para gerar esta hipótese. */
    @Column(name = "cost_usd", precision = 10, scale = 4)
    private BigDecimal costUsd;

    /** Custo estimado em BRL para a hipótese. */
    @Column(precision = 10, scale = 2)
    private BigDecimal cost;

    /** Custo total acumulado a partir desta hipótese. */
    @Column(precision = 12, scale = 2)
    private BigDecimal totalCost;

    /** Despesa estimada em BRL para a hipótese. */
    @Column(precision = 10, scale = 2)
    private BigDecimal expense;

    @ManyToMany
    @JoinTable(name = "hypothesis_prompt_attribute_description",
            joinColumns = @JoinColumn(name = "hypothesis_id"),
            inverseJoinColumns = @JoinColumn(name = "prompt_attribute_description_id"))
    @Builder.Default
    private Set<PromptAttributeDescription> promptAttributeDescriptions = new HashSet<>();

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

    @OneToMany(mappedBy = "hypothesis")
    private java.util.List<TargetingElement> targetingElements;

    @OneToMany(mappedBy = "hypothesis")
    private java.util.List<FacebookInstantForm> instantForms;
}
