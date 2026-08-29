package com.marketinghub.producttype.service.catalog;

import com.marketinghub.producttype.ProductTypeStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Responsabilidade: transportar os dados editáveis de um tipo de produto. */
public record SaveProductTypeRequest(
    @Size(max = 64) String code,
    @NotBlank @Size(max = 191) String name,
    @NotBlank @Size(max = 191) String internalName,
    @Size(max = 1000) String description,
    @Size(max = 20) List<@Size(max = 191) String> aliases,
    ProductTypeStatus status,
    @Valid ProductTypeBlueprintData blueprint) {

  /** Mantém compatibilidade com produtores internos que ainda não enviam base de construção. */
  public SaveProductTypeRequest(
      String code,
      String name,
      String internalName,
      String description,
      List<String> aliases,
      ProductTypeStatus status) {
    this(code, name, internalName, description, aliases, status, null);
  }
}
