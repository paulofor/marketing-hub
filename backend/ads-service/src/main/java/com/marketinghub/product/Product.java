package com.marketinghub.product;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.marketinghub.ads.InstagramAccount;
import com.marketinghub.memberarea.MemberArea;
import com.marketinghub.niche.MarketNiche;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Responsabilidade: representar um produto digital comercial reutilizável pelo Marketing Hub.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador legível e estável do produto em contratos e telas. */
    @Column(name = "slug", length = 191, unique = true)
    private String slug;

    /** Nome comercial público do produto. */
    @Column(name = "name", length = 191)
    private String name;

    /** URL pública da área, página ou experiência principal do produto. */
    @Column(name = "public_url", length = 512)
    private String publicUrl;

    /** URL pública do logo canônico usado em cadastro, relatórios e materiais comerciais. */
    @Column(name = "logo_url", length = 512)
    private String logoUrl;

    /** Paleta de cores canônica usada em páginas, criativos e área de uso. */
    @Lob
    @Column(name = "color_palette", columnDefinition = "LONGTEXT")
    private String colorPalette;

    /** Público alvo prioritário do produto. */
    @Lob
    @Column(name = "target_audience", columnDefinition = "LONGTEXT")
    private String targetAudience;

    /** Estilo de linguagem recomendado para comunicação comercial e produto. */
    @Lob
    @Column(name = "language_style", columnDefinition = "LONGTEXT")
    private String languageStyle;

    /** Módulos de código envolvidos na venda, entrega e operação do produto. */
    @Lob
    @Column(name = "code_modules", columnDefinition = "LONGTEXT")
    private String codeModules;

    /** Tipo comercial do produto dentro da estratégia do Marketing Hub. */
    @Column(name = "product_type", length = 64)
    private String productType;

    /** Status comercial atual do produto. */
    @Column(name = "commercial_status", length = 64)
    private String commercialStatus;

    /** Preço atual praticado para venda direta. */
    @Column(name = "current_price_brl", precision = 10, scale = 2)
    private java.math.BigDecimal currentPriceBrl;

    /** Hipótese ou oferta principal que originou o produto. */
    @Column(name = "primary_hypothesis_id", columnDefinition = "BINARY(16)")
    private UUID primaryHypothesisId;

    /** Descrição estratégica da hipótese ou oferta principal do produto. */
    @Lob
    @Column(name = "primary_hypothesis", columnDefinition = "LONGTEXT")
    private String primaryHypothesis;

    /** Experimentos associados ao histórico de validação e escala do produto. */
    @Lob
    @Column(name = "associated_experiments", columnDefinition = "LONGTEXT")
    private String associatedExperiments;

    /** Observações comerciais livres para evolução futura do cadastro. */
    @Lob
    @Column(name = "commercial_notes", columnDefinition = "LONGTEXT")
    private String commercialNotes;

    /** Jornada comercial resumida que tangibiliza a transformação do produto dia a dia. */
    @Lob
    @Column(name = "seven_day_journey", columnDefinition = "LONGTEXT")
    private String sevenDayJourney;

    /** Orientação comercial sobre como apresentar materiais de apoio sem reduzir valor percebido. */
    @Lob
    @Column(name = "support_material_positioning", columnDefinition = "LONGTEXT")
    private String supportMaterialPositioning;

    /** Chamada principal recomendada para a ação de compra ou avanço no funil. */
    @Column(name = "primary_cta", length = 191)
    private String primaryCta;

    private String niche;
    private String avatar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instagram_account_id")
    private InstagramAccount instagramAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "market_niche_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private MarketNiche marketNiche;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String explicitPain;
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String promise;
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String uniqueMechanism;
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String tripwire;
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String riskReversal;
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String socialProof;

    /** Pacote científico operacional versionado usado por workers na criação e entrega do produto. */
    @Lob
    @Column(name = "scientific_evidence_pack", columnDefinition = "LONGTEXT")
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String scientificEvidencePack;

    /** Contrato JSON versionado que a PDE Platform consome para renderizar a experiência do produto. */
    @Lob
    @Column(name = "pde_experience_json", columnDefinition = "LONGTEXT")
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String pdeExperienceJson;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String checkoutMonetization;
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String funnel;
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String creativeVolume;
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String storytelling;

    private java.math.BigDecimal aiCost;

    @OneToMany(mappedBy = "product")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<MemberArea> memberAreas;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
