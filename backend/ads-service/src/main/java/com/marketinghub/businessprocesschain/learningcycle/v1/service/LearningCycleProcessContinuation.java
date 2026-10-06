package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles.LearningCycleProcessContext.Work;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: resolver a passagem comprovada entre processos preparatórios do mesmo ciclo.
 */
@Component
public class LearningCycleProcessContinuation {
  private static final Set<String> PREPARATORY =
      Set.of(
          "pde-commercial-plan-offer",
          "pde-construction-approval",
          "pde-communication-sales-journey");
  private final LearningSalesCycleRepository cycles;
  private final BusinessProcessChainDefinitionRepository chains;
  private final ProductRepository products;
  private final LearningCycleWorkResolver work;
  private final LearningCycleService service;

  /** Reutiliza o resolvedor da tela e os comandos do ciclo sem acoplar o motor aos agentes. */
  public LearningCycleProcessContinuation(
      LearningSalesCycleRepository cycles,
      BusinessProcessChainDefinitionRepository chains,
      ProductRepository products,
      LearningCycleWorkResolver work,
      @Lazy LearningCycleService service) {
    this.cycles = cycles;
    this.chains = chains;
    this.products = products;
    this.work = work;
    this.service = service;
  }

  /**
   * Confere identidade, PLAY e provas antes de registrar a etapa e indicar um único destino;
   * subprocessos, histórico e etapas comerciais mantêm seus contratos próprios.
   */
  public Next next(ProcessRun run) {
    if (run.getLearningCycleId() == null
        || run.getParentRunId() != null
        || !"COMPLETED".equals(run.getStatus())) return null;
    var cycle = cycles.findLocked(run.getProductId(), run.getLearningCycleId()).orElseThrow();
    if (!Objects.equals(cycle.getChainDefinitionId(), run.getChainDefinitionId()))
      throw new IllegalStateException("A passagem entre processos pertence a outro contexto.");
    if (!"OPEN".equals(cycle.getStatus())
        || !Set.of("PLANNING", "ADJUSTMENT").contains(cycle.getStage())) return null;
    var product = products.findById(run.getProductId()).orElseThrow();
    if (!Boolean.TRUE.equals(product.getAutomaticExecutionEnabled())) return null;
    var chain = chains.findById(cycle.getChainDefinitionId()).orElseThrow();
    var origin =
        chain.getItems().stream()
            .map(item -> item.getProcessDefinition())
            .filter(
                definition ->
                    Objects.equals(definition.getId(), run.getProcessDefinitionId())
                        && PREPARATORY.contains(definition.getProcessCode()))
            .findFirst()
            .orElse(null);
    if (origin == null) return null;
    String constructionSource = LearningCycleExecutionContext.constructionSource(product, cycle);
    String originSource =
        "pde-construction-approval".equals(origin.getProcessCode())
            ? constructionSource
            : "experiment:" + cycle.getExperimentId();
    if (!Objects.equals(originSource, run.getSourceReference()))
      throw new IllegalStateException("A passagem entre processos pertence a outro contexto.");
    var resolution = work.resolvePreparation(cycle);
    if (resolution.completed()) {
      service.completePreparationFromProcesses(run.getProductId(), cycle.getId(), run.getId());
      resolution = work.resolvePreparation(cycle);
    }
    var next = resolution.nextWork();
    if (next == null || Objects.equals(next.processDefinitionId(), run.getProcessDefinitionId()))
      return null;
    var destination =
        chain.getItems().stream()
            .map(item -> item.getProcessDefinition())
            .filter(
                definition ->
                    Objects.equals(definition.getId(), next.processDefinitionId())
                        && PREPARATORY.contains(definition.getProcessCode()))
            .findFirst()
            .orElseThrow(
                () -> new IllegalStateException("Destino fora da preparação autorizada do ciclo."));
    return new Next(
        next,
        "pde-construction-approval".equals(destination.getProcessCode())
            ? constructionSource
            : "experiment:" + cycle.getExperimentId());
  }

  /** Responsabilidade: transportar o destino e sua referência canônica dentro do mesmo ciclo. */
  public record Next(Work work, String sourceReference) {}
}
