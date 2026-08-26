package com.marketinghub.product.web;

import com.marketinghub.product.service.processcommit.ProductProcessCommitRegistrationResult;
import com.marketinghub.product.service.processcommit.ProductProcessCommitResponse;
import com.marketinghub.product.service.processcommit.ProductProcessCommitService;
import com.marketinghub.product.service.processcommit.RegisterProductProcessCommitRequest;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor os commits atribuídos a produtos e processos. */
@RestController
@RequestMapping("/api/products/{productId}/process-commits")
@RequiredArgsConstructor
public class ProductProcessCommitController {
  private final ProductProcessCommitService service;

  /** Lista o histórico de commits segregado pelo produto informado. */
  @Operation(summary = "Lista commits registrados nos processos de um produto")
  @GetMapping
  public List<ProductProcessCommitResponse> list(@PathVariable Long productId) {
    return service.list(productId);
  }

  /** Retorna um commit individual mantendo a segregação pelo produto da rota. */
  @Operation(summary = "Detalha um commit registrado em um processo do produto")
  @GetMapping("/{commitId}")
  public ProductProcessCommitResponse get(
      @PathVariable Long productId, @PathVariable Long commitId) {
    return service.get(productId, commitId);
  }

  /** Registra um commit no processo exato ou devolve o vínculo idempotente já existente. */
  @Operation(summary = "Registra um commit realizado para um produto em um processo")
  @PostMapping
  public ResponseEntity<ProductProcessCommitResponse> register(
      @PathVariable Long productId,
      @Valid @RequestBody RegisterProductProcessCommitRequest request) {
    ProductProcessCommitRegistrationResult result = service.register(productId, request);
    if (!result.created()) {
      return ResponseEntity.ok(result.commit());
    }
    return ResponseEntity.created(
            URI.create("/api/products/" + productId + "/process-commits/" + result.commit().id()))
        .body(result.commit());
  }
}
