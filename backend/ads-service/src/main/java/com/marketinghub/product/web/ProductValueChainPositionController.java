package com.marketinghub.product.web;

import com.marketinghub.product.service.valuechainposition.ProductValueChainPositionResponse;
import com.marketinghub.product.service.valuechainposition.ProductValueChainPositionService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor a posição dos produtos na cadeia de valor vigente. */
@RestController
@RequestMapping("/api/products/value-chain-positions")
@RequiredArgsConstructor
public class ProductValueChainPositionController {
  private final ProductValueChainPositionService service;

  /** Lista o processo atual de cada produto para as telas operacionais. */
  @Operation(summary = "Lista a posição dos produtos na cadeia de valor vigente")
  @GetMapping
  public List<ProductValueChainPositionResponse> listPositions() {
    return service.listPositions();
  }
}
