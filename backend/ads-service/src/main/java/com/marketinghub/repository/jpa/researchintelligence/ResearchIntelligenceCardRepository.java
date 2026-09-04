package com.marketinghub.repository.jpa.researchintelligence;

import com.marketinghub.researchintelligence.v1.ResearchIntelligenceCard;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acessa e bloqueia a identidade lógica usada para numerar versões de cartões. */
public interface ResearchIntelligenceCardRepository
    extends JpaRepository<ResearchIntelligenceCard, String> {

  /** Cria a identidade caso ainda não exista sem transformar concorrência em erro. */
  @Modifying
  @Query(
      value =
          "INSERT IGNORE INTO research_intelligence_card "
              + "(card_key, created_at, updated_at, row_version) VALUES (:cardKey, :now, :now, 0)",
      nativeQuery = true)
  int insertIfMissing(@Param("cardKey") String cardKey, @Param("now") LocalDateTime now);

  /** Serializa novas versões e transições da mesma chave lógica. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select card from ResearchIntelligenceCard card where card.cardKey = :cardKey")
  Optional<ResearchIntelligenceCard> findByCardKeyForUpdate(@Param("cardKey") String cardKey);
}
