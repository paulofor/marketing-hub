package com.marketinghub.product;

import com.marketinghub.media.Asset;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Responsabilidade: vincular imagens geradas ao produto para uso exclusivo em vídeos comerciais.
 */
@Entity
@Table(name = "product_video_image")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVideoImage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Produto dono da galeria de imagens para vídeo. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Product product;

  /** Asset de imagem pronto para ser usado como referência visual de vídeo. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "asset_id", nullable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Asset asset;

  /** Objetivo comercial da imagem dentro do fluxo de vídeo do produto. */
  @Column(name = "purpose", length = 64, nullable = false)
  private String purpose;

  /** Prompt original informado na tela para gerar a imagem. */
  @Lob
  @Column(name = "prompt", columnDefinition = "LONGTEXT", nullable = false)
  private String prompt;

  /** Status de revisão operacional da imagem antes de virar referência de vídeo. */
  @Enumerated(EnumType.STRING)
  @Column(name = "review_status", length = 32, nullable = false)
  private ProductVideoSeedImageReviewStatus reviewStatus;

  /** Observações comerciais da revisão da imagem. */
  @Lob
  @Column(name = "review_notes", columnDefinition = "LONGTEXT")
  private String reviewNotes;

  /** Data em que a imagem foi vinculada à galeria do produto. */
  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
