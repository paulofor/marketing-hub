package com.marketinghub.product.modeling.v1.service.save;

import jakarta.validation.constraints.*;
import java.util.*;

/** Recebe somente identidade, referências e resultado desejado, sem autorizar execução. */
public record SaveModelingRequest(
    @NotNull UUID requestKey,
    @Positive Long productId,
    @Size(max = 191) String newProductName,
    @NotBlank @Size(max = 2000) String desiredOutcome,
    @NotNull @Size(min = 1, max = 5) List<@NotBlank @Size(max = 1000) String> referenceUrls) {}
