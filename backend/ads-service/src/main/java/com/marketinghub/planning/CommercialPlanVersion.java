package com.marketinghub.planning;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: preservar uma versão imutável do contexto oficial de um plano comercial. */
@Entity
@Table(name = "commercial_plan_version")
@Getter
@Setter
public class CommercialPlanVersion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "commercial_plan_id", nullable = false)
  private CommercialPlan plan;

  @Column(name = "version_number", nullable = false)
  private Integer versionNumber;

  @Column(name = "snapshot_json", nullable = false, columnDefinition = "LONGTEXT")
  private String snapshotJson;

  @Column(name = "changed_by", nullable = false, length = 100)
  private String changedBy;

  @Column(name = "change_reason", nullable = false, length = 500)
  private String changeReason;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
