package com.marketinghub.financialplan.v1.controller;

import com.marketinghub.financialplan.v1.FinancialPlanRevision.Environment;
import com.marketinghub.financialplan.v1.service.FinancialPlanService;
import com.marketinghub.financialplan.v1.service.catalog.PlanCatalog;
import com.marketinghub.financialplan.v1.service.getplan.PlanView;
import com.marketinghub.financialplan.v1.service.saveplan.SavePlanRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Responsabilidade: expor o contrato administrativo canônico de planos financeiros e modelos. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/financial-plans/v1")
public class FinancialPlanController {
  private final FinancialPlanService service;

  /** Lista referências válidas para os seletores do plano. */
  @GetMapping("/catalog")
  public PlanCatalog catalog(@RequestParam(required = false) Long productId) {
    return service.catalog(productId);
  }

  /** Lista histórico segregado de um produto. */
  @GetMapping("/products/{id}")
  public List<PlanView> products(
      @PathVariable Long id, @RequestParam(defaultValue = "LIVE") Environment environment) {
    return service.list("PRODUCT", id, environment);
  }

  /** Lista histórico de modelos de um tipo. */
  @GetMapping("/product-types/{id}")
  public List<PlanView> types(
      @PathVariable Long id, @RequestParam(defaultValue = "LIVE") Environment environment) {
    return service.list("TYPE", id, environment);
  }

  /** Cria revisão financeira de um produto específico. */
  @PostMapping("/products/{id}")
  public PlanView createProduct(
      @PathVariable Long id,
      @RequestParam(defaultValue = "LIVE") Environment environment,
      @Valid @RequestBody SavePlanRequest request) {
    return service.create("PRODUCT", id, environment, request);
  }

  /** Cria modelo reutilizável sem aprovar produtos automaticamente. */
  @PostMapping("/product-types/{id}")
  public PlanView createType(
      @PathVariable Long id,
      @RequestParam(defaultValue = "LIVE") Environment environment,
      @Valid @RequestBody SavePlanRequest request) {
    return service.create("TYPE", id, environment, request);
  }

  /** Recupera uma revisão exata do produto. */
  @GetMapping("/products/{id}/revisions/{revisionId}")
  public PlanView product(
      @PathVariable Long id,
      @PathVariable Long revisionId,
      @RequestParam(defaultValue = "LIVE") Environment environment) {
    return service.get("PRODUCT", id, environment, revisionId);
  }

  /** Solicita avaliação de Plutus de forma idempotente para a revisão do produto. */
  @PostMapping("/products/{id}/revisions/{revisionId}/analysis")
  public PlanView analyze(
      @PathVariable Long id,
      @PathVariable Long revisionId,
      @RequestParam(defaultValue = "LIVE") Environment environment) {
    return service.requestAnalysis(id, environment, revisionId);
  }
}
