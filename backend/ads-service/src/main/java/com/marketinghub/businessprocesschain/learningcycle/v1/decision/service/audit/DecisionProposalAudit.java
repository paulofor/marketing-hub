package com.marketinghub.businessprocesschain.learningcycle.v1.decision.service.audit;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;

/** Responsabilidade: transportar o request auditável da proposta comercial assistida. */
public record DecisionProposalAudit(
    @NotBlank @Size(max = 36) String leaseToken,
    @NotBlank @Size(max = 200000) String prompt,
    @NotNull JsonNode schema,
    @NotBlank @Size(max = 120) String model,
    @NotBlank @Size(max = 30) String serviceTier,
    @Size(max = 1200) String serviceTierReason) {}
