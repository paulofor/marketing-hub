package com.marketinghub.planning;

import com.marketinghub.planning.imagestudio.v1.CommercialPlanVisualAssetReviewStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Responsabilidade: vincular uma referência audiovisual reutilizável e governada ao plano
 * comercial.
 */
@Entity
@Table(name = "commercial_plan_visual_asset")
@Getter
@Setter
@NoArgsConstructor
public class CommercialPlanVisualAsset {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "commercial_plan_id", nullable = false)
  private CommercialPlan commercialPlan;

  @Column(name = "asset_url", nullable = false, length = 2048)
  private String assetUrl;

  @Column(name = "media_type", nullable = false, length = 16)
  private String mediaType;

  @Column(name = "label", nullable = false, length = 191)
  private String label;

  @Column(name = "purpose", nullable = false, length = 64)
  private String purpose;

  /** Finalidades reutilizáveis do mesmo arquivo, preservadas sem duplicar a mídia. */
  @Column(name = "purposes_json", columnDefinition = "LONGTEXT")
  private String purposesJson;

  @Column(name = "origin", nullable = false, length = 191)
  private String origin;

  @Column(name = "rights_statement", nullable = false, length = 512)
  private String rightsStatement;

  /** SHA-256 calculado antes da importação para detectar qualquer troca posterior do arquivo. */
  @Column(name = "content_sha256", length = 64)
  private String contentSha256;

  /** Identidade do manifesto que mantém todas as peças e pareceres na mesma revisão. */
  @Column(name = "creative_package_id", length = 64)
  private String creativePackageId;

  @Column(name = "version_number", nullable = false)
  private Integer versionNumber;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private CommercialPlanVisualAssetStatus status;

  /** Asset anterior usado como base para uma edição não destrutiva. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "source_visual_asset_id")
  private CommercialPlanVisualAsset sourceVisualAsset;

  /** Estado da revisão independente obrigatória para imagens produzidas por Dédalo. */
  @Enumerated(EnumType.STRING)
  @Column(name = "agent_review_status", length = 24)
  private CommercialPlanVisualAssetReviewStatus agentReviewStatus;

  /** Horário da reserva usado para recuperar revisão interrompida sem ação manual. */
  @Column(name = "agent_review_started_at")
  private Instant agentReviewStartedAt;

  /** Execução de revisão, necessariamente diferente da execução que produziu a imagem. */
  @Column(name = "reviewer_execution_id", length = 64)
  private String reviewerExecutionId;

  /** Parecer funcional persistido para explicar aprovação ou necessidade de ajuste. */
  @Column(name = "agent_review_json", columnDefinition = "LONGTEXT")
  private String agentReviewJson;

  /** Request bruto da revisão independente. */
  @Column(name = "agent_review_request_json", columnDefinition = "LONGTEXT")
  private String agentReviewRequestJson;

  /** Response bruta da revisão independente. */
  @Column(name = "agent_review_response_json", columnDefinition = "LONGTEXT")
  private String agentReviewResponseJson;

  /** Estado da avaliação independente de percepção feita por Psique sobre o pacote. */
  @Enumerated(EnumType.STRING)
  @Column(name = "customer_review_status", length = 24)
  private CommercialPlanVisualAssetReviewStatus customerReviewStatus;

  /** Identidade da execução de Psique que avaliou o mesmo manifesto. */
  @Column(name = "customer_reviewer_execution_id", length = 64)
  private String customerReviewerExecutionId;

  /** Parecer bruto de Psique preservado junto do ativo selecionado. */
  @Column(name = "customer_review_json", columnDefinition = "LONGTEXT")
  private String customerReviewJson;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
