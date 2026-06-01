package com.marketinghub.facebookads.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.mapper.ExperimentMapper;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.facebookads.dto.ExperimentReadyForAdSetDto;
import com.marketinghub.facebookads.dto.TargetingPackageDto;
import com.marketinghub.hypothesis.mapper.HypothesisMapper;
import com.marketinghub.niche.mapper.MarketNicheMapper;
import com.marketinghub.targeting.TargetingElement;
import com.marketinghub.targeting.TargetingElementType;
import com.marketinghub.targeting.dto.TargetingElementDto;
import com.marketinghub.targeting.mapper.TargetingElementMapper;
import com.marketinghub.repository.jpa.targeting.TargetingElementRepository;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
    private final TargetingElementRepository targetingElementRepository;
    private final ExperimentMapper experimentMapper;
    private final MarketNicheMapper marketNicheMapper;
    private final HypothesisMapper hypothesisMapper;
    private final TargetingElementMapper targetingElementMapper;

    public FacebookAdSetExperimentService(ExperimentRepository experimentRepository,
                                          TargetingElementRepository targetingElementRepository,
                                          ExperimentMapper experimentMapper,
                                          MarketNicheMapper marketNicheMapper,
                                          HypothesisMapper hypothesisMapper,
                                          TargetingElementMapper targetingElementMapper) {
        this.experimentRepository = experimentRepository;
        this.targetingElementRepository = targetingElementRepository;
        this.experimentMapper = experimentMapper;
        this.marketNicheMapper = marketNicheMapper;
        this.hypothesisMapper = hypothesisMapper;
        this.targetingElementMapper = targetingElementMapper;
    }

    /**
     * Lists experiments that are ready to have ad sets generated alongside their
     * approved targeting elements.
     */
    public List<ExperimentReadyForAdSetDto> listExperimentsReadyForAdSets() {
        List<Experiment> experiments = experimentRepository.findAllReadyForAdSets(
                ExperimentPlatform.FACEBOOK, STATUSES);
        if (experiments.isEmpty()) {
            return List.of();
        }
        List<ExperimentReadyForAdSetDto> result = new ArrayList<>();
        for (Experiment experiment : experiments) {
            TargetingPackageDto targeting = buildTargetingPackage(experiment);
            if (targeting == null) {
                continue;
            }
            ExperimentReadyForAdSetDto dto = new ExperimentReadyForAdSetDto(
                    experimentMapper.toDto(experiment),
                    marketNicheMapper.toDto(experiment.getNiche()),
                    experiment.getHypothesisRef() != null ? hypothesisMapper.toDto(experiment.getHypothesisRef()) : null,
                    targeting);
            result.add(dto);
        }
        return result;
    }

    private TargetingPackageDto buildTargetingPackage(Experiment experiment) {
        if (experiment.getNiche() == null) {
            return null;
        }
        Long nicheId = experiment.getNiche().getId();
        UUID hypothesisId = experiment.getHypothesisRef() != null ? experiment.getHypothesisRef().getId() : null;
        Map<TargetingElementType, List<TargetingElementDto>> mapped = new EnumMap<>(TargetingElementType.class);
        for (TargetingElementType type : TargetingElementType.values()) {
            List<TargetingElementDto> dtos = mapElements(nicheId, hypothesisId, type);
            if (type == TargetingElementType.JOB_TITLE && dtos.isEmpty()) {
                return null;
            }
            mapped.put(type, dtos);
        }
        return new TargetingPackageDto(
                mapped.get(TargetingElementType.INTEREST),
                mapped.get(TargetingElementType.JOB_TITLE),
                mapped.get(TargetingElementType.BEHAVIOR));
    }

    private List<TargetingElementDto> mapElements(Long nicheId, UUID hypothesisId, TargetingElementType type) {
        List<TargetingElement> elements = targetingElementRepository.findApprovedForExperiment(nicheId, type, hypothesisId);
        if (elements.isEmpty()) {
            return List.of();
        }
        return elements.stream().map(targetingElementMapper::toDto).toList();
    }
}
