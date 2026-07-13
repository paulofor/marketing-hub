package com.marketinghub.experiment.salespagetype;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Responsabilidade: catalogar formatos comerciais de pagina de venda que podem ser testados em campanhas. */
@Entity
@Table(name = "sales_page_type")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesPageType {
    @Id
    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 191)
    private String name;

    @Column(name = "description", nullable = false, length = 1024)
    private String description;

    @Column(name = "commercial_mechanism", nullable = false, length = 1024)
    private String commercialMechanism;

    @Column(name = "lead_capture_strategy", nullable = false, length = 1024)
    private String leadCaptureStrategy;

    @Column(name = "digital_bait_delivery", nullable = false, length = 1024)
    private String digitalBaitDelivery;

    @Column(name = "default_for_ab_test", nullable = false)
    private boolean defaultForAbTest;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at")
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private Instant updatedAt;
}
