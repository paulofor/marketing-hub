package com.marketinghub.salesvideo.service.clipplan;

import com.marketinghub.salesvideo.SalesVideoProviderModel;
import com.marketinghub.videocontract.v1.VideoCapabilities;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Responsabilidade: simular o plano governado sem consumir saldo nem aprovar produção. */
public final class StudioClipPlanner {
  /** Impede criar estado compartilhado entre produtos ou simulações. */
  private StudioClipPlanner() {}

  /**
   * Prevê clipes válidos e distingue custo parcial de orçamento integral e integração homologada.
   */
  public static StudioClipPlanResponse preview(
      SalesVideoProviderModel persisted, StudioClipPlanRequest request, Instant now) {
    var model =
        VideoCapabilities.provider(request.providerName())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Rota sem contrato compartilhado; homologar adapter e capacidades antes de planejar"));
    var plan = model.plan(request.finalSeconds());
    List<String> blockers = new ArrayList<>();
    if (persisted == null
        || !"ACTIVE".equals(persisted.getLifecycleStatus())
        || !persisted.isAdapterVerified()
        || !persisted.isPricingVerified()
        || !persisted.isQualityGateVerified()
        || !persisted.isCommercialLicenseVerified())
      blockers.add("Integração em cadastro/homologação; concluir gates reais antes de produzir.");
    if (!model.adapterModes().contains(request.imageReferences() > 0 ? "IMAGE" : "TEXT"))
      blockers.add("A modalidade pedida ainda não foi implementada no adapter.");
    if (request.imageReferences() > model.adapterMaxImageReferences()
        || request.referenceVideoSeconds() > 0)
      blockers.add(
          "O modelo público aceita referências, mas o adapter instalado ainda não implementa esta combinação; homologar sem trocar a rota.");
    List<StudioClipPlanResponse.Scene> scenes = new ArrayList<>();
    BigDecimal sum = BigDecimal.ZERO;
    int remaining = request.finalSeconds();
    for (int i = 0; i < plan.clipSeconds().size(); i++) {
      int billed = plan.clipSeconds().get(i), used = Math.min(billed, remaining);
      BigDecimal cost = null;
      try {
        cost =
            model
                .credits(
                    billed,
                    request.resolution(),
                    request.audio(),
                    request.imageReferences(),
                    request.referenceVideoSeconds(),
                    now)
                .movePointLeft(2);
      } catch (IllegalArgumentException ex) {
        if (!blockers.contains(ex.getMessage())) blockers.add(ex.getMessage());
      }
      if (cost != null) sum = sum.add(cost);
      scenes.add(
          new StudioClipPlanResponse.Scene(
              i + 1,
              model.modelId(),
              used,
              used,
              billed,
              cost,
              i == 0
                  ? "Gancho e situação"
                  : i == plan.clipSeconds().size() - 1
                      ? "Resultado e chamada para ação"
                      : "Mecanismo e demonstração real"));
      remaining -= used;
    }
    blockers.add(
        "Esta previsão cobre clipes. Agentes, referências, voz, trilha, montagem, taxas e tentativas precisam de fonte e reserva integral; saldo, direitos e Router precisam de preflight. Não gera vídeo.");
    return new StudioClipPlanResponse(
        VideoCapabilities.catalog().revision(),
        plan.finalSeconds(),
        plan.generatedSeconds(),
        plan.trimSeconds(),
        List.copyOf(scenes),
        scenes.stream().anyMatch(s -> s.maximumCostUsd() == null) ? null : sum,
        false,
        List.copyOf(blockers));
  }
}
