package com.marketinghub.socialdistribution;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

/** Responsabilidade: registrar métricas posteriores de uma publicação orgânica. */
@Entity
@Table(name = "social_publication_metric")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialPublicationMetric {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Publicação à qual a leitura de métrica pertence. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "publication_id", nullable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private SocialVideoPublication publication;

  /** Visualizações lidas na plataforma. */
  @Column(name = "views")
  private Long views;

  /** Visualizações que superaram o critério de engajamento da plataforma. */
  @Column(name = "engaged_views")
  private Long engagedViews;

  /** Duração média assistida em segundos. */
  @Column(name = "average_view_duration_seconds", precision = 12, scale = 2)
  private BigDecimal averageViewDurationSeconds;

  /** Espectadores recorrentes atribuídos ao conteúdo. */
  @Column(name = "recurring_viewers")
  private Long recurringViewers;

  /** Inscritos conquistados no intervalo da leitura. */
  @Column(name = "subscribers_gained")
  private Long subscribersGained;

  /** Curtidas lidas na plataforma. */
  @Column(name = "likes")
  private Long likes;

  /** Comentários lidos na plataforma. */
  @Column(name = "comments")
  private Long comments;

  /** Compartilhamentos lidos na plataforma. */
  @Column(name = "shares")
  private Long shares;

  /** Cliques atribuídos ao link do produto quando disponíveis. */
  @Column(name = "clicks")
  private Long clicks;

  /** Sessões rastreadas na landing pela URL do conteúdo. */
  @Column(name = "landing_sessions")
  private Long landingSessions;

  /** Leads capturados e atribuídos ao conteúdo. */
  @Column(name = "leads")
  private Long leads;

  /** Checkouts iniciados e atribuídos ao conteúdo. */
  @Column(name = "checkouts_started")
  private Long checkoutsStarted;

  /** Vendas aprovadas e atribuídas ao conteúdo. */
  @Column(name = "sales_approved")
  private Long salesApproved;

  /** Receita aprovada e atribuída ao conteúdo. */
  @Column(name = "revenue", precision = 15, scale = 2)
  private BigDecimal revenue;

  /** Payload bruto da leitura de métricas para auditoria. */
  @Lob
  @Column(name = "raw_payload_json", columnDefinition = "LONGTEXT")
  private String rawPayloadJson;

  /** Data da leitura da plataforma. */
  @Column(name = "captured_at", nullable = false)
  private Instant capturedAt;

  @CreationTimestamp
  @Column(name = "created_at")
  private Instant createdAt;
}
