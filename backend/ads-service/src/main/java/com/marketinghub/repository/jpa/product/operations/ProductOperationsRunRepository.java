package com.marketinghub.repository.jpa.product.operations;

import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Responsabilidade: consultar a operação por produto sem ampliar as dependências do coordenador.
 */
public interface ProductOperationsRunRepository extends Repository<ProcessRun, Long> {
  /** Lê uma delegação persistida para conferir sua identidade antes de segui-la. */
  Optional<ProcessRun> findById(Long id);

  /** Consulta a fila real da família PDE, isolando o ciclo e preservando pausas após os ativos. */
  @Query(
      """
      select r from ProcessRun r
      where r.productId = :productId and r.parentRunId is null
        and r.status not in ('COMPLETED', 'CLOSED')
        and ((:cycleId is null and r.learningCycleId is null) or r.learningCycleId = :cycleId)
        and (:sourceReference is null or r.sourceReference = :sourceReference)
        and exists (select c.id from BusinessProcessChainDefinition c
                    where c.id = r.chainDefinitionId and c.chainCode = 'pde-value-creation-delivery')
      order by case when r.status in ('PAUSED', 'ERROR') then 1 else 0 end, r.id
      """)
  List<ProcessRun> findOpenRootsForOperations(
      @Param("productId") Long productId,
      @Param("cycleId") Long cycleId,
      @Param("sourceReference") String sourceReference);
}
