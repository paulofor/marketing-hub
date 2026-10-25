package com.marketinghub.product.modeling.v1;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Preserva a intenção de modelagem e sua ligação imutável à pesquisa e ao produto. */
@Entity
@Table(name = "product_modeling_v1")
@Getter
@Setter
public class ProductModeling {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "request_key", nullable = false, unique = true, length = 36)
  private String requestKey;

  @Column(name = "product_id")
  private Long productId;

  @Column(name = "product_name", nullable = false, length = 191)
  private String productName;

  @Column(name = "desired_outcome", nullable = false, length = 2000)
  private String desiredOutcome;

  @Column(name = "research_scope", nullable = false, length = 32)
  private String researchScope = "MARKET_REFERENCES_V1";

  @Column(name = "references_json", nullable = false, columnDefinition = "LONGTEXT")
  private String referencesJson;

  @Column(name = "big_idea_snapshot", columnDefinition = "LONGTEXT")
  private String bigIdeaSnapshot;

  @Column(name = "discovery_cycle_id", unique = true)
  private Long discoveryCycleId;

  @Column(name = "learning_cycle_id", unique = true)
  private Long learningCycleId;

  @Column(name = "experiment_id", unique = true)
  private Long experimentId;

  @Column(name = "adopted_context_json", columnDefinition = "LONGTEXT")
  private String adoptedContextJson;

  @Column(name = "adopted_at")
  private Instant adoptedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
