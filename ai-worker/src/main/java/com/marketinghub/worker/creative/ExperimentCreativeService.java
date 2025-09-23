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
    private static final int HEADLINE_MAX = 40;
    private static final int PRIMARY_TEXT_MAX = 125;
    private static final int MAX_HASHTAGS = 30;

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
            if (qty == null || qty <= 0) {
                log.info("Skipping experiment {} because creativesToGenerate is {}", exp.getId(), qty);
                continue;
            }
            log.info("Generating {} creatives for experiment {}", qty, exp.getId());
            try {
                List<CreateCreativeRequest> requests = chatGptClient.generateCreatives(exp, qty);
                log.info("ChatGPT returned {} creatives for experiment {}", requests.size(), exp.getId());
                List<Creative> saved = new ArrayList<>();
                for (CreateCreativeRequest req : requests) {
                    if (req.getHeadline() == null || req.getHeadline().isBlank()) {
                        log.error("Skipping creative without headline for experiment {}: {}", exp.getId(), req);
                        continue;
                    }
                    req.setHeadline(truncate(req.getHeadline(), HEADLINE_MAX));
                    String primary = limitHashtags(req.getPrimaryText(), MAX_HASHTAGS);
                    if (primary != null && !primary.contains("#")) {
                        primary = truncate(primary, PRIMARY_TEXT_MAX);
                    }
                    req.setPrimaryText(primary);
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
                log.info("Finished experiment {} with {} creatives persisted", exp.getId(), saved.size());
            } catch (Exception e) {
                log.error("Failed to generate creatives for experiment {}", exp.getId(), e);
            }
        }
        return result;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() > max ? value.substring(0, max) : value;
    }

    private static String limitHashtags(String text, int maxHashtags) {
        if (text == null) {
            return null;
        }
        String[] parts = text.split("\\s+");
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String part : parts) {
            if (part.startsWith("#")) {
                count++;
                if (count > maxHashtags) {
                    continue;
                }
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(part);
        }
        return sb.toString();
    }
}
