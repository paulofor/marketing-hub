package com.marketinghub.product.operations.v1.controller;

import com.marketinghub.product.operations.v1.service.ProductOperationsService;
import com.marketinghub.product.operations.v1.service.inplay.ProductsInPlayResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: disponibilizar a visão consultiva dos produtos em operação. */
@RestController
@RequestMapping("/api/products/operations/v1")
@RequiredArgsConstructor
public class ProductOperationsController {
  private final ProductOperationsService service;

  /** Lista atividades oficiais sem iniciar tarefas, alterar estados ou autorizar consumo. */
  @GetMapping("/in-play")
  @Operation(
      summary = "Lista produtos em PLAY com atividade corrente e status real",
      description =
          "Consulta sem efeitos operacionais. Preserva ciclo, referência e versão da cadeia; falhas parciais aparecem como UNAVAILABLE.")
  public ProductsInPlayResponse inPlay() {
    return service.inPlay();
  }
}
