package com.marketinghub.facebookads.dto;

import com.marketinghub.audience.dto.AudienceDto;
import com.marketinghub.experiment.dto.ExperimentDto;
import com.marketinghub.hypothesis.dto.HypothesisDto;
import com.marketinghub.niche.dto.MarketNicheDto;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Aggregates experiment context required to generate Facebook ad sets.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentReadyForAdSetDto {
    private ExperimentDto experiment;
    private MarketNicheDto niche;
    private HypothesisDto hypothesis;
    private List<AudienceDto> audiences;
}

