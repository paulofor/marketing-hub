package com.marketinghub.businessprocesschain.learningcycle.v1.controller;

import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleService;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.command.LearningCycleCommand;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.createCycle.CreateLearningCycleRequest;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.reconcileMeasurement.ReconcileLearningCycleMeasurementRequest;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.videoBudget.*;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Responsabilidade: expor decisões e leitura dos ciclos comerciais pela interface administrativa.
 */
@RestController
@RequestMapping("/api/business-process-chains/learning-cycles/v1")
@RequiredArgsConstructor
public class LearningCycleController {
  private final LearningCycleService service;

  /** Expõe o grafo e o retorno oficiais, preservando a ocorrência selecionada quando informada. */
  @io.swagger.v3.oas.annotations.Operation(
      summary =
          "Catálogo do ciclo com retorno ao processo pai no mesmo produto, cadeia e ocorrência")
  @GetMapping("/catalog")
  public LearningCycleCatalog catalog(
      @RequestParam Long chainId,
      @RequestParam(required = false) Long productId,
      @RequestParam(required = false) Long cycleId) {
    return service.catalog(chainId, productId, cycleId);
  }

  /** Resolve a entrada pelo BPM pai ou subprocesso sem criar nenhuma ocorrência. */
  @GetMapping("/entry")
  public LearningCycleEntry entry(
      @RequestParam Long processDefinitionId,
      @RequestParam(required = false) Long productId,
      @RequestParam(required = false) Long chainId) {
    return service.entry(processDefinitionId, productId, chainId);
  }

  /** Lista o histórico segregado do produto e, opcionalmente, de uma cadeia. */
  @GetMapping("/products/{productId}")
  public List<LearningCycleResponse> list(
      @PathVariable Long productId, @RequestParam(required = false) Long chainId) {
    return service.list(productId, chainId);
  }

  /** Mostra a passagem atual e a próxima atividade com memória e identidade do ciclo. */
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Contexto do ciclo e continuidade no processo do produto")
  @GetMapping("/products/{productId}/process-context")
  public LearningCycleProcessContext processContext(
      @PathVariable Long productId,
      @RequestParam Long processDefinitionId,
      @RequestParam(required = false) Long cycleId,
      @RequestParam(required = false) Long chainId) {
    return service.processContext(productId, processDefinitionId, cycleId, chainId);
  }

  /** Inicia uma iteração a partir de um experimento escolhido explicitamente na tela. */
  @PostMapping("/products/{productId}")
  public LearningCycleResponse create(
      @PathVariable Long productId, @Valid @RequestBody CreateLearningCycleRequest request) {
    return service.create(productId, request);
  }

  /** Registra a decisão com revisão esperada e chave de idempotência. */
  @PostMapping("/products/{productId}/{cycleId}/commands")
  public LearningCycleResponse command(
      @PathVariable Long productId,
      @PathVariable Long cycleId,
      @Valid @RequestBody LearningCycleCommand request) {
    return service.command(productId, cycleId, request);
  }

  /** Solicita nova leitura das fontes oficiais sem receber métricas digitadas pela tela. */
  @PostMapping("/products/{productId}/{cycleId}/measurement-reconciliation")
  public LearningCycleResponse reconcileMeasurement(
      @PathVariable Long productId,
      @PathVariable Long cycleId,
      @Valid @RequestBody ReconcileLearningCycleMeasurementRequest request) {
    return service.reconcileMeasurement(productId, cycleId, request);
  }

  /** Apresenta o teto de produção e revisão das duas peças e seu histórico por ciclo. */
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Consultar financeiro dos dois vídeos do ciclo")
  @GetMapping("/products/{productId}/{cycleId}/video-budget")
  public VideoBudgetResponse videoBudget(
      @PathVariable("productId") Long productId,
      @PathVariable("cycleId") Long cycleId,
      @RequestParam("chainId") Long chainId) {
    return service.videoBudget(productId, cycleId, chainId);
  }

  /** Registra autorização humana restrita sem gerar vídeos, mídia, cobrança ou publicação. */
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Autorizar teto total de produção e revisão dos dois vídeos")
  @PostMapping("/products/{productId}/{cycleId}/video-budget")
  public VideoBudgetResponse authorizeVideoBudget(
      @PathVariable("productId") Long productId,
      @PathVariable("cycleId") Long cycleId,
      @Valid @RequestBody AuthorizeVideoBudgetRequest request) {
    return service.authorizeVideoBudget(productId, cycleId, request);
  }
}
