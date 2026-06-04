package com.marketinghub.repository.jpa.oprm.nichocnae;

import com.marketinghub.oprm.nichocnae.OprmNicheRoutineCard;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório responsável por persistir e consultar cartões de rotina do pipeline OPRM NichoCNAE. */
public interface OprmNicheRoutineCardRepository extends JpaRepository<OprmNicheRoutineCard, Long> {
  /** Verifica se o ciclo já possui cartão de rotina sintetizado. */
  boolean existsByResearchCycleId(Long researchCycleId);

  /** Busca o cartão de rotina mais recente de um ciclo. */
  Optional<OprmNicheRoutineCard> findFirstByResearchCycleIdOrderByIdDesc(Long researchCycleId);
}
