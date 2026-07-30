package com.marketinghub.pde.service.publishslotcontract;

import jakarta.validation.constraints.Size;

/** Dados enviados pela tela para publicar o contrato comercial de um slot PDE. */
public record PublishPdeProductionSlotContractRequest(
    String experienceJson, @Size(max = 191) String publishedBy) {}
