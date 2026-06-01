package com.marketinghub.worker.deliverable;

import com.marketinghub.deliverable.Deliverable;
import com.marketinghub.deliverable.dto.CreateDeliverableRequest;
import com.marketinghub.deliverable.service.DeliverableService;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.worker.experiment.ExperimentGenerationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service that generates deliverable definitions for experiments requesting them.
 */
@Service
public class ExperimentDeliverableService {
    private static final Logger log = LoggerFactory.getLogger(ExperimentDeliverableService.class);

    private final ExperimentGenerationRepository generationRepository;
    private final DeliverableChatGptClient chatGptClient;
    private final DeliverableService deliverableService;
    private final ExperimentRepository experimentRepository;

    public ExperimentDeliverableService(ExperimentGenerationRepository generationRepository,
                                        DeliverableChatGptClient chatGptClient,
                                        DeliverableService deliverableService,
                                        ExperimentRepository experimentRepository) {
        this.generationRepository = generationRepository;
        this.chatGptClient = chatGptClient;
        this.deliverableService = deliverableService;
        this.experimentRepository = experimentRepository;
    }

    @Transactional
    public Map<Long, List<Deliverable>> generate() {
        Map<Long, List<Deliverable>> result = new LinkedHashMap<>();
        List<Experiment> experiments = generationRepository.findAllToGenerateDeliverables();
        for (Experiment experiment : experiments) {
            Integer quantity = experiment.getDeliverablesToGenerate();
            if (quantity == null || quantity <= 0) {
                log.debug("Skipping experiment {} without deliverables requested", experiment.getId());
                continue;
            }
            MarketNiche niche = experiment.getNiche();
            if (niche == null || niche.getId() == null) {
                log.warn("Experiment {} has no niche associated; deliverables will not be generated", experiment.getId());
                continue;
            }
            log.info("Generating {} deliverables for experiment {}", quantity, experiment.getId());
            try {
                List<CreateDeliverableRequest> requests = chatGptClient.generateDeliverables(experiment, quantity);
                List<Deliverable> saved = new ArrayList<>();
                int processed = 0;
                for (CreateDeliverableRequest request : requests) {
                    if (processed >= quantity) {
                        break;
                    }
                    if (!StringUtils.hasText(request.getTitle())) {
                        log.error("Skipping deliverable without title for experiment {}: {}", experiment.getId(), request);
                        continue;
                    }
                    request.setMarketNicheId(niche.getId());
                    try {
                        Deliverable deliverable = deliverableService.create(request);
                        saved.add(deliverable);
                        processed++;
                    } catch (Exception ex) {
                        log.error("Failed to persist deliverable for experiment {}", experiment.getId(), ex);
                    }
                }
                experiment.setDeliverablesToGenerate(0);
                experimentRepository.save(experiment);
                result.put(experiment.getId(), saved);
                log.info("Finished experiment {} with {} deliverables persisted", experiment.getId(), saved.size());
            } catch (Exception ex) {
                log.error("Failed to generate deliverables for experiment {}", experiment.getId(), ex);
            }
        }
        return result;
    }
}
