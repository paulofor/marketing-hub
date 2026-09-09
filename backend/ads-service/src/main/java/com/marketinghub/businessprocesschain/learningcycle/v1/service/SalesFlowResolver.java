package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainDefinition;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycleEvent;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.getSalesFlow.SalesFlowResponse;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.getSalesFlow.SalesFlowResponse.Activity;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.getSalesFlow.SalesFlowResponse.Transition;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: resolver um único fluxo comercial usando o ciclo e seus eventos persistidos.
 */
@Component
@RequiredArgsConstructor
public class SalesFlowResolver {
  public static final String PARENT_CODE = "pde-sales-delivery-learning";
  public static final String CHAIN_CODE = "pde-value-creation-delivery";
  public static final String OPERATION_CODE = "operacao-otimizacao-experimento";
  public static final String DELIVERY_CODE = "venda-entrega-satisfacao-cliente";
  private final LearningSalesCycleRepository cycles;
  private final LearningSalesCycleEventRepository events;
  private final BusinessProcessDefinitionRepository processes;
  private final BusinessProcessChainDefinitionRepository chains;
  private final LearningCycleJson json;

  /**
   * Resolve a execução do contexto solicitado, preservando a versão original do ciclo histórico.
   */
  @Transactional(readOnly = true)
  public SalesFlowResponse resolve(
      Long productId, BusinessProcessDefinition parent, Long chainId, Long explicitCycleId) {
    if (!PARENT_CODE.equals(parent.getProcessCode())) return null;
    var chain = selectedChain(parent, chainId);
    if (chain == null) return null;
    LearningSalesCycle cycle;
    if (explicitCycleId != null) {
      cycle = cycles.findById(explicitCycleId).orElseThrow(() -> conflict("Ciclo não encontrado."));
      if (!Objects.equals(productId, cycle.getProductId())
          || !chain.getChainCode().equals(cycle.getChainCode())
          || !"PUBLISHED".equals(chain.getStatus())
              && !Objects.equals(chain.getId(), cycle.getChainDefinitionId()))
        throw conflict("Ciclo não pertence ao produto e à cadeia consultados.");
    } else if ("PUBLISHED".equals(chain.getStatus())) {
      cycle =
          cycles.findByProductIdAndChainCodeOrderByIdDesc(productId, chain.getChainCode()).stream()
              .findFirst()
              .orElse(null);
    } else {
      cycle =
          cycles
              .findFirstByProductIdAndChainDefinitionIdOrderByIdDesc(productId, chain.getId())
              .orElse(null);
    }
    if (cycle == null) return null;
    var model =
        processes
            .findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(PARENT_CODE, "PUBLISHED")
            .orElse(parent);
    return describe(cycle, model, events.findByCycleIdOrderByRevisionAsc(cycle.getId()));
  }

  /** Impede que uma tarefa antiga reabra operação fora da fase e do experimento do fluxo atual. */
  @Transactional(readOnly = true)
  public String executionBlocker(Long productId, BusinessProcessDefinition process, String source) {
    if (!List.of(OPERATION_CODE, DELIVERY_CODE).contains(process.getProcessCode())) return null;
    var parent =
        processes
            .findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(PARENT_CODE, "PUBLISHED")
            .orElse(null);
    if (parent == null) return null;
    var flow = resolve(productId, parent, null, null);
    if (flow == null) return null;
    if (!Objects.equals("experiment:" + flow.experimentId(), source))
      return "A execução não corresponde ao experimento do fluxo atual. " + flow.reason();
    String activity = OPERATION_CODE.equals(process.getProcessCode()) ? "optimization" : "delivery";
    var state =
        flow.activities().stream()
            .filter(item -> activity.equals(item.activityId()))
            .findFirst()
            .orElseThrow();
    if (List.of("IN_PROGRESS", "BLOCKED").contains(state.state())) return null;
    return "Esta passagem não autoriza reiniciar o subprocesso. " + flow.reason();
  }

  /** Seleciona apenas cadeias que realmente contêm a versão de processo consultada. */
  private BusinessProcessChainDefinition selectedChain(
      BusinessProcessDefinition parent, Long chainId) {
    if (chainId != null) {
      var chain = chains.findById(chainId).orElseThrow(() -> conflict("Cadeia não encontrada."));
      if (chain.getItems().stream()
          .noneMatch(item -> Objects.equals(item.getProcessDefinition().getId(), parent.getId())))
        throw conflict("O processo não pertence à versão da cadeia consultada.");
      return chain;
    }
    return chains.findByProcessDefinitionId(parent.getId()).stream()
        .filter(chain -> CHAIN_CODE.equals(chain.getChainCode()))
        .findFirst()
        .orElse(null);
  }

  /** Protege também a fila dos agentes quando a referência contém um experimento já governado. */
  public String executionBlocker(BusinessProcessDefinition process, String source) {
    if (process == null || source == null || !source.matches("experiment:[0-9]{1,18}")) return null;
    var cycle =
        cycles
            .findByExperimentId(Long.valueOf(source.substring("experiment:".length())))
            .orElse(null);
    return cycle == null ? null : executionBlocker(cycle.getProductId(), process, source);
  }

  /** Consolida atividades e passagens sem alterar tarefas, métricas, autorizações ou instâncias. */
  SalesFlowResponse describe(
      LearningSalesCycle cycle,
      BusinessProcessDefinition model,
      List<LearningSalesCycleEvent> history) {
    var diagram = json.read(model.getDiagramJson());
    var adoption =
        history.stream()
            .filter(event -> "ADOPT_BASELINE".equals(event.getAction()))
            .findFirst()
            .orElse(null);
    var measurement =
        history.stream()
            .filter(event -> "MEASURE".equals(event.getAction()))
            .reduce((first, last) -> last)
            .orElse(null);
    var latest = history.isEmpty() ? null : history.getLast();
    JsonNode metrics =
        measurement == null ? json.read("{}") : json.read(measurement.getEvidenceJson());
    boolean measured =
        measurement != null
            && (cycle.getVersionChangedAt() == null
                || !measurement.getCreatedAt().isBefore(cycle.getVersionChangedAt()))
            && (java.util.Set.of("MEASUREMENT", "DECISION", "SCALE_AUTHORIZATION")
                    .contains(cycle.getStage())
                || cycle.getClosedAt() != null)
            && metrics.path("dataValid").asBoolean(false)
            && metrics.path("testDataExcluded").asBoolean(false)
            && metrics.path("experimentId").asLong(-1) == cycle.getExperimentId();
    boolean noSales =
        measured
            && !"MEASUREMENT".equals(cycle.getStage())
            && metrics.path("netSales").isNumber()
            && metrics.path("refunds").isNumber()
            && metrics.path("netSales").asLong() == 0;
    boolean deliveryPending =
        measured
            && !"MEASUREMENT".equals(cycle.getStage())
            && !noSales
            && !metrics.path("deliveryVerified").asBoolean(false);
    boolean measuring = "MEASUREMENT".equals(cycle.getStage());
    boolean blocked =
        measuring && latest != null && "MEASUREMENT_BLOCKED".equals(latest.getAction());
    boolean finished = cycle.getClosedAt() != null;
    boolean awaitingSuccessor =
        finished
            && "ADJUSTED".equals(cycle.getStatus())
            && cycles.findByPreviousCycleId(cycle.getId()).isEmpty();
    String current =
        deliveryPending
            ? "delivery"
            : finished && !awaitingSuccessor ? null : measuring ? "consolidate" : "learningCycle";
    var states = new ArrayList<Activity>();
    states.add(
        activity(
            "optimization",
            1,
            "Operar e otimizar o experimento",
            adoption != null
                ? "HISTORICAL"
                : measuring ? "IN_PROGRESS" : measured ? "RECORDED" : "WAITING",
            false,
            adoption != null
                ? "Operação anterior preservada como referência histórica; não exige homologação retroativa."
                : measured
                    ? "Rodada operacional representada na medição; uma nova passagem exige decisão registrada."
                    : "A operação depende da homologação, autorização e publicação deste experimento.",
            adoption != null ? adoption : measurement));
    states.add(
        activity(
            "delivery",
            2,
            "Entregar cada venda e acompanhar satisfação",
            noSales
                ? "NOT_APPLICABLE"
                : deliveryPending
                    ? "BLOCKED"
                    : measured && metrics.path("deliveryVerified").asBoolean(false)
                        ? "COMPLETED"
                        : "WAITING",
            measured && !noSales && metrics.path("deliveryVerified").asBoolean(false),
            noSales
                ? metrics.path("refunds").asLong() > 0
                    ? "Reembolsos conciliados; nenhuma venda líquida pendente de entrega nesta passagem."
                    : "Nenhuma venda no recorte conciliado. Entrega não aplicável nesta passagem."
                : deliveryPending
                    ? "Há vendas sem entrega comprovada. Conclua a entrega e reconcilie a medição antes de avançar."
                    : measured
                        ? "Entrega comprovada nas fontes conciliadas do experimento."
                        : "Entrega acompanha cada venda; a ausência de medição não comprova ausência de vendas.",
            measurement));
    states.add(
        activity(
            "consolidate",
            3,
            "Consolidar resultado comercial",
            measuring ? blocked ? "BLOCKED" : "IN_PROGRESS" : measured ? "COMPLETED" : "WAITING",
            !measuring && measured,
            measuring
                ? blocked
                    ? latest.getSummary()
                    : "Conciliação automática das fontes deste experimento."
                : measured
                    ? measurement.getSummary()
                    : "Aguardar publicação ou adoção histórica comprovada para conciliar.",
            measuring ? latest : measurement));
    states.add(
        activity(
            "learningCycle",
            4,
            "Conduzir o ciclo de aprendizado e vendas",
            finished
                ? awaitingSuccessor ? "IN_PROGRESS" : "COMPLETED"
                : measuring || deliveryPending ? "WAITING" : "IN_PROGRESS",
            finished && !awaitingSuccessor,
            "Ciclo #"
                + cycle.getId()
                + " · experimento #"
                + cycle.getExperimentId()
                + " · "
                + (finished
                    ? "ADJUSTED".equals(cycle.getStatus())
                        ? awaitingSuccessor
                            ? "Ajuste registrado; preparar sucessor com o aprendizado e os gates próprios."
                            : "Sucessor vinculado; consulte a nova passagem sem reabrir a anterior."
                        : "Decisão encerrada; resultados preservados."
                    : LearningCycleRules.label(cycle.getStage()))
                + targetDescription(cycle),
            latest));
    var active =
        states.stream()
            .filter(item -> Objects.equals(current, item.activityId()))
            .findFirst()
            .orElse(null);
    String workspace =
        "/business-process-chains/learning-cycles?chainId="
            + cycle.getChainDefinitionId()
            + "&productId="
            + cycle.getProductId()
            + "&cycleId="
            + cycle.getId();
    return new SalesFlowResponse(
        cycle.getProductId(),
        cycle.getExperimentId(),
        cycle.getId(),
        cycle.getProcessDefinitionId(),
        cycle.getChainDefinitionId(),
        model.getId(),
        current,
        active == null ? null : active.name(),
        active == null ? null : active.sequenceNumber(),
        active == null ? "COMPLETED" : active.state(),
        active == null ? "Ciclo encerrado; evidências preservadas." : active.reason(),
        workspace,
        "/business-processes?processId=" + model.getId() + "&productId=" + cycle.getProductId(),
        List.copyOf(states),
        history.stream().map(event -> transition(event, diagram)).toList());
  }

  /** Expõe o destino já decidido, sem mudar o estado comercial nem simular sua execução. */
  private String targetDescription(LearningSalesCycle cycle) {
    if (cycle.getReturnProcessId() == null) return ".";
    var target = processes.findById(cycle.getReturnProcessId()).orElseThrow();
    return ". Retorno registrado: "
        + target.getName()
        + " · atividade "
        + cycle.getReturnActivityId()
        + ".";
  }

  /** Mantém datas e referências da evidência utilizada, sem inventar uma ocorrência de tarefa. */
  private Activity activity(
      String id,
      int number,
      String name,
      String state,
      boolean complete,
      String reason,
      LearningSalesCycleEvent event) {
    return new Activity(
        id,
        number,
        name,
        state,
        complete,
        reason,
        event == null ? null : event.getEvidenceReference(),
        event == null ? null : event.getCreatedAt(),
        complete && event != null ? event.getCreatedAt() : null);
  }

  /** Relaciona cada evento ao caminho declarado no BPM, inclusive decisões e retrabalho. */
  private Transition transition(LearningSalesCycleEvent event, JsonNode diagram) {
    String id =
        switch (event.getAction()) {
          case "ADOPT_BASELINE" -> "historical-entry";
          case "MEASURE" -> {
            var proof = json.read(event.getEvidenceJson());
            yield proof.path("netSales").asLong() > 0
                    && !proof.path("deliveryVerified").asBoolean(false)
                ? "delivery-blocked"
                : "measurement-ready";
          }
          case "MEASUREMENT_BLOCKED" -> "measurement-blocked";
          case "FIX_MEASUREMENT" -> "fix-measurement";
          case "CONTINUE" -> "continue-collection";
          case "ADJUST", "REWORK" -> "directed-adjustment";
          case "SCALE" -> "request-scale";
          case "AUTHORIZE_SCALE" -> "authorized-scale";
          case "STOP", "INCONCLUSIVE" -> "close-cycle";
          default ->
              "PUBLICATION".equals(event.getFromStage())
                  ? "publication-measurement"
                  : "delegated-progress";
        };
    JsonNode flow = null;
    for (var candidate : diagram.path("flows"))
      if (id.equals(candidate.path("id").asText())) flow = candidate;
    String destination =
        flow == null ? event.getToStage() : nodeLabel(diagram, flow.path("to").asText());
    if ("directed-adjustment".equals(id)) {
      var evidence = json.read(event.getEvidenceJson());
      if (evidence.path("returnProcessId").canConvertToLong()) {
        var target = processes.findById(evidence.path("returnProcessId").asLong()).orElse(null);
        if (target != null)
          destination =
              target.getName()
                  + " → "
                  + nodeLabel(
                      json.read(target.getDiagramJson()),
                      evidence.path("returnActivityId").asText());
      }
    }
    return new Transition(
        event.getId(),
        event.getRevision(),
        event.getAction(),
        flow == null ? null : id,
        flow == null ? event.getFromStage() : nodeLabel(diagram, flow.path("from").asText()),
        destination,
        flow != null && "REWORK".equals(flow.path("kind").asText()),
        event.getSummary(),
        event.getOperatorName(),
        event.getEvidenceReference(),
        event.getCreatedAt());
  }

  /** Usa os rótulos publicados do diagrama para explicar uma passagem registrada. */
  private String nodeLabel(JsonNode diagram, String id) {
    for (var node : diagram.path("nodes"))
      if (id.equals(node.path("id").asText())) return node.path("label").asText(id);
    return id;
  }

  /** Reporta contexto incompatível sem selecionar silenciosamente outro produto ou ciclo. */
  private ResponseStatusException conflict(String message) {
    return new ResponseStatusException(HttpStatus.CONFLICT, message);
  }
}
