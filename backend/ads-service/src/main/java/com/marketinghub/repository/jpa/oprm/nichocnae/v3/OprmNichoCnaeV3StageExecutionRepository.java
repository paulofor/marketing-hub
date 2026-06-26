package com.marketinghub.repository.jpa.oprm.nichocnae.v3;

import com.marketinghub.pipelines.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecution;
import com.marketinghub.pipelines.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecutionStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository JPA canônico das execuções de etapa do NichoCNAE v3. */
public interface OprmNichoCnaeV3StageExecutionRepository extends JpaRepository<OprmNichoCnaeV3StageExecution, Long> {
    /** Lista pendências de uma etapa específica em ordem de criação. */
    List<OprmNichoCnaeV3StageExecution> findTop10ByStageCodeAndStatusOrderByCreatedAtAsc(
            String stageCode, OprmNichoCnaeV3StageExecutionStatus status);

    /** Verifica se o job já possui pendência ou conclusão para uma etapa específica. */
    boolean existsByJobIdAndStageCode(String jobId, String stageCode);

    /** Busca uma etapa específica dentro do job para confirmação manual do avanço. */
    Optional<OprmNichoCnaeV3StageExecution> findByJobIdAndStageCode(String jobId, String stageCode);

    /** Busca a entrada mais recente de um CNAE para recuperar o último job iniciado na tela. */
    Optional<OprmNichoCnaeV3StageExecution> findTop1ByCnaeCodeAndStageCodeOrderByCreatedAtDesc(
            String cnaeCode, String stageCode);

    /** Lista todas as etapas persistidas de um job em ordem de criação. */
    List<OprmNichoCnaeV3StageExecution> findByJobIdOrderByCreatedAtAsc(String jobId);

    /** Lista pendências antigas para o Ops Monitor detectar fila v3 parada. */
    List<OprmNichoCnaeV3StageExecution> findTop20ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
            OprmNichoCnaeV3StageExecutionStatus status, Instant threshold);
}
