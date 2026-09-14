package com.marketinghub.product.executionprofile.v1.controller;

import com.marketinghub.product.executionprofile.v1.service.ExecutionProfileService;
import com.marketinghub.product.executionprofile.v1.service.bind.BindProfileRequest;
import com.marketinghub.product.executionprofile.v1.service.getprofile.ProfileView;
import com.marketinghub.product.executionprofile.v1.service.review.ReviewProfileRequest;
import com.marketinghub.product.executionprofile.v1.service.saveprofile.SaveProfileRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Responsabilidade: expor o contrato administrativo de fichas versionadas e checkpoints
 * financeiros.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products/{productId}/execution-profiles/v1")
public class ExecutionProfileController {
  private final ExecutionProfileService service;

  /** Expõe opções do cadastro sem classificar automaticamente o produto. */
  @GetMapping("/catalog")
  public com.marketinghub.product.executionprofile.v1.service.getprofile.ProfileCatalog catalog(
      @PathVariable Long productId) {
    return service.catalog(productId);
  }

  /** Lista revisões e vínculos existentes. */
  @GetMapping
  public List<ProfileView> list(@PathVariable Long productId) {
    return service.list(productId);
  }

  /** Consulta uma revisão exata. */
  @GetMapping("/{id}")
  public ProfileView get(@PathVariable Long productId, @PathVariable Long id) {
    return service.get(productId, id);
  }

  /** Cria uma revisão, preservando as anteriores. */
  @PostMapping
  public ProfileView create(
      @PathVariable Long productId, @Valid @RequestBody SaveProfileRequest request) {
    return service.create(productId, request);
  }

  /** Congela uma revisão no contexto de uma nova execução. */
  @PostMapping("/{id}/bindings")
  public ProfileView bind(
      @PathVariable Long productId,
      @PathVariable Long id,
      @Valid @RequestBody BindProfileRequest request) {
    return service.bind(productId, id, request);
  }

  /** Solicita análise de Plutus somente quando a revisão ainda não possui parecer. */
  @PostMapping("/{id}/financial-analysis")
  public ProfileView analyze(@PathVariable Long productId, @PathVariable Long id) {
    return service.requestAnalysis(productId, id);
  }

  /** Registra decisão identificada após leitura do parecer. */
  @PostMapping("/{id}/financial-reviews")
  public ProfileView review(
      @PathVariable Long productId,
      @PathVariable Long id,
      @Valid @RequestBody ReviewProfileRequest request) {
    return service.review(productId, id, request);
  }

  /** Concilia custo pendente com comprovante oficial e responsabilidade humana explícita. */
  @PostMapping("/{id}/consumption-reconciliations")
  public ProfileView reconcile(
      @PathVariable Long productId,
      @PathVariable Long id,
      @Valid @RequestBody
          com.marketinghub.product.executionprofile.v1.service.reconcile.ReconcileConsumptionRequest
              request) {
    return service.reconcile(productId, id, request);
  }
}
