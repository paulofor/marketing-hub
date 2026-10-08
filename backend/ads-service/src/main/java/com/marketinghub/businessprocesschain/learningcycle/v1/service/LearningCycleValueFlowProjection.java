package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles.LearningCycleValueFlow.*;
import com.marketinghub.pde.kit.privateprototype.v1.service.KitPrototypeCapabilities;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.kit.KitPrivateArtifactRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository;
import com.marketinghub.repository.jpa.processautomation.ProcessRunRepository;
import java.math.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: resumir provas persistidas sem executar agentes ou recomputar números
 * comerciais.
 */
@Component
@RequiredArgsConstructor
public class LearningCycleValueFlowProjection {
  private final KitPrototypeCapabilities capabilities;
  private final KitPrivateArtifactRepository artifacts;
  private final AgentTaskRepository tasks;
  private final LearningSalesCycleEventRepository events;
  private final ProcessRunRepository runs;
  private final LearningCyclePrototypeContext prototype;
  private final LearningCycleJson json;
  private static final Set<String> ACTIVE = Set.of("PENDING", "RUNNING", "IN_PROGRESS", "QUEUED");

  /** Resolve a atividade e sua pausa no escopo exato, mantendo valores sem fonte desconhecidos. */
  public LearningCycleValueFlow resolve(
      LearningSalesCycle cycle, LearningCycleProcessContext.Work work) {
    var capability = capabilities.resolve(cycle);
    var compositions = artifacts.summaries(cycle.getId());
    boolean registered = prototype.resolve(cycle).isPresent();
    var pausedRun =
        !"OPEN".equals(cycle.getStatus()) || work == null
            ? Optional.<ProcessRun>empty()
            : runs.findFirstByProductIdAndProcessDefinitionIdAndChainDefinitionIdAndLearningCycleIdAndSourceReferenceOrderByIdDesc(
                    cycle.getProductId(),
                    work.processDefinitionId(),
                    cycle.getChainDefinitionId(),
                    cycle.getId(),
                    "experiment:" + cycle.getExperimentId())
                .filter(r -> "PAUSED".equals(r.getStatus()));
    boolean paused = pausedRun.isPresent();
    var snapshots =
        tasks.findFunctionalSnapshots(
            "experiment:" + cycle.getExperimentId(),
            Set.of(
                "pde-commercial-plan-offer",
                "pde-construction-approval",
                "pde-communication-sales-journey"),
            cycle.getCreatedAt());
    var latest =
        new LinkedHashMap<String, com.marketinghub.agenttask.AgentTaskFunctionalSnapshot>();
    snapshots.stream()
        .sorted(
            Comparator.comparing(com.marketinghub.agenttask.AgentTaskFunctionalSnapshot::id)
                .reversed())
        .forEach(t -> latest.putIfAbsent(t.processCode() + ":" + t.processActivityId(), t));
    boolean active =
        compositions.stream().anyMatch(c -> Set.of("QUEUED", "RUNNING").contains(c.getStatus()))
            || latest.values().stream().anyMatch(t -> ACTIVE.contains(t.status()))
            || work != null && "IN_PROGRESS".equals(work.state());
    int ready = (int) compositions.stream().filter(c -> "READY".equals(c.getStatus())).count();
    boolean implementationPending = capability.available() && !registered;
    String situation =
        paused
            ? "A continuidade automática deste ciclo está pausada. As entregas aceitas foram preservadas."
            : implementationPending
                ? (active
                    ? "A implementação privada está em composição."
                    : ready > 0
                        ? "O pacote está utilizável; a prova da versão aguarda registro para homologação."
                        : "Os contratos estão prontos; falta comprovar a implementação privada.")
                : registered && work != null
                    ? "A versão privada está registrada e aguarda a entrega de "
                        + work.responsible()
                        + "."
                    : work == null
                        ? cycle.getStatus().equals("OPEN")
                            ? "O ciclo aguarda sua próxima passagem autorizada."
                            : "Este ciclo está encerrado; suas provas e custos foram preservados."
                        : work.reason();
    String blocker =
        paused
            ? "A próxima atividade aguarda a retomada da execução #"
                + pausedRun.orElseThrow().getId()
                + ". "
                + pausedRun.orElseThrow().getReason()
            : implementationPending
                ? "A versão precisa comprovar uso, arquivos íntegros, recuperação e isolamento antes da revisão independente."
                : work != null
                    ? work.reason()
                    : "AUTHORIZATION".equals(cycle.getStage())
                        ? "A autorização comercial própria ainda precisa ser registrada."
                        : "As provas de venda e contribuição dependem do experimento comercial autorizado e da conciliação.";
    Instant since =
        active || !"OPEN".equals(cycle.getStatus())
            ? null
            : latest.values().stream()
                .map(t -> t.deliveredAt() == null ? t.createdAt() : t.deliveredAt())
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(cycle.getCreatedAt());
    var ledger = events.findByCycleIdOrderByRevisionAsc(cycle.getId());
    if (since != null) {
      var delivery =
          compositions.stream()
              .map(c -> c.getFinishedAt())
              .filter(Objects::nonNull)
              .max(Comparator.naturalOrder())
              .orElse(since);
      var registration =
          ledger.stream()
              .filter(e -> "REGISTER_PROTOTYPE".equals(e.getAction()))
              .map(e -> e.getCreatedAt())
              .max(Comparator.naturalOrder())
              .orElse(since);
      if (delivery.isAfter(since)) since = delivery;
      if (registration.isAfter(since)) since = registration;
    }
    var target =
        ledger.stream()
            .filter(e -> "DEFINE_CONTRIBUTION_TARGET".equals(e.getAction()))
            .reduce((a, b) -> b)
            .map(
                e ->
                    json.read(e.getEvidenceJson())
                        .path("minimumContributionPercent")
                        .decimalValue())
            .orElse(null);
    var measure =
        ledger.stream()
            .filter(
                e ->
                    "MEASURE".equals(e.getAction())
                        || "RECONCILE_MEASUREMENT".equals(e.getAction())
                        || "RECONCILE".equals(e.getAction())
                        || "MEASUREMENT".equals(e.getToStage()))
            .filter(
                e ->
                    json.read(e.getEvidenceJson()).has("netSales")
                        && json.read(e.getEvidenceJson()).path("dataValid").asBoolean(false)
                        && json.read(e.getEvidenceJson()).path("testDataExcluded").asBoolean(false))
            .reduce((a, b) -> b)
            .orElse(null);
    var scenarios = new ArrayList<Scenario>();
    var outputs = new ArrayList<Deliverable>();
    for (var task : latest.values()) {
      var result = task.resultJson() == null ? null : json.read(task.resultJson());
      boolean completed = "COMPLETED".equals(task.status());
      String output =
          !completed
              ? "Ainda não produziu uma saída aceita."
              : Set.of("journey", "deliverables", "access", "productArchitecture")
                      .contains(task.processActivityId())
                  ? "Contrato estruturado. A especificação não comprova implementação."
                  : "technicalHomologation".equals(task.processActivityId())
                      ? "Parecer técnico independente com suas evidências."
                      : "Artefato estruturado disponível na atividade; não comprova venda.";
      outputs.add(
          new Deliverable(
              task.id(),
              task.processActivityId(),
              task.agentKey(),
              task.status(),
              output,
              task.deliveredAt()));
      if (completed && "economics".equals(task.processActivityId()) && result != null)
        for (var s : result.path("scenarios")) {
          if (!s.path("priceBrl").isNumber() || !s.path("variableCostBrl").isNumber()) continue;
          var price = s.path("priceBrl").decimalValue();
          var cost = s.path("variableCostBrl").decimalValue();
          var remainder = price.subtract(cost);
          scenarios.add(
              new Scenario(
                  s.path("name").asText(),
                  price,
                  cost,
                  remainder,
                  price.signum() > 0
                      ? remainder
                          .multiply(BigDecimal.valueOf(100))
                          .divide(price, 2, RoundingMode.HALF_UP)
                      : null,
                  s.path("risk").asText(),
                  task.id()));
        }
    }
    Boolean decision;
    if (paused) decision = Boolean.TRUE;
    else if (implementationPending) decision = Boolean.FALSE;
    else if ("AUTHORIZATION".equals(cycle.getStage())) decision = Boolean.TRUE;
    else if (work != null && Set.of("BLOCKED", "WAITING_INPUT").contains(work.state()))
      decision = null;
    else decision = Boolean.FALSE;
    return new LearningCycleValueFlow(
        situation,
        blocker,
        paused
            ? "Operador da execução"
            : implementationPending
                ? "Dédalo · compositor de kits"
                : work == null ? "Marketing Hub" : work.responsible(),
        (paused || implementationPending) && work != null ? work.responsible() : null,
        active,
        decision,
        paused
            ? "Confira o motivo da pausa e os limites registrados antes de retomar pela atividade. A disponibilidade técnica não libera novos gastos."
            : implementationPending
                ? "Nenhuma nova data ou meta de margem é necessária para esta implementação privada."
                : Boolean.TRUE.equals(decision)
                    ? "Confira as opções e os limites no formulário de autorização deste ciclo."
                    : decision == null
                        ? "A atividade contém a causa e a ação necessária; consulte sua pendência."
                        : "Nenhuma decisão nova foi identificada nesta passagem.",
        paused
            ? "Retomada autorizada da execução #"
                + pausedRun.orElseThrow().getId()
                + " e aceite da saída da atividade "
                + work.activityName()
                + "."
            : implementationPending
                ? "Pacote íntegro, primeira aplicação, retomada e limites comprovados; depois Psique revisa a mesma versão."
                : work == null
                    ? "Aceite persistido da próxima passagem e métricas conciliadas da mesma versão."
                    : "Saída utilizável aceita na atividade "
                        + work.activityName()
                        + " e retorno ao processo pai.",
        since,
        since == null ? null : Math.max(0, Duration.between(since, Instant.now()).getSeconds()),
        Instant.now(),
        capability,
        registered,
        ready,
        outputs,
        scenarios,
        target,
        measure == null ? null : json.read(measure.getEvidenceJson()),
        measure == null ? null : "Evento #" + measure.getId() + " · " + measure.getCreatedAt());
  }
}
