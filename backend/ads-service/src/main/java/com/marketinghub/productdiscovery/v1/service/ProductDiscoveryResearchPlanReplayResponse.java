package com.marketinghub.productdiscovery.v1.service;

import com.fasterxml.jackson.databind.JsonNode;

/** Transporta o plano original da mesma pesquisa para coleta sem nova inferência. */
public record ProductDiscoveryResearchPlanReplayResponse(
    Long cycleId, JsonNode plan, String rawResponse, String model) {}
