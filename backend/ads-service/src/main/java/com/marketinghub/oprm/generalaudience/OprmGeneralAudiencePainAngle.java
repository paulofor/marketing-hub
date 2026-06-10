package com.marketinghub.oprm.generalaudience;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

/** Responsável por armazenar uma dor e seu ângulo seguro antes de oferta, nicho ou campanha. */
@Entity
@Table(name = "oprm_general_audience_pain_angle")
public class OprmGeneralAudiencePainAngle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subniche_id", nullable = false)
    private OprmGeneralAudienceSubniche subniche;

    @Column(name = "pain", nullable = false, columnDefinition = "LONGTEXT")
    private String pain;

    @Column(name = "desired_result", nullable = false, columnDefinition = "LONGTEXT")
    private String desiredResult;

    @Column(name = "mechanism_direction", columnDefinition = "LONGTEXT")
    private String mechanismDirection;

    @Column(name = "proof_or_lead_magnet", columnDefinition = "LONGTEXT")
    private String proofOrLeadMagnet;

    @Column(name = "safe_promise", columnDefinition = "LONGTEXT")
    private String safePromise;

    @Column(name = "first_ad_hook", columnDefinition = "LONGTEXT")
    private String firstAdHook;

    @Column(name = "landing_confirmation_question", columnDefinition = "LONGTEXT")
    private String landingConfirmationQuestion;

    @Column(name = "compliance_notes", columnDefinition = "LONGTEXT")
    private String complianceNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OprmGeneralAudiencePainAngleStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Inicializa os carimbos de criação e atualização do ângulo. */
    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    /** Atualiza o carimbo de modificação do ângulo. */
    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    /** Retorna o identificador do ângulo. */
    public Long getId() { return id; }

    /** Define o identificador do ângulo. */
    public void setId(Long id) { this.id = id; }

    /** Retorna o subnicho dono da dor. */
    public OprmGeneralAudienceSubniche getSubniche() { return subniche; }

    /** Define o subnicho dono da dor. */
    public void setSubniche(OprmGeneralAudienceSubniche subniche) { this.subniche = subniche; }

    /** Retorna a dor observada no subnicho. */
    public String getPain() { return pain; }

    /** Define a dor observada no subnicho. */
    public void setPain(String pain) { this.pain = pain; }

    /** Retorna o resultado desejado pelo público. */
    public String getDesiredResult() { return desiredResult; }

    /** Define o resultado desejado pelo público. */
    public void setDesiredResult(String desiredResult) { this.desiredResult = desiredResult; }

    /** Retorna a direção de mecanismo plausível para resolver a dor. */
    public String getMechanismDirection() { return mechanismDirection; }

    /** Define a direção de mecanismo plausível para resolver a dor. */
    public void setMechanismDirection(String mechanismDirection) { this.mechanismDirection = mechanismDirection; }

    /** Retorna a prova ou isca sugerida para teste futuro. */
    public String getProofOrLeadMagnet() { return proofOrLeadMagnet; }

    /** Define a prova ou isca sugerida para teste futuro. */
    public void setProofOrLeadMagnet(String proofOrLeadMagnet) { this.proofOrLeadMagnet = proofOrLeadMagnet; }

    /** Retorna a promessa segura do ângulo. */
    public String getSafePromise() { return safePromise; }

    /** Define a promessa segura do ângulo. */
    public void setSafePromise(String safePromise) { this.safePromise = safePromise; }

    /** Retorna o primeiro gancho de anúncio sugerido. */
    public String getFirstAdHook() { return firstAdHook; }

    /** Define o primeiro gancho de anúncio sugerido. */
    public void setFirstAdHook(String firstAdHook) { this.firstAdHook = firstAdHook; }

    /** Retorna a pergunta de confirmação da landing. */
    public String getLandingConfirmationQuestion() { return landingConfirmationQuestion; }

    /** Define a pergunta de confirmação da landing. */
    public void setLandingConfirmationQuestion(String landingConfirmationQuestion) { this.landingConfirmationQuestion = landingConfirmationQuestion; }

    /** Retorna as notas de compliance do ângulo. */
    public String getComplianceNotes() { return complianceNotes; }

    /** Define as notas de compliance do ângulo. */
    public void setComplianceNotes(String complianceNotes) { this.complianceNotes = complianceNotes; }

    /** Retorna o status de revisão do ângulo. */
    public OprmGeneralAudiencePainAngleStatus getStatus() { return status; }

    /** Define o status de revisão do ângulo. */
    public void setStatus(OprmGeneralAudiencePainAngleStatus status) { this.status = status; }

    /** Retorna a data de criação do ângulo. */
    public Instant getCreatedAt() { return createdAt; }

    /** Define a data de criação do ângulo. */
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    /** Retorna a data da última atualização do ângulo. */
    public Instant getUpdatedAt() { return updatedAt; }

    /** Define a data da última atualização do ângulo. */
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
