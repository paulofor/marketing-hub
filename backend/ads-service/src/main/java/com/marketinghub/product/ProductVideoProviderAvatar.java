package com.marketinghub.product;

import com.marketinghub.media.Asset;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Responsabilidade: registrar personagens de vídeo do produto cadastrados ou reutilizáveis por
 * provider.
 */
@Entity
@Table(name = "product_video_provider_avatar")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVideoProviderAvatar {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Produto dono do personagem recorrente para vídeos comerciais. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Product product;

  /** Imagem aprovada usada como fonte visual do personagem no provider. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "source_asset_id", nullable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Asset sourceAsset;

  /** Nome do provider de vídeo associado ao cadastro do personagem. */
  @Column(name = "provider", length = 64, nullable = false)
  private String provider;

  /** Nome comercial da personagem usado nas telas e prompts. */
  @Column(name = "character_name", length = 191, nullable = false)
  private String characterName;

  /** Identificador retornado pelo provider quando houver avatar reutilizável real. */
  @Column(name = "provider_avatar_id", length = 255)
  private String providerAvatarId;

  /** Grupo ou identidade retornada pelo provider quando houver. */
  @Column(name = "provider_avatar_group_id", length = 255)
  private String providerAvatarGroupId;

  /** Status atual informado pelo provider ou pela política operacional do Marketing Hub. */
  @Column(name = "provider_status", length = 64, nullable = false)
  private String providerStatus;

  /** URL pública da imagem fonte enviada ou usada como referência pelo provider. */
  @Column(name = "source_image_url", length = 1024, nullable = false)
  private String sourceImageUrl;

  /**
   * Indica se o provider retornou um avatar reutilizável por ID, não apenas imagem de referência.
   */
  @Column(name = "supports_reusable_avatar", nullable = false)
  private boolean supportsReusableAvatar;

  /** Observações comerciais e técnicas para uso seguro do personagem nos próximos vídeos. */
  @Lob
  @Column(name = "notes", columnDefinition = "LONGTEXT")
  private String notes;

  /** Data de criação do cadastro do personagem no Marketing Hub. */
  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** Data da última atualização do cadastro do personagem. */
  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
