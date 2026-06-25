package com.marketinghub.repository.jpa.oprm.nichocnae.v3;

import com.marketinghub.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecution;
import com.marketinghub.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecutionStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository JPA canônico das execuções de etapa do NichoCNAE v3. */
public interface OprmNichoCnaeV3StageExecutionRepository extends JpaRepository<OprmNichoCnaeV3StageExecution, Long> {
    /** Lista pendências de uma etapa específica em ordem de criação. */
    List<OprmNichoCnaeV3StageExecution> findTop10ByStageCodeAndStatusOrderByCreatedAtAsc(
            String stageCode, OprmNichoCnaeV3StageExecutionStatus status);

    /** Verifica se o job já possui pendência ou conclusão para uma etapa específica. */
    boolean existsByJobIdAndStageCode(String jobId, String stageCode);
}
