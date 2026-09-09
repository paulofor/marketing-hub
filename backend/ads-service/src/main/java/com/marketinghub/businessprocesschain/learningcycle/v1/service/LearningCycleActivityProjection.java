package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityExecutionControlResponse;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityExecutionGroupResponse;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: apresentar o ciclo persistido na atividade de chamada do processo pai. */
@Component
@RequiredArgsConstructor
public class LearningCycleActivityProjection {
  private final LearningCycleService service;
  private final LearningSalesCycleRepository cycles;
  private final BusinessProcessActivityInstanceRepository instances;
  private final LearningSalesCycleEventRepository events;

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private SalesFlowResolver salesFlowResolver;

  /** Projeta o fluxo vinculado sem criar execução nem aprovar retroativamente tarefas antigas. */
  @Transactional(readOnly = true)
  public List<ProductProcessActivityExecutionGroupResponse> apply(
      BusinessProcessDefinition process,
      Long productId,
      Long explicitCycleId,
      Map<String, BusinessProcessActivityDefinition> definitions,
      List<ProductProcessActivityExecutionGroupResponse> groups) {
    return apply(process, productId, explicitCycleId, definitions, groups, null);
  }

  /** Mantém a cadeia solicitada, inclusive quando o mesmo processo participa de várias versões. */
  @Transactional(readOnly = true)
  public List<ProductProcessActivityExecutionGroupResponse> apply(
      BusinessProcessDefinition process,
      Long productId,
      Long explicitCycleId,
      Map<String, BusinessProcessActivityDefinition> definitions,
      List<ProductProcessActivityExecutionGroupResponse> groups,
      Long chainId) {
    boolean callsCycle =
        definitions.values().stream()
            .anyMatch(
                definition ->
                    LearningCycleRules.PROCESS_CODE.equals(definition.getSubprocessCode()));
    if (!callsCycle) return groups;
    if (salesFlowResolver != null) {
      var flow = salesFlowResolver.resolve(productId, process, chainId, explicitCycleId);
      if (flow != null) return projectFlow(flow, groups);
    }
    var explicit =
        explicitCycleId == null
            ? null
            : cycles
                .findById(explicitCycleId)
                .filter(cycle -> Objects.equals(cycle.getProductId(), productId))
                .orElse(null);
    if (explicit != null
        && chainId != null
        && !Objects.equals(explicit.getChainDefinitionId(), chainId))
      return unavailable(definitions, groups);
    var entry =
        service.entry(
            process.getId(),
            productId,
            chainId != null ? chainId : explicit == null ? null : explicit.getChainDefinitionId());
    if (entry == null
        || !entry.integrated()
        || !Objects.equals(entry.parentProcessDefinitionId(), process.getId())
        || explicitCycleId != null && explicit == null) return unavailable(definitions, groups);
    var cycle =
        explicitCycleId == null
            ? cycles
                .findFirstByProductIdAndChainDefinitionIdOrderByIdDesc(
                    productId, entry.chainDefinitionId())
                .orElse(null)
            : explicit;
    if (cycle != null && !Objects.equals(cycle.getChainDefinitionId(), entry.chainDefinitionId()))
      return unavailable(definitions, groups);
    var latestEvent =
        cycle == null
            ? null
            : events.findFirstByCycleIdOrderByRevisionDesc(cycle.getId()).orElse(null);
    boolean measurementBlocked =
        latestEvent != null
            && "MEASUREMENT_BLOCKED".equals(latestEvent.getAction())
            && "MEASUREMENT".equals(cycle.getStage());
    var instance =
        cycle == null || cycle.getCurrentInstanceId() == null
            ? null
            : instances.findById(cycle.getCurrentInstanceId()).orElse(null);
    boolean blocked =
        cycle != null
            && "OPEN".equals(cycle.getStatus())
            && (measurementBlocked || instance != null && "BLOCKED".equals(instance.getStatus()));
    boolean finished = cycle != null && cycle.getClosedAt() != null;
    String reason =
        cycle == null
            ? "Nenhum ciclo registrado nesta versão da cadeia. Abra o subprocesso para selecionar o experimento."
            : "Ciclo #"
                + cycle.getId()
                + " · experimento #"
                + cycle.getExperimentId()
                + " · "
                + LearningCycleRules.label(cycle.getStage())
                + ". "
                + (blocked
                    ? measurementBlocked ? latestEvent.getSummary() : instance.getBlockedReason()
                    : finished
                        ? "Decisão encerrada e aprendizado preservado; consulte o resultado e a continuidade."
                        : "Retome o subprocesso para executar a próxima ação desta etapa.");
    String url =
        "/business-process-chains/learning-cycles?chainId="
            + entry.chainDefinitionId()
            + "&productId="
            + productId
            + (cycle == null ? "" : "&cycleId=" + cycle.getId());
    var control =
        new ProductProcessActivityExecutionControlResponse(
            "BACKEND",
            "SUBPROCESS",
            cycle == null
                ? "Abrir subprocesso de aprendizado e vendas"
                : finished
                    ? "Consultar subprocesso · ciclo #" + cycle.getId()
                    : "Retomar subprocesso · ciclo #" + cycle.getId(),
            "Subprocesso: "
                + entry.processName()
                + ". O resultado e a próxima decisão pertencem a esta atividade do processo pai.",
            cycle != null || entry.canStartCycle(),
            reason,
            false,
            null,
            null,
            null,
            null,
            null,
            cycle == null ? entry.processDefinitionId() : cycle.getProcessDefinitionId(),
            List.of(),
            "DETAILED",
            cycle == null ? null : "internal://learning-cycles/" + cycle.getId(),
            url);
    return groups.stream()
        .map(
            group -> {
              if (!Objects.equals(group.activityId(), entry.activityId())) return group;
              return new ProductProcessActivityExecutionGroupResponse(
                  group.activityDefinitionId(),
                  group.activityId(),
                  group.activityName(),
                  "Conduzir a decisão comercial a partir dos resultados conciliados e direcionar a próxima melhoria de produto, comunicação ou investimento.",
                  group.activityOwnerName(),
                  group.sequenceNumber(),
                  group.selectedVersionActivity(),
                  cycle == null
                      ? "NOT_STARTED"
                      : blocked ? "BLOCKED" : finished ? "COMPLETED" : "IN_PROGRESS",
                  reason,
                  finished,
                  cycle == null ? "NOT_RECORDED" : "SUBPROCESS",
                  null,
                  null,
                  group.taskCount(),
                  group.tasks(),
                  false,
                  reason,
                  control);
            })
        .toList();
  }

  /** Projeta todas as atividades a partir da mesma passagem comercial usada pelo histórico. */
  private List<ProductProcessActivityExecutionGroupResponse> projectFlow(
      com.marketinghub.businessprocesschain.learningcycle.v1.service.getSalesFlow.SalesFlowResponse
          flow,
      List<ProductProcessActivityExecutionGroupResponse> groups) {
    return groups.stream()
        .map(
            group -> {
              var state =
                  flow.activities().stream()
                      .filter(item -> item.activityId().equals(group.activityId()))
                      .findFirst()
                      .orElse(null);
              if (state == null) return group;
              boolean cycleEntry = "learningCycle".equals(group.activityId());
              boolean measurement = "consolidate".equals(group.activityId());
              boolean actionable = cycleEntry || measurement;
              boolean executing = List.of("IN_PROGRESS", "BLOCKED").contains(state.state());
              var previous = group.executionControl();
              String url =
                  actionable
                      ? flow.navigationUrl()
                      : previous == null ? null : previous.navigationUrl();
              Long target =
                  actionable
                      ? cycles.findById(flow.cycleId()).orElseThrow().getProcessDefinitionId()
                      : previous == null ? null : previous.targetProcessDefinitionId();
              if (!actionable && target != null && executing)
                url =
                    "/products/"
                        + flow.productId()
                        + "/value-chain-history/processes/"
                        + target
                        + "/activities?learningCycleId="
                        + flow.cycleId();
              var control =
                  new ProductProcessActivityExecutionControlResponse(
                      "BACKEND",
                      measurement
                          ? "AUTOMATIC"
                          : cycleEntry
                              ? "SUBPROCESS"
                              : previous == null ? "SUBPROCESS" : previous.interactionType(),
                      cycleEntry
                          ? "Retomar subprocesso · ciclo #" + flow.cycleId()
                          : measurement
                              ? "Consultar conciliação automática"
                              : executing
                                  ? "Abrir subprocesso"
                                  : "Consultar histórico do subprocesso",
                      state.reason(),
                      actionable || target != null,
                      state.reason(),
                      false,
                      null,
                      null,
                      null,
                      null,
                      null,
                      target,
                      List.of(),
                      "DETAILED",
                      state.evidenceReference(),
                      url);
              return new ProductProcessActivityExecutionGroupResponse(
                  group.activityDefinitionId(),
                  group.activityId(),
                  group.activityName(),
                  group.activityObjective(),
                  group.activityOwnerName(),
                  state.sequenceNumber(),
                  group.selectedVersionActivity(),
                  state.state(),
                  state.reason(),
                  state.objectiveAchieved(),
                  state.evidenceReference() == null ? "NOT_RECORDED" : "SALES_FLOW_EVENT",
                  null,
                  null,
                  group.taskCount(),
                  group.tasks(),
                  false,
                  state.reason(),
                  control);
            })
        .sorted(
            java.util.Comparator.comparing(
                ProductProcessActivityExecutionGroupResponse::sequenceNumber))
        .toList();
  }

  /**
   * Mantém o histórico legível e retira atalhos quando o vínculo operacional não foi comprovado.
   */
  private List<ProductProcessActivityExecutionGroupResponse> unavailable(
      Map<String, BusinessProcessActivityDefinition> definitions,
      List<ProductProcessActivityExecutionGroupResponse> groups) {
    String reason =
        "O vínculo desta atividade com o ciclo não está disponível nesta versão da cadeia. Consulte o BPM responsável.";
    return groups.stream()
        .map(
            group -> {
              var definition = definitions.get(group.activityId());
              if (definition == null
                  || !LearningCycleRules.PROCESS_CODE.equals(definition.getSubprocessCode()))
                return group;
              var control =
                  new ProductProcessActivityExecutionControlResponse(
                      "BACKEND",
                      "SUBPROCESS",
                      null,
                      reason,
                      false,
                      reason,
                      false,
                      null,
                      null,
                      null,
                      null,
                      null,
                      null,
                      List.of());
              return new ProductProcessActivityExecutionGroupResponse(
                  group.activityDefinitionId(),
                  group.activityId(),
                  group.activityName(),
                  group.activityObjective(),
                  group.activityOwnerName(),
                  group.sequenceNumber(),
                  group.selectedVersionActivity(),
                  group.operationalState(),
                  group.stateReason(),
                  group.objectiveAchieved(),
                  group.stateEvidence(),
                  group.activityInstanceId(),
                  group.occurrenceNumber(),
                  group.taskCount(),
                  group.tasks(),
                  false,
                  reason,
                  control);
            })
        .toList();
  }
}
