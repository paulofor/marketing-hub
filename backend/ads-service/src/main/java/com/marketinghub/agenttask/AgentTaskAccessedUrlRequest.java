package com.marketinghub.agenttask;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Responsabilidade: receber uma URL realmente acessada durante a execução de uma tarefa. */
public record AgentTaskAccessedUrlRequest(
    @NotBlank @Size(max = 2048) String url,
    @NotBlank @Size(max = 200) String label,
    @NotBlank @Size(max = 32) String accessMethod,
    Instant accessedAt) {}
