package com.marketinghub.producttype.controller;

import com.marketinghub.producttype.service.ProductTypeService;
import com.marketinghub.producttype.service.catalog.ProductTypeCatalogItemResponse;
import com.marketinghub.producttype.service.catalog.SaveProductTypeRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor o cadastro administrativo de tipos de produto. */
@RestController
@RequestMapping("/api/product-types")
public class ProductTypeController {
  private final ProductTypeService service;

  /** Inicializa o controller com o serviço canônico do catálogo. */
  public ProductTypeController(ProductTypeService service) {
    this.service = service;
  }

  /** Lista tipos por nome, codinome, código ou apelido e permite consultar aposentados. */
  @GetMapping
  public List<ProductTypeCatalogItemResponse> list(
      @RequestParam(required = false) String query,
      @RequestParam(defaultValue = "false") boolean includeRetired) {
    return service.list(query, includeRetired);
  }

  /** Retorna uma definição de tipo pelo identificador interno. */
  @GetMapping("/{id}")
  public ProductTypeCatalogItemResponse get(@PathVariable Long id) {
    return service.get(id);
  }

  /** Cadastra um tipo extensível com codinome e apelidos internos. */
  @PostMapping
  public ProductTypeCatalogItemResponse create(@Valid @RequestBody SaveProductTypeRequest request) {
    return service.create(request);
  }

  /** Atualiza a classificação preservando os produtos e o histórico vinculados. */
  @PutMapping("/{id}")
  public ProductTypeCatalogItemResponse update(
      @PathVariable Long id, @Valid @RequestBody SaveProductTypeRequest request) {
    return service.update(id, request);
  }
}
