package com.marketinghub.repository.jpa.agenttask;

import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.AgentTaskMeasurementSnapshot;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: persistir e consultar as caixas de entrada dos agentes. */
public interface AgentTaskRepository extends JpaRepository<AgentTask, Long> {
  /** Informa se uma definição de processo já possui trabalho operacional vinculado. */
  boolean existsByProcessDefinitionId(Long processDefinitionId);

  /** Lista a caixa do agente com as solicitações mais recentes primeiro. */
  List<AgentTask> findByAssignedAgentAgentKeyOrderByCreatedAtDescIdDesc(String agentKey);

  /** Lista todas as tarefas que ainda exigem atuação, priorizando a atividade mais recente. */
  List<AgentTask> findByStatusInOrderByUpdatedAtDescIdDesc(List<String> statuses);

  /** Lista os registros vinculados a qualquer versão de um plano comercial. */
  List<AgentTask> findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc(
      String sourceReferencePrefix);

  /** Lista somente os campos necessários para medir tarefas de todas as versões de um plano. */
  @Query(
      """
      select new com.marketinghub.agenttask.AgentTaskMeasurementSnapshot(
        task.id,
        process.id,
        process.processCode,
        process.parentProcessCode,
        task.processActivityId,
        task.processActivityName,
        task.sourceReference,
        task.status,
        activityInstance.status,
        task.createdAt,
        task.updatedAt,
        task.deliveredAt,
        task.resultJson,
        task.evidenceJson,
        task.estimatedCostUsd)
      from AgentTask task
      left join task.processDefinition process
      left join task.activityInstance activityInstance
      where task.sourceReference like concat(:sourceReferencePrefix, '%')
      order by task.updatedAt desc, task.id desc
      """)
  List<AgentTaskMeasurementSnapshot> findMeasurementSnapshotsBySourceReferenceStartingWith(
      @Param("sourceReferencePrefix") String sourceReferencePrefix);

  /** Busca a tarefa mais recente pela referência exata, sem confundir ids com prefixo comum. */
  Optional<AgentTask> findTopBySourceReferenceOrderByUpdatedAtDescIdDesc(String sourceReference);

  /** Lista todo o histórico operacional de uma entidade para montar a instância BPM. */
  List<AgentTask> findBySourceReferenceOrderByCreatedAtAscIdAsc(String sourceReference);

  /** Lista somente os campos necessários para medir o histórico exato de um experimento. */
  @Query(
      """
      select new com.marketinghub.agenttask.AgentTaskMeasurementSnapshot(
        task.id,
        process.id,
        process.processCode,
        process.parentProcessCode,
        task.processActivityId,
        task.processActivityName,
        task.sourceReference,
        task.status,
        activityInstance.status,
        task.createdAt,
        task.updatedAt,
        task.deliveredAt,
        task.resultJson,
        task.evidenceJson,
        task.estimatedCostUsd)
      from AgentTask task
      left join task.processDefinition process
      left join task.activityInstance activityInstance
      where task.sourceReference = :sourceReference
      order by task.createdAt asc, task.id asc
      """)
  List<AgentTaskMeasurementSnapshot> findMeasurementSnapshotsBySourceReference(
      @Param("sourceReference") String sourceReference);

  /** Busca a delegação mais recente de um agente para uma origem operacional exata. */
  Optional<AgentTask> findTopByAssignedAgentAgentKeyAndSourceReferenceOrderByUpdatedAtDescIdDesc(
      String agentKey, String sourceReference);

  /** Lista tarefas operacionais da Têmis nos estados que ainda exigem execução. */
  List<AgentTask> findByAssignedAgentAgentKeyAndTaskKindAndStatusInOrderByCreatedAtAscIdAsc(
      String agentKey, String taskKind, List<String> statuses);

  /** Lista o trabalho ainda não reservado de um executor na ordem em que entrou no processo. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  List<AgentTask> findByAssignedAgentAgentKeyAndTaskKindAndStatusOrderByCreatedAtAscIdAsc(
      String agentKey, String taskKind, String status);

  /** Lista as tarefas da mesma execução de processo para validar predecessoras e gates. */
  List<AgentTask> findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
      Long processDefinitionId, String sourceReference);

  /** Lista todas as tentativas de uma instância para consolidar estado, custo e objetivo. */
  List<AgentTask> findByActivityInstanceIdOrderByCreatedAtAscIdAsc(Long activityInstanceId);

  /** Lista as atividades que já produziram ao menos um documento auditável concluído. */
  @Query(
      """
      select distinct task.processActivityId
      from AgentTask task
      where task.processDefinition.id = :processDefinitionId
        and task.processActivityId is not null
        and task.status = 'COMPLETED'
        and ((task.resultJson is not null and task.resultJson <> '')
          or (task.evidenceJson is not null and task.evidenceJson <> ''))
      """)
  List<String> findDocumentActivityIds(@Param("processDefinitionId") Long processDefinitionId);

  /** Busca os documentos mais recentes de uma atividade sem misturar processos ou atividades. */
  @Query(
      """
      select task
      from AgentTask task
      where task.processDefinition.id = :processDefinitionId
        and task.processActivityId = :activityId
        and task.status = 'COMPLETED'
        and ((task.resultJson is not null and task.resultJson <> '')
          or (task.evidenceJson is not null and task.evidenceJson <> ''))
      order by task.deliveredAt desc, task.updatedAt desc, task.id desc
      """)
  List<AgentTask> findRecentActivityDocuments(
      @Param("processDefinitionId") Long processDefinitionId,
      @Param("activityId") String activityId,
      Pageable pageable);

  /** Busca as tarefas mais recentes da atividade estável em todas as versões do mesmo processo. */
  @Query(
      """
      select task
      from AgentTask task
      where task.processDefinition.processCode = :processCode
        and (task.processActivityId = :activityId
          or exists (
            select coverage.id
            from AgentTaskActivityCoverage coverage
            where coverage.agentTask = task
              and coverage.activityDefinition.processDefinition.processCode = :processCode
              and coverage.activityDefinition.activityId = :activityId
          ))
      order by task.createdAt desc, task.id desc
      """)
  List<AgentTask> findRecentActivityExecutions(
      @Param("processCode") String processCode,
      @Param("activityId") String activityId,
      Pageable pageable);

  /** Busca os documentos mais recentes da definição inteira sem misturar outros processos. */
  @Query(
      """
      select task
      from AgentTask task
      where task.processDefinition.id = :processDefinitionId
        and task.processActivityId is not null
        and task.status = 'COMPLETED'
        and ((task.resultJson is not null and task.resultJson <> '')
          or (task.evidenceJson is not null and task.evidenceJson <> ''))
      order by task.deliveredAt desc, task.updatedAt desc, task.id desc
      """)
  List<AgentTask> findRecentProcessDocuments(
      @Param("processDefinitionId") Long processDefinitionId, Pageable pageable);
}
