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

/** Responsável por armazenar dados de público que o Facebook Ads buscará no backend sem acoplamento OPRM-targeting. */
@Entity
@Table(name = "oprm_general_audience_facebook_ads_data")
public class OprmGeneralAudienceFacebookAdsData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pain_angle_id", nullable = false)
    private OprmGeneralAudiencePainAngle painAngle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subniche_id", nullable = false)
    private OprmGeneralAudienceSubniche subniche;

    @Column(name = "market_niche_id")
    private Long marketNicheId;

    @Column(name = "hypothesis_id", length = 36)
    private String hypothesisId;

    @Enumerated(EnumType.STRING)
    @Column(name = "signal_type", nullable = false, length = 32)
    private OprmGeneralAudienceAdSignalType signalType;

    @Column(name = "term", nullable = false, length = 255)
    private String term;

    @Column(name = "meta_id", length = 191)
    private String metaId;

    @Column(name = "required_for_publication", nullable = false)
    private boolean requiredForPublication;

    @Column(name = "ready_for_facebook_ads", nullable = false)
    private boolean readyForFacebookAds;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OprmGeneralAudienceAdSignalStatus status;

    @Column(name = "creative_screening_phrase", columnDefinition = "LONGTEXT")
    private String creativeScreeningPhrase;

    @Column(name = "demographic_guidance", columnDefinition = "LONGTEXT")
    private String demographicGuidance;

    @Column(name = "landing_confirmation_instruction", columnDefinition = "LONGTEXT")
    private String landingConfirmationInstruction;

    @Column(name = "reviewed_by", length = 191)
    private String reviewedBy;

    @Column(name = "notes", columnDefinition = "LONGTEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Inicializa os carimbos de criação e atualização do dado de público. */
    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    /** Atualiza o carimbo de modificação do dado de público. */
    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    /** Retorna o identificador do dado de público. */
    public Long getId() { return id; }

    /** Define o identificador do dado de público. */
    public void setId(Long id) { this.id = id; }

    /** Retorna o ângulo de dor que originou o dado de público. */
    public OprmGeneralAudiencePainAngle getPainAngle() { return painAngle; }

    /** Define o ângulo de dor que originou o dado de público. */
    public void setPainAngle(OprmGeneralAudiencePainAngle painAngle) { this.painAngle = painAngle; }

    /** Retorna o subnicho associado ao dado de público. */
    public OprmGeneralAudienceSubniche getSubniche() { return subniche; }

    /** Define o subnicho associado ao dado de público. */
    public void setSubniche(OprmGeneralAudienceSubniche subniche) { this.subniche = subniche; }

    /** Retorna o MarketNiche já materializado para uso pelo backend. */
    public Long getMarketNicheId() { return marketNicheId; }

    /** Define o MarketNiche já materializado para uso pelo backend. */
    public void setMarketNicheId(Long marketNicheId) { this.marketNicheId = marketNicheId; }

    /** Retorna a hipótese vinculada ao dado de público. */
    public String getHypothesisId() { return hypothesisId; }

    /** Define a hipótese vinculada ao dado de público. */
    public void setHypothesisId(String hypothesisId) { this.hypothesisId = hypothesisId; }

    /** Retorna o tipo do sinal de público. */
    public OprmGeneralAudienceAdSignalType getSignalType() { return signalType; }

    /** Define o tipo do sinal de público. */
    public void setSignalType(OprmGeneralAudienceAdSignalType signalType) { this.signalType = signalType; }

    /** Retorna o termo de público informado. */
    public String getTerm() { return term; }

    /** Define o termo de público informado. */
    public void setTerm(String term) { this.term = term; }

    /** Retorna o identificador oficial da Meta quando já conhecido. */
    public String getMetaId() { return metaId; }

    /** Define o identificador oficial da Meta quando já conhecido. */
    public void setMetaId(String metaId) { this.metaId = metaId; }

    /** Retorna se o dado é obrigatório antes da publicação. */
    public boolean isRequiredForPublication() { return requiredForPublication; }

    /** Define se o dado é obrigatório antes da publicação. */
    public void setRequiredForPublication(boolean requiredForPublication) { this.requiredForPublication = requiredForPublication; }

    /** Retorna se o dado está pronto para coleta pelo Facebook Ads. */
    public boolean isReadyForFacebookAds() { return readyForFacebookAds; }

    /** Define se o dado está pronto para coleta pelo Facebook Ads. */
    public void setReadyForFacebookAds(boolean readyForFacebookAds) { this.readyForFacebookAds = readyForFacebookAds; }

    /** Retorna o status operacional do dado de público. */
    public OprmGeneralAudienceAdSignalStatus getStatus() { return status; }

    /** Define o status operacional do dado de público. */
    public void setStatus(OprmGeneralAudienceAdSignalStatus status) { this.status = status; }

    /** Retorna a frase de triagem sugerida para o criativo. */
    public String getCreativeScreeningPhrase() { return creativeScreeningPhrase; }

    /** Define a frase de triagem sugerida para o criativo. */
    public void setCreativeScreeningPhrase(String creativeScreeningPhrase) { this.creativeScreeningPhrase = creativeScreeningPhrase; }

    /** Retorna a orientação demográfica registrada para avaliação posterior. */
    public String getDemographicGuidance() { return demographicGuidance; }

    /** Define a orientação demográfica registrada para avaliação posterior. */
    public void setDemographicGuidance(String demographicGuidance) { this.demographicGuidance = demographicGuidance; }

    /** Retorna a instrução de confirmação que deve aparecer na landing. */
    public String getLandingConfirmationInstruction() { return landingConfirmationInstruction; }

    /** Define a instrução de confirmação que deve aparecer na landing. */
    public void setLandingConfirmationInstruction(String landingConfirmationInstruction) {
        this.landingConfirmationInstruction = landingConfirmationInstruction;
    }

    /** Retorna quem revisou o dado antes do envio ao backend. */
    public String getReviewedBy() { return reviewedBy; }

    /** Define quem revisou o dado antes do envio ao backend. */
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    /** Retorna observações operacionais para o Facebook Ads. */
    public String getNotes() { return notes; }

    /** Define observações operacionais para o Facebook Ads. */
    public void setNotes(String notes) { this.notes = notes; }

    /** Retorna quando o dado foi criado. */
    public Instant getCreatedAt() { return createdAt; }

    /** Define quando o dado foi criado. */
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    /** Retorna quando o dado foi atualizado. */
    public Instant getUpdatedAt() { return updatedAt; }

    /** Define quando o dado foi atualizado. */
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
