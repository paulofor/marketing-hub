package com.marketinghub.socialdistribution;

import com.marketinghub.product.Product;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Responsabilidade: persistir a estratégia comercial de um ciclo de crescimento orgânico. */
@Entity
@Table(name = "social_growth_plan")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocialGrowthPlan {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Produto promovido pelo plano. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Product product;

  /** Nome operacional do ciclo editorial. */
  @Column(name = "name", nullable = false, length = 191)
  private String name;

  /** Público específico que o conteúdo pretende aquecer. */
  @Lob
  @Column(name = "audience", nullable = false, columnDefinition = "LONGTEXT")
  private String audience;

  /** Hipótese comercial testada pelo plano. */
  @Lob
  @Column(name = "commercial_hypothesis", nullable = false, columnDefinition = "LONGTEXT")
  private String commercialHypothesis;

  /** Objetivo comercial mensurável do ciclo. */
  @Lob
  @Column(name = "commercial_objective", nullable = false, columnDefinition = "LONGTEXT")
  private String commercialObjective;

  /** CTA principal que conecta conteúdo e próxima etapa do funil. */
  @Column(name = "primary_cta", nullable = false, length = 255)
  private String primaryCta;

  /** Destino oficial usado para gerar URLs rastreáveis. */
  @Column(name = "destination_url", nullable = false, length = 1024)
  private String destinationUrl;

  /** Identificador UTM estável do ciclo. */
  @Column(name = "utm_campaign", nullable = false, length = 191)
  private String utmCampaign;

  /** Estado editorial do plano. */
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private SocialGrowthPlanStatus status;

  /** Início planejado do ciclo. */
  @Column(name = "starts_on")
  private LocalDate startsOn;

  /** Fim planejado do ciclo. */
  @Column(name = "ends_on")
  private LocalDate endsOn;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
