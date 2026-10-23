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

  /** Consulta a pesquisa de Atena desta identidade sem iniciar trabalho. */
  @GetMapping("/research")
  public com.marketinghub.product.bigidea.v1.service.research.BigIdeaResearchView research(
      @PathVariable Long productId) {
    return service.research(productId);
  }

  /** Solicita somente a primeira Big Idea, preservando planejamento e autorizações comerciais. */
  @PostMapping("/research")
  public com.marketinghub.product.bigidea.v1.service.research.BigIdeaResearchView requestResearch(
      @PathVariable Long productId,
      @Valid @RequestBody
          com.marketinghub.product.bigidea.v1.service.research.StartBigIdeaResearchRequest
              request) {
    return service.requestResearch(productId, request);
  }

  /** Registra proposta concluída de Atena após comando explícito da gestão de produtos. */
  @PostMapping("/research/{executionId}/adopt")
  public BigIdeaView adopt(@PathVariable Long productId, @PathVariable Long executionId) {
    return service.adoptResearch(productId, executionId);
  }

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
