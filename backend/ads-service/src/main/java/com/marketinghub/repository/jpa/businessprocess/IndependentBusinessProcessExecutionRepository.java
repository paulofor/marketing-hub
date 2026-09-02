package com.marketinghub.repository.jpa.businessprocess;

import com.marketinghub.businessprocess.independent.IndependentBusinessProcessExecution;
import com.marketinghub.businessprocess.independent.service.executions.IndependentBusinessProcessExecutionListSnapshot;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: persistir a identidade das execuções de processo sem produto. */
public interface IndependentBusinessProcessExecutionRepository
    extends JpaRepository<IndependentBusinessProcessExecution, Long> {

  /** Localiza uma solicitação idempotente antes de criar trabalho operacional. */
  Optional<IndependentBusinessProcessExecution> findByRequestKey(String requestKey);

  /** Lista a primeira página sem hidratar os campos extensos da definição do processo. */
  @Query(
      """
      select new com.marketinghub.businessprocess.independent.service.executions.IndependentBusinessProcessExecutionListSnapshot(
        execution.id,
        execution.requestKey,
        process.id,
        process.processCode,
        process.name,
        process.versionNumber,
        execution.sourceReference,
        execution.displayName,
        execution.requestedByName,
        execution.inputJson,
        execution.createdAt)
      from IndependentBusinessProcessExecution execution
      join execution.processDefinition process
      where execution.sourceReference is not null
      order by execution.id desc
      """)
  List<IndependentBusinessProcessExecutionListSnapshot> findListSnapshots(Pageable pageable);

  /** Lista uma página histórica anterior usando cursor apoiado pela chave primária. */
  @Query(
      """
      select new com.marketinghub.businessprocess.independent.service.executions.IndependentBusinessProcessExecutionListSnapshot(
        execution.id,
        execution.requestKey,
        process.id,
        process.processCode,
        process.name,
        process.versionNumber,
        execution.sourceReference,
        execution.displayName,
        execution.requestedByName,
        execution.inputJson,
        execution.createdAt)
      from IndependentBusinessProcessExecution execution
      join execution.processDefinition process
      where execution.sourceReference is not null
        and execution.id < :beforeId
      order by execution.id desc
      """)
  List<IndependentBusinessProcessExecutionListSnapshot> findListSnapshotsBeforeId(
      @Param("beforeId") Long beforeId, Pageable pageable);
}
