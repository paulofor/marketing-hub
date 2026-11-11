package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleRules.require;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.businessprocesschain.learningcycle.v1.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.planningAlignment.*;
import com.marketinghub.experiment.*;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.*;
import com.marketinghub.repository.jpa.product.*;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: reconciliar apenas planejamento privado, preservando provas e decisões
 * anteriores.
 */
@Component
@RequiredArgsConstructor
public class LearningCyclePlanningAlignment {
  private static final String ACTION = "RECONCILE_PLANNING";
  private final ProductRepository products;
  private final LearningSalesCycleRepository cycles;
  private final ExperimentRepository experiments;
  private final LearningSalesCycleEventRepository events;
  private final ProductBigIdeaBindingRepository bindings;
  private final AgentTaskRepository tasks;
  private final LearningCyclePlanningDefinition definitions;
  private final LearningCycleJson json;

  /** Libera uma nova análise apenas quando um recibo compatível corrigiu a entrada após a falha. */
  public boolean correctedAfter(LearningSalesCycle cycle, Instant failedTaskCreatedAt) {
    if (failedTaskCreatedAt == null) return false;
    var definition = json.read(cycle.getBriefJson()).path("planningDefinition");
    if (!definition.isObject()) return false;
    return events.findByCycleIdAndActionOrderByRevisionDesc(cycle.getId(), ACTION).stream()
        .filter(
            event ->
                Objects.equals(event.getCycleId(), cycle.getId())
                    && event.getRevision() <= cycle.getRevision()
                    && event.getCreatedAt() != null
                    && event.getCreatedAt().isAfter(failedTaskCreatedAt))
        .anyMatch(
            event -> {
              var evidence = json.read(event.getEvidenceJson());
              return evidence.path("productId").asLong(-1) == cycle.getProductId()
                  && evidence.path("experimentId").asLong(-1) == cycle.getExperimentId()
                  && cycle.getProductVersion().equals(evidence.path("productVersion").asText())
                  && definition.equals(evidence.path("planningDefinition"));
            });
  }

  /** Consulta a proposta e os bloqueios sem alterar o ciclo nem criar tarefas. */
  public PlanningAlignmentView preview(Long productId, Long cycleId) {
    var product = products.findById(productId).orElseThrow();
    var cycle = cycles.findById(cycleId).orElseThrow();
    require(Objects.equals(productId, cycle.getProductId()), "Ciclo pertence a outro produto.");
    return preview(product, cycle, experiments.findById(cycle.getExperimentId()).orElseThrow());
  }

  /** Aplica uma correção idempotente sob os mesmos locks usados para iniciar os processos. */
  public void apply(Long productId, Long cycleId, PlanningAlignmentRequest request) {
    var product = products.findLockedById(productId).orElseThrow();
    var cycle = cycles.findLocked(productId, cycleId).orElseThrow();
    var receipt = events.findByCycleIdAndRequestKey(cycleId, request.requestKey().toString());
    if (receipt.isPresent()) {
      require(
          ACTION.equals(receipt.get().getAction())
              && json.read(receipt.get().getRequestJson()).equals(json.read(json.write(request))),
          "A chave já foi usada para outra decisão.");
      return;
    }
    require(cycle.getRevision() == request.expectedRevision(), "O ciclo mudou; atualize a tela.");
    var experiment = experiments.findById(cycle.getExperimentId()).orElseThrow();
    var view = preview(product, cycle, experiment);
    require(view.available(), view.reason());
    require(
        view.bigIdeaRevision() == request.expectedBigIdeaRevision(),
        "A direção do produto mudou; confira a revisão atual.");
    var before = (ObjectNode) json.read("{}");
    before.set("brief", json.read(cycle.getBriefJson()));
    before.put("unitPriceBrl", experiment.getUnitPrice());
    before.put("hypothesis", experiment.getHypothesis());
    before.put(
        "hypothesisId",
        experiment.getHypothesisRef() == null
            ? null
            : experiment.getHypothesisRef().getId().toString());
    var definition = definitions.snapshot(product);
    var brief = (ObjectNode) json.read(cycle.getBriefJson());
    brief.put("offer", view.proposedOffer());
    brief.set("planningDefinition", definition);
    var hypothesisVersion = definitions.hypothesisFor(experiment);
    brief.put("planningHypothesisId", hypothesisVersion.getId().toString());
    cycle.setBriefJson(json.write(brief));
    experiment.setUnitPrice(view.proposedPriceBrl());
    experiment.setHypothesisRef(hypothesisVersion);
    String hypothesis = brief.path("hypothesis").asText();
    experiment.setHypothesis(
        hypothesis.codePointCount(0, hypothesis.length()) <= 255
            ? hypothesis
            : hypothesis.substring(0, hypothesis.offsetByCodePoints(0, 252)) + "...");
    cycle.setRevision(cycle.getRevision() + 1);
    cycle.setUpdatedAt(Instant.now());
    var proof = (ObjectNode) json.read("{}");
    proof.set("before", before);
    proof.set("planningDefinition", definition);
    proof.put("hypothesisId", hypothesisVersion.getId().toString());
    proof.put("productId", productId);
    proof.put("experimentId", experiment.getId());
    proof.put("productVersion", cycle.getProductVersion());
    proof.put("reason", request.reason());
    proof.put("costsAndCeilingsPreserved", true);
    var event = new LearningSalesCycleEvent();
    event.setCycleId(cycleId);
    event.setRequestKey(request.requestKey().toString());
    event.setRequestJson(json.write(request));
    event.setRevision(cycle.getRevision());
    event.setFromStage(cycle.getStage());
    event.setToStage(cycle.getStage());
    event.setAction(ACTION);
    event.setOperatorName(request.operatorName());
    event.setSummary(
        "Planejamento alinhado à definição registrada do produto; histórico, custos e tetos preservados. Retome Atena no Processo 2.");
    event.setEvidenceReference(
        "internal://products/" + productId + "/big-idea/" + view.bigIdeaRevision());
    event.setEvidenceJson(json.write(proof));
    event.setCreatedAt(cycle.getUpdatedAt());
    experiments.save(experiment);
    cycles.save(cycle);
    events.save(event);
  }

  /** Recusa histórico, exposição, trabalho ativo ou estratégia aceita antes de oferecer a ação. */
  private PlanningAlignmentView preview(
      Product product, LearningSalesCycle cycle, Experiment experiment) {
    String blocker = null;
    if (!Objects.equals(product.getId(), cycle.getProductId())
        || experiment.getProduct() == null
        || !Objects.equals(product.getId(), experiment.getProduct().getId()))
      blocker = "O experimento não pertence ao produto deste ciclo.";
    else if (!"OPEN".equals(cycle.getStatus())
        || !"PLANNING".equals(cycle.getStage())
        || cycle.isBaseline()
        || experiment.getStatus() != ExperimentStatus.PLANNED
        || experiment.getFacebookReleaseRequestedAt() != null)
      blocker = "Somente planejamento privado aberto pode ser reconciliado; preserve o histórico.";
    else if (!Boolean.TRUE.equals(product.getAutomaticExecutionEnabled()))
      blocker = "Produto em STOP; a correção aguarda retomada autorizada.";
    else if (product.getBigIdea() == null
        || bindings
            .findByProductIdAndSourceReference(product.getId(), "experiment:" + experiment.getId())
            .map(
                binding ->
                    !Objects.equals(binding.getBigIdea().getId(), product.getBigIdea().getId()))
            .orElse(true))
      blocker = "A direção fixada neste experimento difere do produto; use um sucessor compatível.";
    else if (tasks.existsBySourceReferenceAndStatusIn(
        "experiment:" + experiment.getId(), List.of("PENDING", "IN_PROGRESS", "COMPLETED")))
      blocker =
          "Há trabalho pendente, em execução ou aceito neste experimento; preserve sua entrada.";
    var brief = json.read(cycle.getBriefJson());
    ObjectNode definition = blocker == null ? definitions.snapshot(product) : null;
    if (blocker == null
        && definition.equals(brief.path("planningDefinition"))
        && definition.path("offer").asText().equals(brief.path("offer").asText())
        && Objects.equals(experiment.getUnitPrice(), product.getCurrentPriceBrl())
        && experiment.getHypothesisRef() != null
        && experiment
            .getHypothesisRef()
            .getId()
            .toString()
            .equals(brief.path("planningHypothesisId").asText()))
      blocker = "O planejamento já está alinhado à definição registrada.";
    return new PlanningAlignmentView(
        blocker == null,
        blocker == null
            ? "Alinhar a oferta e retirar preço e hipótese legados, preservando as tentativas anteriores. Esta ação não inicia agentes nem autoriza cobrança, mídia ou vídeos."
            : blocker,
        cycle.getRevision(),
        product.getBigIdea() == null ? 0 : product.getBigIdea().getRevision(),
        brief.path("offer").asText(),
        definition == null ? null : definition.path("offer").asText(),
        product.getCurrentPriceBrl());
  }
}
