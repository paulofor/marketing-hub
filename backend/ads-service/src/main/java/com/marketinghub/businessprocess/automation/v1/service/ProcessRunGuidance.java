package com.marketinghub.businessprocess.automation.v1.service;

import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocess.automation.v1.service.status.ProcessRunUserAction;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleVideoBudget;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Responsabilidade: distinguir ações pendentes do operador de trabalho automático do ciclo. */
@Component
@RequiredArgsConstructor
public class ProcessRunGuidance {
  private final LearningSalesCycleRepository cycles;
  private final BusinessProcessDefinitionRepository processes;
  private final LearningCycleVideoBudget videoBudget;
  private final ProcessRunVideoGuidance videoGuidance;

  /** Resolve a próxima ação pela ocorrência exata, sem autorizar consumo ou alterar estado. */
  public ProcessRunUserAction resolve(ProcessRun run) {
    if (!Set.of("QUEUED", "WAITING_ACTIVITY", "WAITING_HUMAN").contains(run.getStatus()))
      return null;
    var cycle = manualCycle(run);
    if (cycle == null) return null;
    if (!"VIDEO_BRIEF".equals(cycle.getStage())) return videoGuidance.resolve(cycle);
    var authorization = videoBudget.current(cycle);
    if (authorization == null)
      return new ProcessRunUserAction(
          "AUTHORIZE_VIDEO_BUDGET",
          "Falta informar o teto dos dois vídeos",
          "O processo aguarda sua decisão sobre o valor total, em USD, para produzir e revisar o anúncio e a demonstração na entrada. Aguardar nesta tela não registra essa autorização.",
          "Você · responsável pelo orçamento",
          "Informar teto dos vídeos",
          videoBudget.financeUrl(cycle),
          "Depois de registrar o teto, continue no ciclo para concluir o briefing e prosseguir com a avaliação financeira. O teto não autoriza mídia, cobrança ou publicação comercial.",
          null);
    return new ProcessRunUserAction(
        "COMPLETE_VIDEO_BRIEF",
        "Teto registrado. Falta definir os vídeos",
        "O teto desta versão está registrado. Conclua o briefing do anúncio e da demonstração com a referência financeira para prosseguir com a avaliação.",
        "Operador do ciclo · briefing com Íris e avaliação de Plutus",
        "Continuar definição dos vídeos",
        "/business-process-chains/learning-cycles?chainId="
            + cycle.getChainDefinitionId()
            + "&productId="
            + cycle.getProductId()
            + "&cycleId="
            + cycle.getId(),
        "Produção e revisão dependem dos gates próprios. O registro do teto não conclui o processo de venda, entrega e aprendizado.",
        authorization.reference());
  }

  /** Reconhece entrada ou correção pendente durante a pausa, sem confundir com preflight ativo. */
  public boolean awaitingInput(ProcessRun run) {
    var cycle = manualCycle(run);
    return cycle != null
        && ("VIDEO_BRIEF".equals(cycle.getStage()) || videoGuidance.resolve(cycle) != null);
  }

  /** Confere vínculo, versão e etapas elegíveis para orientar uma intervenção do operador. */
  private LearningSalesCycle manualCycle(ProcessRun run) {
    if (run.getLearningCycleId() == null || !"learningCycle".equals(run.getCurrentActivityId()))
      return null;
    var process = processes.findById(run.getProcessDefinitionId()).orElseThrow();
    if (!"pde-sales-delivery-learning".equals(process.getProcessCode())) return null;
    var cycle = cycles.findById(run.getLearningCycleId()).orElseThrow();
    if (!Objects.equals(run.getProductId(), cycle.getProductId())
        || !Objects.equals(run.getChainDefinitionId(), cycle.getChainDefinitionId())
        || !Objects.equals(run.getSourceReference(), "experiment:" + cycle.getExperimentId()))
      throw new IllegalStateException("A pendência pertence a outro contexto de execução.");
    if (!"OPEN".equals(cycle.getStatus())
        || !Set.of("VIDEO_BRIEF", "CAMPAIGN_VIDEO", "PDE_ENTRY_VIDEO").contains(cycle.getStage()))
      return null;
    var definition = processes.findById(cycle.getProcessDefinitionId()).orElseThrow();
    return definition.getVersionNumber() >= 2 ? cycle : null;
  }
}
