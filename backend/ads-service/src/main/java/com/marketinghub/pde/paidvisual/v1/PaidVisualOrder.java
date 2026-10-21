package com.marketinghub.pde.paidvisual.v1;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: preservar compra, direito de acesso e tentativas da entrega visual paga. */
@Entity
@Table(name = "pde_paid_visual_order")
@Getter
@Setter
public class PaidVisualOrder {
  @Id
  @Column(name = "payment_id", length = 80)
  private String paymentId;

  @Column(name = "product_id", nullable = false)
  private Long productId;

  @Column(name = "experiment_id", nullable = false)
  private Long experimentId;

  @Column(name = "product_slug", nullable = false, length = 191)
  private String productSlug;

  @Column(name = "experience_version", nullable = false, length = 120)
  private String experienceVersion;

  @Column(name = "buyer_email", nullable = false, length = 254)
  private String buyerEmail;

  @Column(name = "amount_brl", nullable = false, precision = 12, scale = 2)
  private BigDecimal amountBrl;

  @Column(name = "payment_status", nullable = false, length = 32)
  private String paymentStatus;

  @Column(name = "paid_at", nullable = false)
  private Instant paidAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "access_hash", length = 64, unique = true)
  private String accessHash;

  @Column(name = "last_email_at")
  private Instant lastEmailAt;

  @Column(name = "initial_job_id", length = 96)
  private String initialJobId;

  @Column(name = "regeneration_job_id", length = 96)
  private String regenerationJobId;

  @Column(name = "contract_json", nullable = false, columnDefinition = "LONGTEXT")
  private String contractJson;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
