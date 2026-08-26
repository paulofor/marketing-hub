package com.marketinghub.repository.jpa.businessprocess;

import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir as atividades relacionais das versões de processo. */
public interface BusinessProcessActivityDefinitionRepository
    extends JpaRepository<BusinessProcessActivityDefinition, Long> {
  /** Lista as atividades de uma versão na ordem em que foram persistidas. */
  List<BusinessProcessActivityDefinition> findAllByProcessDefinitionIdOrderByIdAsc(
      Long processDefinitionId);

  /** Busca a identidade relacional de uma atividade do grafo. */
  Optional<BusinessProcessActivityDefinition> findByProcessDefinitionIdAndActivityId(
      Long processDefinitionId, String activityId);

  /** Remove a projeção relacional de um rascunho antes de recriá-la. */
  void deleteByProcessDefinitionId(Long processDefinitionId);
}
