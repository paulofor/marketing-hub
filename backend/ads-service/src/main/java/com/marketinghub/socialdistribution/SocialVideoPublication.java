package com.marketinghub.socialdistribution;

import com.marketinghub.media.Asset;
import com.marketinghub.product.Product;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Responsabilidade: controlar uma publicação orgânica de vídeo na fila por rede social. */
@Entity
@Table(name = "social_video_publication")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialVideoPublication {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Produto comercial que reutiliza o vídeo como ativo orgânico. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Product product;

  /** Asset de vídeo renderizado que será distribuído. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "asset_id")
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Asset asset;

  /** Conta social de destino da publicação. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "social_account_id")
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private SocialAccount socialAccount;

  /** Rede social de destino. */
  @Enumerated(EnumType.STRING)
  @Column(name = "platform", length = 32, nullable = false)
  private SocialPlatform platform;

  /** Formato usado para reaproveitar o criativo na rede. */
  @Enumerated(EnumType.STRING)
  @Column(name = "video_format", length = 32, nullable = false)
  private SocialVideoFormat videoFormat;

  /** Status operacional da publicação. */
  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 32, nullable = false)
  private SocialVideoPublicationStatus status;

  /** Título usado em redes que suportam título, principalmente YouTube. */
  @Column(name = "title", length = 191, nullable = false)
  private String title;

  /** Legenda ou descrição publicada com o vídeo. */
  @Lob
  @Column(name = "caption", columnDefinition = "LONGTEXT")
  private String caption;

  /** Hashtags recomendadas em texto simples para a rede. */
  @Lob
  @Column(name = "hashtags", columnDefinition = "LONGTEXT")
  private String hashtags;

  /** URL pública do vídeo quando ainda não existe asset cadastrado. */
  @Column(name = "video_url", length = 1024)
  private String videoUrl;

  /** URL pública do post publicado. */
  @Column(name = "published_url", length = 1024)
  private String publishedUrl;

  /** Identificador externo retornado pela plataforma. */
  @Column(name = "external_post_id", length = 191)
  private String externalPostId;

  /** Explicação persistida quando a fila bloquear ou falhar. */
  @Lob
  @Column(name = "failure_reason", columnDefinition = "LONGTEXT")
  private String failureReason;

  /** Payload técnico estruturado para o worker de publicação. */
  @Lob
  @Column(name = "publish_payload_json", columnDefinition = "LONGTEXT")
  private String publishPayloadJson;

  /** Data desejada para publicar ou iniciar processamento. */
  @Column(name = "scheduled_at")
  private Instant scheduledAt;

  /** Data em que a publicação entrou na fila. */
  @Column(name = "queued_at")
  private Instant queuedAt;

  /** Data em que a rede confirmou publicação. */
  @Column(name = "published_at")
  private Instant publishedAt;

  @CreationTimestamp
  @Column(name = "created_at")
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private Instant updatedAt;
}
