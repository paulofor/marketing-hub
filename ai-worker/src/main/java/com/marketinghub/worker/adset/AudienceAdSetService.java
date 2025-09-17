package com.marketinghub.worker.adset;

import com.marketinghub.audience.Audience;
import com.marketinghub.audience.repository.AudienceRepository;
import com.marketinghub.experiment.AdSet;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.dto.CreateAdSetRequest;
import com.marketinghub.experiment.repository.AdSetRepository;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.experiment.service.AdSetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service responsible for creating ad set records from approved audiences.
 */
@Service
public class AudienceAdSetService {
    private static final Logger log = LoggerFactory.getLogger(AudienceAdSetService.class);
    private final ExperimentRepository experimentRepository;
    private final AudienceRepository audienceRepository;
    private final AdSetRepository adSetRepository;
    private final AdSetService adSetService;
    private final AudienceAdSetChatGptClient chatGptClient;

    public AudienceAdSetService(ExperimentRepository experimentRepository,
                                AudienceRepository audienceRepository,
                                AdSetRepository adSetRepository,
                                AdSetService adSetService,
                                AudienceAdSetChatGptClient chatGptClient) {
        this.experimentRepository = experimentRepository;
        this.audienceRepository = audienceRepository;
        this.adSetRepository = adSetRepository;
        this.adSetService = adSetService;
        this.chatGptClient = chatGptClient;
    }

    /**
     * Generates ad sets for experiments whose audiences were approved by the user.
     *
     * @return map keyed by experiment id listing the persisted ad sets
     */
    public Map<Long, List<AdSet>> generate() {
        Map<Long, List<AdSet>> result = new HashMap<>();
        List<Experiment> experiments = experimentRepository.findAllReadyForAdSets(
                ExperimentPlatform.FACEBOOK,
                List.of(ExperimentStatus.PLANNED, ExperimentStatus.RUNNING, ExperimentStatus.PAUSED)
        );
        for (Experiment experiment : experiments) {
            long existing = adSetRepository.countByExperimentId(experiment.getId());
            if (existing > 0) {
                log.info("Skipping experiment {} because ad sets already exist", experiment.getId());
                result.put(experiment.getId(), Collections.emptyList());
                continue;
            }
            List<Audience> audiences = audienceRepository.findDetailedByNicheId(experiment.getNiche().getId());
            List<Audience> relevantAudiences = filterAudiencesForExperiment(audiences, experiment);
            if (relevantAudiences.isEmpty()) {
                log.warn("No audiences found for experiment {} to generate ad sets", experiment.getId());
                result.put(experiment.getId(), Collections.emptyList());
                continue;
            }
            List<AdSet> saved = new ArrayList<>();
            for (Audience audience : relevantAudiences) {
                try {
                    AdSetPlan plan = chatGptClient.planAdSet(experiment, audience);
                    CreateAdSetRequest request = new CreateAdSetRequest();
                    request.setExperimentId(experiment.getId());
                    request.setLocation(plan.location());
                    request.setInterests(join(plan.interests()));
                    request.setLookalikes(join(plan.lookalikes()));
                    request.setTargetingJson(plan.targetingJson());
                    request.setBudget(plan.budget());
                    request.setDurationDays(plan.durationDays());
                    request.setPrompt(plan.prompt());
                    request.setModel(plan.model());
                    AdSet adSet = adSetService.create(request);
                    saved.add(adSet);
                } catch (Exception e) {
                    log.error("Failed to create ad set for experiment {} audience {}", experiment.getId(), audience.getId(), e);
                }
            }
            result.put(experiment.getId(), saved);
        }
        return result;
    }

    private static List<Audience> filterAudiencesForExperiment(List<Audience> audiences, Experiment experiment) {
        if (audiences.isEmpty()) {
            return List.of();
        }
        UUID hypothesisId = experiment.getHypothesisRef() != null ? experiment.getHypothesisRef().getId() : null;
        List<Audience> filtered = new ArrayList<>();
        for (Audience audience : audiences) {
            if (audience.getHypothesis() == null) {
                filtered.add(audience);
            } else if (hypothesisId != null && hypothesisId.equals(audience.getHypothesis().getId())) {
                filtered.add(audience);
            }
        }
        return filtered;
    }

    private static String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return String.join("\n", values);
    }
}
