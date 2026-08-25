package com.marketinghub.repository.jpa.salesvideo;

import com.marketinghub.salesvideo.VideoReferenceAnalysisExecution;
import com.marketinghub.salesvideo.VideoReferenceAnalysisStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositório canônico da fila versionada de análise de vídeos de referência. */
public interface VideoReferenceAnalysisExecutionRepository
    extends JpaRepository<VideoReferenceAnalysisExecution, Long> {

  /** Bloqueia as primeiras execuções pendentes ou abandonadas para claim transacional. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select e from VideoReferenceAnalysisExecution e "
          + "where e.status = :queued or (e.status = :running and e.claimedAt < :staleBefore) "
          + "order by e.createdAt asc, e.id asc")
  List<VideoReferenceAnalysisExecution> findClaimable(
      @Param("queued") VideoReferenceAnalysisStatus queued,
      @Param("running") VideoReferenceAnalysisStatus running,
      @Param("staleBefore") Instant staleBefore,
      Pageable pageable);

  /** Consulta a execução mais recente da referência no tenant informado. */
  Optional<VideoReferenceAnalysisExecution> findFirstByTenantIdAndReferenceIdOrderByIdDesc(
      String tenantId, Long referenceId);

  /** Consulta o maior número de tentativa já persistido para a referência. */
  Optional<VideoReferenceAnalysisExecution> findFirstByReferenceIdOrderByAttemptNumberDesc(
      Long referenceId);
}
