package com.marketinghub.facebookads.service;

import com.marketinghub.audience.Audience;
import com.marketinghub.audience.TargetingStatus;
import com.marketinghub.audience.dto.AudienceDto;
import com.marketinghub.audience.mapper.AudienceMapper;
import com.marketinghub.audience.repository.AudienceRepository;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.mapper.ExperimentMapper;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.facebookads.dto.ExperimentReadyForAdSetDto;
import com.marketinghub.hypothesis.mapper.HypothesisMapper;
import com.marketinghub.niche.mapper.MarketNicheMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Builds the payload consumed by workers to generate Facebook ad sets.
 */
@Service
public class FacebookAdSetExperimentService {
    private static final List<ExperimentStatus> STATUSES = List.of(
            ExperimentStatus.PLANNED, ExperimentStatus.RUNNING, ExperimentStatus.PAUSED);

    private final ExperimentRepository experimentRepository;
    private final AudienceRepository audienceRepository;
    private final ExperimentMapper experimentMapper;
    private final MarketNicheMapper marketNicheMapper;
    private final HypothesisMapper hypothesisMapper;
    private final AudienceMapper audienceMapper;

    public FacebookAdSetExperimentService(ExperimentRepository experimentRepository,
                                          AudienceRepository audienceRepository,
                                          ExperimentMapper experimentMapper,
                                          MarketNicheMapper marketNicheMapper,
                                          HypothesisMapper hypothesisMapper,
                                          AudienceMapper audienceMapper) {
        this.experimentRepository = experimentRepository;
        this.audienceRepository = audienceRepository;
        this.experimentMapper = experimentMapper;
        this.marketNicheMapper = marketNicheMapper;
        this.hypothesisMapper = hypothesisMapper;
        this.audienceMapper = audienceMapper;
    }

    /**
     * Lists experiments that are ready to have ad sets generated alongside their
     * approved audiences.
     */
    public List<ExperimentReadyForAdSetDto> listExperimentsReadyForAdSets() {
        List<Experiment> experiments = experimentRepository.findAllReadyForAdSets(
                ExperimentPlatform.FACEBOOK, STATUSES);
        if (experiments.isEmpty()) {
            return List.of();
        }
        List<ExperimentReadyForAdSetDto> result = new ArrayList<>();
        for (Experiment experiment : experiments) {
            List<AudienceDto> audiences = mapAudiencesForExperiment(experiment);
            if (audiences.isEmpty()) {
                continue;
            }
            ExperimentReadyForAdSetDto dto = new ExperimentReadyForAdSetDto(
                    experimentMapper.toDto(experiment),
                    marketNicheMapper.toDto(experiment.getNiche()),
                    experiment.getHypothesisRef() != null ? hypothesisMapper.toDto(experiment.getHypothesisRef()) : null,
                    audiences);
            result.add(dto);
        }
        return result;
    }

    private List<AudienceDto> mapAudiencesForExperiment(Experiment experiment) {
        if (experiment.getNiche() == null) {
            return List.of();
        }
        List<Audience> audiences = audienceRepository.findDetailedByNicheId(experiment.getNiche().getId());
        if (audiences.isEmpty()) {
            return List.of();
        }
        List<Audience> filtered = filterAudiencesForExperiment(audiences, experiment);
        if (filtered.isEmpty()) {
            return List.of();
        }
        return filtered.stream().map(audienceMapper::toDto).toList();
    }

    private static List<Audience> filterAudiencesForExperiment(List<Audience> audiences, Experiment experiment) {
        if (audiences.isEmpty()) {
            return List.of();
        }
        UUID hypothesisId = experiment.getHypothesisRef() != null ? experiment.getHypothesisRef().getId() : null;
        List<Audience> filtered = new ArrayList<>();
        for (Audience audience : audiences) {
            if (!audience.isApproved()) {
                continue;
            }
            if (audience.getTargetingStatus() != TargetingStatus.READY) {
                continue;
            }
            if (audience.getTargetingSpec() == null || audience.getTargetingSpec().isBlank()) {
                continue;
            }
            if (audience.getHypothesis() == null) {
                filtered.add(audience);
            } else if (hypothesisId != null && hypothesisId.equals(audience.getHypothesis().getId())) {
                filtered.add(audience);
            }
        }
        return filtered.isEmpty() ? List.of() : List.copyOf(filtered);
    }
}
