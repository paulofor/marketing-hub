package com.marketinghub.producttype.service.catalog;

import com.marketinghub.producttype.ProductTypeStatus;
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
    ProductTypeStatus status) {}
