package com.marketinghub.experiment.directcontact.v1;

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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: representar um contato humano consentido da amostra direta do experimento. */
@Entity
@Table(
    name = "experiment_direct_contact",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_experiment_direct_contact_fingerprint",
            columnNames = {"experiment_id", "contact_fingerprint"}))
@Getter
@Setter
public class ExperimentDirectContact {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "experiment_id", nullable = false)
  private Experiment experiment;

  @Column(name = "contact_fingerprint", nullable = false, length = 64)
  private String contactFingerprint;

  @Column(name = "consent_evidence_reference", nullable = false, length = 500)
  private String consentEvidenceReference;

  @Column(name = "consent_recorded_at", nullable = false)
  private Instant consentRecordedAt;

  @Column(name = "contacted_at", nullable = false)
  private Instant contactedAt;

  @Column(name = "audience_fit_confirmed", nullable = false)
  private boolean audienceFitConfirmed;

  @Column(name = "recorded_by", nullable = false, length = 100)
  private String recordedBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
