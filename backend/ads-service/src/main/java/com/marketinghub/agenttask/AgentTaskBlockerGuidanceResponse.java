package com.marketinghub.agenttask;

import java.util.List;

/** Responsabilidade: apresentar a categoria, a ação e os links de uma tarefa bloqueada. */
public record AgentTaskBlockerGuidanceResponse(
    String category, String recommendedAction, List<AgentTaskAuditLinkResponse> helpLinks) {}
