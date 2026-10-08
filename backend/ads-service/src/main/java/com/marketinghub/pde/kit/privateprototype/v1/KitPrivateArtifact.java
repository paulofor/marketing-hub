package com.marketinghub.pde.kit.privateprototype.v1;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: preservar a composição privada, suas reservas e o artefato íntegro de QA. */
@Entity
@Table(name = "kit_private_artifact_v1")
@Getter
@Setter
public class KitPrivateArtifact {
  @Id
  @Column(name = "id", length = 36, columnDefinition = "CHAR(36)")
  private String id;

  @Column(name = "cycle_id", nullable = false)
  private Long cycleId;

  @Column(name = "product_id", nullable = false)
  private Long productId;

  @Column(name = "experiment_id", nullable = false)
  private Long experimentId;

  @Column(name = "prototype_version", nullable = false, length = 160)
  private String prototypeVersion;

  @Column(name = "profile_code", nullable = false, length = 24)
  private String profileCode;

  @Column(name = "fixture_owner", nullable = false, length = 24)
  private String fixtureOwner;

  @Column(name = "input_sha256", nullable = false, length = 64, columnDefinition = "CHAR(64)")
  private String inputSha256;

  @Column(name = "input_json", nullable = false, columnDefinition = "LONGTEXT")
  private String inputJson;

  @Column(name = "status", nullable = false, length = 24)
  private String status;

  @Column(name = "claim_key", length = 36, columnDefinition = "CHAR(36)")
  private String claimKey;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "finished_at")
  private Instant finishedAt;

  @Column(name = "zip_sha256", length = 64, columnDefinition = "CHAR(64)")
  private String zipSha256;

  @Column(name = "manifest_json", columnDefinition = "LONGTEXT")
  private String manifestJson;

  @Lob
  @Column(name = "zip_bytes", columnDefinition = "LONGBLOB")
  private byte[] zipBytes;
}
