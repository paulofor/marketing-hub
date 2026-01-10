package com.marketinghub.niche;

import jakarta.persistence.*;
import lombok.*;
import com.marketinghub.appidea.AppIdea;
import com.marketinghub.chat.ChatDialog;
import com.marketinghub.deliverable.Deliverable;
import com.marketinghub.differentiatedtechnology.DifferentiatedTechnology;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.audience.Audience;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entity representing a market niche that can be tested manually or via AI.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketNiche {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    /** Optional description or notes about this niche. */
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String description;

    /** Categoria principal de interesse associada ao nicho. */
    private String interestCategory;

    /** Categoria de cargo associada ao nicho. */
    private String roleCategory;

    /** Results of demand volume tests. */
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String demandVolume;

    /** Promises validated for this niche. */
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String promises;

    /** Offers validated for this niche. */
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String offers;

    /** Custo estimado para o nicho. */
    @Column(precision = 10, scale = 2)
    private BigDecimal cost;

    /** Despesa estimada para o nicho. */
    @Column(precision = 10, scale = 2)
    private BigDecimal expense;

    /** Custo total acumulado para o nicho. */
    @Column(precision = 12, scale = 2)
    private BigDecimal totalCost;

    /** Receita total acumulada para o nicho. */
    @Column(precision = 12, scale = 2)
    private BigDecimal totalRevenue;

    /** Quantidade de hipóteses a serem geradas para este nicho. */
    private Integer hypothesesToGenerate;

    /** Quantidade de públicos a serem gerados para este nicho. */
    private Integer audiencesToGenerate;

    /** Modelo do OpenAI a ser utilizado para gerar hipóteses. */
    @Column(length = 191)
    private String hypothesisModel;

    /** Tecnologia diferenciada selecionada para guiar hipóteses. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "differentiated_technology_id")
    private DifferentiatedTechnology differentiatedTechnology;

    /** Base segmentation for the Brazilian market. */
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String baseSegmentation;

    /** Main interests or behaviors for this niche. */
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String interests;

    /** Demographic filters and job roles. */
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String demographicFilters;

    /** Extra tips for advertising this niche. */
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String extraTips;

    /** ChatGPT dialog that originated this niche, if any. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_dialog_id")
    @ToString.Exclude
    private ChatDialog chatDialog;

    @OneToMany(mappedBy = "niche")
    private java.util.List<Experiment> experiments;

    @OneToMany(mappedBy = "niche")
    private java.util.List<Audience> audiences;

    @OneToMany(mappedBy = "niche")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private java.util.List<AppIdea> appIdeas;

    @OneToMany(mappedBy = "niche")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private java.util.List<Deliverable> deliverables;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
