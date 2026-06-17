package com.marketinghub.repository.jpa.oprm.nichocnae;

import com.marketinghub.oprm.nichocnae.OprmExtractedSignal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório responsável por persistir e consultar sinais extraídos do pipeline OPRM nicho CNAE. */
public interface OprmExtractedSignalRepository extends JpaRepository<OprmExtractedSignal, Long> {
  /** Lista os sinais extraídos de um ciclo na ordem de persistência para síntese posterior. */
  List<OprmExtractedSignal> findByResearchCycleIdOrderByIdAsc(Long researchCycleId);

  /** Verifica se um snapshot curto já teve sinais extraídos e persistidos. */
  boolean existsBySourceSnapshotId(Long sourceSnapshotId);

  /** Remove sinais de um ciclo antes de reexecutar etapas do mesmo job. */
  void deleteByResearchCycleId(Long researchCycleId);
}
