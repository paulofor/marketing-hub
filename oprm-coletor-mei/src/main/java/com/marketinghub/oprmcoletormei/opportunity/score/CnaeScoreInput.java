package com.marketinghub.oprmcoletormei.opportunity.score;

import com.marketinghub.oprmcoletormei.opportunity.dto.OprmCnaeOpportunityCandidateDto;

/** Entrada da etapa concreta que calcula score de oportunidade para um CNAE. */
public record CnaeScoreInput(OprmCnaeOpportunityCandidateDto candidate) {}
