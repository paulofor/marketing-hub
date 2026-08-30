package com.marketinghub.pde.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Representa um estado financeiro fictício usado somente na homologação local autenticada. */
public record InternalPaymentEntitlementRequest(
        @Email @NotBlank String email,
        @NotBlank String transactionId,
        @NotBlank String paymentStatus,
        @NotBlank String experienceVersion) {}
