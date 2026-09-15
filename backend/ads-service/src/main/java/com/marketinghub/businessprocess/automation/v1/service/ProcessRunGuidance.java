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

  private final com.marketinghub.businessprocesschain.learningcycle.v1.service
          .LearningCycleCommercialReadiness
      commercialReadiness;

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleVideoBinding
      videoBinding;

  /** Resolve a próxima ação pela ocorrência exata, sem autorizar consumo ou alterar estado. */
  public ProcessRunUserAction resolve(ProcessRun run) {
    if (!Set.of("QUEUED", "WAITING_ACTIVITY", "WAITING_HUMAN", "WAITING_INPUT")
            .contains(run.getStatus())
        || run.getFailureCount() > 0) return null;
    var cycle = manualCycle(run);
    if (cycle == null) return null;
    if (Set.of("AUTHORIZATION", "PUBLICATION").contains(cycle.getStage())) {
      var preparation = commercialReadiness.inspect(cycle);
      if (preparation != null && !preparation.readyForReview())
        return new ProcessRunUserAction(
            "PREPARE_CYCLE_COMMERCIAL",
            "A jornada comercial precisa ser preparada",
            preparation.guidance(),
            "Preparação comercial · responsáveis pelas pendências do experimento",
            "Ver pendências do ciclo",
            cycleUrl(cycle),
            "Conclua os vínculos desta versão pelo fluxo oficial. Os vídeos aprovados e o histórico permanecem preservados; autorização de mídia continua sendo uma decisão explícita.",
            "internal://learning-cycles/" + cycle.getId());
    }
    if ("AUTHORIZATION".equals(cycle.getStage()))
      return new ProcessRunUserAction(
          "AUTHORIZE_CYCLE_MEDIA",
          "Falta sua decisão sobre o teto de mídia",
          "O ciclo #"
              + cycle.getId()
              + " · experimento #"
              + cycle.getExperimentId()
              + " aguarda a confirmação do orçamento e da janela. A homologação já registrada não autoriza mídia. Aguardar nesta tela não registra essa decisão.",
          "Você · responsável pelo orçamento de mídia",
          "Revisar orçamento e pendências",
          cycleUrl(cycle),
          "O aceite registra os limites no experimento. A preparação, a homologação comercial e a autorização final de ativação continuam obrigatórias antes da campanha.",
          "internal://learning-cycles/" + cycle.getId());
    if ("PUBLICATION".equals(cycle.getStage()))
      return new ProcessRunUserAction(
          "COMPLETE_COMMERCIAL_PREPARATION",
          "Continuar a preparação e conferir a publicação",
          "O teto do ciclo foi registrado. Conclua as pendências e a homologação comercial pelo processo indicado no ciclo; depois confira a publicação do mesmo experimento.",
          "Operador do ciclo · preparação e autorização final",
          "Continuar no ciclo",
          cycleUrl(cycle),
          "Somente a confirmação da campanha pelo fluxo oficial permite avançar para medição. Venda, entrega e aprendizado exigem suas próprias evidências.",
          "internal://learning-cycles/" + cycle.getId());
    if ("VIDEO_APPROVAL".equals(cycle.getStage())
        && videoBinding != null
        && videoBinding.supports(cycle)) {
      if (!videoBinding.awaitingApproval(cycle)) return null;
      return new ProcessRunUserAction(
          "APPROVE_CYCLE_VIDEOS",
          "Aprovar os vídeos do ciclo",
          "Aprove as peças na biblioteca. As aprovações existentes são reutilizadas e a integração privada segue automaticamente pelo processo.",
          "Você · aprovação das peças",
          "Ver vídeos e aprovações",
          "/videos",
          "A aprovação de uso não autoriza campanha, cobrança ou gasto de mídia.",
          "internal://learning-cycles/" + cycle.getId());
    }
    if ("VIDEO_APPROVAL".equals(cycle.getStage()))
      return new ProcessRunUserAction(
          "REVIEW_AND_INTEGRATE_VIDEOS",
          "Revisar e integrar os dois vídeos",
          "A produção foi registrada. Conclua as revisões independentes, a decisão humana de uso e a integração dos vídeos desta versão antes da homologação.",
          "Operador do ciclo · Psique, Têmis e aprovação humana",
          "Revisar vídeos e integração",
          "/business-process-chains/learning-cycles?chainId="
              + cycle.getChainDefinitionId()
              + "&productId="
              + cycle.getProductId()
              + "&cycleId="
              + cycle.getId(),
          "Registre somente as evidências verificadas. A aprovação de uso e a integração não autorizam campanha, cobrança ou gasto de mídia.",
          "internal://learning-cycles/" + cycle.getId());
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
    if (cycle != null
        && "VIDEO_APPROVAL".equals(cycle.getStage())
        && videoBinding != null
        && videoBinding.supports(cycle)) return videoBinding.awaitingApproval(cycle);
    return cycle != null
        && (Set.of("VIDEO_BRIEF", "VIDEO_APPROVAL", "AUTHORIZATION", "PUBLICATION")
                .contains(cycle.getStage())
            || videoGuidance.resolve(cycle) != null);
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
        || !Set.of(
                "VIDEO_BRIEF",
                "CAMPAIGN_VIDEO",
                "PDE_ENTRY_VIDEO",
                "VIDEO_APPROVAL",
                "AUTHORIZATION",
                "PUBLICATION")
            .contains(cycle.getStage())) return null;
    var definition = processes.findById(cycle.getProcessDefinitionId()).orElseThrow();
    return definition.getVersionNumber()
            >= (Set.of("AUTHORIZATION", "PUBLICATION").contains(cycle.getStage()) ? 1 : 2)
        ? cycle
        : null;
  }

  /** Preserva as três identidades na navegação de ida e retorno à decisão comercial. */
  private String cycleUrl(LearningSalesCycle cycle) {
    return "/business-process-chains/learning-cycles?chainId="
        + cycle.getChainDefinitionId()
        + "&productId="
        + cycle.getProductId()
        + "&cycleId="
        + cycle.getId()
        + "#cycle-decision";
  }
}
