package com.marketinghub.microservice;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Representa os dados físicos, financeiros e operacionais editáveis de um host VPS. */
@Entity
@Table(name = "ops_vps_host_inventory")
@Getter
@Setter
public class VpsHostInventory {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "host", nullable = false, unique = true, length = 120)
  private String host;

  @Column(name = "provider_name", length = 180)
  private String providerName;

  @Column(name = "provider_evidence", length = 1000)
  private String providerEvidence;

  @Column(name = "cpu", length = 120)
  private String cpu;

  @Column(name = "memory_gb")
  private Integer memoryGb;

  @Column(name = "disk_gb")
  private Integer diskGb;

  @Column(name = "operating_system", length = 180)
  private String operatingSystem;

  @Column(name = "monthly_cost_brl", precision = 10, scale = 2)
  private BigDecimal monthlyCostBrl;

  @Column(name = "billing_cycle", length = 120)
  private String billingCycle;

  @Column(name = "cost_evidence", length = 1000)
  private String costEvidence;

  @Column(name = "physical_specs_evidence", length = 1000)
  private String physicalSpecsEvidence;

  @Column(name = "notes", length = 1500)
  private String notes;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
