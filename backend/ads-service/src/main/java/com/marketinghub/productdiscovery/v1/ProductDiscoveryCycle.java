package com.marketinghub.productdiscovery.v1;

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

/** Representa um ciclo auditável de pesquisa de dores e oportunidades para produtos PDE. */
@Entity
@Table(name = "product_discovery_cycle")
public class ProductDiscoveryCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "theme", nullable = false, length = 191)
    private String theme;

    @Column(name = "target_audience", length = 191)
    private String targetAudience;

    @Column(name = "country", nullable = false, length = 16)
    private String country;

    @Column(name = "language", nullable = false, length = 16)
    private String language;

    @Column(name = "acquisition_channel", length = 120)
    private String acquisitionChannel;

    @Column(name = "commercial_constraints", columnDefinition = "LONGTEXT")
    private String commercialConstraints;

    @Column(name = "forbidden_categories", columnDefinition = "LONGTEXT")
    private String forbiddenCategories;

    @Column(name = "objective", columnDefinition = "LONGTEXT")
    private String objective;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private ProductDiscoveryCycleStatus status;

    @Column(name = "stage_code", nullable = false, length = 80)
    private String stageCode;

    @Column(name = "decision_summary", columnDefinition = "LONGTEXT")
    private String decisionSummary;

    @Column(name = "error_message", columnDefinition = "LONGTEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Preenche timestamps e valores padrão antes da criação. */
    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = ProductDiscoveryCycleStatus.DRAFT;
        }
        if (stageCode == null) {
            stageCode = "research";
        }
    }

    /** Atualiza o timestamp de alteração antes de salvar mudanças. */
    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    /** Retorna o identificador do ciclo. */
    public Long getId() {
        return id;
    }

    /** Define o identificador do ciclo. */
    public void setId(Long id) {
        this.id = id;
    }

    /** Retorna o tema amplo da pesquisa. */
    public String getTheme() {
        return theme;
    }

    /** Define o tema amplo da pesquisa. */
    public void setTheme(String theme) {
        this.theme = theme;
    }

    /** Retorna o público-alvo desejado. */
    public String getTargetAudience() {
        return targetAudience;
    }

    /** Define o público-alvo desejado. */
    public void setTargetAudience(String targetAudience) {
        this.targetAudience = targetAudience;
    }

    /** Retorna o país da pesquisa. */
    public String getCountry() {
        return country;
    }

    /** Define o país da pesquisa. */
    public void setCountry(String country) {
        this.country = country;
    }

    /** Retorna o idioma da pesquisa. */
    public String getLanguage() {
        return language;
    }

    /** Define o idioma da pesquisa. */
    public void setLanguage(String language) {
        this.language = language;
    }

    /** Retorna o canal provável de aquisição. */
    public String getAcquisitionChannel() {
        return acquisitionChannel;
    }

    /** Define o canal provável de aquisição. */
    public void setAcquisitionChannel(String acquisitionChannel) {
        this.acquisitionChannel = acquisitionChannel;
    }

    /** Retorna as restrições comerciais do ciclo. */
    public String getCommercialConstraints() {
        return commercialConstraints;
    }

    /** Define as restrições comerciais do ciclo. */
    public void setCommercialConstraints(String commercialConstraints) {
        this.commercialConstraints = commercialConstraints;
    }

    /** Retorna categorias proibidas para a pesquisa. */
    public String getForbiddenCategories() {
        return forbiddenCategories;
    }

    /** Define categorias proibidas para a pesquisa. */
    public void setForbiddenCategories(String forbiddenCategories) {
        this.forbiddenCategories = forbiddenCategories;
    }

    /** Retorna o objetivo do ciclo. */
    public String getObjective() {
        return objective;
    }

    /** Define o objetivo do ciclo. */
    public void setObjective(String objective) {
        this.objective = objective;
    }

    /** Retorna o status operacional. */
    public ProductDiscoveryCycleStatus getStatus() {
        return status;
    }

    /** Define o status operacional. */
    public void setStatus(ProductDiscoveryCycleStatus status) {
        this.status = status;
    }

    /** Retorna a etapa atual. */
    public String getStageCode() {
        return stageCode;
    }

    /** Define a etapa atual. */
    public void setStageCode(String stageCode) {
        this.stageCode = stageCode;
    }

    /** Retorna o resumo da decisão do ciclo. */
    public String getDecisionSummary() {
        return decisionSummary;
    }

    /** Define o resumo da decisão do ciclo. */
    public void setDecisionSummary(String decisionSummary) {
        this.decisionSummary = decisionSummary;
    }

    /** Retorna a mensagem de erro do ciclo. */
    public String getErrorMessage() {
        return errorMessage;
    }

    /** Define a mensagem de erro do ciclo. */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /** Retorna a data de criação. */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Retorna a data de atualização. */
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
