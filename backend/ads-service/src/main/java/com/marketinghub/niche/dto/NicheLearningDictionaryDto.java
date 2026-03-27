package com.marketinghub.niche.dto;

import java.time.Instant;
import java.util.List;
import lombok.Data;

/**
 * Estrutura agrupando as melhores leituras por categoria do framework.
 */
@Data
public class NicheLearningDictionaryDto {
    private Instant updatedAt;
    private List<LearningStatementDto> pains;
    private List<LearningStatementDto> results;
    private List<LearningStatementDto> mechanisms;
    private List<LearningStatementDto> proofs;
    private List<LearningStatementDto> offers;
}
