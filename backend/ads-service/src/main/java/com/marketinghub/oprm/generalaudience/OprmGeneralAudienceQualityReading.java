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
import java.math.BigDecimal;
import java.time.Instant;

/** Responsável por registrar a leitura de qualidade real dos leads de um subnicho de público geral. */
@Entity
@Table(name = "oprm_general_audience_quality_reading")
public class OprmGeneralAudienceQualityReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subniche_id", nullable = false)
    private OprmGeneralAudienceSubniche subniche;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pain_angle_id")
    private OprmGeneralAudiencePainAngle painAngle;

    @Column(name = "experiment_id")
    private Long experimentId;

    @Column(name = "total_leads", nullable = false)
    private Integer totalLeads;

    @Column(name = "correct_profession_leads", nullable = false)
    private Integer correctProfessionLeads;

    @Column(name = "real_pain_responses", nullable = false)
    private Integer realPainResponses;

    @Column(name = "material_requests", nullable = false)
    private Integer materialRequests;

    @Column(name = "whatsapp_replies", nullable = false)
    private Integer whatsappReplies;

    @Column(name = "price_or_next_step_questions", nullable = false)
    private Integer priceOrNextStepQuestions;

    @Column(name = "out_of_profile_leads", nullable = false)
    private Integer outOfProfileLeads;

    @Column(name = "curious_without_profession", nullable = false)
    private Integer curiousWithoutProfession;

    @Column(name = "low_completion_events", nullable = false)
    private Integer lowCompletionEvents;

    @Column(name = "confusing_promise_reports", nullable = false)
    private Integer confusingPromiseReports;

    @Column(name = "lead_magnet_no_response", nullable = false)
    private Integer leadMagnetNoResponse;

    @Column(name = "quality_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal qualityScore;

    @Column(name = "approved", nullable = false)
    private boolean approved;

    @Column(name = "blockers", columnDefinition = "LONGTEXT")
    private String blockers;

    @Column(name = "recommendations", columnDefinition = "LONGTEXT")
    private String recommendations;

    @Column(name = "notes", columnDefinition = "LONGTEXT")
    private String notes;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Inicializa datas e contadores antes da primeira gravação. */
    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (capturedAt == null) {
            capturedAt = now;
        }
    }

    /** Atualiza a data de modificação antes de salvar alterações. */
    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    /** Retorna o identificador da leitura. */
    public Long getId() { return id; }

    /** Define o identificador da leitura. */
    public void setId(Long id) { this.id = id; }

    /** Retorna o subnicho avaliado. */
    public OprmGeneralAudienceSubniche getSubniche() { return subniche; }

    /** Define o subnicho avaliado. */
    public void setSubniche(OprmGeneralAudienceSubniche subniche) { this.subniche = subniche; }

    /** Retorna o ângulo relacionado à leitura. */
    public OprmGeneralAudiencePainAngle getPainAngle() { return painAngle; }

    /** Define o ângulo relacionado à leitura. */
    public void setPainAngle(OprmGeneralAudiencePainAngle painAngle) { this.painAngle = painAngle; }

    /** Retorna o experimento relacionado à leitura. */
    public Long getExperimentId() { return experimentId; }

    /** Define o experimento relacionado à leitura. */
    public void setExperimentId(Long experimentId) { this.experimentId = experimentId; }

    /** Retorna a quantidade total de leads observados. */
    public Integer getTotalLeads() { return totalLeads; }

    /** Define a quantidade total de leads observados. */
    public void setTotalLeads(Integer totalLeads) { this.totalLeads = totalLeads; }

    /** Retorna quantos leads informaram a profissão correta. */
    public Integer getCorrectProfessionLeads() { return correctProfessionLeads; }

    /** Define quantos leads informaram a profissão correta. */
    public void setCorrectProfessionLeads(Integer correctProfessionLeads) { this.correctProfessionLeads = correctProfessionLeads; }

    /** Retorna quantas respostas trouxeram dor real. */
    public Integer getRealPainResponses() { return realPainResponses; }

    /** Define quantas respostas trouxeram dor real. */
    public void setRealPainResponses(Integer realPainResponses) { this.realPainResponses = realPainResponses; }

    /** Retorna quantas pessoas pediram o material. */
    public Integer getMaterialRequests() { return materialRequests; }

    /** Define quantas pessoas pediram o material. */
    public void setMaterialRequests(Integer materialRequests) { this.materialRequests = materialRequests; }

    /** Retorna quantas pessoas responderam no WhatsApp. */
    public Integer getWhatsappReplies() { return whatsappReplies; }

    /** Define quantas pessoas responderam no WhatsApp. */
    public void setWhatsappReplies(Integer whatsappReplies) { this.whatsappReplies = whatsappReplies; }

    /** Retorna quantas pessoas perguntaram preço ou próximo passo. */
    public Integer getPriceOrNextStepQuestions() { return priceOrNextStepQuestions; }

    /** Define quantas pessoas perguntaram preço ou próximo passo. */
    public void setPriceOrNextStepQuestions(Integer priceOrNextStepQuestions) { this.priceOrNextStepQuestions = priceOrNextStepQuestions; }

    /** Retorna quantos leads vieram fora do perfil. */
    public Integer getOutOfProfileLeads() { return outOfProfileLeads; }

    /** Define quantos leads vieram fora do perfil. */
    public void setOutOfProfileLeads(Integer outOfProfileLeads) { this.outOfProfileLeads = outOfProfileLeads; }

    /** Retorna quantos curiosos vieram sem profissão aderente. */
    public Integer getCuriousWithoutProfession() { return curiousWithoutProfession; }

    /** Define quantos curiosos vieram sem profissão aderente. */
    public void setCuriousWithoutProfession(Integer curiousWithoutProfession) { this.curiousWithoutProfession = curiousWithoutProfession; }

    /** Retorna quantos eventos indicaram baixo preenchimento. */
    public Integer getLowCompletionEvents() { return lowCompletionEvents; }

    /** Define quantos eventos indicaram baixo preenchimento. */
    public void setLowCompletionEvents(Integer lowCompletionEvents) { this.lowCompletionEvents = lowCompletionEvents; }

    /** Retorna quantos relatos apontaram promessa confusa. */
    public Integer getConfusingPromiseReports() { return confusingPromiseReports; }

    /** Define quantos relatos apontaram promessa confusa. */
    public void setConfusingPromiseReports(Integer confusingPromiseReports) { this.confusingPromiseReports = confusingPromiseReports; }

    /** Retorna quantos leads baixaram a isca sem responder depois. */
    public Integer getLeadMagnetNoResponse() { return leadMagnetNoResponse; }

    /** Define quantos leads baixaram a isca sem responder depois. */
    public void setLeadMagnetNoResponse(Integer leadMagnetNoResponse) { this.leadMagnetNoResponse = leadMagnetNoResponse; }

    /** Retorna o score de qualidade calculado. */
    public BigDecimal getQualityScore() { return qualityScore; }

    /** Define o score de qualidade calculado. */
    public void setQualityScore(BigDecimal qualityScore) { this.qualityScore = qualityScore; }

    /** Indica se a leitura aprovou a qualidade do público. */
    public boolean isApproved() { return approved; }

    /** Define se a leitura aprovou a qualidade do público. */
    public void setApproved(boolean approved) { this.approved = approved; }

    /** Retorna bloqueios calculados para a leitura. */
    public String getBlockers() { return blockers; }

    /** Define bloqueios calculados para a leitura. */
    public void setBlockers(String blockers) { this.blockers = blockers; }

    /** Retorna recomendações calculadas para a leitura. */
    public String getRecommendations() { return recommendations; }

    /** Define recomendações calculadas para a leitura. */
    public void setRecommendations(String recommendations) { this.recommendations = recommendations; }

    /** Retorna observações operacionais da leitura. */
    public String getNotes() { return notes; }

    /** Define observações operacionais da leitura. */
    public void setNotes(String notes) { this.notes = notes; }

    /** Retorna a data em que os sinais foram capturados. */
    public Instant getCapturedAt() { return capturedAt; }

    /** Define a data em que os sinais foram capturados. */
    public void setCapturedAt(Instant capturedAt) { this.capturedAt = capturedAt; }

    /** Retorna a data de criação da leitura. */
    public Instant getCreatedAt() { return createdAt; }

    /** Define a data de criação da leitura. */
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    /** Retorna a data de atualização da leitura. */
    public Instant getUpdatedAt() { return updatedAt; }

    /** Define a data de atualização da leitura. */
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
