package com.marketinghub.customeragent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: preservar um vetor motivacional auditavel sem alterar a evidencia original. */
@Getter
@Setter
@Entity
@Table(name = "customer_agent_memory_motivation")
public class CustomerAgentMemoryMotivation {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "persona_id", nullable = false)
  private CustomerPersona persona;

  @ManyToOne(optional = false)
  @JoinColumn(name = "observation_id", nullable = false)
  private CustomerDigitalObservation observation;

  @Column(name = "origin_type", nullable = false, length = 30)
  private String originType;

  @Column(name = "motivational_direction", nullable = false, length = 30)
  private String motivationalDirection;

  @Column(name = "pain_intensity", nullable = false)
  private Integer painIntensity;

  @Column(name = "pleasure_intensity", nullable = false)
  private Integer pleasureIntensity;

  @Column(name = "fear_weight", nullable = false)
  private Integer fearWeight;

  @Column(name = "frustration_weight", nullable = false)
  private Integer frustrationWeight;

  @Column(name = "effort_weight", nullable = false)
  private Integer effortWeight;

  @Column(name = "relief_weight", nullable = false)
  private Integer reliefWeight;

  @Column(name = "desire_weight", nullable = false)
  private Integer desireWeight;

  @Column(name = "trust_weight", nullable = false)
  private Integer trustWeight;

  @Column(name = "belonging_weight", nullable = false)
  private Integer belongingWeight;

  @Column(name = "evidence_strength", nullable = false)
  private Integer evidenceStrength;

  @Column(name = "confidence_score", nullable = false)
  private Integer confidenceScore;

  @Column(name = "source_reference", nullable = false, columnDefinition = "TEXT")
  private String sourceReference;

  @Column(name = "rationale", nullable = false, columnDefinition = "TEXT")
  private String rationale;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** Registra o horario do vetor sem aceitar sobrescrita pelo cliente. */
  @PrePersist
  void initialize() {
    createdAt = Instant.now();
  }
}
