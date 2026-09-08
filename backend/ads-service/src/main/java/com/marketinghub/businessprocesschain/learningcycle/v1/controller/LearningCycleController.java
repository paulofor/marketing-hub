package com.marketinghub.businessprocesschain.learningcycle.v1.controller;

import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleService;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.command.LearningCycleCommand;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.createCycle.CreateLearningCycleRequest;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles.*;
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

  /** Expõe o grafo e as opções oficiais para a cadeia e o produto selecionados. */
  @GetMapping("/catalog")
  public LearningCycleCatalog catalog(
      @RequestParam Long chainId, @RequestParam(required = false) Long productId) {
    return service.catalog(chainId, productId);
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
}
