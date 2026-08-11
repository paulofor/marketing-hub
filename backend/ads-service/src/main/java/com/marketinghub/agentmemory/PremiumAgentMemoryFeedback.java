package com.marketinghub.agentmemory;

import jakarta.persistence.*;
import java.time.Instant;

/** Responsabilidade: registrar a evidência independente que recalibra uma memória premium. */
@Entity
@Table(name = "premium_agent_memory_feedback")
public class PremiumAgentMemoryFeedback {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "memory_id", nullable = false)
  private PremiumAgentMemory memory;

  @Column(name = "outcome", nullable = false, length = 30)
  private String outcome;

  @Lob
  @Column(name = "evidence_text", nullable = false, columnDefinition = "LONGTEXT")
  private String evidence;

  @Column(name = "source_reference", length = 700)
  private String sourceReference;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** Define a memória avaliada. */
  public void setMemory(PremiumAgentMemory value) {
    memory = value;
  }

  /** Define o resultado independente. */
  public void setOutcome(String value) {
    outcome = value;
  }

  /** Define a evidência do resultado. */
  public void setEvidence(String value) {
    evidence = value;
  }

  /** Define a fonte oficial. */
  public void setSourceReference(String value) {
    sourceReference = value;
  }

  /** Define o instante do feedback. */
  public void setCreatedAt(Instant value) {
    createdAt = value;
  }
}
