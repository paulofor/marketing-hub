package com.marketinghub.worker.creative;

import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.creative.dto.CreateCreativeRequest;
import com.marketinghub.creative.service.CreativeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.worker.creative.pipeline.ExperimentPipelineAdExtractor;
import com.marketinghub.worker.creative.pipeline.PipelineAdCreativePlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Method;

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
    private final ObjectMapper objectMapper;
    private static final Logger log = LoggerFactory.getLogger(ExperimentCreativeService.class);
    private static final int HEADLINE_MAX = 40;
    private static final int PRIMARY_TEXT_MAX = 125;
    private static final int MAX_HASHTAGS = 30;

    public ExperimentCreativeService(ExperimentRepository experimentRepository,
                                     CreativeChatGptClient chatGptClient,
                                     CreativeImageClient imageClient,
                                     CreativeService creativeService,
                                     ObjectMapper objectMapper) {
        this.experimentRepository = experimentRepository;
        this.chatGptClient = chatGptClient;
        this.imageClient = imageClient;
        this.creativeService = creativeService;
        this.objectMapper = objectMapper;
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
            boolean pipelineMode = isPipelineAdsMode(exp);
            log.info("Generating {} creatives for experiment {} ({})", qty, exp.getId(), pipelineMode ? "pipeline" : "default");
            try {
                List<Creative> saved = pipelineMode
                        ? generateFromPipeline(exp, qty)
                        : generateWithChatGpt(exp, qty);
                exp.setCreativesToGenerate(0);
                if (pipelineMode) {
                    setCreativeGenerationMode(exp, "DEFAULT");
                }
                experimentRepository.save(exp);
                result.put(exp.getId(), saved);
                log.info("Finished experiment {} with {} creatives persisted", exp.getId(), saved.size());
            } catch (Exception e) {
                handleGenerationFailure(exp, pipelineMode, e);
            }
        }
        return result;
    }


    /**
     * Clears a failed creative generation request so the UI does not stay indefinitely blocked after logging the root cause.
     */
    private void handleGenerationFailure(Experiment experiment, boolean pipelineMode, Exception exception) {
        log.error("Failed to generate creatives for experiment {}. pendingCreatives={} generationMode={} rootCauseCategory={} rootCauseMessage={}",
                experiment.getId(), experiment.getCreativesToGenerate(), pipelineMode ? "PIPELINE_ADS" : "DEFAULT",
                classifyRootCause(exception), rootCauseMessage(exception), exception);
        experiment.setCreativesToGenerate(0);
        if (pipelineMode) {
            setCreativeGenerationMode(experiment, "DEFAULT");
        }
        experimentRepository.save(experiment);
        log.warn("Cleared failed creative generation request for experiment {} to avoid an indefinite UI lock; user can request generation again after fixing the root cause",
                experiment.getId());
    }

    /**
     * Generates default creatives and persists the batch only when every image URL is available.
     */
    private List<Creative> generateWithChatGpt(Experiment experiment, int quantity) {
        CreativeChatGptClient.Generation generation = chatGptClient.generateCreatives(experiment, quantity);
        List<CreateCreativeRequest> requests = generation.creatives();
        log.info("ChatGPT returned {} creatives for experiment {}", requests.size(), experiment.getId());
        List<CreateCreativeRequest> preparedRequests = new ArrayList<>();
        for (CreateCreativeRequest req : requests) {
            if (!StringUtils.hasText(req.getHeadline())) {
                log.error("Skipping creative without headline for experiment {}: {}", experiment.getId(), req);
                continue;
            }
            req.setHeadline(truncate(req.getHeadline(), HEADLINE_MAX));
            String primary = limitHashtags(req.getPrimaryText(), MAX_HASHTAGS);
            if (primary != null && !primary.contains("#")) {
                primary = truncate(primary, PRIMARY_TEXT_MAX);
            }
            req.setPrimaryText(primary);
            attachDefaultImage(experiment, req);
            preparedRequests.add(req);
        }
        return persistCreatives(experiment, preparedRequests, "creative");
    }

    /**
     * Generates pipeline creatives and persists the batch only when every image URL is available.
     */
    private List<Creative> generateFromPipeline(Experiment experiment, int quantity) {
        ExperimentPipelineAdExtractor extractor = new ExperimentPipelineAdExtractor(objectMapper);
        List<PipelineAdCreativePlan> plans = extractor.extract(experiment);
        if (plans.isEmpty()) {
            log.warn("No pipeline variants were found for experiment {}", experiment.getId());
            return List.of();
        }
        int limit = Math.min(quantity, plans.size());
        List<CreateCreativeRequest> requests = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            PipelineAdCreativePlan plan = plans.get(i);
            CreateCreativeRequest req = buildRequestFromPlan(experiment, plan);
            if (req == null) {
                continue;
            }
            attachPipelineImage(experiment, plan, req);
            requests.add(req);
        }
        if (requests.isEmpty()) {
            log.warn("Pipeline mode did not prepare creatives for experiment {}", experiment.getId());
            return List.of();
        }
        return persistCreatives(experiment, requests, "pipeline creative");
    }


    /**
     * Generates and attaches the default image URL, failing the batch when image generation is incomplete.
     */
    private void attachDefaultImage(Experiment experiment, CreateCreativeRequest request) {
        try {
            String imagePrompt = buildImagePrompt(experiment, request);
            String context = "experimentId=" + experiment.getId()
                    + ";mode=DEFAULT;headline=" + request.getHeadline();
            String imageUrl = imageClient.generateImage(imagePrompt, null, context);
            requireImageUrl(imageUrl, experiment.getId(), "headline " + request.getHeadline());
            request.setImageUrl(imageUrl);
        } catch (Exception ex) {
            throw new CreativeImageGenerationException("Image generation failed for experiment "
                    + experiment.getId() + " and headline " + request.getHeadline(), ex);
        }
    }

    /**
     * Generates and attaches the pipeline image URL, failing the batch when image generation is incomplete.
     */
    private void attachPipelineImage(Experiment experiment, PipelineAdCreativePlan plan, CreateCreativeRequest request) {
        try {
            String intermediatePrompt = buildPipelineIntermediatePrompt(experiment, plan, request);
            String imagePrompt = buildPipelineImagePrompt(experiment, plan, request);
            String context = "experimentId=" + experiment.getId()
                    + ";mode=PIPELINE_ADS;variant=" + plan.variantKey()
                    + ";format=" + request.getFormat();
            String imageUrl = imageClient.generateImage(imagePrompt, intermediatePrompt, context);
            requireImageUrl(imageUrl, experiment.getId(), "variant " + plan.variantKey());
            request.setImageUrl(imageUrl);
        } catch (Exception ex) {
            throw new CreativeImageGenerationException("Pipeline image generation failed for experiment "
                    + experiment.getId() + " and variant " + plan.variantKey(), ex);
        }
    }

    /**
     * Ensures the image generation returned a URL before any creative can be persisted.
     */
    private void requireImageUrl(String imageUrl, Long experimentId, String context) {
        if (!StringUtils.hasText(imageUrl)) {
            throw new CreativeImageGenerationException("Image generation returned no URL for experiment "
                    + experimentId + " and " + context);
        }
    }

    /**
     * Persists a fully generated batch only after all requests have valid image URLs.
     */
    private List<Creative> persistCreatives(Experiment experiment, List<CreateCreativeRequest> requests, String label) {
        List<Creative> saved = new ArrayList<>();
        for (CreateCreativeRequest request : requests) {
            log.info("Saving {} for experiment {}: {}", label, experiment.getId(), request);
            saved.add(creativeService.create(experiment.getId(), request));
        }
        return saved;
    }

    /**
     * Classifies the failing layer so logs can separate OpenAI errors from Marketing Hub upload/configuration errors.
     */
    private String classifyRootCause(Throwable throwable) {
        String message = rootCauseMessage(throwable).toLowerCase(Locale.ROOT);
        if (message.contains("openai api key")) {
            return "CONFIG_OPENAI_KEY";
        }
        if (message.contains("backend asset") || message.contains("asset url") || message.contains("upload")) {
            return "MARKETING_HUB_ASSET_UPLOAD";
        }
        if (message.contains("openai") || message.contains("no image returned") || message.contains("image generation returned no url")) {
            return "OPENAI_IMAGE_API";
        }
        return "UNKNOWN";
    }

    /**
     * Extracts the deepest exception message available for operational logs.
     */
    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != null) {
            current = current.getCause();
        }
        if (current == null || current.getMessage() == null) {
            return "";
        }
        return current.getMessage();
    }

    /**
     * Checks whether the experiment is currently queued for pipeline ad generation.
     */
    private boolean isPipelineAdsMode(Experiment experiment) {
        String mode = readCreativeGenerationMode(experiment);
        return "PIPELINE_ADS".equalsIgnoreCase(mode);
    }

    /**
     * Reads the creative generation mode through reflection for backend model compatibility.
     */
    private String readCreativeGenerationMode(Experiment experiment) {
        if (experiment == null) {
            return null;
        }
        try {
            Method getter = experiment.getClass().getMethod("getCreativeGenerationMode");
            Object modeValue = getter.invoke(experiment);
            return modeValue != null ? modeValue.toString() : null;
        } catch (Exception ex) {
            Long experimentId = experiment != null ? experiment.getId() : null;
            log.debug("Unable to read creative generation mode for experiment {}", experimentId, ex);
            return null;
        }
    }

    /**
     * Updates the creative generation mode through reflection for backend model compatibility.
     */
    private void setCreativeGenerationMode(Experiment experiment, String modeName) {
        if (experiment == null || !StringUtils.hasText(modeName)) {
            return;
        }
        try {
            Method getter = experiment.getClass().getMethod("getCreativeGenerationMode");
            Object current = getter.invoke(experiment);
            if (current == null) {
                return;
            }
            Class<?> enumType = current.getClass();
            if (!Enum.class.isAssignableFrom(enumType)) {
                return;
            }
            @SuppressWarnings("unchecked")
            Class<? extends Enum> typedEnum = (Class<? extends Enum>) enumType;
            Enum<?> enumValue = Enum.valueOf(typedEnum, modeName);
            Method setter = experiment.getClass().getMethod("setCreativeGenerationMode", enumType);
            setter.invoke(experiment, enumValue);
        } catch (Exception ex) {
            log.debug("Unable to set creative generation mode to {} for experiment {}", modeName,
                    experiment.getId(), ex);
        }
    }

    private CreateCreativeRequest buildRequestFromPlan(Experiment experiment, PipelineAdCreativePlan plan) {
        if (plan == null) {
            return null;
        }
        if (!StringUtils.hasText(plan.headline()) || !StringUtils.hasText(plan.primaryText())) {
            log.warn("Skipping pipeline variant {} because it is missing headline or primary text", plan.variantKey());
            return null;
        }
        CreateCreativeRequest req = new CreateCreativeRequest();
        req.setFormat(StringUtils.hasText(plan.format()) ? plan.format() : "LINK");
        req.setHeadline(truncate(plan.headline(), HEADLINE_MAX));
        String primary = limitHashtags(plan.primaryText(), MAX_HASHTAGS);
        if (primary != null && !primary.contains("#")) {
            primary = truncate(primary, PRIMARY_TEXT_MAX);
        }
        req.setPrimaryText(primary);
        req.setDescription(plan.description());
        req.setCta(resolveCallToAction(plan.ctaText()));
        req.setDestinationUrl(resolveDestinationUrl(experiment));
        req.setStatus(CreativeStatus.DRAFT);
        return req;
    }

    private String resolveDestinationUrl(Experiment experiment) {
        if (experiment == null) {
            return null;
        }
        return StringUtils.hasText(experiment.getFollowUpActionUrl())
                ? experiment.getFollowUpActionUrl().trim()
                : null;
    }

    private String resolveCallToAction(String suggestion) {
        if (!StringUtils.hasText(suggestion)) {
            return "LEARN_MORE";
        }
        String normalized = Normalizer.normalize(suggestion, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        if (normalized.contains("inscrev") || normalized.contains("cadastre") || normalized.contains("assine")) {
            return "SIGN_UP";
        }
        if (normalized.contains("baixe") || normalized.contains("download")) {
            return "DOWNLOAD";
        }
        if (normalized.contains("compre") || normalized.contains("comprar") || normalized.contains("loja")) {
            return "SHOP_NOW";
        }
        if (normalized.contains("teste") || normalized.contains("experimente") || normalized.contains("demon")) {
            return "TRY_DEMO";
        }
        if (normalized.contains("fale") || normalized.contains("contato") || normalized.contains("converse")) {
            return "CONTACT_US";
        }
        return "LEARN_MORE";
    }

    private String buildPipelineImagePrompt(Experiment experiment,
                                            PipelineAdCreativePlan plan,
                                            CreateCreativeRequest request) {
        List<String> parts = new ArrayList<>();
        parts.add("A imagem deve ter a persona representando o nicho, uma headline e uma subheadline esteticamente bem posicionada");
        parts.add("""
                A mensagem deve:
                - criar uma cena mental concreta
                - conectar a ideia a uma experiência já vivida
                - tocar identidade, desejo ou medo silencioso do leitor
                - incluir um gatilho cotidiano que faça a mensagem retornar à mente
                - ligar o presente a uma consequência futura

                Evite:
                - generalidades
                - clichês
                - exagero publicitário
                - abstração sem imagem
                - frases motivacionais genéricas
                """);
        parts.add("Você é um diretor de arte criando criativos originais para anúncios do Meta Ads.");
        String nicheName = resolveNicheName(experiment);
        parts.add("Essa imagem vai aparecer no feed de muitas pessoas e precisa deixar explícito que é um produto voltado para o nicho "
                + nicheName
                + ".");
        if ("STORY".equalsIgnoreCase(plan.format())) {
            parts.add("Formato vertical 1080x1920 (Stories/Reels) com foco no terço central e respeitando o CTA nativo.");
        } else {
            parts.add("Formato feed 1080x1350 com margens de 10% sem texto próximo às bordas.");
        }
        if (plan.imageBriefing() != null) {
            if (StringUtils.hasText(plan.imageBriefing().visualBriefing())) {
                parts.add("Briefing visual:\n\n " + plan.imageBriefing().visualBriefing());
            }
            if (StringUtils.hasText(plan.imageBriefing().hierarchy())) {
                parts.add("Hierarquia sugerida:\n\n " + plan.imageBriefing().hierarchy());
            }
            if (StringUtils.hasText(plan.imageBriefing().safeMargins())) {
                parts.add("Margens de segurança:\n\n " + plan.imageBriefing().safeMargins());
            }
            if (StringUtils.hasText(plan.imageBriefing().formatByPlacement())) {
                parts.add("Adaptação desejada:\n\n " + plan.imageBriefing().formatByPlacement());
            }
            if (StringUtils.hasText(plan.imageBriefing().messageMatchNotes())) {
                parts.add("Mensagem obrigatória:\n\n " + plan.imageBriefing().messageMatchNotes());
            }
            if (StringUtils.hasText(plan.imageBriefing().complianceNotes())) {
                parts.add("Notas de compliance:\n\n " + plan.imageBriefing().complianceNotes());
            }
            if (plan.imageBriefing().supportingKeywords() != null && !plan.imageBriefing().supportingKeywords().isEmpty()) {
                parts.add("Palavras-chave de apoio:\n\n " + String.join(", ", plan.imageBriefing().supportingKeywords()));
            }
            if (plan.imageBriefing().imageTextMaxWords() != null) {
                parts.add("Limite máximo de palavras sobre a imagem:\n\n "
                        + plan.imageBriefing().imageTextMaxWords()
                        + " palavras.");
            }
        }
        if (StringUtils.hasText(plan.variantKey())) {
            parts.add("Ângulo da variação:\n\n " + plan.variantKey() + ".");
        }
        String imageFilterTitle = resolveImageFilterTitle(experiment);
        if (StringUtils.hasText(imageFilterTitle)) {
            parts.add("Obrigatório: usar o título de filtro \"" + imageFilterTitle
                    + "\" em destaque dentro da imagem, com hierarquia visual elegante, tipografia legível e contraste forte.");
            parts.add("Esse título deve ser o elemento textual mais perceptível da composição, sem poluição visual.");
        }
        if (StringUtils.hasText(plan.headline())) {
            parts.add("Headline de referência:\n\n \"" + plan.headline() + "\".");
        }
        if (StringUtils.hasText(plan.primaryText())) {
            parts.add("Texto principal orientado para dor/promessa:\n\n " + plan.primaryText());
        }
        if (StringUtils.hasText(plan.description())) {
            parts.add("Complemento/contexto:\n\n " + plan.description());
        }
        if (StringUtils.hasText(plan.ctaText())) {
            parts.add("CTA textual visível:\n\n \"" + plan.ctaText() + "\".");
        }
        if (StringUtils.hasText(request.getDestinationUrl())) {
            parts.add("Representar a ideia de destino digital (landing page) em vez de uma conversa humana.");
        }
        Hypothesis hypothesis = experiment.getHypothesisRef();
        if (hypothesis != null && StringUtils.hasText(hypothesis.getPromise())) {
            parts.add("Promessa central da hipótese:\n\n " + hypothesis.getPromise() + ".");
        }
        parts.add("Lembre-se de que o Worker AI usará o modelo gpt-imagem-1.5.");
        parts.add("Não inclua logos das plataformas e evite rostos genéricos sem contexto.");
        return String.join(" ", parts);
    }

    /** Exception used to abort a creative batch before incomplete records are persisted. */
    private static class CreativeImageGenerationException extends RuntimeException {
        /** Creates a batch-aborting exception with a root cause. */
        CreativeImageGenerationException(String message, Throwable cause) {
            super(message, cause);
        }

        /** Creates a batch-aborting exception without a nested cause. */
        CreativeImageGenerationException(String message) {
            super(message);
        }
    }

    private String resolveNicheName(Experiment experiment) {
        if (experiment == null || experiment.getNiche() == null || !StringUtils.hasText(experiment.getNiche().getName())) {
            return "do experimento";
        }
        return "\"" + experiment.getNiche().getName().trim() + "\"";
    }

    private String buildPipelineIntermediatePrompt(Experiment experiment,
                                                   PipelineAdCreativePlan plan,
                                                   CreateCreativeRequest request) {
        List<String> parts = new ArrayList<>();
        if (plan.imageBriefing() != null) {
            if (StringUtils.hasText(plan.imageBriefing().visualBriefing())) {
                parts.add("Briefing visual: " + plan.imageBriefing().visualBriefing());
            }
            if (StringUtils.hasText(plan.imageBriefing().hierarchy())) {
                parts.add("Hierarquia: " + plan.imageBriefing().hierarchy());
            }
            if (StringUtils.hasText(plan.imageBriefing().formatByPlacement())) {
                parts.add("Formato por placement: " + plan.imageBriefing().formatByPlacement());
            }
            if (StringUtils.hasText(plan.imageBriefing().messageMatchNotes())) {
                parts.add("Mensagem espelhada: " + plan.imageBriefing().messageMatchNotes());
            }
            if (StringUtils.hasText(plan.imageBriefing().complianceNotes())) {
                parts.add("Compliance: " + plan.imageBriefing().complianceNotes());
            }
        }
        if (StringUtils.hasText(plan.variantKey())) {
            parts.add("Variação: " + plan.variantKey());
        }
        String imageFilterTitle = resolveImageFilterTitle(experiment);
        if (StringUtils.hasText(imageFilterTitle)) {
            parts.add("Título de filtro obrigatório em destaque: " + imageFilterTitle);
        }
        if (StringUtils.hasText(request.getHeadline())) {
            parts.add("Headline: " + request.getHeadline());
        }
        if (StringUtils.hasText(request.getPrimaryText())) {
            parts.add("Texto principal: " + request.getPrimaryText());
        }
        return String.join("\n", parts);
    }

    private String resolveImageFilterTitle(Experiment experiment) {
        if (experiment == null || experiment.getHypothesisRef() == null) {
            return null;
        }
        String imageFilterTitle = experiment.getHypothesisRef().getImageFilterTitle();
        if (!StringUtils.hasText(imageFilterTitle)) {
            return null;
        }
        return imageFilterTitle.trim();
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
