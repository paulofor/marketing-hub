package com.marketinghub.salesvideo.autonomy.v1;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Responsabilidade: expor o contrato v1 dos ciclos governados de Apolo e Plutus. */
@RestController
public class VideoProductionCycleController {
  private final VideoProductionCycleService service;

  /** Configura o coordenador canônico dos ciclos. */
  public VideoProductionCycleController(VideoProductionCycleService service) {
    this.service = service;
  }

  /** Solicita um ciclo pelo Estúdio sem iniciar consumo. */
  @PostMapping("/api/sales-videos/autonomy/v1/cycles")
  @ResponseStatus(HttpStatus.CREATED)
  public VideoProductionCycleContracts.Response create(
      @Valid @RequestBody VideoProductionCycleContracts.CreateRequest request) {
    return service.create(request);
  }

  /** Lista ciclos de um projeto para acompanhamento administrativo. */
  @GetMapping("/api/sales-videos/projects/{projectId}/autonomy/v1/cycles")
  public List<VideoProductionCycleContracts.Response> list(@PathVariable Long projectId) {
    return service.list(projectId);
  }

  /** Entrega a Plutus apenas ciclos ainda bloqueados financeiramente. */
  @GetMapping("/api/internal/sales-videos/autonomy/v1/financial-review/pending")
  public List<VideoProductionCycleContracts.Response> pending() {
    return service.pendingFinancialReview();
  }

  /** Recebe a decisão financeira e enfileira Apolo somente quando aprovada. */
  @PostMapping("/api/internal/sales-videos/autonomy/v1/cycles/{cycleId}/financial-decision")
  public VideoProductionCycleContracts.Response decide(
      @PathVariable Long cycleId,
      @Valid @RequestBody VideoProductionCycleContracts.FinancialDecisionRequest request) {
    return service.decide(cycleId, request);
  }

  /** Reconcilia a fila de Apolo antes de o worker consultar novos jobs. */
  @PostMapping("/api/internal/sales-videos/autonomy/v1/apollo/reconcile")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void reconcileApollo() {
    service.reconcileApolloQueue();
  }
}
