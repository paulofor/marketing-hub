package com.marketinghub.productdiscovery.v1;

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

/** Representa uma oportunidade PDE derivada de sinais públicos de dor e lacuna. */
@Entity
@Table(name = "product_discovery_opportunity")
public class ProductDiscoveryOpportunity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cycle_id", nullable = false)
  private ProductDiscoveryCycle cycle;

  @Column(name = "name", nullable = false, length = 191)
  private String name;

  @Column(name = "primary_audience", nullable = false, length = 191)
  private String primaryAudience;

  @Column(name = "root_pain", nullable = false, columnDefinition = "LONGTEXT")
  private String rootPain;

  @Column(name = "practical_pain", columnDefinition = "LONGTEXT")
  private String practicalPain;

  @Column(name = "emotional_pain", columnDefinition = "LONGTEXT")
  private String emotionalPain;

  @Column(name = "scale_evidence", columnDefinition = "LONGTEXT")
  private String scaleEvidence;

  @Column(name = "unmetness_evidence", columnDefinition = "LONGTEXT")
  private String unmetnessEvidence;

  @Column(name = "pde_experience", columnDefinition = "LONGTEXT")
  private String pdeExperience;

  @Column(name = "first_campaign_angle", columnDefinition = "LONGTEXT")
  private String firstCampaignAngle;

  @Column(name = "commercial_risk", columnDefinition = "LONGTEXT")
  private String commercialRisk;

  @Column(name = "evidence_json", columnDefinition = "LONGTEXT")
  private String evidenceJson;

  @Column(name = "score", nullable = false, precision = 5, scale = 2)
  private BigDecimal score;

  @Enumerated(EnumType.STRING)
  @Column(name = "decision", nullable = false, length = 40)
  private ProductDiscoveryOpportunityDecision decision;

  @Enumerated(EnumType.STRING)
  @Column(name = "maturity_status", nullable = false, length = 32)
  private ProductDiscoveryOpportunityMaturity maturity;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** Preenche timestamps e decisão padrão antes da criação. */
  @PrePersist
  public void prePersist() {
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
    if (decision == null) {
      decision = ProductDiscoveryOpportunityDecision.RESEARCH_MORE;
    }
    if (maturity == null) {
      maturity = ProductDiscoveryOpportunityMaturity.SIGNAL;
    }
    if (score == null) {
      score = BigDecimal.ZERO;
    }
  }

  /** Atualiza o timestamp de alteração antes de salvar mudanças. */
  @PreUpdate
  public void preUpdate() {
    updatedAt = Instant.now();
  }

  /** Retorna o identificador da oportunidade. */
  public Long getId() {
    return id;
  }

  /** Retorna o ciclo associado. */
  public ProductDiscoveryCycle getCycle() {
    return cycle;
  }

  /** Define o ciclo associado. */
  public void setCycle(ProductDiscoveryCycle cycle) {
    this.cycle = cycle;
  }

  /** Retorna o nome da oportunidade. */
  public String getName() {
    return name;
  }

  /** Define o nome da oportunidade. */
  public void setName(String name) {
    this.name = name;
  }

  /** Retorna o público primário. */
  public String getPrimaryAudience() {
    return primaryAudience;
  }

  /** Define o público primário. */
  public void setPrimaryAudience(String primaryAudience) {
    this.primaryAudience = primaryAudience;
  }

  /** Retorna a dor raiz. */
  public String getRootPain() {
    return rootPain;
  }

  /** Define a dor raiz. */
  public void setRootPain(String rootPain) {
    this.rootPain = rootPain;
  }

  /** Retorna a dor prática. */
  public String getPracticalPain() {
    return practicalPain;
  }

  /** Define a dor prática. */
  public void setPracticalPain(String practicalPain) {
    this.practicalPain = practicalPain;
  }

  /** Retorna a dor emocional. */
  public String getEmotionalPain() {
    return emotionalPain;
  }

  /** Define a dor emocional. */
  public void setEmotionalPain(String emotionalPain) {
    this.emotionalPain = emotionalPain;
  }

  /** Retorna a evidência de escala. */
  public String getScaleEvidence() {
    return scaleEvidence;
  }

  /** Define a evidência de escala. */
  public void setScaleEvidence(String scaleEvidence) {
    this.scaleEvidence = scaleEvidence;
  }

  /** Retorna a evidência de desatendimento. */
  public String getUnmetnessEvidence() {
    return unmetnessEvidence;
  }

  /** Define a evidência de desatendimento. */
  public void setUnmetnessEvidence(String unmetnessEvidence) {
    this.unmetnessEvidence = unmetnessEvidence;
  }

  /** Retorna a microexperiência PDE recomendada. */
  public String getPdeExperience() {
    return pdeExperience;
  }

  /** Define a microexperiência PDE recomendada. */
  public void setPdeExperience(String pdeExperience) {
    this.pdeExperience = pdeExperience;
  }

  /** Retorna o primeiro ângulo de campanha. */
  public String getFirstCampaignAngle() {
    return firstCampaignAngle;
  }

  /** Define o primeiro ângulo de campanha. */
  public void setFirstCampaignAngle(String firstCampaignAngle) {
    this.firstCampaignAngle = firstCampaignAngle;
  }

  /** Retorna o risco comercial principal. */
  public String getCommercialRisk() {
    return commercialRisk;
  }

  /** Define o risco comercial principal. */
  public void setCommercialRisk(String commercialRisk) {
    this.commercialRisk = commercialRisk;
  }

  /** Retorna o JSON de evidências. */
  public String getEvidenceJson() {
    return evidenceJson;
  }

  /** Define o JSON de evidências. */
  public void setEvidenceJson(String evidenceJson) {
    this.evidenceJson = evidenceJson;
  }

  /** Retorna o score comercial. */
  public BigDecimal getScore() {
    return score;
  }

  /** Define o score comercial. */
  public void setScore(BigDecimal score) {
    this.score = score;
  }

  /** Retorna a decisão da oportunidade. */
  public ProductDiscoveryOpportunityDecision getDecision() {
    return decision;
  }

  /** Define a decisão da oportunidade. */
  public void setDecision(ProductDiscoveryOpportunityDecision decision) {
    this.decision = decision;
  }

  /** Retorna a maturidade factual que controla a passagem para Atena. */
  public ProductDiscoveryOpportunityMaturity getMaturity() {
    return maturity;
  }

  /** Define a maturidade factual produzida por Argos. */
  public void setMaturity(ProductDiscoveryOpportunityMaturity maturity) {
    this.maturity = maturity;
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
