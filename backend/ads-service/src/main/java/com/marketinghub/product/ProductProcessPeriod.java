package com.marketinghub.product;

import com.marketinghub.businessprocess.BusinessProcessDefinition;
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

/** Responsabilidade: preservar a permanência auditável de um produto em um macroprocesso. */
@Entity
@Table(name = "product_process_period")
@Getter
@Setter
public class ProductProcessPeriod {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "process_definition_id", nullable = false)
  private BusinessProcessDefinition processDefinition;

  @Column(name = "process_code_snapshot", nullable = false, length = 100)
  private String processCodeSnapshot;

  @Column(name = "process_name_snapshot", nullable = false, length = 160)
  private String processNameSnapshot;

  @Column(name = "sequence_number", nullable = false)
  private Integer sequenceNumber;

  @Column(name = "entered_at", nullable = false)
  private Instant enteredAt;

  @Column(name = "exited_at")
  private Instant exitedAt;

  @Column(name = "entry_evidence", nullable = false, length = 40)
  private String entryEvidence;

  @Column(name = "exit_evidence", length = 40)
  private String exitEvidence;

  @Column(name = "objective_achieved", nullable = false)
  private boolean objectiveAchieved;

  @Column(name = "open_slot")
  private Integer openSlot;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
