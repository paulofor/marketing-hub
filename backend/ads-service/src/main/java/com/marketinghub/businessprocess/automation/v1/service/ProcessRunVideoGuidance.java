package com.marketinghub.businessprocess.automation.v1.service;

import com.marketinghub.businessprocess.automation.v1.service.status.ProcessRunUserAction;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.repository.jpa.salesvideo.VideoProductionCycleRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProviderPreflightRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Responsabilidade: expor o impedimento persistido da produção audiovisual no processo pai. */
@Component
@RequiredArgsConstructor
public class ProcessRunVideoGuidance {
  private final VideoProductionCycleRepository productions;
  private final VideoProviderPreflightRepository preflights;

  /** Projeta apenas preflight bloqueado da peça e versão atuais, sem iniciar novas tentativas. */
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
    if (production == null
        || !Set.of("PROVIDER_PREFLIGHT_ONLY_BLOCKED", "PROVIDER_PREFLIGHT_BLOCKED")
            .contains(production.getStatus())) return null;
    var preflight = preflights.findByVideoProductionCycleId(production.getId()).orElse(null);
    if (preflight == null || !"BLOCKED".equals(preflight.getStatus())) return null;
    boolean missingConfig = "PROVIDER_ROUTER_CONFIG_MISSING".equals(preflight.getFailureCode());
    String piece = "CAMPAIGN_VIDEO".equals(cycle.getStage()) ? "anúncio" : "demonstração";
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
