package com.marketinghub.product.bigidea.v1;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Representa uma revisão imutável da ideia central que orienta um produto. */
@Entity
@Table(name = "product_big_idea_v1")
@Getter
@Setter
public class ProductBigIdea {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "product_id", nullable = false)
  private Long productId;

  @Column(name = "revision", nullable = false)
  private int revision;

  @Column(name = "central_idea", nullable = false, length = 500)
  private String centralIdea;

  @Column(name = "content_json", nullable = false, columnDefinition = "LONGTEXT")
  private String contentJson;

  @Column(name = "change_reason", nullable = false, length = 2000)
  private String changeReason;

  @Column(name = "created_by", nullable = false, length = 191)
  private String createdBy;

  @Column(name = "source_task_id")
  private Long sourceTaskId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
