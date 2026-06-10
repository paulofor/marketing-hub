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
import java.math.BigDecimal;
import java.time.Instant;

/** Responsável por armazenar um subnicho descoberto a partir de uma semente de público geral. */
@Entity
@Table(name = "oprm_general_audience_subniche")
public class OprmGeneralAudienceSubniche {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seed_id", nullable = false)
    private OprmGeneralAudienceSeed seed;

    @Column(name = "name", nullable = false, length = 191)
    private String name;

    @Column(name = "persona_summary", columnDefinition = "LONGTEXT")
    private String personaSummary;

    @Column(name = "pain_summary", columnDefinition = "LONGTEXT")
    private String painSummary;

    @Column(name = "desired_outcome_summary", columnDefinition = "LONGTEXT")
    private String desiredOutcomeSummary;

    @Column(name = "language_patterns", columnDefinition = "LONGTEXT")
    private String languagePatterns;

    @Column(name = "channels_summary", columnDefinition = "LONGTEXT")
    private String channelsSummary;

    @Column(name = "qualification_question", columnDefinition = "LONGTEXT")
    private String qualificationQuestion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OprmGeneralAudienceSubnicheStatus status;

    @Column(name = "opportunity_score", precision = 5, scale = 2)
    private BigDecimal opportunityScore;

    @Column(name = "risk_score", precision = 5, scale = 2)
    private BigDecimal riskScore;

    @Column(name = "market_niche_id")
    private Long marketNicheId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Inicializa os carimbos de data antes da primeira persistência. */
    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    /** Atualiza o carimbo de modificação antes de salvar alterações. */
    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    /** Retorna o identificador do subnicho. */
    public Long getId() {
        return id;
    }

    /** Define o identificador do subnicho. */
    public void setId(Long id) {
        this.id = id;
    }

    /** Retorna a semente que originou o subnicho. */
    public OprmGeneralAudienceSeed getSeed() {
        return seed;
    }

    /** Define a semente que originou o subnicho. */
    public void setSeed(OprmGeneralAudienceSeed seed) {
        this.seed = seed;
    }

    /** Retorna o nome específico do subnicho. */
    public String getName() {
        return name;
    }

    /** Define o nome específico do subnicho. */
    public void setName(String name) {
        this.name = name;
    }

    /** Retorna quem é a pessoa representada pelo subnicho. */
    public String getPersonaSummary() {
        return personaSummary;
    }

    /** Define quem é a pessoa representada pelo subnicho. */
    public void setPersonaSummary(String personaSummary) {
        this.personaSummary = personaSummary;
    }

    /** Retorna o resumo das dores relevantes do subnicho. */
    public String getPainSummary() {
        return painSummary;
    }

    /** Define o resumo das dores relevantes do subnicho. */
    public void setPainSummary(String painSummary) {
        this.painSummary = painSummary;
    }

    /** Retorna o resultado desejado pelo subnicho. */
    public String getDesiredOutcomeSummary() {
        return desiredOutcomeSummary;
    }

    /** Define o resultado desejado pelo subnicho. */
    public void setDesiredOutcomeSummary(String desiredOutcomeSummary) {
        this.desiredOutcomeSummary = desiredOutcomeSummary;
    }

    /** Retorna padrões de linguagem úteis para confirmar o público. */
    public String getLanguagePatterns() {
        return languagePatterns;
    }

    /** Define padrões de linguagem úteis para confirmar o público. */
    public void setLanguagePatterns(String languagePatterns) {
        this.languagePatterns = languagePatterns;
    }

    /** Retorna os canais usados pelo subnicho. */
    public String getChannelsSummary() {
        return channelsSummary;
    }

    /** Define os canais usados pelo subnicho. */
    public void setChannelsSummary(String channelsSummary) {
        this.channelsSummary = channelsSummary;
    }

    /** Retorna a pergunta qualificadora para separar o lead certo do público errado. */
    public String getQualificationQuestion() {
        return qualificationQuestion;
    }

    /** Define a pergunta qualificadora para separar o lead certo do público errado. */
    public void setQualificationQuestion(String qualificationQuestion) {
        this.qualificationQuestion = qualificationQuestion;
    }

    /** Retorna o status operacional do subnicho. */
    public OprmGeneralAudienceSubnicheStatus getStatus() {
        return status;
    }

    /** Define o status operacional do subnicho. */
    public void setStatus(OprmGeneralAudienceSubnicheStatus status) {
        this.status = status;
    }

    /** Retorna o score de oportunidade do subnicho. */
    public BigDecimal getOpportunityScore() {
        return opportunityScore;
    }

    /** Define o score de oportunidade do subnicho. */
    public void setOpportunityScore(BigDecimal opportunityScore) {
        this.opportunityScore = opportunityScore;
    }

    /** Retorna o score de risco do subnicho. */
    public BigDecimal getRiskScore() {
        return riskScore;
    }

    /** Define o score de risco do subnicho. */
    public void setRiskScore(BigDecimal riskScore) {
        this.riskScore = riskScore;
    }

    /** Retorna o nicho de mercado gerado, quando houver conversão controlada. */
    public Long getMarketNicheId() {
        return marketNicheId;
    }

    /** Define o nicho de mercado gerado, quando houver conversão controlada. */
    public void setMarketNicheId(Long marketNicheId) {
        this.marketNicheId = marketNicheId;
    }

    /** Retorna a data de criação do subnicho. */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Define a data de criação do subnicho. */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /** Retorna a data da última alteração do subnicho. */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /** Define a data da última alteração do subnicho. */
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
