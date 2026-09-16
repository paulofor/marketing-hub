package com.marketinghub.catalogovivo.v1.controller;

import com.marketinghub.catalogovivo.v1.service.CatalogoVivoService;
import com.marketinghub.catalogovivo.v1.service.OpalaCycleAdoption;
import com.marketinghub.catalogovivo.v1.service.adoption.*;
import com.marketinghub.catalogovivo.v1.service.catalog.*;
import com.marketinghub.catalogovivo.v1.service.commands.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Responsabilidade: expor a gestão administrativa dos textos e a adesão explícita ao piloto Opala.
 */
@RestController
@RequestMapping("/api/catalogo-vivo/v1/opala")
@Tag(name = "Catálogo Vivo — Piloto Opala")
@RequiredArgsConstructor
public class CatalogoVivoController {
  private final CatalogoVivoService service;
  private final OpalaCycleAdoption adoption;

  /** Consulta cobertura, textos, revisões e decisões sem iniciar trabalho. */
  @GetMapping
  @Operation(summary = "Consulta o conjunto completo de prompts textuais Opala")
  public CatalogResponse catalog() {
    return service.catalog();
  }

  /** Cria outra versão textual, sem sobrescrever o rascunho ou a versão selecionada. */
  @PostMapping("/bindings/{bindingId}/drafts")
  @Operation(summary = "Cria um rascunho de prompt para revisão")
  public CatalogVersion draft(
      @PathVariable long bindingId, @Valid @RequestBody CatalogDraftRequest request) {
    return service.draft(bindingId, request);
  }

  /** Registra a revisão do conteúdo exato apresentado ao responsável. */
  @PostMapping("/versions/{versionId}/review")
  @Operation(summary = "Revisa o hash exato da versão textual")
  public CatalogVersion review(
      @PathVariable long versionId, @Valid @RequestBody CatalogReviewRequest request) {
    return service.review(versionId, request);
  }

  /** Ativa o pacote completo ou recupera versões anteriores já revisadas. */
  @PostMapping("/activation")
  @Operation(summary = "Ativa versões revisadas em transação única com controle de concorrência")
  public CatalogResponse activate(@Valid @RequestBody CatalogActivationRequest request) {
    return service.activate(request);
  }

  /** Informa se o ciclo pode aderir, sem modificar cadeia, orçamento ou histórico. */
  @GetMapping("/products/{productId}/cycles/{cycleId}/adoption")
  @Operation(summary = "Consulta prontidão e navegação da adesão do ciclo ao piloto")
  public OpalaAdoptionResponse status(@PathVariable long productId, @PathVariable long cycleId) {
    return adoption.status(productId, cycleId);
  }

  /** Incorpora Opala à passagem e inicia preparação governada pelo backend. */
  @PostMapping("/products/{productId}/cycles/{cycleId}/adoption")
  @Operation(summary = "Integra e inicia preparação Opala preservando orçamento e aprovações")
  public OpalaAdoptionResponse adopt(
      @PathVariable long productId,
      @PathVariable long cycleId,
      @Valid @RequestBody OpalaAdoptionRequest request) {
    return adoption.adopt(productId, cycleId, request);
  }
}
