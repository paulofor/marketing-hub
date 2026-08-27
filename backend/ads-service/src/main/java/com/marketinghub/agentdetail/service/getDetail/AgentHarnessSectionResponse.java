package com.marketinghub.agentdetail.service.getDetail;

import java.util.List;

/** Responsabilidade: agrupar os componentes relacionados de uma parte do harness do agente. */
public record AgentHarnessSectionResponse(
    String code, String title, String description, List<AgentHarnessItemResponse> items) {}
