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
  /** Reserva a ocorrência mais recente para comandos de escrita na referência operacional. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<BusinessProcessActivityInstance>
      findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
          Long activityDefinitionId, String sourceReference);

  /** Consulta a ocorrência mais recente sem adquirir lock de escrita durante a leitura. */
  Optional<BusinessProcessActivityInstance>
      findFirstByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
          Long activityDefinitionId, String sourceReference);

  /** Lista as ocorrências de uma versão de processo para montar a visão hierárquica. */
  List<BusinessProcessActivityInstance>
      findAllByActivityDefinitionProcessDefinitionIdAndSourceReferenceOrderByActivityDefinitionIdAscOccurrenceNumberAsc(
          Long processDefinitionId, String sourceReference);

  /**
   * Lista ocorrências do processo estável vinculadas a um plano comercial do produto, inclusive
   * quando ainda não existe tarefa de agente.
   */
  List<BusinessProcessActivityInstance>
      findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceStartingWithOrderByCreatedAtDescIdDesc(
          String processCode, String sourceReferencePrefix);

  /** Lista ocorrências do processo estável para uma referência operacional exata. */
  List<BusinessProcessActivityInstance>
      findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceOrderByCreatedAtDescIdDesc(
          String processCode, String sourceReference);
}
