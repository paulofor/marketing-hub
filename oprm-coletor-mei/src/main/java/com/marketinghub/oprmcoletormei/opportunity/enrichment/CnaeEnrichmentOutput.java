package com.marketinghub.oprmcoletormei.opportunity.enrichment;

import com.marketinghub.oprmcoletormei.opportunity.dto.OprmCnaeEnrichmentRequestDto;

/** Saída da etapa concreta de enriquecimento com o payload oficial de publicação no backend. */
public record CnaeEnrichmentOutput(String cnaeCode, OprmCnaeEnrichmentRequestDto enrichmentRequest) {}
