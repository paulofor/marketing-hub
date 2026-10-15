package com.marketinghub.agenttask.audioreview.v1;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Preserva a chamada auditiva e sua identidade antes de permitir consumo ou aceite. */
@Entity
@Table(name = "agent_audio_review")
@Getter
@Setter
public class AgentAudioReview {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "task_id", nullable = false)
  private Long taskId;

  @Column(name = "asset_id", nullable = false)
  private Long assetId;

  @Column(name = "product_id", nullable = false)
  private Long productId;

  @Column(name = "experiment_id", nullable = false)
  private Long experimentId;

  @Column(name = "video_sha256", length = 64, nullable = false)
  private String videoSha256;

  @Column(name = "audio_sha256", length = 64, nullable = false)
  private String audioSha256;

  @Column(name = "request_hash", length = 64, nullable = false)
  private String requestHash;

  @Column(name = "request_json", columnDefinition = "LONGTEXT", nullable = false)
  private String requestJson;

  @Column(name = "response_json", columnDefinition = "LONGTEXT")
  private String responseJson;

  @Column(name = "result_json", columnDefinition = "LONGTEXT")
  private String resultJson;

  @Column(name = "status", length = 32, nullable = false)
  private String status;

  @Column(name = "error_message", length = 1000)
  private String errorMessage;

  @Column(name = "reserved_usd", precision = 14, scale = 6, nullable = false)
  private BigDecimal reservedUsd;

  @Column(name = "estimated_usd", precision = 14, scale = 6)
  private BigDecimal estimatedUsd;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "finished_at")
  private Instant finishedAt;
}
