package com.marketinghub.product.modeling.v1.service;

import com.marketinghub.product.modeling.v1.ProductModeling;
import com.marketinghub.product.modeling.v1.service.get.ModelingCycleOption;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryCycleRepository;
import com.marketinghub.repository.jpa.productmodeling.ProductModelingRepository;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Vincula pesquisa a uma preparação ainda não executada, preservando a cadeia e seus gates. */
@Component
@RequiredArgsConstructor
public class ModelingAdoption {
  private final ProductModelingRepository models;
  private final LearningSalesCycleRepository cycles;
  private final ProductDiscoveryCycleRepository discovery;
  private final AgentTaskRepository tasks;
  private final ExperimentRepository experiments;
  private final ModelingHandoff handoff;
  private final com.marketinghub.repository.jpa.product.ProductRepository products;
  private final com.marketinghub.repository.jpa.processautomation.ProcessRunRepository runs;

  /** Lista somente ciclos do produto que ainda podem receber a direção pesquisada. */
  public List<ModelingCycleOption> options(ProductModeling m) {
    if (m.getProductId() == null || m.getLearningCycleId() != null) return List.of();
    return cycles.findByProductIdAndOpenSlot(m.getProductId(), 1).stream()
        .filter(c -> eligible(c) && models.findByLearningCycleId(c.getId()).isEmpty())
        .map(
            c ->
                new ModelingCycleOption(
                    c.getId(), c.getExperimentId(), c.getProductVersion(), url(c)))
        .toList();
  }

  /** Congela fontes uma vez e deixa execução, autorizações e avanço nos serviços já existentes. */
  @Transactional
  public void adopt(Long id, Long cycleId) {
    var identity = models.findById(id).orElseThrow();
    require(identity.getProductId() != null, "A pesquisa ainda não possui produto existente.");
    var product = products.findLockedById(identity.getProductId()).orElseThrow();
    var m = models.findLockedById(id).orElseThrow();
    if (Objects.equals(m.getLearningCycleId(), cycleId)) return;
    require(
        m.getLearningCycleId() == null && m.getProductId() != null,
        "Esta modelagem não pode ser vinculada a outro ciclo.");
    require(
        Objects.equals(
            m.getBigIdeaSnapshot(),
            product.getBigIdea() == null ? null : product.getBigIdea().getContentJson()),
        "A Big Idea evoluiu após a preparação. Prepare uma modelagem com a direção atual antes de adotar.");
    var c =
        cycles
            .findLocked(m.getProductId(), cycleId)
            .orElseThrow(() -> error("Ciclo de outro produto ou inexistente."));
    require(
        eligible(c),
        "Use uma preparação ainda não executada; o histórico não pode ser substituído.");
    require(
        models.findByLearningCycleId(cycleId).isEmpty(),
        "O ciclo já possui uma modelagem adotada.");
    var research = discovery.findByIdForUpdate(m.getDiscoveryCycleId()).orElseThrow();
    require(
        research.getStatus()
            == com.marketinghub.productdiscovery.v1.ProductDiscoveryCycleStatus.COMPLETED,
        "A pesquisa factual precisa estar concluída.");
    var snapshot = handoff.context(m.getDiscoveryCycleId());
    require(
        snapshot.path("factualSources").isArray() && !snapshot.path("factualSources").isEmpty(),
        "A pesquisa não possui fontes factuais persistidas.");
    boolean ready = false;
    for (var candidate : snapshot.path("candidates"))
      ready |= "DOSSIER_READY".equals(candidate.path("maturity").asText());
    require(ready, "Argos ainda não produziu dossiê factual apto ao planejamento.");
    m.setLearningCycleId(cycleId);
    m.setExperimentId(c.getExperimentId());
    m.setAdoptedContextJson(snapshot.toString());
    m.setAdoptedAt(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
    models.save(m);
  }

  /** Aponta à operação canônica; nenhuma tarefa ou campanha é criada pela vinculação. */
  public String url(ProductModeling m) {
    return m.getLearningCycleId() == null
        ? null
        : cycles.findById(m.getLearningCycleId()).map(this::url).orElse(null);
  }

  /** Mantém cadeia e ciclo nos links de atividade, sem selecionar a versão mais recente. */
  public String activityQuery(ProductModeling m) {
    return m.getLearningCycleId() == null
        ? ""
        : cycles
            .findById(m.getLearningCycleId())
            .map(c -> "&chainId=" + c.getChainDefinitionId() + "&learningCycleId=" + c.getId())
            .orElse("");
  }

  /** Recusa ciclo encerrado, exposto ou cujo planejamento já recebeu outro contexto. */
  private boolean eligible(
      com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle c) {
    if (!"OPEN".equals(c.getStatus())
        || c.isBaseline()
        || !Set.of("LEARNING", "PLANNING", "ADJUSTMENT").contains(c.getStage())) return false;
    var experiment = experiments.findById(c.getExperimentId()).orElse(null);
    return experiment != null
        && experiment.getStatus() == com.marketinghub.experiment.ExperimentStatus.PLANNED
        && !runs.existsByProductIdAndSourceReference(
            c.getProductId(), "experiment:" + c.getExperimentId())
        && tasks
            .findBySourceReferenceOrderByCreatedAtAscIdAsc("experiment:" + c.getExperimentId())
            .isEmpty();
  }

  /** Compõe a navegação com todas as identidades da ocorrência, sem selecionar outra versão. */
  private String url(com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle c) {
    return "/business-process-chains/learning-cycles?productId="
        + c.getProductId()
        + "&chainId="
        + c.getChainDefinitionId()
        + "&cycleId="
        + c.getId();
  }

  /** Converte pré-condição violada em bloqueio acionável sem efeito parcial. */
  private static void require(boolean value, String message) {
    if (!value) throw error(message);
  }

  /** Uniformiza erros funcionais da adoção. */
  private static ResponseStatusException error(String message) {
    return new ResponseStatusException(HttpStatus.CONFLICT, message);
  }
}
