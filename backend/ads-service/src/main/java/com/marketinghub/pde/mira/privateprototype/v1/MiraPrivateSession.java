package com.marketinghub.pde.mira.privateprototype.v1;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: persistir um pacote privado de Mira e sua auditoria segregada por ciclo. */
@Entity
@Table(name = "mira_private_session_v1")
@Getter
@Setter
public class MiraPrivateSession {
  @Id
  @Column(name = "id", length = 36)
  private String id;

  @Column(name = "cycle_id", nullable = false)
  private Long cycleId;

  @Column(name = "product_id", nullable = false)
  private Long productId;

  @Column(name = "experiment_id", nullable = false)
  private Long experimentId;

  @Column(name = "prototype_version", nullable = false, length = 160)
  private String prototypeVersion;

  @Column(name = "session_hash", nullable = false, length = 64, unique = true)
  private String sessionHash;

  @Column(name = "revoked", nullable = false)
  private boolean revoked;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "payload_json", nullable = false, columnDefinition = "LONGTEXT")
  private String payloadJson;
}
