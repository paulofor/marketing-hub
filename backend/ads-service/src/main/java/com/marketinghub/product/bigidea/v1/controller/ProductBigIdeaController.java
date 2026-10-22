package com.marketinghub.product.bigidea.v1.controller;

import com.marketinghub.product.bigidea.v1.service.BigIdeaService;
import com.marketinghub.product.bigidea.v1.service.get.*;
import com.marketinghub.product.bigidea.v1.service.save.SaveBigIdeaRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Expõe o registro e a evolução da Big Idea na gestão de produtos. */
@RestController
@RequestMapping("/api/products/{productId}/big-idea")
@RequiredArgsConstructor
public class ProductBigIdeaController {
  private final BigIdeaService service;

  /** Lê ideia vigente e histórico do produto. */
  @GetMapping
  public BigIdeaWorkspace get(@PathVariable Long productId) {
    return service.get(productId);
  }

  /** Cria revisão sem iniciar IA, campanha ou publicação comercial. */
  @PostMapping("/revisions")
  public BigIdeaView save(
      @PathVariable Long productId,
      @Valid @RequestBody SaveBigIdeaRequest request,
      Principal principal) {
    return service.save(
        productId,
        request,
        principal == null ? "Operador do painel (identidade não informada)" : principal.getName());
  }
}
