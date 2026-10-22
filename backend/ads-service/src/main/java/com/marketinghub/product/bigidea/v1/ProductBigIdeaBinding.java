package com.marketinghub.product.bigidea.v1;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Fixa a revisão de Big Idea de um experimento ou plano para preservar suas passagens. */
@Entity
@Table(name = "product_big_idea_binding_v1")
@Getter
@Setter
public class ProductBigIdeaBinding {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "product_id", nullable = false)
  private Long productId;

  @Column(name = "source_reference", nullable = false, length = 200)
  private String sourceReference;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "big_idea_id", nullable = false)
  private ProductBigIdea bigIdea;
}
