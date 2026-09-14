package com.marketinghub.product.executionprofile.v1.service.reconcile;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/** Responsabilidade: receber conciliação humana apoiada em comprovante do provedor. */
public record ReconcileConsumptionRequest(
    @NotNull @Positive Long consumptionId,
    @NotNull @DecimalMin("0") @Digits(integer = 6, fraction = 6) BigDecimal actualBrl,
    @NotBlank @Size(max = 1000) String providerReceipt,
    @NotBlank @Size(max = 100) String reviewedBy,
    @NotBlank @Size(max = 2000) String rationale,
    boolean failed) {}
