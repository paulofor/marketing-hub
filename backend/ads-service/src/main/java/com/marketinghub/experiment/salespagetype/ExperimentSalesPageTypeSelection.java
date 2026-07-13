package com.marketinghub.experiment.salespagetype;

import com.marketinghub.experiment.Experiment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Responsabilidade: vincular um experimento aos tipos de pagina de venda escolhidos para teste. */
@Entity
@Table(name = "experiment_sales_page_type_selection")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentSalesPageTypeSelection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "experiment_id", nullable = false)
    @ToString.Exclude
    private Experiment experiment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_page_type_code", nullable = false)
    @ToString.Exclude
    private SalesPageType salesPageType;

    @Column(name = "variant_key", nullable = false, length = 16)
    private String variantKey;

    @Column(name = "traffic_weight", nullable = false, precision = 5, scale = 2)
    private BigDecimal trafficWeight;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "notes", length = 1024)
    private String notes;

    @Column(name = "created_at")
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private Instant updatedAt;
}
