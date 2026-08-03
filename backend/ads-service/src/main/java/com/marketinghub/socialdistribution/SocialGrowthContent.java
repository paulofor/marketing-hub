package com.marketinghub.socialdistribution;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Responsabilidade: persistir uma pauta rastreável dentro do plano de crescimento orgânico. */
@Entity
@Table(name = "social_growth_content")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocialGrowthContent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Plano ao qual a pauta pertence. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "plan_id", nullable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private SocialGrowthPlan plan;

  /** Publicação operacional criada após aprovação, quando existir. */
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "publication_id")
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private SocialVideoPublication publication;

  /** Tipo do conteúdo no encadeamento Short para vídeo longo. */
  @Enumerated(EnumType.STRING)
  @Column(name = "content_type", nullable = false, length = 32)
  private SocialGrowthContentType contentType;

  /** Pilar editorial que agrupa aprendizados. */
  @Column(name = "pillar", nullable = false, length = 191)
  private String pillar;

  /** Pauta ou promessa específica do conteúdo. */
  @Column(name = "topic", nullable = false, length = 255)
  private String topic;

  /** Etapa do funil atendida pelo conteúdo. */
  @Column(name = "funnel_stage", nullable = false, length = 64)
  private String funnelStage;

  /** CTA específico da pauta. */
  @Column(name = "cta", nullable = false, length = 255)
  private String cta;

  /** Código único usado como utm_content. */
  @Column(name = "tracking_code", nullable = false, unique = true, length = 191)
  private String trackingCode;

  /** URL final com UTMs gerada pelo backend. */
  @Column(name = "tracking_url", nullable = false, length = 2048)
  private String trackingUrl;

  /** Estado de aprovação humana da pauta. */
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private SocialGrowthContentStatus status;

  /** Data editorial planejada. */
  @Column(name = "planned_at")
  private Instant plannedAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
