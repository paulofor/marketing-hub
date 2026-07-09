package com.marketinghub.experiment.salespageab;

import com.marketinghub.experiment.Experiment;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Responsabilidade: guardar o plano de teste A/B de pagina de venda de um experimento. */
@Entity
@Table(name = "experiment_sales_page_ab_test")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentSalesPageAbTest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "experiment_id", nullable = false)
    @ToString.Exclude
    private Experiment experiment;

    @Column(name = "name", nullable = false, length = 191)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ExperimentSalesPageAbTestStatus status;

    @Column(name = "hypothesis", nullable = false, length = 1024)
    private String hypothesis;

    @Column(name = "primary_metric", nullable = false, length = 191)
    private String primaryMetric;

    @Column(name = "secondary_metrics", length = 512)
    private String secondaryMetrics;

    @Column(name = "winner_rule", nullable = false, length = 1024)
    private String winnerRule;

    @Column(name = "minimum_runtime_days", nullable = false)
    private Integer minimumRuntimeDays;

    @Column(name = "minimum_sample_size", nullable = false)
    private Integer minimumSampleSize;

    @Column(name = "meta_split_test_recommended", nullable = false)
    private boolean metaSplitTestRecommended;

    @Column(name = "notes", columnDefinition = "LONGTEXT")
    private String notes;

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<ExperimentSalesPageAbVariant> variants = new ArrayList<>();

    @Column(name = "created_at")
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private Instant updatedAt;
}
