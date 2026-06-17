package com.marketinghub.oprmcoletormei.opportunity.enrichment;

import com.marketinghub.oprmcoletormei.opportunity.dto.OprmCnaeOpportunityScoreResponseDto;

/** Entrada da etapa concreta que transforma score CNAE em sinais e candidatos de nicho. */
public record CnaeEnrichmentInput(OprmCnaeOpportunityScoreResponseDto score) {}
