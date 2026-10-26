package com.marketinghub.product.modeling.v1.controller;

import com.marketinghub.product.modeling.v1.service.ModelingService;
import com.marketinghub.product.modeling.v1.service.get.ModelingView;
import com.marketinghub.product.modeling.v1.service.save.SaveModelingRequest;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Expõe a preparação e a pesquisa por referências na gestão de produtos. */
@RestController
@RequestMapping("/api/product-modeling/v1")
@RequiredArgsConstructor
public class ProductModelingController {
  private final ModelingService service;

  /** Lista modelagens sem iniciar trabalho. */
  @GetMapping
  @Operation(summary = "Lista modelagens por referências")
  public List<ModelingView> list() {
    return service.list();
  }

  /** Consulta uma preparação com identidade e situação oficial. */
  @GetMapping("/{id}")
  @Operation(summary = "Consulta modelagem e sua pesquisa")
  public ModelingView get(@PathVariable Long id) {
    return service.get(id);
  }

  /** Prepara um rascunho idempotente sem consumir IA. */
  @PostMapping
  @Operation(summary = "Prepara modelagem sem iniciar inferência")
  public ModelingView prepare(@Valid @RequestBody SaveModelingRequest request) {
    return service.prepare(request);
  }

  /** Vincula a pesquisa à preparação escolhida, sem autorizar campanha ou consumir modelos. */
  @PostMapping("/{id}/adopt")
  @Operation(summary = "Vincula fontes a um ciclo ainda não executado")
  public ModelingView adopt(
      @PathVariable Long id,
      @Valid @RequestBody
          com.marketinghub.product.modeling.v1.service.save.AdoptModelingRequest request) {
    return service.adopt(id, request.cycleId());
  }

  /** Inicia explicitamente a pesquisa, preservando replay e histórico. */
  @PostMapping("/{id}/start")
  @Operation(summary = "Inicia Argos na fila de descoberta existente")
  public ModelingView start(@PathVariable Long id) {
    return service.start(id);
  }

  /** Retoma a mesma pesquisa somente após comprovar a correção da autenticação de Argos. */
  @PostMapping("/{id}/resume")
  @Operation(summary = "Retoma pesquisa inicial após reconexão autenticada, preservando histórico")
  public ModelingView resume(@PathVariable Long id) {
    return service.resume(id);
  }
}
