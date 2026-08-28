package com.marketinghub.planning.imagestudio.v1;

import com.marketinghub.creative.Creative;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.CommercialPlanVisualAsset;
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
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Responsabilidade: persistir uma criação ou edição de imagem materializada por Dédalo. */
@Entity
@Table(name = "commercial_plan_image_studio_job")
@Getter
@Setter
@NoArgsConstructor
public class CommercialPlanImageStudioJob {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "commercial_plan_id", nullable = false)
  private CommercialPlan commercialPlan;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "source_visual_asset_id")
  private CommercialPlanVisualAsset sourceVisualAsset;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "result_visual_asset_id")
  private CommercialPlanVisualAsset resultVisualAsset;

  /**
   * Criativo que originou o retrabalho, quando a imagem ainda precisa voltar ao gate de anúncio.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "source_creative_id")
  private Creative sourceCreative;

  @Enumerated(EnumType.STRING)
  @Column(name = "operation", nullable = false, length = 16)
  private CommercialPlanImageStudioOperation operation;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 24)
  private CommercialPlanImageStudioStatus status;

  @Column(name = "label", nullable = false, length = 191)
  private String label;

  @Column(name = "prompt", nullable = false, columnDefinition = "LONGTEXT")
  private String prompt;

  @Column(name = "purposes_json", nullable = false, columnDefinition = "LONGTEXT")
  private String purposesJson;

  @Column(name = "reference_asset_ids_json", columnDefinition = "LONGTEXT")
  private String referenceAssetIdsJson;

  @Column(name = "size", nullable = false, length = 32)
  private String size;

  @Column(name = "quality", nullable = false, length = 16)
  private String quality;

  @Column(name = "model", length = 100)
  private String model;

  /** Versão do playbook governado congelada antes do consumo do modelo visual. */
  @Column(name = "playbook_version", length = 80)
  private String playbookVersion;

  /** Contexto segregado usado para recuperar somente aprendizados compatíveis. */
  @Column(name = "playbook_context_key", length = 120)
  private String playbookContextKey;

  /** Snapshot do playbook e dos exemplos aprovados efetivamente entregues ao executor. */
  @Column(name = "playbook_json", columnDefinition = "LONGTEXT")
  private String playbookJson;

  @Column(name = "producer_execution_id", length = 64)
  private String producerExecutionId;

  @Column(name = "request_json", columnDefinition = "LONGTEXT")
  private String requestJson;

  @Column(name = "response_json", columnDefinition = "LONGTEXT")
  private String responseJson;

  @Column(name = "usage_json", columnDefinition = "LONGTEXT")
  private String usageJson;

  @Column(name = "cost_usd", precision = 12, scale = 4)
  private BigDecimal costUsd;

  @Column(name = "error", columnDefinition = "LONGTEXT")
  private String error;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "finished_at")
  private Instant finishedAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
