package com.marketinghub.researchintelligence.v1;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Mantém a identidade lógica bloqueável de um cartão ao longo de suas versões. */
@Entity
@Table(name = "research_intelligence_card")
@Getter
@NoArgsConstructor
public class ResearchIntelligenceCard {
  @Id
  @Column(name = "card_key", nullable = false, length = 120)
  private String cardKey;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Version
  @Column(name = "row_version", nullable = false)
  private Long rowVersion;

  /** Cria a raiz imutável usada para serializar a numeração de versões. */
  public ResearchIntelligenceCard(String cardKey, LocalDateTime now) {
    this.cardKey = cardKey;
    this.createdAt = now;
    this.updatedAt = now;
    this.rowVersion = 0L;
  }

  /** Atualiza a raiz para materializar o bloqueio otimista após nova versão. */
  public void touch(LocalDateTime now) {
    this.updatedAt = now;
  }
}
