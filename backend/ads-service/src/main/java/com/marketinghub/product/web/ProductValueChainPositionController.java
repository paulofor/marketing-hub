package com.marketinghub.product.web;

import com.marketinghub.product.service.valuechainposition.ProductValueChainPositionResponse;
import com.marketinghub.product.service.valuechainposition.ProductValueChainPositionService;
import com.marketinghub.product.service.valuechainposition.summary.ProductValueChainSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor a posição dos produtos na cadeia de valor vigente. */
@RestController
@RequestMapping("/api/products/value-chain-positions")
@RequiredArgsConstructor
public class ProductValueChainPositionController {
  private final ProductValueChainPositionService service;

  /** Lista o processo atual de cada produto para as telas operacionais. */
  @Operation(summary = "Lista a posição dos produtos na cadeia de valor vigente")
  @GetMapping
  public List<ProductValueChainPositionResponse> listPositions(
      @RequestParam(defaultValue = "false") boolean playOnly) {
    return service.listPositions(playOnly);
  }

  /** Retorna a passagem auditável de um produto pelos processos e subprocessos da cadeia. */
  @Operation(summary = "Retorna o histórico de um produto na cadeia de valor vigente")
  @GetMapping("/{productId}")
  public ProductValueChainPositionResponse getPosition(@PathVariable Long productId) {
    return service.getPosition(productId);
  }

  /** Retorna a posição atual sem percorrer tarefas, custos e evidências históricas. */
  @Operation(summary = "Retorna o resumo leve da posição atual do produto")
  @GetMapping("/{productId}/summary")
  public ProductValueChainSummaryResponse getSummary(@PathVariable Long productId) {
    return service.getSummary(productId);
  }
}
