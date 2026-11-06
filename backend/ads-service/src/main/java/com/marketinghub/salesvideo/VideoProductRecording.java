package com.marketinghub.salesvideo;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

/**
 * Responsabilidade: preservar a gravação funcional privada e sua proveniência sem análise paga ou
 * aprovação automática.
 */
@Entity
@Table(name = "video_product_recording_v1")
@Data
@NoArgsConstructor
public class VideoProductRecording {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "video_project_id", nullable = false)
  private Long videoProjectId;

  @Column(name = "tenant_id", nullable = false, length = 64)
  private String tenantId;

  @Column(name = "sha256", nullable = false, length = 64)
  private String sha256;

  @Column(name = "source_version", nullable = false, length = 191)
  private String sourceVersion;

  @Column(name = "manifest_json", nullable = false, columnDefinition = "LONGTEXT")
  private String manifestJson;

  @Lob
  @Column(name = "video_bytes", nullable = false, columnDefinition = "LONGBLOB")
  private byte[] videoBytes;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
