package com.marketinghub.repository.jpa.feo.fabricacao.v1;

import com.marketinghub.feo.fabricacao.v1.FeoFabricacaoV1StageExecution;
import com.marketinghub.feo.fabricacao.v1.FeoFabricacaoV1StageStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: consultar e persistir execuções de etapa da FEO v1. */
public interface FeoFabricacaoV1StageExecutionRepository
    extends JpaRepository<FeoFabricacaoV1StageExecution, Long> {

  /** Lista pendências da etapa em ordem de criação para consumo pelo worker. */
  List<FeoFabricacaoV1StageExecution> findByStageCodeAndStatusOrderByCreatedAtAsc(
      String stageCode, FeoFabricacaoV1StageStatus status, Pageable pageable);

  /** Lista pendências novas ou execuções antigas sem callback para retry operacional. */
  @Query(
      """
            select e
            from FeoFabricacaoV1StageExecution e
            where e.stageCode = :stageCode
              and (
                e.status = com.marketinghub.feo.fabricacao.v1.FeoFabricacaoV1StageStatus.PENDING
                or (
                  e.status = com.marketinghub.feo.fabricacao.v1.FeoFabricacaoV1StageStatus.RUNNING
                  and e.startedAt < :staleBefore
                )
              )
            order by e.createdAt asc
            """)
  List<FeoFabricacaoV1StageExecution> findPendingOrStaleRunning(
      @Param("stageCode") String stageCode,
      @Param("staleBefore") Instant staleBefore,
      Pageable pageable);

  /** Busca execução específica protegendo o callback contra troca de etapa. */
  Optional<FeoFabricacaoV1StageExecution> findByIdAndStageCode(Long id, String stageCode);

  /** Verifica se já existe etapa inicial ativa para evitar duplicidade por experimento. */
  boolean existsByExperimentIdAndStageCodeAndStatusIn(
      Long experimentId, String stageCode, List<FeoFabricacaoV1StageStatus> statuses);

  /** Lista histórico recente da FEO para um experimento. */
  List<FeoFabricacaoV1StageExecution> findTop20ByExperimentIdOrderByCreatedAtDesc(
      Long experimentId);

  /**
   * Busca a montagem final mais recente concluída para publicar artefatos no ZIP do experimento.
   */
  Optional<FeoFabricacaoV1StageExecution>
      findFirstByExperimentIdAndStageCodeAndStatusOrderByFinishedAtDesc(
          Long experimentId, String stageCode, FeoFabricacaoV1StageStatus status);
}
