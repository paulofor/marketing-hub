package com.marketinghub.businessprocesschain.learningcycle.v1.decision.controller;

import com.marketinghub.businessprocesschain.learningcycle.v1.decision.service.LearningCycleDecisionService;
import com.marketinghub.businessprocesschain.learningcycle.v1.decision.service.audit.DecisionProposalAudit;
import com.marketinghub.businessprocesschain.learningcycle.v1.decision.service.get.DecisionProposalResponse;
import com.marketinghub.businessprocesschain.learningcycle.v1.decision.service.pending.PendingDecisionProposal;
import com.marketinghub.businessprocesschain.learningcycle.v1.decision.service.result.DecisionProposalResult;
import com.marketinghub.businessprocesschain.learningcycle.v1.decision.service.retry.RetryDecisionProposal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Responsabilidade: expor a fila de Atena e a leitura administrativa da proposta da decisão. */
@RestController
@RequiredArgsConstructor
public class LearningCycleDecisionController {
  private static final String ADMIN =
      "/api/business-process-chains/learning-cycles/v1/products/{productId}/{cycleId}/decision-proposal";
  private static final String INTERNAL =
      "/api/internal/business-process-chains/learning-cycles/v1/decision/stage-executions";
  private final LearningCycleDecisionService service;

  /** Mostra a proposta e seu estado sem disparar execução pela navegação. */
  @GetMapping(ADMIN)
  public DecisionProposalResponse get(@PathVariable Long productId, @PathVariable Long cycleId) {
    return service.get(productId, cycleId);
  }

  /** Carrega sob demanda a entrada, resposta bruta e aprovação de cada tentativa. */
  @GetMapping(ADMIN + "/audit")
  public List<Map<String, Object>> audit(@PathVariable Long productId, @PathVariable Long cycleId) {
    return service.audit(productId, cycleId);
  }

  /** Solicita recuperação explícita preservando a execução anterior. */
  @PostMapping(ADMIN + "/retry")
  public DecisionProposalResponse retry(
      @PathVariable Long productId,
      @PathVariable Long cycleId,
      @Valid @RequestBody RetryDecisionProposal request) {
    return service.retry(productId, cycleId, request);
  }

  /** Reserva uma decisão elegível para o worker de Atena. */
  @GetMapping(INTERNAL + "/pending")
  public List<PendingDecisionProposal> pending() {
    return service.pending();
  }

  /** Recebe o prompt e a configuração antes de qualquer resposta do modelo. */
  @PutMapping(INTERNAL + "/{id}/request")
  public void request(@PathVariable Long id, @Valid @RequestBody DecisionProposalAudit request) {
    service.recordAudit(id, request);
  }

  /** Recebe a proposta, falha e consumo sem aprovar nem avançar o ciclo comercial. */
  @PostMapping(INTERNAL + "/{id}/result")
  public DecisionProposalResponse result(
      @PathVariable Long id, @Valid @RequestBody DecisionProposalResult request) {
    return service.result(id, request);
  }
}
