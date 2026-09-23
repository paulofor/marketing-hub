package com.marketinghub.productdiscovery.v1.service;

import com.marketinghub.productdiscovery.v1.ProductDiscoveryInterviewOutcome;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Entrada anônima de uma entrevista sobre uma decisão de compra passada. */
public record ProductDiscoveryCustomerInterviewRequest(
    @NotNull Long opportunityId,
    @NotBlank @Size(max = 7) @Pattern(regexp = "[A-Za-z]{1,3}[0-9]{1,4}")
        String anonymousParticipantCode,
    @NotNull ProductDiscoveryInterviewOutcome outcome,
    @NotNull LocalDate occurredOn,
    @NotBlank @Size(max = 3000) String purchaseSituation,
    @NotBlank @Size(max = 3000) String desiredResult,
    @NotBlank @Size(max = 3000) String difficulty,
    @NotBlank @Size(max = 3000) String alternativeTried,
    @DecimalMin(value = "0.00") BigDecimal amountSpent,
    @Size(min = 3, max = 3) String currency,
    @NotBlank @Size(max = 3000) String remainingDifficulty,
    @AssertTrue boolean consentConfirmed,
    @AssertTrue boolean noPersonalDataConfirmed) {}
