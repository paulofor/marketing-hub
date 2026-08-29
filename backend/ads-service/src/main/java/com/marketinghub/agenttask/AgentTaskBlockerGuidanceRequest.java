package com.marketinghub.agenttask;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Responsabilidade: receber a orientação acionável produzida para uma tarefa bloqueada. */
public record AgentTaskBlockerGuidanceRequest(
    @NotBlank @Size(max = 40) String category,
    @NotBlank @Size(max = 8000) String recommendedAction,
    @NotEmpty @Size(max = 10) List<@Valid AgentTaskHelpLinkRequest> helpLinks) {}
