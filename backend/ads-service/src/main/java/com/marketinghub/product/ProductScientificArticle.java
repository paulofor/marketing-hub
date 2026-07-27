package com.marketinghub.product;

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
 * Responsabilidade: registrar artigos científicos usados para sustentar o mecanismo de um produto.
 */
@Entity
@Table(name = "product_scientific_article")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductScientificArticle {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Produto cujo mecanismo comercial usa o artigo como evidência de plausibilidade. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Product product;

  /** URL pública do artigo, DOI ou página editorial consultada. */
  @Column(name = "link", length = 1024, nullable = false)
  private String link;

  /** Hash SHA-256 do link usado para unicidade compatível com MySQL 5.7. */
  @Column(name = "link_hash", length = 64, nullable = false)
  private String linkHash;

  /** Título original do artigo conforme publicação. */
  @Column(name = "original_title", length = 512, nullable = false)
  private String originalTitle;

  /** Título traduzido para uso interno em português. */
  @Column(name = "portuguese_title", length = 512, nullable = false)
  private String portugueseTitle;

  /** Resumo operacional do artigo para leitura comercial rápida. */
  @Lob
  @Column(name = "summary", columnDefinition = "LONGTEXT", nullable = false)
  private String summary;

  /** Explicação de como o artigo sustenta a definição do mecanismo do produto. */
  @Lob
  @Column(name = "mechanism_application", columnDefinition = "LONGTEXT", nullable = false)
  private String mechanismApplication;

  /** Data de criação do cadastro da evidência científica. */
  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** Data da última atualização do cadastro da evidência científica. */
  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
