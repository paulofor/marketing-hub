package com.marketinghub.repository.jpa.oprm.nichocnae;

import com.marketinghub.oprm.nichocnae.OprmSourceSnapshot;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositório responsável por persistir snapshots curtos de fontes coletadas no OPRM nicho CNAE. */
public interface OprmSourceSnapshotRepository extends JpaRepository<OprmSourceSnapshot, Long> {
  /** Lista snapshots de um ciclo na ordem de criação para detalhamento operacional. */
  List<OprmSourceSnapshot> findByResearchCycleIdOrderByIdAsc(Long researchCycleId);

  /** Verifica se uma fonte candidata já possui snapshot persistido. */
  boolean existsBySourceCandidateId(Long sourceCandidateId);

  /** Conta snapshots de um ciclo que ainda estão em determinado status de extração de sinais. */
  long countByResearchCycleIdAndSignalExtractionStatus(Long researchCycleId, String signalExtractionStatus);

  /** Lista snapshots completos pendentes de extração de sinais na ordem operacional. */
  List<OprmSourceSnapshot> findByFetchStatusAndSignalExtractionStatusOrderByResearchCycleIdAscIdAsc(
      String fetchStatus, String signalExtractionStatus, Pageable pageable);

  /** Lista snapshots pendentes somente de ciclos posicionados na etapa atual de extração de sinais. */
  @Query("""
      select snapshot
      from OprmSourceSnapshot snapshot
      join OprmRoutineResearchCycle cycle on cycle.id = snapshot.researchCycleId
      where snapshot.fetchStatus = :fetchStatus
        and snapshot.signalExtractionStatus = :signalExtractionStatus
        and cycle.currentStageCode = :currentStageCode
      order by snapshot.researchCycleId asc, snapshot.id asc
      """)
  List<OprmSourceSnapshot> findPendingByStatusAndCycleStage(
      @Param("fetchStatus") String fetchStatus,
      @Param("signalExtractionStatus") String signalExtractionStatus,
      @Param("currentStageCode") String currentStageCode,
      Pageable pageable);

  /** Remove snapshots de um ciclo antes de reexecutar etapas do mesmo job. */
  void deleteByResearchCycleId(Long researchCycleId);
}
