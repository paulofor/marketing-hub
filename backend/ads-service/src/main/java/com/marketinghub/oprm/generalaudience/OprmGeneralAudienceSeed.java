package com.marketinghub.oprm.generalaudience;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

/** Responsável por armazenar a semente ampla de público geral que ainda não é nicho nem campanha. */
@Entity
@Table(name = "oprm_general_audience_seed")
public class OprmGeneralAudienceSeed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 191)
    private String name;

    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;

    @Column(name = "market_context", columnDefinition = "LONGTEXT")
    private String marketContext;

    @Column(name = "country", nullable = false, length = 64)
    private String country;

    @Column(name = "language", nullable = false, length = 32)
    private String language;

    @Enumerated(EnumType.STRING)
    @Column(name = "seed_type", nullable = false, length = 32)
    private OprmGeneralAudienceSeedType seedType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OprmGeneralAudienceSeedStatus status;

    @Column(name = "business_goal", columnDefinition = "LONGTEXT")
    private String businessGoal;

    @Column(name = "risk_notes", columnDefinition = "LONGTEXT")
    private String riskNotes;

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

    /** Retorna o identificador da semente. */
    public Long getId() {
        return id;
    }

    /** Define o identificador da semente. */
    public void setId(Long id) {
        this.id = id;
    }

    /** Retorna o nome de negócio da semente. */
    public String getName() {
        return name;
    }

    /** Define o nome de negócio da semente. */
    public void setName(String name) {
        this.name = name;
    }

    /** Retorna a descrição da semente ampla. */
    public String getDescription() {
        return description;
    }

    /** Define a descrição da semente ampla. */
    public void setDescription(String description) {
        this.description = description;
    }

    /** Retorna o contexto de mercado usado para orientar a revisão da semente. */
    public String getMarketContext() {
        return marketContext;
    }

    /** Define o contexto de mercado usado para orientar a revisão da semente. */
    public void setMarketContext(String marketContext) {
        this.marketContext = marketContext;
    }

    /** Retorna o país alvo da semente. */
    public String getCountry() {
        return country;
    }

    /** Define o país alvo da semente. */
    public void setCountry(String country) {
        this.country = country;
    }

    /** Retorna o idioma alvo da semente. */
    public String getLanguage() {
        return language;
    }

    /** Define o idioma alvo da semente. */
    public void setLanguage(String language) {
        this.language = language;
    }

    /** Retorna o tipo comercial da semente. */
    public OprmGeneralAudienceSeedType getSeedType() {
        return seedType;
    }

    /** Define o tipo comercial da semente. */
    public void setSeedType(OprmGeneralAudienceSeedType seedType) {
        this.seedType = seedType;
    }

    /** Retorna o status operacional da semente. */
    public OprmGeneralAudienceSeedStatus getStatus() {
        return status;
    }

    /** Define o status operacional da semente. */
    public void setStatus(OprmGeneralAudienceSeedStatus status) {
        this.status = status;
    }

    /** Retorna o objetivo comercial esperado para a semente. */
    public String getBusinessGoal() {
        return businessGoal;
    }

    /** Define o objetivo comercial esperado para a semente. */
    public void setBusinessGoal(String businessGoal) {
        this.businessGoal = businessGoal;
    }

    /** Retorna as observações de risco e compliance da semente. */
    public String getRiskNotes() {
        return riskNotes;
    }

    /** Define as observações de risco e compliance da semente. */
    public void setRiskNotes(String riskNotes) {
        this.riskNotes = riskNotes;
    }

    /** Retorna a data de criação da semente. */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Define a data de criação da semente. */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /** Retorna a data da última alteração da semente. */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /** Define a data da última alteração da semente. */
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
