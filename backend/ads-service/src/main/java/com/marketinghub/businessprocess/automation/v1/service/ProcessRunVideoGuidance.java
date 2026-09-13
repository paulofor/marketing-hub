package com.marketinghub.businessprocess.automation.v1.service;

import com.marketinghub.businessprocess.automation.v1.service.status.ProcessRunUserAction;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.repository.jpa.salesvideo.VideoProductionCycleRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProviderPreflightRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Responsabilidade: expor a próxima ação da produção audiovisual persistida no processo pai. */
@Component
@RequiredArgsConstructor
public class ProcessRunVideoGuidance {
  private final VideoProductionCycleRepository productions;
  private final VideoProviderPreflightRepository preflights;

  /** Distingue consulta encerrada e preflight bloqueado da peça atual, sem iniciar tentativas. */
  public ProcessRunUserAction resolve(LearningSalesCycle cycle) {
    String role =
        switch (cycle.getStage()) {
          case "CAMPAIGN_VIDEO" -> "CAMPAIGN_QUALIFICATION";
          case "PDE_ENTRY_VIDEO" -> "PDE_HERO_CONVERSION";
          default -> null;
        };
    if (role == null || cycle.getVersionChangedAt() == null) return null;
    var production =
        productions
            .findLatestForLearningCycle(
                cycle.getProductId(),
                cycle.getExperimentId(),
                cycle.getProductVersion(),
                role,
                cycle.getVersionChangedAt())
            .orElse(null);
    if (production == null) return null;
    boolean consultationFinished =
        "PROVIDER_PREFLIGHT_ONLY_COMPLETED".equals(production.getStatus());
    if (!consultationFinished
        && !Set.of("PROVIDER_PREFLIGHT_ONLY_BLOCKED", "PROVIDER_PREFLIGHT_BLOCKED")
            .contains(production.getStatus())) return null;
    var preflight = preflights.findByVideoProductionCycleId(production.getId()).orElse(null);
    if (preflight == null) return null;
    String piece = "CAMPAIGN_VIDEO".equals(cycle.getStage()) ? "anúncio" : "demonstração";
    if (consultationFinished) {
      if (!Set.of("READY", "EXPIRED").contains(preflight.getStatus())
          || production.getAgentTaskId() != null
          || production.getSalesVideoJobId() != null) return null;
      return new ProcessRunUserAction(
          "REQUEST_VIDEO_PRODUCTION",
          "Solicitar produção do " + piece,
          "A consulta do fornecedor terminou e não iniciou a produção. No Estúdio, solicite a produção a Apolo sob controle de Plutus, usando o teto registrado para esta versão.",
          "Operador do ciclo · avaliação de Plutus e produção de Apolo",
          "Solicitar produção do vídeo",
          "/audio-video-studio/projects/" + production.getVideoProjectId(),
          "A solicitação executa um novo preflight e passa por Plutus antes da geração. Vídeo, revisões e aprovações continuam obrigatórios; a consulta anterior não autoriza gasto nem publicação.",
          "internal://sales-videos/autonomy/v1/cycles/"
              + production.getId()
              + "/provider-preflight/"
              + preflight.getId());
    }
    if (!"BLOCKED".equals(preflight.getStatus())) return null;
    boolean missingConfig = "PROVIDER_ROUTER_CONFIG_MISSING".equals(preflight.getFailureCode());
    String reason =
        missingConfig
            ? "A configuração de produção não foi encontrada ou não está acessível na conta do fornecedor. O responsável pela integração precisa corrigir o acesso ou a configuração antes de um novo preflight."
            : "O preflight bloqueou a produção. Consulte o motivo registrado no projeto e corrija o impedimento antes de tentar novamente.";
    return new ProcessRunUserAction(
        "RESOLVE_VIDEO_PREFLIGHT",
        "Produção do " + piece + " bloqueada",
        reason,
        "Responsável pela integração de vídeo",
        "Ver impedimento do vídeo",
        "/audio-video-studio/projects/" + production.getVideoProjectId(),
        "Depois da correção, execute somente o preflight. Produção e revisão exigem seus gates; este bloqueio não comprova venda ou conclusão do processo.",
        "internal://sales-videos/autonomy/v1/cycles/"
            + production.getId()
            + "/provider-preflight/"
            + preflight.getId());
  }
}
