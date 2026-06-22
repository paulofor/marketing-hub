package com.marketinghub.repository.jpa.oprm.nichocnae.v2;

import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecution;
import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2StageExecutionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório responsável por persistir execuções de etapa versionadas do NichoCNAE v2. */
public interface OprmNichoCnaeV2StageExecutionRepository extends JpaRepository<OprmNichoCnaeV2StageExecution, Long> {
    /** Lista execuções pendentes por etapa em ordem operacional estável. */
    List<OprmNichoCnaeV2StageExecution> findByStageCodeAndStatusOrderByCreatedAtAsc(
            String stageCode, OprmNichoCnaeV2StageExecutionStatus status, Pageable pageable);

    /** Verifica se o candidato já possui execução registrada para a etapa. */
    boolean existsBySourceNicheIdAndStageCode(Long sourceNicheId, String stageCode);

    /** Conta execuções já abertas para o candidato na etapa para gerar jobId estável de nova rodada manual. */
    long countBySourceNicheIdAndStageCode(Long sourceNicheId, String stageCode);

    /** Lista execuções de um CNAE para montar o acompanhamento administrativo por job. */
    List<OprmNichoCnaeV2StageExecution> findByCnaeCodeOrderByUpdatedAtDesc(String cnaeCode);

    /** Conta execuções por CNAE e estados informados para impedir dois jobs simultâneos no mesmo mercado. */
    long countByCnaeCodeAndStatusIn(String cnaeCode, List<OprmNichoCnaeV2StageExecutionStatus> statuses);

    /** Lista as etapas de um job em ordem cronológica para relatório de falha ou sucesso. */
    List<OprmNichoCnaeV2StageExecution> findByJobIdOrderByCreatedAtAsc(String jobId);

    /** Lista execuções abertas de um job para encerramento manual seguro pela tela administrativa. */
    List<OprmNichoCnaeV2StageExecution> findByJobIdAndStatusIn(
            String jobId, List<OprmNichoCnaeV2StageExecutionStatus> statuses);

    /** Localiza uma execução específica por etapa para callbacks internos do executor. */
    Optional<OprmNichoCnaeV2StageExecution> findByIdAndStageCode(Long id, String stageCode);
}
