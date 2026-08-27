package com.marketinghub.product;

import com.marketinghub.ads.InstagramAccount;
import com.marketinghub.media.Asset;
import com.marketinghub.memberarea.MemberArea;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.producttype.ProductTypeDefinition;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/** Responsabilidade: representar um produto digital comercial reutilizável pelo Marketing Hub. */
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

  /** Nome de trabalho estável usado por pessoas e agentes dentro do Marketing Hub. */
  @Column(name = "internal_name", length = 191)
  private String internalName;

  /** Nomes internos alternativos e históricos que permitem localizar o mesmo produto. */
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "product_alias", joinColumns = @JoinColumn(name = "product_id"))
  @Column(name = "alias", nullable = false, length = 191)
  @Builder.Default
  private Set<String> aliases = new LinkedHashSet<>();

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
  @Column(name = "product_type", length = 191)
  private String productType;

  /** Definição estável do tipo no catálogo extensível de produtos. */
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "product_type_id")
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private ProductTypeDefinition productTypeDefinition;

  /** Formato principal entregue ao cliente, comparável entre produtos e experimentos. */
  @Column(name = "product_format", length = 64)
  private String productFormat;

  /** Forma operacional de entrega: automática, personalizada, híbrida ou acompanhada. */
  @Column(name = "delivery_mode", length = 64)
  private String deliveryMode;

  /** Modelo de receita testado, como compra única, assinatura ou serviço recorrente. */
  @Column(name = "revenue_model", length = 64)
  private String revenueModel;

  /** Unidade de valor que o cliente recebe e consegue perceber ou usar. */
  @Column(name = "value_unit", length = 191)
  private String valueUnit;

  /** Evidência principal usada para confirmar valor após a compra e a entrega. */
  @Column(name = "value_evidence_metric", length = 191)
  private String valueEvidenceMetric;

  /** Versão do contrato modular de descoberta comercial aplicado ao produto. */
  @Column(name = "validation_definition_version", length = 32)
  private String validationDefinitionVersion;

  /** Contrato JSON com critérios comparáveis de continuar, ajustar, parar e escalar. */
  @Lob
  @Column(name = "validation_definition_json", columnDefinition = "LONGTEXT")
  private String validationDefinitionJson;

  /** Versão do mapa que liga o produto a estados desejados de forma auditável. */
  @Column(name = "desire_association_map_version", length = 32)
  private String desireAssociationMapVersion;

  /** Mapa JSON de territórios, cadeias causais, evidências e limites de promessa. */
  @Lob
  @Column(name = "desire_association_map_json", columnDefinition = "LONGTEXT")
  private String desireAssociationMapJson;

  /** Status comercial atual do produto. */
  @Column(name = "commercial_status", length = 64)
  private String commercialStatus;

  /** Define se novas execuções automáticas podem ser iniciadas para o produto. */
  @Builder.Default
  @Column(name = "automatic_execution_enabled", nullable = false)
  private Boolean automaticExecutionEnabled = true;

  /** Data da última alteração administrativa do controle PLAY/STOP. */
  @Column(name = "automatic_execution_changed_at")
  private Instant automaticExecutionChangedAt;

  /** Operador que realizou a última alteração do controle PLAY/STOP. */
  @Column(name = "automatic_execution_changed_by", length = 100)
  private String automaticExecutionChangedBy;

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

  /** Asset aprovado ou em revisão para ser a imagem semente canônica dos vídeos do produto. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "video_seed_image_asset_id")
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Asset videoSeedImageAsset;

  /** Nome comercial da personagem usada como rosto recorrente do produto. */
  @Column(name = "video_seed_character_name", length = 191)
  private String videoSeedCharacterName;

  /** Status de aprovação humana da imagem semente antes de virar avatar ou vídeo. */
  @Enumerated(EnumType.STRING)
  @Column(name = "video_seed_review_status", length = 32)
  private ProductVideoSeedImageReviewStatus videoSeedReviewStatus;

  /** Observações comerciais da aprovação ou reprovação da imagem semente. */
  @Lob
  @Column(name = "video_seed_review_notes", columnDefinition = "LONGTEXT")
  private String videoSeedReviewNotes;

  /** Pessoa que registrou a última revisão da imagem semente. */
  @Column(name = "video_seed_reviewed_by", length = 255)
  private String videoSeedReviewedBy;

  /** Data da última revisão humana da imagem semente. */
  @Column(name = "video_seed_reviewed_at")
  private Instant videoSeedReviewedAt;

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

  /**
   * Contrato JSON versionado que a PDE Platform consome para renderizar a experiência do produto.
   */
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

  @CreationTimestamp private Instant createdAt;

  @UpdateTimestamp private Instant updatedAt;
}
