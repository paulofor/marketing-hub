package com.marketinghub.oprm.cnae.repository;

import com.marketinghub.oprm.cnae.OprmCnaeProcessingCycle;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Repositório responsável por persistir e listar ciclos automáticos CNAE executados pelo OPRM.
 */
public interface OprmCnaeProcessingCycleRepository extends JpaRepository<OprmCnaeProcessingCycle, String> {
    /**
     * Lista ciclos mais recentes para acompanhamento operacional.
     */
    List<OprmCnaeProcessingCycle> findAllByOrderByStartedAtDesc(Pageable pageable);

    /**
     * Calcula o próximo número sequencial de ciclo por tipo.
     */
    @Query("select coalesce(max(c.cycleNumber), 0) + 1 from OprmCnaeProcessingCycle c where c.cycleType = :cycleType")
    Long nextCycleNumber(String cycleType);
}
