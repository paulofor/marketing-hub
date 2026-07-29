package com.marketinghub.socialdistribution;

import jakarta.persistence.*;
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
