package com.marketinghub.product.fichas.v1.controller;

import com.marketinghub.product.fichas.v1.service.FichasService;
import com.marketinghub.product.fichas.v1.service.list.FichasView;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

/** Responsabilidade: expor consulta de fichas exclusivamente quando habilitada em homologação. */
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "marketinghub.fichas.homologacao", havingValue = "true")
@RequestMapping("/api/products/{productId}/fichas/v1")
public class FichasController {
  private final FichasService service;

  /** Consulta revisões históricas sem modificar permissões ou execuções. */
  @GetMapping
  public FichasView list(@PathVariable Long productId, @RequestParam(required = false) Long ciclo) {
    return service.list(productId, ciclo);
  }
}
