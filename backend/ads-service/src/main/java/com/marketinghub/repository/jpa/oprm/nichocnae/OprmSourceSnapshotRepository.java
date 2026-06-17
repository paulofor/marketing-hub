package com.marketinghub.repository.jpa.oprm.nichocnae;

import com.marketinghub.oprm.nichocnae.OprmSourceSnapshot;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório responsável por persistir snapshots curtos de fontes coletadas no OPRM nicho CNAE. */
public interface OprmSourceSnapshotRepository extends JpaRepository<OprmSourceSnapshot, Long> {
  /** Lista snapshots de um ciclo na ordem de criação para detalhamento operacional. */
  List<OprmSourceSnapshot> findByResearchCycleIdOrderByIdAsc(Long researchCycleId);

  /** Verifica se uma fonte candidata já possui snapshot persistido. */
  boolean existsBySourceCandidateId(Long sourceCandidateId);

  /** Lista snapshots completos pendentes de extração de sinais na ordem operacional. */
  List<OprmSourceSnapshot> findByFetchStatusAndSignalExtractionStatusOrderByResearchCycleIdAscIdAsc(
      String fetchStatus, String signalExtractionStatus, Pageable pageable);

  /** Remove snapshots de um ciclo antes de reexecutar etapas do mesmo job. */
  void deleteByResearchCycleId(Long researchCycleId);
}
