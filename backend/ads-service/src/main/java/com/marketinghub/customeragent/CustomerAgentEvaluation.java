package com.marketinghub.customeragent;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: separar a avaliacao simulada do resultado humano observado. */
@Getter
@Setter
@Entity
@Table(name = "customer_agent_evaluation")
public class CustomerAgentEvaluation {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "persona_id")
  private CustomerPersona persona;

  @Column(name = "asset_type", nullable = false)
  private String assetType;

  @Column(name = "asset_reference", nullable = false)
  private String assetReference;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "simulated_assessment", columnDefinition = "LONGTEXT")
  private String simulatedAssessment;

  @Column(name = "hypothesis_json", columnDefinition = "LONGTEXT")
  private String hypothesisJson;

  @Column(name = "human_result_json", columnDefinition = "LONGTEXT")
  private String humanResultJson;

  @Column(name = "raw_model_response", columnDefinition = "LONGTEXT")
  private String rawModelResponse;

  @Column(name = "model_name")
  private String modelName;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "finished_at")
  private Instant finishedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** Inicializa a auditoria da avaliacao. */
  @PrePersist
  void initialize() {
    createdAt = Instant.now();
    updatedAt = createdAt;
  }

  /** Atualiza o horario da avaliacao. */
  @PreUpdate
  void updateTimestamp() {
    updatedAt = Instant.now();
  }
}
