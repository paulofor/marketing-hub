package com.marketinghub.winningadlibrary;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Responsabilidade: representar um anúncio vencedor reutilizável em campanhas comerciais. */
@Entity
@Table(name = "winning_ad")
@Getter
@Setter
@NoArgsConstructor
public class WinningAd {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Identificador comercial do produto associado ao anúncio. */
  @Column(name = "product_slug", nullable = false, length = 191)
  private String productSlug;

  /** Nome público do produto associado ao anúncio. */
  @Column(name = "product_name", nullable = false, length = 191)
  private String productName;

  /** Nicho ou público prioritário reconhecido pelo anúncio. */
  @Column(name = "niche", nullable = false, length = 255)
  private String niche;

  /** Etapa do funil em que o anúncio deve ser usado. */
  @Column(name = "funnel_stage", nullable = false, length = 64)
  private String funnelStage;

  /** Canal de aquisição recomendado para o anúncio. */
  @Column(name = "channel", nullable = false, length = 64)
  private String channel;

  /** Formato criativo recomendado para execução. */
  @Column(name = "format", nullable = false, length = 64)
  private String format;

  /** Status comercial do aprendizado dentro da biblioteca. */
  @Column(name = "winning_status", nullable = false, length = 64)
  private String winningStatus;

  /** Nota comercial de prioridade para reuso e teste. */
  @Column(name = "score", nullable = false)
  private Integer score;

  /** Gancho inicial do anúncio. */
  @Lob
  @Column(name = "hook", nullable = false, columnDefinition = "LONGTEXT")
  private String hook;

  /** Texto principal sugerido para o anúncio. */
  @Lob
  @Column(name = "primary_text", nullable = false, columnDefinition = "LONGTEXT")
  private String primaryText;

  /** Brief visual ou criativo para orientar imagem, vídeo ou carrossel. */
  @Lob
  @Column(name = "creative_brief", nullable = false, columnDefinition = "LONGTEXT")
  private String creativeBrief;

  /** Ângulo de oferta que o anúncio tenta validar. */
  @Lob
  @Column(name = "offer_angle", nullable = false, columnDefinition = "LONGTEXT")
  private String offerAngle;

  /** Sinal de prova usado para aumentar confiança e clique qualificado. */
  @Lob
  @Column(name = "proof_signal", nullable = false, columnDefinition = "LONGTEXT")
  private String proofSignal;

  /** Métrica observada ou alvo que justifica o status comercial do anúncio. */
  @Lob
  @Column(name = "metric_snapshot", nullable = false, columnDefinition = "LONGTEXT")
  private String metricSnapshot;

  /** Aprendizado comercial extraído para reuso em novas campanhas. */
  @Lob
  @Column(name = "learning", nullable = false, columnDefinition = "LONGTEXT")
  private String learning;

  /** Próxima ação recomendada para transformar o anúncio em teste comercial. */
  @Lob
  @Column(name = "next_action", nullable = false, columnDefinition = "LONGTEXT")
  private String nextAction;

  /** Referência de origem do insight usado no piloto. */
  @Column(name = "source_reference", nullable = false, length = 255)
  private String sourceReference;

  /** Data de criação do registro. */
  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  /** Data da última atualização do registro. */
  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
