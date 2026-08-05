package com.marketinghub.imagegenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Responsabilidade: registrar a auditoria de uma geração manual de imagem por IA no Marketing Hub.
 */
@Entity
@Table(name = "image_generation_request")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageGenerationRequest {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "product_id", nullable = false)
  private Long productId;

  @Column(name = "commercial_plan_id", nullable = false)
  private Long commercialPlanId;

  @Column(name = "experiment_id")
  private Long experimentId;

  @Column(name = "job_id", nullable = false, length = 96, unique = true)
  private String jobId;

  @Column(name = "status", nullable = false, length = 32)
  private String status;

  @Column(name = "model", nullable = false, length = 128)
  private String model;

  @Column(name = "service_tier", nullable = false, length = 32)
  private String serviceTier;

  @Column(name = "output_format", nullable = false, length = 20)
  private String outputFormat;

  @Lob
  @Column(name = "prompt", nullable = false, columnDefinition = "LONGTEXT")
  private String prompt;

  @Lob
  @Column(name = "openai_request_body", columnDefinition = "LONGTEXT")
  private String openAiRequestBody;

  @Lob
  @Column(name = "openai_response_body", columnDefinition = "LONGTEXT")
  private String openAiResponseBody;

  @Lob
  @Column(name = "error_message", columnDefinition = "LONGTEXT")
  private String errorMessage;

  @Column(name = "openai_response_id", length = 128)
  private String openAiResponseId;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "finished_at")
  private Instant finishedAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
