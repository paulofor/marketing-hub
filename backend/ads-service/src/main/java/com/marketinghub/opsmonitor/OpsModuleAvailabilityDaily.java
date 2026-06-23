package com.marketinghub.opsmonitor;

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
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Consolida a disponibilidade diária de um módulo para consultas administrativas rápidas. */
@Entity
@Table(name = "ops_module_availability_daily")
@Getter
@Setter
public class OpsModuleAvailabilityDaily {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private OpsMonitoredModule module;

    @Column(name = "availability_date", nullable = false)
    private LocalDate availabilityDate;

    @Column(name = "total_checks", nullable = false)
    private Integer totalChecks;

    @Column(name = "successful_checks", nullable = false)
    private Integer successfulChecks;

    @Column(name = "failed_checks", nullable = false)
    private Integer failedChecks;

    @Column(name = "availability_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal availabilityPercentage;

    @Column(name = "offline_seconds", nullable = false)
    private Long offlineSeconds;

    @Column(name = "degraded_seconds", nullable = false)
    private Long degradedSeconds;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
