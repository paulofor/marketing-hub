package com.marketinghub.worker.adset;

import com.marketinghub.audience.Audience;
import com.marketinghub.experiment.dto.AdSetDto;
import com.marketinghub.experiment.dto.CreateAdSetRequest;
import com.marketinghub.worker.adset.BackendExperimentClient.BackendClientException;
import com.marketinghub.worker.adset.BackendExperimentClient.ReadyExperiment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service responsible for creating ad set records from approved audiences.
 */
@Service
public class AudienceAdSetService {
    private static final Logger log = LoggerFactory.getLogger(AudienceAdSetService.class);
    private final BackendExperimentClient backendClient;
    private final AudienceAdSetChatGptClient chatGptClient;

    public AudienceAdSetService(BackendExperimentClient backendClient,
                                AudienceAdSetChatGptClient chatGptClient) {
        this.backendClient = backendClient;
        this.chatGptClient = chatGptClient;
    }

    /**
     * Generates ad sets for experiments whose audiences were approved by the user.
     *
     * @return map keyed by experiment id listing the persisted ad sets
     */
    public Map<Long, List<AdSetDto>> generate() {
        Map<Long, List<AdSetDto>> result = new HashMap<>();
        List<ReadyExperiment> experiments;
        try {
            experiments = backendClient.listExperimentsReadyForAdSets();
        } catch (BackendClientException e) {
            log.error("Failed to fetch experiments ready for ad sets: {}", e.getMessage());
            return Map.of();
        }
        for (ReadyExperiment ready : experiments) {
            var experiment = ready.experiment();
            if (experiment == null || experiment.getId() == null) {
                log.warn("Skipping experiment without identifier in backend payload");
                continue;
            }
            Long experimentId = experiment.getId();
            boolean hasExisting;
            try {
                hasExisting = backendClient.hasAdSets(experimentId);
            } catch (BackendClientException e) {
                log.error("Failed to check existing ad sets for experiment {}: {}", experimentId, e.getMessage());
                result.put(experimentId, List.of());
                continue;
            }
            if (hasExisting) {
                log.info("Skipping experiment {} because ad sets already exist", experimentId);
                result.put(experimentId, List.of());
                continue;
            }
            List<Audience> audiences = ready.audiences();
            if (audiences == null || audiences.isEmpty()) {
                log.warn("No audiences found for experiment {} to generate ad sets", experimentId);
                result.put(experimentId, List.of());
                continue;
            }
            List<AdSetDto> saved = new ArrayList<>();
            for (Audience audience : audiences) {
                if (audience == null) {
                    log.warn("Skipping null audience entry for experiment {}", experimentId);
                    continue;
                }
                Long audienceId = audience.getId();
                if (audienceId == null) {
                    log.warn("Skipping audience without identifier for experiment {}", experimentId);
                    continue;
                }
                try {
                    AdSetPlan plan = chatGptClient.planAdSet(experiment, audience);
                    CreateAdSetRequest request = new CreateAdSetRequest();
                    request.setExperimentId(experimentId);
                    request.setLocation(plan.location());
                    request.setInterests(join(plan.interests()));
                    request.setLookalikes(join(plan.lookalikes()));
                    request.setTargetingJson(plan.targetingJson());
                    request.setBudget(plan.budget());
                    request.setDurationDays(plan.durationDays());
                    request.setPrompt(plan.prompt());
                    request.setModel(plan.model());
                    AdSetDto adSet = backendClient.createAdSet(request);
                    if (adSet != null) {
                        saved.add(adSet);
                    } else {
                        log.warn("Backend returned null ad set for experiment {} audience {}", experimentId, audienceId);
                    }
                } catch (BackendClientException e) {
                    log.error("Backend rejected ad set for experiment {} audience {}: {}", experimentId, audienceId, e.getMessage());
                } catch (Exception e) {
                    log.error("Failed to create ad set for experiment {} audience {}", experimentId, audienceId, e);
                }
            }
            result.put(experimentId, saved);
        }
        return result;
    }

    private static String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return String.join("\n", values);
    }
}
