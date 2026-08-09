package com.marketinghub.agentmemory.service.registerMemory;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

/** Contrato para registrar uma hipótese de aprendizado sem promovê-la a verdade. */
public record RegisterMemoryRequest(
    @Size(max = 120) String tenantKey,
    @NotBlank @Size(max = 60) String scopeType,
    @NotBlank @Size(max = 120) String scopeId,
    @NotBlank @Size(max = 120) String specialty,
    @NotBlank @Size(max = 4000) String content,
    @NotBlank @Size(max = 4000) String evidence,
    @Size(max = 700) String sourceReference,
    @NotBlank @Size(max = 120) String sourceExecutionId,
    @NotNull @DecimalMin("0.0000") @DecimalMax("1.0000") BigDecimal confidence,
    Instant validUntil) {}
