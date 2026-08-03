package com.marketinghub.payments.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Contrato público dos dados necessários para personalizar o kit Agenda Cheia. */
public record AgendaCheiaBriefingRequest(
        @NotBlank String paymentId,
        @NotBlank @Email String buyerEmail,
        @NotBlank @Size(max = 180) String professionalName,
        @NotBlank @Size(max = 180) String cityRegion,
        @NotBlank @Size(max = 40) String whatsapp,
        @NotBlank @Size(max = 2000) String services,
        @NotBlank @Size(max = 120) String visualStyle,
        @Size(max = 180) String preferredColors,
        @NotBlank @Size(max = 180) String weeklyGoal,
        @Size(max = 2000) String notes) {}
