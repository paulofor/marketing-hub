package com.marketinghub.repository.jpa.agenttask;

import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

/** Responsabilidade: persistir ocorrências de atividades e seus estados consolidados. */
public interface BusinessProcessActivityInstanceRepository
    extends JpaRepository<BusinessProcessActivityInstance, Long> {
  /** Busca a ocorrência atual ou mais recente de uma atividade para a referência operacional. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<BusinessProcessActivityInstance>
      findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
          Long activityDefinitionId, String sourceReference);

  /** Lista as ocorrências de uma versão de processo para montar a visão hierárquica. */
  List<BusinessProcessActivityInstance>
      findAllByActivityDefinitionProcessDefinitionIdAndSourceReferenceOrderByActivityDefinitionIdAscOccurrenceNumberAsc(
          Long processDefinitionId, String sourceReference);
}
