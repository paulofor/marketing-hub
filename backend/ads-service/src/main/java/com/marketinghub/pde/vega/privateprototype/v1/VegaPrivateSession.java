package com.marketinghub.pde.vega.privateprototype.v1;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: persistir a leitura privada e seus sinais segregados por ciclo. */
@Entity
@Table(name = "vega_private_session_v1")
@Getter
@Setter
public class VegaPrivateSession {
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

  @Column(name = "origin", nullable = false, length = 32)
  private String origin;

  @Column(name = "reading_number")
  private Integer readingNumber;

  @Column(name = "grant_hash", length = 64, unique = true)
  private String grantHash;

  @Column(name = "session_hash", length = 64, unique = true)
  private String sessionHash;

  @Column(name = "revoked", nullable = false)
  private boolean revoked;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "consented_at")
  private Instant consentedAt;

  @Column(name = "input_json", columnDefinition = "LONGTEXT")
  private String inputJson;

  @Column(name = "events_json", nullable = false, columnDefinition = "LONGTEXT")
  private String eventsJson;

  @Column(name = "execution_id")
  private Long executionId;

  @Column(name = "state", nullable = false, length = 32)
  private String state;

  @Column(name = "preference", length = 32)
  private String preference;
}
