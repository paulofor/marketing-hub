package com.marketinghub.oprmcoletormei.opportunity.score;

import com.marketinghub.oprmcoletormei.opportunity.dto.OprmCnaeOpportunityScoreRequestDto;

/** Saída da etapa concreta de score com o payload oficial de gravação no backend. */
public record CnaeScoreOutput(String cnaeCode, OprmCnaeOpportunityScoreRequestDto scoreRequest) {}
