package com.marketinghub.financialagent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/** Responsabilidade: registrar uma recarga pré-paga de créditos de um provedor do Estúdio. */
@Entity
@Table(
    name = "studio_provider_credit_purchase",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_studio_provider_credit_purchase",
            columnNames = {"provider", "purchased_at", "amount", "currency", "credits_purchased"}))
@Getter
@Setter
@NoArgsConstructor
public class StudioProviderCreditPurchase {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "provider", nullable = false, length = 64)
  private String provider;

  @Column(name = "purchased_at", nullable = false)
  private Instant purchasedAt;

  @Column(name = "amount", nullable = false, precision = 14, scale = 6)
  private BigDecimal amount;

  @Column(name = "currency", nullable = false, length = 3)
  private String currency;

  @Column(name = "credits_purchased", nullable = false)
  private Integer creditsPurchased;

  @Column(name = "evidence_reference", length = 500)
  private String evidenceReference;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
