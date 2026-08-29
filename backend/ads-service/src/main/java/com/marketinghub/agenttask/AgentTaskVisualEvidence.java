package com.marketinghub.agenttask;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Responsabilidade: representar um snapshot visual privado vinculado a uma tarefa de agente. */
@Entity
@Table(name = "agent_task_visual_evidence")
@Getter
@Setter
public class AgentTaskVisualEvidence {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "agent_task_id", nullable = false)
  private AgentTask task;

  @Column(name = "capture_session_id", nullable = false, length = 64)
  private String captureSessionId;

  @Column(name = "evidence_key", nullable = false, length = 160)
  private String evidenceKey;

  @Column(name = "evidence_type", nullable = false, length = 24)
  private String evidenceType;

  @Column(name = "device_profile", nullable = false, length = 32)
  private String deviceProfile;

  @Column(name = "page_number", nullable = false)
  private Integer pageNumber;

  @Column(name = "fold_number")
  private Integer foldNumber;

  @Column(name = "viewport_width", nullable = false)
  private Integer viewportWidth;

  @Column(name = "viewport_height", nullable = false)
  private Integer viewportHeight;

  @Column(name = "page_height_px", nullable = false)
  private Integer pageHeightPx;

  @Column(name = "scroll_y", nullable = false)
  private Integer scrollY;

  @Column(name = "source_url", nullable = false, length = 2048)
  private String sourceUrl;

  @Column(name = "final_url", nullable = false, length = 2048)
  private String finalUrl;

  @Column(name = "object_key", nullable = false, length = 1024)
  private String objectKey;

  @Column(name = "content_type", nullable = false, length = 100)
  private String contentType;

  @Column(name = "size_bytes", nullable = false)
  private Long sizeBytes;

  @Column(name = "sha256", nullable = false, length = 64)
  private String sha256;

  @Column(name = "captured_at", nullable = false)
  private Instant capturedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
