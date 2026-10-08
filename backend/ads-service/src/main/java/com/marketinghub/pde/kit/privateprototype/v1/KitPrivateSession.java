package com.marketinghub.pde.kit.privateprototype.v1;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: persistir acesso e ações de QA de um kit no contexto exato do ciclo. */
@Entity
@Table(name = "kit_private_session_v1")
@Getter
@Setter
public class KitPrivateSession {
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

  @Column(name = "request_key", nullable = false, length = 36, columnDefinition = "CHAR(36)")
  private String requestKey;

  @Column(name = "session_hash", nullable = false, length = 64, columnDefinition = "CHAR(64)")
  private String sessionHash;

  @Column(name = "scenario_code", nullable = false, length = 24)
  private String scenarioCode;

  @Column(name = "device_profile", nullable = false, length = 24)
  private String deviceProfile;

  @Column(name = "artifact_id", length = 36, columnDefinition = "CHAR(36)")
  private String artifactId;

  @Column(name = "revoked", nullable = false)
  private boolean revoked;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "payload_json", nullable = false, columnDefinition = "LONGTEXT")
  private String payloadJson;
}
