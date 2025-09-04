package com.marketinghub.worker.creative;

import com.marketinghub.creative.Creative;
import com.marketinghub.creative.dto.CreateCreativeRequest;
import com.marketinghub.creative.service.CreativeService;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service that loops through experiments with {@code creativesToGenerate > 0}
 * and asks ChatGPT to generate creatives for each one.
 */
@Service
public class ExperimentCreativeService {
    private final ExperimentRepository experimentRepository;
    private final CreativeChatGptClient chatGptClient;
    private final CreativeImageClient imageClient;
    private final CreativeService creativeService;
    private static final Logger log = LoggerFactory.getLogger(ExperimentCreativeService.class);

    public ExperimentCreativeService(ExperimentRepository experimentRepository,
                                     CreativeChatGptClient chatGptClient,
                                     CreativeImageClient imageClient,
                                     CreativeService creativeService) {
        this.experimentRepository = experimentRepository;
        this.chatGptClient = chatGptClient;
        this.imageClient = imageClient;
        this.creativeService = creativeService;
    }

    /**
     * Generates creatives for all configured experiments.
     *
     * @return map keyed by experiment id containing the generated creatives
     */
    @Transactional
    public Map<Long, List<Creative>> generate() {
        Map<Long, List<Creative>> result = new HashMap<>();
        Iterable<Experiment> experiments = experimentRepository.findAllToGenerateCreatives();
        for (Experiment exp : experiments) {
            Integer qty = exp.getCreativesToGenerate();
            log.info("Generating {} creatives for experiment {}", qty, exp.getId());
            List<CreateCreativeRequest> requests = chatGptClient.generateCreatives(exp, qty);
            log.info("ChatGPT returned {} creatives for experiment {}", requests.size(), exp.getId());
            List<Creative> saved = new ArrayList<>();
            for (CreateCreativeRequest req : requests) {
                if (req.getHeadline() == null || req.getHeadline().isBlank()) {
                    log.error("Skipping creative without headline for experiment {}: {}", exp.getId(), req);
                    continue;
                }
                try {
                    String imageUrl = imageClient.generateImage(req.getHeadline());
                    req.setImageUrl(imageUrl);
                } catch (Exception e) {
                    log.error("Failed to generate image for experiment {}: {}", exp.getId(), req.getHeadline(), e);
                }
                log.info("Saving creative for experiment {}: {}", exp.getId(), req);
                saved.add(creativeService.create(exp.getId(), req));
            }
            log.info("Resetting creativesToGenerate for experiment {} to 0", exp.getId());
            exp.setCreativesToGenerate(0);
            experimentRepository.save(exp);
            result.put(exp.getId(), saved);
        }
        return result;
    }
}
