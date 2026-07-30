package com.marketinghub.pde;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Responsabilidade: representar uma URL produtiva versionada de PDE para testes comerciais
 * paralelos.
 */
@Entity
@Table(name = "pde_production_slot")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PdeProductionSlot {

  private static final String DEFAULT_LAYOUT_KEY = "video-explicativo";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Código curto usado para identificar o slot na operação comercial. */
  @Column(name = "slot_code", nullable = false, length = 64)
  private String slotCode;

  /** Produto PDE publicado no slot. */
  @Column(name = "product_slug", nullable = false, length = 191)
  private String productSlug;

  /** Domínio público do slot, sem protocolo. */
  @Column(name = "domain", nullable = false, length = 191)
  private String domain;

  /** URL pública usada em anúncios e revisões comerciais. */
  @Column(name = "public_url", nullable = false, length = 512)
  private String publicUrl;

  /** URL administrativa do backend PDE quando houver instância dedicada ao slot. */
  @Column(name = "backend_url", length = 512)
  private String backendUrl;

  /** Versão comercial da experiência PDE servida pelo slot. */
  @Column(name = "experience_version", nullable = false, length = 120)
  private String experienceVersion;

  /** Chave do layout público usado para renderizar a versão sem herdar outra URL. */
  @Column(name = "layout_key", nullable = false, length = 80)
  private String layoutKey;

  /** Ambiente alvo usado pelo pipeline oficial de publicação. */
  @Column(name = "target_environment", nullable = false, length = 64)
  private String targetEnvironment;

  /** Status operacional do slot no Marketing Hub. */
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private PdeProductionSlotStatus status;

  /** Experimento que originou ou controla o teste comercial deste slot. */
  @Column(name = "source_experiment_id")
  private Long sourceExperimentId;

  /** Observações comerciais e operacionais do slot. */
  @Column(name = "notes", columnDefinition = "LONGTEXT")
  private String notes;

  /** Contrato PDE em rascunho editado pelo Marketing Hub para este slot. */
  @Column(name = "draft_experience_json", columnDefinition = "LONGTEXT")
  private String draftExperienceJson;

  /** Contrato PDE publicado e consumido pela URL versionada deste slot. */
  @Column(name = "published_experience_json", columnDefinition = "LONGTEXT")
  private String publishedExperienceJson;

  /** Usuário operacional que publicou o último contrato do slot. */
  @Column(name = "published_by", length = 191)
  private String publishedBy;

  /** Data da última publicação de contrato comercial deste slot. */
  @Column(name = "published_at")
  private Instant publishedAt;

  /** Status da última validação real da URL pública do slot. */
  @Column(name = "validation_status", length = 32)
  private String validationStatus;

  /** Data em que a URL pública foi validada pela última vez. */
  @Column(name = "validation_checked_at")
  private Instant validationCheckedAt;

  /** Código HTTP principal observado na última validação da URL pública. */
  @Column(name = "validation_http_status")
  private Integer validationHttpStatus;

  /** Resumo curto da última validação operacional do slot. */
  @Column(name = "validation_summary", length = 512)
  private String validationSummary;

  /** Detalhe técnico ou comercial da última validação operacional do slot. */
  @Column(name = "validation_detail", columnDefinition = "LONGTEXT")
  private String validationDetail;

  /** Slug informado pelo contrato público do PDE durante a última validação. */
  @Column(name = "validation_contract_slug", length = 191)
  private String validationContractSlug;

  /** Caminho público validado como entrada do funil na última validação. */
  @Column(name = "validation_contract_health_path", length = 191)
  private String validationContractHealthPath;

  /** URL final acessada na última validação após normalização do contrato. */
  @Column(name = "validation_resolved_url", length = 512)
  private String validationResolvedUrl;

  /** Data em que o slot foi criado no Marketing Hub. */
  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** Data da última alteração do slot no Marketing Hub. */
  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** Garante o layout público padrão antes de persistir slots criados fora do serviço canônico. */
  @PrePersist
  void ensureLayoutKey() {
    if (layoutKey == null || layoutKey.isBlank()) {
      layoutKey = DEFAULT_LAYOUT_KEY;
    }
  }
}
