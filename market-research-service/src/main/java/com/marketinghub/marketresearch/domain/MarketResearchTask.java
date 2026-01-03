package com.marketinghub.marketresearch.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "market_research_tasks")
public class MarketResearchTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String query;

    private String analysisObjective;

    @Enumerated(EnumType.STRING)
    private MarketResearchStatus status = MarketResearchStatus.PENDING;

    @ElementCollection
    @CollectionTable(name = "market_research_sources", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "source_url")
    private List<String> sources = new ArrayList<>();

    @Lob
    private String contextSnapshot;

    @Lob
    private String summary;

    private String model;

    private String errorMessage;

    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getAnalysisObjective() {
        return analysisObjective;
    }

    public void setAnalysisObjective(String analysisObjective) {
        this.analysisObjective = analysisObjective;
    }

    public MarketResearchStatus getStatus() {
        return status;
    }

    public void setStatus(MarketResearchStatus status) {
        this.status = status;
    }

    public List<String> getSources() {
        return sources;
    }

    public void setSources(List<String> sources) {
        this.sources = sources;
    }

    public String getContextSnapshot() {
        return contextSnapshot;
    }

    public void setContextSnapshot(String contextSnapshot) {
        this.contextSnapshot = contextSnapshot;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
