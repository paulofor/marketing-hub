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
import java.time.LocalDate;

/** Preserva uma entrevista consentida e anonimizada sobre comportamento passado de compra. */
@Entity
@Table(name = "product_discovery_customer_interview")
public class ProductDiscoveryCustomerInterview {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cycle_id", nullable = false)
  private ProductDiscoveryCycle cycle;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "opportunity_id", nullable = false)
  private ProductDiscoveryOpportunity opportunity;

  @Column(name = "anonymous_participant_code", nullable = false, length = 7)
  private String anonymousParticipantCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "outcome", nullable = false, length = 24)
  private ProductDiscoveryInterviewOutcome outcome;

  @Column(name = "occurred_on", nullable = false)
  private LocalDate occurredOn;

  @Column(name = "consent_captured_at", nullable = false)
  private Instant consentCapturedAt;

  @Column(name = "purchase_situation", nullable = false, columnDefinition = "LONGTEXT")
  private String purchaseSituation;

  @Column(name = "desired_result", nullable = false, columnDefinition = "LONGTEXT")
  private String desiredResult;

  @Column(name = "difficulty", nullable = false, columnDefinition = "LONGTEXT")
  private String difficulty;

  @Column(name = "alternative_tried", nullable = false, columnDefinition = "LONGTEXT")
  private String alternativeTried;

  @Column(name = "amount_spent", precision = 12, scale = 2)
  private BigDecimal amountSpent;

  @Column(name = "currency", length = 3)
  private String currency;

  @Column(name = "remaining_difficulty", nullable = false, columnDefinition = "LONGTEXT")
  private String remainingDifficulty;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** Preenche os timestamps técnicos sem alterar a data relatada da experiência. */
  @PrePersist
  public void prePersist() {
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
  }

  /** Atualiza o timestamp técnico quando o registro persistido mudar. */
  @PreUpdate
  public void preUpdate() {
    updatedAt = Instant.now();
  }

  /** Retorna a identidade técnica da entrevista. */
  public Long getId() {
    return id;
  }

  /** Define a identidade técnica da entrevista. */
  public void setId(Long id) {
    this.id = id;
  }

  /** Retorna o ciclo ao qual a entrevista pertence. */
  public ProductDiscoveryCycle getCycle() {
    return cycle;
  }

  /** Vincula a entrevista ao ciclo de descoberta. */
  public void setCycle(ProductDiscoveryCycle cycle) {
    this.cycle = cycle;
  }

  /** Retorna a candidata cuja situação foi vivida pela pessoa. */
  public ProductDiscoveryOpportunity getOpportunity() {
    return opportunity;
  }

  /** Vincula a experiência narrada a uma candidata factual do mesmo ciclo. */
  public void setOpportunity(ProductDiscoveryOpportunity opportunity) {
    this.opportunity = opportunity;
  }

  /** Retorna o código anônimo usado para evitar duplicidade de participante. */
  public String getAnonymousParticipantCode() {
    return anonymousParticipantCode;
  }

  /** Define um código anônimo sem nome, e-mail, telefone ou outro dado pessoal. */
  public void setAnonymousParticipantCode(String anonymousParticipantCode) {
    this.anonymousParticipantCode = anonymousParticipantCode;
  }

  /** Retorna se a pessoa comprou ou abandonou a alternativa considerada. */
  public ProductDiscoveryInterviewOutcome getOutcome() {
    return outcome;
  }

  /** Registra a decisão passada narrada na entrevista. */
  public void setOutcome(ProductDiscoveryInterviewOutcome outcome) {
    this.outcome = outcome;
  }

  /** Retorna a data da situação concreta lembrada pela pessoa. */
  public LocalDate getOccurredOn() {
    return occurredOn;
  }

  /** Define a data da situação concreta, sem substituí-la pela data da entrevista. */
  public void setOccurredOn(LocalDate occurredOn) {
    this.occurredOn = occurredOn;
  }

  /** Retorna quando o consentimento foi confirmado no cadastro. */
  public Instant getConsentCapturedAt() {
    return consentCapturedAt;
  }

  /** Registra o momento da confirmação de consentimento. */
  public void setConsentCapturedAt(Instant consentCapturedAt) {
    this.consentCapturedAt = consentCapturedAt;
  }

  /** Retorna a ocasião concreta em que o desejo ganhou prioridade. */
  public String getPurchaseSituation() {
    return purchaseSituation;
  }

  /** Preserva a ocasião narrada sem transformá-la em segmento aprovado. */
  public void setPurchaseSituation(String purchaseSituation) {
    this.purchaseSituation = purchaseSituation;
  }

  /** Retorna o resultado que a pessoa buscava naquela ocasião. */
  public String getDesiredResult() {
    return desiredResult;
  }

  /** Registra o resultado desejado nas palavras resumidas da entrevista. */
  public void setDesiredResult(String desiredResult) {
    this.desiredResult = desiredResult;
  }

  /** Retorna a dificuldade concreta encontrada. */
  public String getDifficulty() {
    return difficulty;
  }

  /** Registra a dificuldade sem induzir uma causa não relatada. */
  public void setDifficulty(String difficulty) {
    this.difficulty = difficulty;
  }

  /** Retorna a alternativa que a pessoa tentou ou considerou. */
  public String getAlternativeTried() {
    return alternativeTried;
  }

  /** Registra a alternativa realmente tentada ou considerada. */
  public void setAlternativeTried(String alternativeTried) {
    this.alternativeTried = alternativeTried;
  }

  /** Retorna o gasto relatado, quando conhecido. */
  public BigDecimal getAmountSpent() {
    return amountSpent;
  }

  /** Define o gasto relatado sem converter ausência em zero. */
  public void setAmountSpent(BigDecimal amountSpent) {
    this.amountSpent = amountSpent;
  }

  /** Retorna a moeda do gasto relatado. */
  public String getCurrency() {
    return currency;
  }

  /** Define a moeda somente quando existe valor informado. */
  public void setCurrency(String currency) {
    this.currency = currency;
  }

  /** Retorna o que continuou difícil depois da alternativa. */
  public String getRemainingDifficulty() {
    return remainingDifficulty;
  }

  /** Registra a lacuna residual narrada pela pessoa. */
  public void setRemainingDifficulty(String remainingDifficulty) {
    this.remainingDifficulty = remainingDifficulty;
  }

  /** Retorna quando o registro foi criado. */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /** Retorna quando o registro foi atualizado. */
  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
