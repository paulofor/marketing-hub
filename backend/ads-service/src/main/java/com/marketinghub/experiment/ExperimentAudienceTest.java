package com.marketinghub.experiment;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Guarda uma variação de público planejada para comparar hipóteses de segmentação em um
 * experimento.
 */
@Entity
@Table(name = "experiment_audience_test")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentAudienceTest {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "experiment_id", nullable = false)
  @ToString.Exclude
  private Experiment experiment;

  @Column(name = "name", nullable = false, length = 120)
  private String name;

  @Column(name = "hypothesis", nullable = false, length = 500)
  private String hypothesis;

  @Column(name = "success_metric", nullable = false, length = 191)
  private String successMetric;

  @Column(name = "daily_budget", precision = 10, scale = 2)
  private BigDecimal dailyBudget;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  @Builder.Default
  private ExperimentAudienceTestStatus status = ExperimentAudienceTestStatus.DRAFT;

  @Column(name = "created_at", nullable = false)
  @CreationTimestamp
  private Instant createdAt;

  @Column(name = "updated_at")
  @UpdateTimestamp
  private Instant updatedAt;

  @OneToMany(mappedBy = "audienceTest", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("candidateType ASC, term ASC")
  @Builder.Default
  @ToString.Exclude
  private List<ExperimentAudienceTestItem> items = new ArrayList<>();

  /** Substitui os itens preservando o vínculo bidirecional da entidade. */
  public void replaceItems(List<ExperimentAudienceTestItem> nextItems) {
    items.clear();
    if (nextItems == null) {
      return;
    }
    nextItems.forEach(
        item -> {
          item.setAudienceTest(this);
          items.add(item);
        });
  }
}
