package com.marketinghub.creative;

import com.marketinghub.creative.label.Angle;
import com.marketinghub.creative.label.EmotionalTrigger;
import com.marketinghub.creative.label.VisualProof;
import com.marketinghub.experiment.Experiment;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.*;

/** Responsabilidade: representar um criativo vinculado a um experimento. */
@Entity
@Table(name = "creative")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Creative {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Criativo anterior que originou esta revisão, quando existir. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "source_creative_id")
  private Creative sourceCreative;

  /** Número sequencial da versão dentro da linhagem do criativo. */
  @Column(name = "version_number", nullable = false)
  @Builder.Default
  private Integer versionNumber = 1;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "experiment_id", nullable = false)
  private Experiment experiment;

  private String headline;

  @Column(name = "primary_text")
  private String primaryText;

  @Column(name = "image_url")
  private String imageUrl;

  /** Hash da imagem hospedada no Facebook. */
  @Column(name = "image_hash")
  private String imageHash;

  /** ID de um vídeo já enviado para o Facebook. */
  @Column(name = "video_id")
  private String videoId;

  /** URL pública do vídeo que será enviado para a Meta pelo worker. */
  @Column(name = "video_url")
  private String videoUrl;

  /** Custo em USD da produção do criativo em vídeo, inclusive quando for reprovado. */
  @Column(name = "cost_usd", precision = 12, scale = 4)
  private BigDecimal costUsd;

  @Column(name = "ad_format")
  private String format;

  @Column(name = "description")
  private String description;

  @Column(name = "call_to_action", length = 32)
  private String cta;

  @Column(name = "destination_url")
  private String destinationUrl;

  @Column(name = "lead_gen_form_id")
  private String leadGenFormId;

  @Column(name = "instagram_user_id")
  private String instagramUserId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status")
  private CreativeStatus status;

  /** Motivo comercial informado quando o criativo é reprovado na revisão humana. */
  @Column(name = "rejection_reason", columnDefinition = "LONGTEXT")
  private String rejectionReason;

  /** Data e hora da última aprovação ou reprovação humana do criativo. */
  @Column(name = "reviewed_at")
  private Instant reviewedAt;

  /** Estado da avaliação multimodal obrigatória anterior à aprovação humana. */
  @Enumerated(EnumType.STRING)
  @Column(name = "agent_review_status", length = 24)
  private CreativeAgentReviewStatus agentReviewStatus;

  /** Notas e parecer funcional produzidos pelo agente especialista. */
  @Column(name = "agent_review_json", columnDefinition = "LONGTEXT")
  private String agentReviewJson;

  /** Request bruto enviado ao modelo para auditoria. */
  @Column(name = "agent_review_request_json", columnDefinition = "LONGTEXT")
  private String agentReviewRequestJson;

  /** Response bruto recebido do modelo para auditoria. */
  @Column(name = "agent_review_response_json", columnDefinition = "LONGTEXT")
  private String agentReviewResponseJson;

  /** Modelo multimodal usado na avaliação. */
  @Column(name = "agent_review_model", length = 100)
  private String agentReviewModel;

  /** Momento de conclusão da avaliação do agente. */
  @Column(name = "agent_reviewed_at")
  private Instant agentReviewedAt;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "creative_angle",
      joinColumns = @JoinColumn(name = "creative_id"),
      inverseJoinColumns = @JoinColumn(name = "angle_id"))
  private java.util.Set<Angle> angles = new java.util.HashSet<>();

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "creative_visual_proof",
      joinColumns = @JoinColumn(name = "creative_id"),
      inverseJoinColumns = @JoinColumn(name = "proof_id"))
  private java.util.Set<VisualProof> visualProofs = new java.util.HashSet<>();

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "creative_emotional_trigger",
      joinColumns = @JoinColumn(name = "creative_id"),
      inverseJoinColumns = @JoinColumn(name = "trigger_id"))
  private java.util.Set<EmotionalTrigger> emotionalTriggers = new java.util.HashSet<>();
}
