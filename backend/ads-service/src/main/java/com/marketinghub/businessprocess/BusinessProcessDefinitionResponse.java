package com.marketinghub.businessprocess;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/** Contrato de leitura de uma versão do catálogo de processos. */
public record BusinessProcessDefinitionResponse(
    Long id,
    String processCode,
    String name,
    String purpose,
    String ownerName,
    String triggerDescription,
    String outcomeDescription,
    Integer versionNumber,
    String status,
    String technicalReference,
    JsonNode diagram,
    Instant createdAt,
    Instant publishedAt) {}
