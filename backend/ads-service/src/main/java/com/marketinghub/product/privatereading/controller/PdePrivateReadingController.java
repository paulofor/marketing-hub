package com.marketinghub.product.privatereading.controller;

import com.marketinghub.product.privatereading.service.PdePrivateReadingService;
import com.marketinghub.product.privatereading.service.workspace.PrivateReadingWorkspace;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor a assistência de leitura privada na tela do próprio produto. */
@RestController
@RequestMapping("/api/products/{productId}/private-readings")
public class PdePrivateReadingController {
  private final PdePrivateReadingService service;

  /** Recebe o serviço que consulta e valida a prova do protótipo. */
  public PdePrivateReadingController(PdePrivateReadingService service) {
    this.service = service;
  }

  /** Entrega acesso sem segredo e sinais verificados sem criar sessão ou leitura humana. */
  @GetMapping("/{activityId}")
  public ResponseEntity<PrivateReadingWorkspace> workspace(
      @PathVariable long productId, @PathVariable String activityId) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(service.workspace(productId, activityId));
  }
}
