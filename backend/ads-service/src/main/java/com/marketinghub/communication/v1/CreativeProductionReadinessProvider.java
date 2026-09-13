package com.marketinghub.communication.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskFunctionalSnapshot;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.agentactivity.*;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorService;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Responsabilidade: impedir revisão sem peça final e reabrir conclusões anteriores à materialização
 * real.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreativeProductionReadinessProvider
    implements AgentProductProcessActivityReadinessProvider {
  private final AgentTaskRepository tasks;
  private final ProductProcessActivityPredecessorService predecessors;
  private final ObjectMapper json;

  /** Governa produção e revisões do subprocesso criativo em todas as suas definições. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activity) {
    return "creative-production-approval".equals(process.getProcessCode())
        && Set.of("nonAudiovisual", "customer", "commercial").contains(activity.getActivityId());
  }

  /** Exige prova real e explica qual parecer devolveu a peça à produção antes de nova revisão. */
  @Override
  public AgentProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String reference) {
    var prior = predecessors.readiness(process, activity, reference);
    if (!prior.ready()) return new AgentProductProcessActivityReadiness(false, prior.reason());
    if (!"nonAudiovisual".equals(activity.getActivityId())
        && producer(process, reference).filter(this::rendered).isEmpty())
      return new AgentProductProcessActivityReadiness(
          false,
          "Conclua a produção da imagem final com Íris; briefing não é peça visual para revisão.");
    if ("nonAudiovisual".equals(activity.getActivityId())) {
      var correction =
          producer(process, reference)
              .flatMap(producer -> correction(process, reference, producer.id()));
      if (correction.isPresent())
        return new AgentProductProcessActivityReadiness(
            true,
            "Íris deve corrigir a peça conforme o parecer da tarefa #"
                + correction.get().id()
                + "; depois, Psique e Têmis revisarão a nova imagem.");
    }
    return new AgentProductProcessActivityReadiness(
        true, "Os predecessores estão prontos para produzir ou revisar a peça real.");
  }

  /**
   * Solicita nova produção para briefings históricos e nova revisão para peças posteriores ao
   * parecer.
   */
  @Override
  public boolean requiresFreshExecution(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String reference) {
    var producer = producer(process, reference);
    if (producer.isEmpty()) return false;
    if ("nonAudiovisual".equals(activity.getActivityId()))
      return "COMPLETED".equals(producer.get().status())
          && (!rendered(producer.get())
              || correction(process, reference, producer.get().id()).isPresent());
    var review = latest(process, reference, activity.getActivityId());
    return review
        .filter(t -> "COMPLETED".equals(t.status()) && t.id() < producer.get().id())
        .isPresent();
  }

  /**
   * Consulta somente o resultado funcional mais recente, sem carregar prompts no acompanhamento.
   */
  private Optional<AgentTaskFunctionalSnapshot> latest(
      BusinessProcessDefinition process, String reference, String activity) {
    return tasks.findFunctionalSnapshotsByProcessSince(process.getId(), reference, null).stream()
        .filter(t -> activity.equals(t.processActivityId()))
        .max(Comparator.comparing(AgentTaskFunctionalSnapshot::id));
  }

  /** Localiza o produtor da mesma definição e referência para impedir mistura de ciclos. */
  private Optional<AgentTaskFunctionalSnapshot> producer(
      BusinessProcessDefinition process, String reference) {
    return latest(process, reference, "nonAudiovisual");
  }

  /** Retorna à produção quando o revisor registra um ajuste funcional posterior à peça atual. */
  private Optional<AgentTaskFunctionalSnapshot> correction(
      BusinessProcessDefinition process, String reference, Long producerId) {
    for (String reviewer : Set.of("customer", "commercial")) {
      var review = latest(process, reference, reviewer);
      if (review.isEmpty()
          || review.get().id() <= producerId
          || !"BLOCKED".equals(review.get().status())
          || review.get().resultJson() == null) continue;
      try {
        if ("ADJUST".equals(json.readTree(review.get().resultJson()).path("decision").asText()))
          return review;
      } catch (Exception ex) {
        log.error("Parecer de correção criativa inválido. taskId={}", review.get().id(), ex);
      }
    }
    return Optional.empty();
  }

  /**
   * Reconhece a existência do contrato de renderização; a conclusão e o consumo validam seus
   * arquivos.
   */
  private boolean rendered(AgentTaskFunctionalSnapshot task) {
    if (!"COMPLETED".equals(task.status()) || task.resultJson() == null) return false;
    try {
      JsonNode output = json.readTree(task.resultJson()).path("functionalOutput");
      var renders = output.path("renderedAssets");
      return renders.isArray()
          && !renders.isEmpty()
          && renders.size() == output.path("staticAssets").size();
    } catch (Exception ex) {
      log.error(
          "Contrato de renderização inválido. taskId={} sourceProcessId={}",
          task.id(),
          task.processDefinitionId(),
          ex);
      return false;
    }
  }
}
