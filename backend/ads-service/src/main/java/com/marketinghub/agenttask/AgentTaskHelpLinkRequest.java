package com.marketinghub.agenttask;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Responsabilidade: receber um link seguro que ajude a corrigir ou retomar uma tarefa bloqueada.
 */
public record AgentTaskHelpLinkRequest(
    @NotBlank @Size(max = 200) String label, @NotBlank @Size(max = 2048) String url) {}
