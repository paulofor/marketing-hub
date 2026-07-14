package com.marketinghub.repository.jpa.mds;

import com.marketinghub.mds.productevidence.v1.MdsProductEvidenceStageExecution;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA responsável pela persistência das etapas de evidência científica de produto. */
public interface MdsProductEvidenceStageExecutionRepository
        extends JpaRepository<MdsProductEvidenceStageExecution, Long> {

    /** Busca pendências de uma etapa científica em ordem de criação. */
    List<MdsProductEvidenceStageExecution> findByStageCodeAndStatusOrderByCreatedAtAsc(
            String stageCode,
            String status,
            Pageable pageable);

    /** Busca uma execução de etapa específica pelo identificador técnico. */
    Optional<MdsProductEvidenceStageExecution> findByIdAndStageCode(Long id, String stageCode);

    /** Busca a execução mais recente de uma etapa e status para um nicho. */
    Optional<MdsProductEvidenceStageExecution> findTopByMarketNicheIdAndStageCodeAndStatusOrderByCreatedAtDesc(
            Long marketNicheId,
            String stageCode,
            String status);

    /** Busca a execução mais recente de uma etapa e lista de status para um nicho. */
    Optional<MdsProductEvidenceStageExecution> findTopByMarketNicheIdAndStageCodeAndStatusInOrderByCreatedAtDesc(
            Long marketNicheId,
            String stageCode,
            List<String> statuses);

    /** Busca a execução mais recente ainda aberta para um nicho. */
    Optional<MdsProductEvidenceStageExecution> findTopByMarketNicheIdAndStatusInOrderByCreatedAtDesc(
            Long marketNicheId,
            List<String> statuses);
}
