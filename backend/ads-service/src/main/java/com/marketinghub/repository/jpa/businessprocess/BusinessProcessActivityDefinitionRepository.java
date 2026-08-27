package com.marketinghub.repository.jpa.businessprocess;

import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: persistir as atividades relacionais das versões de processo. */
public interface BusinessProcessActivityDefinitionRepository
    extends JpaRepository<BusinessProcessActivityDefinition, Long> {
  /** Lista as atividades de uma versão na ordem em que foram persistidas. */
  List<BusinessProcessActivityDefinition> findAllByProcessDefinitionIdOrderByIdAsc(
      Long processDefinitionId);

  /** Busca a identidade relacional de uma atividade do grafo. */
  Optional<BusinessProcessActivityDefinition> findByProcessDefinitionIdAndActivityId(
      Long processDefinitionId, String activityId);

  /** Remove imediatamente a projeção relacional antes de recriá-la com as mesmas identidades. */
  @Modifying(flushAutomatically = true)
  @Query(
      "delete from BusinessProcessActivityDefinition activity "
          + "where activity.processDefinition.id = :processDefinitionId")
  int deleteByProcessDefinitionId(@Param("processDefinitionId") Long processDefinitionId);
}
