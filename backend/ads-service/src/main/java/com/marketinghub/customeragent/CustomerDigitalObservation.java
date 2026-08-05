package com.marketinghub.customeragent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: persistir uma experiencia digital observacional auditavel de uma persona. */
@Getter
@Setter
@Entity
@Table(name = "customer_digital_observation")
public class CustomerDigitalObservation {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "persona_id", nullable = false)
  private CustomerPersona persona;

  @Column(name = "objective", nullable = false, columnDefinition = "TEXT")
  private String objective;

  @Column(name = "authorized_sources_json", nullable = false, columnDefinition = "LONGTEXT")
  private String authorizedSourcesJson;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "device_profile", nullable = false)
  private String deviceProfile;

  @Column(name = "observation_json", columnDefinition = "LONGTEXT")
  private String observationJson;

  @Column(name = "simulated_reaction_json", columnDefinition = "LONGTEXT")
  private String simulatedReactionJson;

  @Column(name = "commercial_hypothesis_json", columnDefinition = "LONGTEXT")
  private String commercialHypothesisJson;

  @Column(name = "human_confirmation_json", columnDefinition = "LONGTEXT")
  private String humanConfirmationJson;

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

  /** Inicializa o estado e os horarios da observacao. */
  @PrePersist
  void initialize() {
    Instant now = Instant.now();
    status = status == null ? "PENDING" : status;
    deviceProfile = deviceProfile == null ? "MOBILE" : deviceProfile;
    createdAt = now;
    updatedAt = now;
  }

  /** Atualiza o horario de auditoria. */
  @PreUpdate
  void updateTimestamp() {
    updatedAt = Instant.now();
  }
}
