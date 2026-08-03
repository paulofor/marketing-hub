package com.marketinghub.payments.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Persiste a execução auditável da produção e entrega do kit Agenda Cheia. */
@Entity
@Table(name = "agenda_cheia_delivery")
public class AgendaCheiaDelivery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "briefing_id", nullable = false, unique = true)
    private Long briefingId;

    @Column(name = "payment_id", nullable = false, unique = true, length = 100)
    private String paymentId;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "stage_code", nullable = false, length = 80)
    private String stageCode;

    @Column(name = "artifact_path", length = 1200)
    private String artifactPath;

    @Column(name = "download_token", unique = true, length = 64)
    private String downloadToken;

    @Column(name = "manifest_json", columnDefinition = "LONGTEXT")
    private String manifestJson;

    @Column(name = "quality_score")
    private Integer qualityScore;

    @Column(name = "error_message", columnDefinition = "LONGTEXT")
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    /** Retorna o identificador da execução. */
    public Long getId() { return id; }
    /** Retorna o briefing de origem. */
    public Long getBriefingId() { return briefingId; }
    /** Define o briefing de origem. */
    public void setBriefingId(Long value) { briefingId = value; }
    /** Retorna o pagamento correlacionado. */
    public String getPaymentId() { return paymentId; }
    /** Define o pagamento correlacionado. */
    public void setPaymentId(String value) { paymentId = value; }
    /** Retorna o status funcional. */
    public String getStatus() { return status; }
    /** Define o status funcional. */
    public void setStatus(String value) { status = value; }
    /** Retorna a etapa atual. */
    public String getStageCode() { return stageCode; }
    /** Define a etapa atual. */
    public void setStageCode(String value) { stageCode = value; }
    /** Retorna o caminho privado do artefato. */
    public String getArtifactPath() { return artifactPath; }
    /** Define o caminho privado do artefato. */
    public void setArtifactPath(String value) { artifactPath = value; }
    /** Retorna o token público opaco. */
    public String getDownloadToken() { return downloadToken; }
    /** Define o token público opaco. */
    public void setDownloadToken(String value) { downloadToken = value; }
    /** Retorna o manifesto funcional. */
    public String getManifestJson() { return manifestJson; }
    /** Define o manifesto funcional. */
    public void setManifestJson(String value) { manifestJson = value; }
    /** Retorna a nota da revisão. */
    public Integer getQualityScore() { return qualityScore; }
    /** Define a nota da revisão. */
    public void setQualityScore(Integer value) { qualityScore = value; }
    /** Retorna a causa de falha. */
    public String getErrorMessage() { return errorMessage; }
    /** Define a causa de falha. */
    public void setErrorMessage(String value) { errorMessage = value; }
    /** Retorna o início. */
    public Instant getStartedAt() { return startedAt; }
    /** Define o início. */
    public void setStartedAt(Instant value) { startedAt = value; }
    /** Retorna a conclusão. */
    public Instant getFinishedAt() { return finishedAt; }
    /** Define a conclusão. */
    public void setFinishedAt(Instant value) { finishedAt = value; }
    /** Retorna o envio. */
    public Instant getDeliveredAt() { return deliveredAt; }
    /** Define o envio. */
    public void setDeliveredAt(Instant value) { deliveredAt = value; }
}
