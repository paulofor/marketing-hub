package com.marketinghub.worker.creative;

import com.marketinghub.creative.Creative;
import com.marketinghub.creative.dto.CreateCreativeRequest;
import com.marketinghub.creative.service.CreativeService;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.hypothesis.Hypothesis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
                CreativeChatGptClient.Generation generation = chatGptClient.generateCreatives(exp, qty);
                List<CreateCreativeRequest> requests = generation.creatives();
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
                        String imagePrompt = buildImagePrompt(exp, req);
                        String imageUrl = imageClient.generateImage(imagePrompt);
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

    private static String buildImagePrompt(Experiment experiment, CreateCreativeRequest request) {
        if (experiment != null && hasText(experiment.getCreativeImagePrompt())) {
            return applyImagePromptTemplate(experiment, request);
        }
        List<String> parts = new ArrayList<>();
        parts.add("Crie uma imagem envolvente para um anúncio de Facebook e Instagram que desperte interesse e desejo.");
        if (hasText(request.getHeadline())) {
            parts.add("A imagem deve acompanhar a headline \"" + request.getHeadline() + "\".");
        }
        if (experiment != null) {
            if (hasText(experiment.getName())) {
                parts.add("Experimento: " + experiment.getName() + ".");
            } else if (experiment.getId() != null) {
                parts.add("Experimento ID: " + experiment.getId() + ".");
            }
            if (hasText(experiment.getHypothesis())) {
                parts.add("Resumo do experimento: " + experiment.getHypothesis() + ".");
            }
        }
        Hypothesis hypothesis = experiment != null ? experiment.getHypothesisRef() : null;
        if (hypothesis != null) {
            if (hasText(hypothesis.getTitle())) {
                parts.add("Baseie-se na hipótese \"" + hypothesis.getTitle() + "\".");
            } else if (hypothesis.getId() != null) {
                parts.add("Baseie-se na hipótese de ID " + hypothesis.getId() + ".");
            }
            if (hasText(hypothesis.getPersona())) {
                parts.add("Persona: " + hypothesis.getPersona() + ".");
            }
            if (hasText(hypothesis.getProblem())) {
                parts.add("Problema: " + hypothesis.getProblem() + ".");
            }
            if (hasText(hypothesis.getPromise())) {
                parts.add("Promessa: " + hypothesis.getPromise() + ".");
            }
            if (hasText(hypothesis.getMechanism())) {
                parts.add("Mecanismo: " + hypothesis.getMechanism() + ".");
            }
            if (hasText(hypothesis.getUniqueMechanism())) {
                parts.add("Mecanismo único: " + hypothesis.getUniqueMechanism() + ".");
            }
            if (hasText(hypothesis.getEntrega())) {
                parts.add("Entrega: " + hypothesis.getEntrega() + ".");
            }
        }
        return String.join(" ", parts);
    }

    private static String applyImagePromptTemplate(Experiment experiment, CreateCreativeRequest request) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("headline", safe(request.getHeadline()));
        placeholders.put("primaryText", safe(request.getPrimaryText()));
        placeholders.put("experimentName", experiment != null ? safe(experiment.getName()) : "");
        placeholders.put("experimentId", experiment != null && experiment.getId() != null ? String.valueOf(experiment.getId()) : "");
        Hypothesis hypothesis = experiment != null ? experiment.getHypothesisRef() : null;
        placeholders.put("hypothesisId", hypothesis != null && hypothesis.getId() != null ? hypothesis.getId().toString() : "");
        placeholders.put("hypothesisTitle", hypothesis != null ? safe(hypothesis.getTitle()) : "");
        placeholders.put("persona", hypothesis != null ? safe(hypothesis.getPersona()) : "");
        placeholders.put("problem", hypothesis != null ? safe(hypothesis.getProblem()) : "");
        placeholders.put("promise", hypothesis != null ? safe(hypothesis.getPromise()) : "");
        placeholders.put("mechanism", hypothesis != null ? safe(hypothesis.getMechanism()) : "");
        placeholders.put("uniqueMechanism", hypothesis != null ? safe(hypothesis.getUniqueMechanism()) : "");
        placeholders.put("entrega", hypothesis != null ? safe(hypothesis.getEntrega()) : "");
        return replacePlaceholders(experiment.getCreativeImagePrompt(), placeholders);
    }

    private static String replacePlaceholders(String template, Map<String, String> placeholders) {
        if (template == null) {
            return null;
        }
        String result = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String token = "{{" + entry.getKey() + "}}";
            result = result.replace(token, entry.getValue());
        }
        return result;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
