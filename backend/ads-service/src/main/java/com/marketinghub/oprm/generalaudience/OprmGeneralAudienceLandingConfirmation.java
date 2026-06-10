package com.marketinghub.oprm.generalaudience;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** Responsável por armazenar a situação de confirmação preparada pelo OPRM para uso posterior por outros módulos. */
@Entity
@Table(name = "oprm_general_audience_landing_confirmation")
public class OprmGeneralAudienceLandingConfirmation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pain_angle_id", nullable = false)
    private OprmGeneralAudiencePainAngle painAngle;

    @Column(name = "experiment_id")
    private Long experimentId;

    @Column(name = "market_niche_id", nullable = false)
    private Long marketNicheId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "slug", nullable = false, length = 255)
    private String slug;

    @Column(name = "audience_summary", nullable = false, columnDefinition = "LONGTEXT")
    private String audienceSummary;

    @Column(name = "pain_summary", nullable = false, columnDefinition = "LONGTEXT")
    private String painSummary;

    @Column(name = "audience_confirmation_question", nullable = false, columnDefinition = "LONGTEXT")
    private String audienceConfirmationQuestion;

    @Column(name = "qualification_options", nullable = false, columnDefinition = "LONGTEXT")
    private String qualificationOptions;

    @Column(name = "pain_confirmation_question", nullable = false, columnDefinition = "LONGTEXT")
    private String painConfirmationQuestion;

    @Column(name = "delivery_description", nullable = false, columnDefinition = "LONGTEXT")
    private String deliveryDescription;

    @Column(name = "why_it_makes_sense", nullable = false, columnDefinition = "LONGTEXT")
    private String whyItMakesSense;

    @Column(name = "next_step", nullable = false, columnDefinition = "LONGTEXT")
    private String nextStep;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Inicializa os carimbos de criação e atualização do registro de confirmação. */
    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    /** Atualiza o carimbo de modificação do registro de confirmação. */
    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    /** Retorna o identificador do registro. */
    public Long getId() { return id; }

    /** Define o identificador do registro. */
    public void setId(Long id) { this.id = id; }

    /** Retorna o ângulo de dor relacionado. */
    public OprmGeneralAudiencePainAngle getPainAngle() { return painAngle; }

    /** Define o ângulo de dor relacionado. */
    public void setPainAngle(OprmGeneralAudiencePainAngle painAngle) { this.painAngle = painAngle; }

    /** Retorna o identificador externo do experimento informado, sem depender do módulo de experimentos. */
    public Long getExperimentId() { return experimentId; }

    /** Define o identificador externo do experimento informado. */
    public void setExperimentId(Long experimentId) { this.experimentId = experimentId; }

    /** Retorna o identificador do MarketNiche convertido. */
    public Long getMarketNicheId() { return marketNicheId; }

    /** Define o identificador do MarketNiche convertido. */
    public void setMarketNicheId(Long marketNicheId) { this.marketNicheId = marketNicheId; }

    /** Retorna o nome operacional da confirmação. */
    public String getName() { return name; }

    /** Define o nome operacional da confirmação. */
    public void setName(String name) { this.name = name; }

    /** Retorna o slug operacional sugerido. */
    public String getSlug() { return slug; }

    /** Define o slug operacional sugerido. */
    public void setSlug(String slug) { this.slug = slug; }

    /** Retorna o resumo do público confirmado. */
    public String getAudienceSummary() { return audienceSummary; }

    /** Define o resumo do público confirmado. */
    public void setAudienceSummary(String audienceSummary) { this.audienceSummary = audienceSummary; }

    /** Retorna o resumo da dor confirmada. */
    public String getPainSummary() { return painSummary; }

    /** Define o resumo da dor confirmada. */
    public void setPainSummary(String painSummary) { this.painSummary = painSummary; }

    /** Retorna a pergunta que confirma pertencimento ao público. */
    public String getAudienceConfirmationQuestion() { return audienceConfirmationQuestion; }

    /** Define a pergunta que confirma pertencimento ao público. */
    public void setAudienceConfirmationQuestion(String audienceConfirmationQuestion) {
        this.audienceConfirmationQuestion = audienceConfirmationQuestion;
    }

    /** Retorna as opções de qualificação separadas por quebra de linha. */
    public String getQualificationOptions() { return qualificationOptions; }

    /** Define as opções de qualificação separadas por quebra de linha. */
    public void setQualificationOptions(String qualificationOptions) { this.qualificationOptions = qualificationOptions; }

    /** Retorna a pergunta que confirma a dor. */
    public String getPainConfirmationQuestion() { return painConfirmationQuestion; }

    /** Define a pergunta que confirma a dor. */
    public void setPainConfirmationQuestion(String painConfirmationQuestion) {
        this.painConfirmationQuestion = painConfirmationQuestion;
    }

    /** Retorna a descrição de entrega sugerida. */
    public String getDeliveryDescription() { return deliveryDescription; }

    /** Define a descrição de entrega sugerida. */
    public void setDeliveryDescription(String deliveryDescription) { this.deliveryDescription = deliveryDescription; }

    /** Retorna por que a confirmação faz sentido. */
    public String getWhyItMakesSense() { return whyItMakesSense; }

    /** Define por que a confirmação faz sentido. */
    public void setWhyItMakesSense(String whyItMakesSense) { this.whyItMakesSense = whyItMakesSense; }

    /** Retorna o próximo passo operacional sugerido. */
    public String getNextStep() { return nextStep; }

    /** Define o próximo passo operacional sugerido. */
    public void setNextStep(String nextStep) { this.nextStep = nextStep; }

    /** Retorna o status operacional do registro. */
    public String getStatus() { return status; }

    /** Define o status operacional do registro. */
    public void setStatus(String status) { this.status = status; }
}
