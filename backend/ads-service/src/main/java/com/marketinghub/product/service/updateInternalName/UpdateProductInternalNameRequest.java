package com.marketinghub.product.service.updateInternalName;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Responsabilidade: transportar a atualização isolada do nome interno de um produto. */
public record UpdateProductInternalNameRequest(@NotBlank @Size(max = 191) String internalName) {}
