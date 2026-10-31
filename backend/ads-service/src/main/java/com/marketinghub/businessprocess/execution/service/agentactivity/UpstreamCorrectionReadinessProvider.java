package com.marketinghub.businessprocess.execution.service.agentactivity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.AgentTaskFunctionalSnapshot;
import com.marketinghub.businessprocess.*;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.util.*;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Responsabilidade: encaminhar rejeições estruturadas à origem no BPM, limitando retrabalho. */
@Component
@RequiredArgsConstructor
@Slf4j
public class UpstreamCorrectionReadinessProvider
    implements AgentProductProcessActivityReadinessProvider {
  private static final Pattern EXPERIMENT = Pattern.compile("^experiment:([1-9][0-9]{0,17})$");
  private static final int MAX_RETURNS = 2;
  private final LearningSalesCycleRepository cycles;
  private final AgentTaskRepository tasks;
  private final ObjectMapper json;

  /** Usa o grafo publicado; contratos antigos sem retorno estruturado permanecem inalterados. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activity) {
    return process != null && activity != null && process.getDiagramJson() != null;
  }

  /** Expõe a origem, o critério de correção e os bloqueios sem criar tarefa nem gastar recursos. */
  @Override
  public AgentProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String reference) {
    var state = state(process, activity, product, reference);
    if (state == null) return ready();
    for (var request : state.requests()) {
      String id = activity.getActivityId();
      if (!request.valid()) {
        if (id.equals(request.source().processActivityId()))
          return blocked(
              "O retorno solicitado não identifica uma atividade de agente predecessora válida no mesmo processo. Corrija o contrato de retorno antes de repetir.");
        continue;
      }
      if (!affected(state, request, id)) continue;
      if (request.count() > MAX_RETURNS)
        return blocked(
            "Limite de dois retornos à atividade "
                + label(state, request.target())
                + " atingido neste ciclo. Investigue a causa persistente antes de autorizar outro trabalho; retomar não reinicia o limite.");
      var target = latest(state.history(), request.target());
      if (id.equals(request.target())) {
        if (target != null
            && target.id() > request.source().id()
            && "BLOCKED".equals(target.status()))
          return blocked(
              "A correção na origem também foi bloqueada. Resolva sua causa antes de repetir a mesma entrada.");
        return new AgentProductProcessActivityReadiness(
            true,
            "Retorno para "
                + label(state, request.target())
                + ". Motivo: "
                + request.reason()
                + " Aceite: "
                + request.criterion()
                + ". Retorno "
                + request.count()
                + " de "
                + MAX_RETURNS
                + ".");
      }
      if (target == null || target.id() <= request.source().id() || !approved(target))
        return blocked(
            "Aguardando correção em "
                + label(state, request.target())
                + ": "
                + request.reason()
                + " Aceite: "
                + request.criterion());
      if (!changed(state, request, target))
        return blocked(
            "A correção em "
                + label(state, request.target())
                + " não mudou a entrega funcional. Uma nova tentativa ou justificativa não comprova progresso; revise a causa antes de repetir.");
      for (String previous : ancestors(state.graph(), id, new HashSet<>())) {
        if (previous.equals(request.target())
            || !ancestors(state.graph(), previous, new HashSet<>()).contains(request.target()))
          continue;
        if (!ancestors(state.graph(), request.source().processActivityId(), new HashSet<>())
            .contains(previous)) continue;
        var proof = latest(state.history(), previous);
        if (proof == null || proof.id() < target.id() || !approved(proof))
          return blocked(
              "Aguardando revalidação de "
                  + label(state, previous)
                  + " após a correção da origem.");
      }
    }
    return ready();
  }

  /** Reabre apenas a origem e os pareceres que dependem dela, preservando tarefas em andamento. */
  @Override
  public boolean requiresFreshExecution(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String reference) {
    var state = state(process, activity, product, reference);
    if (state == null) return false;
    var current = latest(state.history(), activity.getActivityId());
    if (current == null || !Set.of("COMPLETED", "BLOCKED").contains(current.status())) return false;
    for (var request : state.requests()) {
      if (!request.valid() || !affected(state, request, activity.getActivityId())) continue;
      if (activity.getActivityId().equals(request.target())) {
        if (current.id() < request.source().id()) return true;
      } else {
        var target = latest(state.history(), request.target());
        if (target == null || target.id() <= request.source().id() || current.id() < target.id())
          return true;
      }
    }
    return false;
  }

  /** Lê projeções do contexto exato; nunca consulta prompts, outro produto ou ciclos encerrados. */
  private State state(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String reference) {
    if (!supports(process, activity) || product == null || reference == null) return null;
    var match = EXPERIMENT.matcher(reference);
    if (!match.matches()) return null;
    var cycle =
        cycles
            .findByExperimentId(Long.valueOf(match.group(1)))
            .filter(
                c ->
                    Objects.equals(product.getId(), c.getProductId())
                        && "OPEN".equals(c.getStatus()));
    if (cycle.isEmpty()) return null;
    var history =
        tasks.findFunctionalSnapshotsByProcessSince(
            process.getId(), reference, cycle.get().getCreatedAt());
    var declared =
        history.stream()
            .filter(t -> "BLOCKED".equals(t.status()))
            .filter(t -> "ADJUST".equals(result(t).path("decision").asText()))
            .filter(t -> result(t).hasNonNull("upstreamCorrection"))
            .toList();
    if (declared.isEmpty()) return null;
    try {
      var graph = new BusinessProcessGraphTopology(json.readTree(process.getDiagramJson()));
      List<Request> requests = new ArrayList<>();
      for (var source : declared) {
        if (!Objects.equals(latest(history, source.processActivityId()).id(), source.id()))
          continue;
        var contract = result(source).path("upstreamCorrection");
        String target = contract.path("targetActivityId").asText();
        var node = graph.node(target);
        boolean valid =
            "PROCESS_UPSTREAM_CORRECTION_V1".equals(contract.path("contractVersion").asText())
                && !contract.path("reason").asText().isBlank()
                && !contract.path("acceptanceCriterion").asText().isBlank()
                && ancestors(graph, source.processActivityId(), new HashSet<>()).contains(target)
                && node != null
                && "TASK".equals(node.path("type").asText())
                && node.path("responsibleAgentKeys").isArray()
                && !node.path("responsibleAgentKeys").isEmpty()
                && !"HUMAN".equals(node.path("executorType").asText())
                && !node.path("confirmationRequired").asBoolean();
        long count =
            declared.stream()
                .filter(t -> source.processActivityId().equals(t.processActivityId()))
                .filter(
                    t ->
                        target.equals(
                            result(t).path("upstreamCorrection").path("targetActivityId").asText()))
                .count();
        requests.add(
            new Request(
                source,
                target,
                contract.path("reason").asText(),
                contract.path("acceptanceCriterion").asText(),
                valid,
                count));
      }
      requests.sort(Comparator.comparing(r -> r.source().id()));
      return new State(graph, history, requests);
    } catch (Exception ex) {
      log.error(
          "Falha no contrato de retorno processId={} productId={} reference={}",
          process.getId(),
          product.getId(),
          reference,
          ex);
      throw new IllegalStateException(
          "Não foi possível validar o retorno à atividade responsável.", ex);
    }
  }

  /** Restringe a invalidação ao caminho entre a origem e a atividade que pediu o ajuste. */
  private boolean affected(State state, Request request, String activity) {
    return request.target().equals(activity)
        || request.source().processActivityId().equals(activity)
        || (ancestors(state.graph(), request.source().processActivityId(), new HashSet<>())
                .contains(activity)
            && ancestors(state.graph(), activity, new HashSet<>()).contains(request.target()));
  }

  /**
   * Compara entregas estruturadas, ignorando decisão, justificativas e referências de auditoria.
   */
  private boolean changed(State state, Request request, AgentTaskFunctionalSnapshot corrected) {
    var baseline =
        state.history().stream()
            .filter(
                t ->
                    request.target().equals(t.processActivityId())
                        && t.id() < request.source().id())
            .max(Comparator.comparing(AgentTaskFunctionalSnapshot::id))
            .orElse(null);
    var output = functionalOutput(corrected);
    return output != null
        && !output.isEmpty()
        && (baseline == null || !output.equals(functionalOutput(baseline)));
  }

  /**
   * Extrai somente a parte utilizável da resposta; texto de explicação não libera nova avaliação.
   */
  private JsonNode functionalOutput(AgentTaskFunctionalSnapshot task) {
    if (!(result(task) instanceof ObjectNode node)) return null;
    var output = node.deepCopy();
    output.remove(
        List.of(
            "activity",
            "decision",
            "rationale",
            "sources",
            "assumptions",
            "requiredChanges",
            "upstreamCorrection",
            "alternatives",
            "selectedApproach"));
    return output;
  }

  /** Exige sucesso funcional explícito antes de usar a entrega corrigida. */
  private boolean approved(AgentTaskFunctionalSnapshot task) {
    return "COMPLETED".equals(task.status())
        && "APPROVE".equals(result(task).path("decision").asText());
  }

  /** Preserva falhas de leitura visíveis, sem tratar resposta inválida como aceite. */
  private JsonNode result(AgentTaskFunctionalSnapshot task) {
    if (task.resultJson() == null || task.resultJson().isBlank()) return json.createObjectNode();
    try {
      return Objects.requireNonNullElseGet(
          json.readTree(task.resultJson()), json::createObjectNode);
    } catch (Exception ex) {
      log.warn(
          "Retorno funcional inválido taskId={} processId={}",
          task.id(),
          task.processDefinitionId(),
          ex);
      return json.createObjectNode();
    }
  }

  /** Percorre somente arestas progressivas para impedir ciclos e retornos a sucessoras. */
  private Set<String> ancestors(
      BusinessProcessGraphTopology graph, String id, Set<String> visited) {
    Set<String> result = new HashSet<>();
    for (String from : graph.directPredecessors(id)) {
      if (!visited.add(from)) continue;
      result.add(from);
      result.addAll(ancestors(graph, from, visited));
    }
    return result;
  }

  /** Seleciona a tentativa mais recente, incluindo trabalho ativo e bloqueado. */
  private AgentTaskFunctionalSnapshot latest(
      List<AgentTaskFunctionalSnapshot> history, String activity) {
    return history.stream()
        .filter(t -> activity.equals(t.processActivityId()))
        .max(Comparator.comparing(AgentTaskFunctionalSnapshot::id))
        .orElse(null);
  }

  /** Usa o nome do BPM para tornar o encaminhamento compreensível na tela. */
  private String label(State state, String id) {
    return state.graph().node(id).path("label").asText(id);
  }

  /** Mantém neutra a prontidão quando não há retorno funcional pendente. */
  private AgentProductProcessActivityReadiness ready() {
    return new AgentProductProcessActivityReadiness(true, "");
  }

  /** Impede novo trabalho e entrega a causa persistida à interface existente. */
  private AgentProductProcessActivityReadiness blocked(String reason) {
    return new AgentProductProcessActivityReadiness(false, reason);
  }

  /** Responsabilidade: reunir o retorno proposto e sua validação pelo backend. */
  private record Request(
      AgentTaskFunctionalSnapshot source,
      String target,
      String reason,
      String criterion,
      boolean valid,
      long count) {}

  /** Responsabilidade: reunir as provas do mesmo contexto durante a decisão de prontidão. */
  private record State(
      BusinessProcessGraphTopology graph,
      List<AgentTaskFunctionalSnapshot> history,
      List<Request> requests) {}
}
