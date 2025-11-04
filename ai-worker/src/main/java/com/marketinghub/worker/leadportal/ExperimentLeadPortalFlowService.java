package com.marketinghub.worker.leadportal;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.LeadPortalFlowQuestion;
import com.marketinghub.leadportal.LeadPortalQuestionType;
import com.marketinghub.leadportal.repository.LeadPortalFlowRepository;
import com.marketinghub.worker.experiment.ExperimentGenerationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Serviço responsável por gerar fluxos do portal do lead solicitados pelos experimentos.
 */
@Service
public class ExperimentLeadPortalFlowService {
    private static final Logger log = LoggerFactory.getLogger(ExperimentLeadPortalFlowService.class);

    private final ExperimentRepository experimentRepository;
    private final LeadPortalFlowRepository leadPortalFlowRepository;
    private final ExperimentGenerationRepository generationRepository;
    private final ExperimentLeadPortalFlowChatGptClient chatGptClient;

    public ExperimentLeadPortalFlowService(ExperimentRepository experimentRepository,
                                           LeadPortalFlowRepository leadPortalFlowRepository,
                                           ExperimentGenerationRepository generationRepository,
                                           ExperimentLeadPortalFlowChatGptClient chatGptClient) {
        this.experimentRepository = experimentRepository;
        this.leadPortalFlowRepository = leadPortalFlowRepository;
        this.generationRepository = generationRepository;
        this.chatGptClient = chatGptClient;
    }

    @Transactional
    public Map<Long, List<LeadPortalFlow>> generate() {
        Map<Long, List<LeadPortalFlow>> result = new LinkedHashMap<>();
        List<Experiment> experiments = generationRepository.findAllToGenerateLeadPortalFlows();
        for (Experiment experiment : experiments) {
            Integer quantity = experiment.getLeadPortalFlowsToGenerate();
            if (quantity == null || quantity <= 0) {
                log.debug("Ignorando experimento {} sem solicitação de fluxos do portal", experiment.getId());
                continue;
            }
            log.info("Gerando {} fluxo(s) do portal para o experimento {}", quantity, experiment.getId());
            try {
                ExperimentLeadPortalFlowChatGptClient.Generation generation = chatGptClient.generateFlows(experiment, quantity);
                List<ExperimentLeadPortalFlowChatGptClient.FlowPlan> plans = generation.plans();
                if (plans.isEmpty()) {
                    log.warn("ChatGPT não retornou fluxos para o experimento {}", experiment.getId());
                }
                List<LeadPortalFlow> savedFlows = new ArrayList<>();
                int produced = 0;
                for (ExperimentLeadPortalFlowChatGptClient.FlowPlan plan : plans) {
                    if (produced >= quantity) {
                        break;
                    }
                    LeadPortalFlow flow = buildFlowFromPlan(plan, generation, experiment);
                    if (flow == null) {
                        continue;
                    }
                    savedFlows.add(leadPortalFlowRepository.save(flow));
                    produced++;
                }
                experiment.setLeadPortalFlowsToGenerate(0);
                experimentRepository.save(experiment);
                result.put(experiment.getId(), savedFlows);
                log.info("Fluxos do portal gerados para experimento {}: {}", experiment.getId(), savedFlows.size());
            } catch (Exception ex) {
                log.error("Falha ao gerar fluxos do portal para o experimento {}", experiment.getId(), ex);
            }
        }
        return result;
    }

    private LeadPortalFlow buildFlowFromPlan(ExperimentLeadPortalFlowChatGptClient.FlowPlan plan,
                                             ExperimentLeadPortalFlowChatGptClient.Generation generation,
                                             Experiment experiment) {
        String name = StringUtils.hasText(plan.name()) ? plan.name().trim() : null;
        if (!StringUtils.hasText(name)) {
            log.warn("Plano de fluxo sem nome ignorado para experimento {}", experiment.getId());
            return null;
        }
        String slug = generateUniqueSlug(plan.slug(), name, experiment.getId());
        LeadPortalFlow flow = LeadPortalFlow.builder()
                .name(name)
                .slug(slug)
                .description(trimToNull(plan.description()))
                .model(generation.model())
                .prompt(generation.auditTrail())
                .build();

        List<LeadPortalFlowQuestion> questions = buildQuestions(flow, plan.questions());
        if (questions.isEmpty()) {
            log.warn("Fluxo {} ignorado porque nenhuma pergunta válida foi gerada", name);
            return null;
        }
        flow.getQuestions().addAll(questions);
        return flow;
    }

    private List<LeadPortalFlowQuestion> buildQuestions(LeadPortalFlow flow,
                                                        List<ExperimentLeadPortalFlowChatGptClient.QuestionPlan> questionPlans) {
        List<LeadPortalFlowQuestion> questions = new ArrayList<>();
        Set<String> usedKeys = new HashSet<>();
        if (questionPlans != null) {
            int index = 0;
            for (ExperimentLeadPortalFlowChatGptClient.QuestionPlan plan : questionPlans) {
                LeadPortalFlowQuestion question = toQuestion(flow, plan, index, usedKeys);
                if (question != null) {
                    questions.add(question);
                    index++;
                }
            }
        }
        if (questions.isEmpty() || questions.get(questions.size() - 1).getType() != LeadPortalQuestionType.IMAGE_UPLOAD) {
            questions.add(createImageUploadQuestion(flow, usedKeys, questions.size()));
        }
        return questions;
    }

    private LeadPortalFlowQuestion toQuestion(LeadPortalFlow flow,
                                              ExperimentLeadPortalFlowChatGptClient.QuestionPlan plan,
                                              int index,
                                              Set<String> usedKeys) {
        if (plan == null || !StringUtils.hasText(plan.title()) || plan.type() == null) {
            return null;
        }
        LeadPortalQuestionType type = plan.type();
        List<String> options = sanitizeOptions(plan.options(), type);
        if (options == null) {
            log.warn("Pergunta '{}' ignorada por falta de opções obrigatórias", plan.title());
            return null;
        }
        String dataKey = normalizeDataKey(plan.dataKey(), usedKeys, index);
        usedKeys.add(dataKey);
        return LeadPortalFlowQuestion.builder()
                .flow(flow)
                .title(plan.title().trim())
                .dataKey(dataKey)
                .type(type)
                .required(plan.required())
                .description(trimToNull(plan.description()))
                .placeholder(trimToNull(plan.placeholder()))
                .position(index)
                .options(options)
                .build();
    }

    private LeadPortalFlowQuestion createImageUploadQuestion(LeadPortalFlow flow,
                                                             Set<String> usedKeys,
                                                             int index) {
        String baseKey = "foto_problema";
        String dataKey = baseKey;
        int suffix = 2;
        while (usedKeys.contains(dataKey)) {
            dataKey = baseKey + "_" + suffix++;
        }
        usedKeys.add(dataKey);
        return LeadPortalFlowQuestion.builder()
                .flow(flow)
                .title("Envie uma foto que represente o problema mencionado")
                .dataKey(dataKey)
                .type(LeadPortalQuestionType.IMAGE_UPLOAD)
                .required(true)
                .description("Essa imagem nos ajuda a entender melhor o desafio enfrentado.")
                .position(index)
                .options(new ArrayList<>())
                .build();
    }

    private List<String> sanitizeOptions(List<String> rawOptions, LeadPortalQuestionType type) {
        boolean expectsOptions = type == LeadPortalQuestionType.SINGLE_CHOICE
                || type == LeadPortalQuestionType.MULTIPLE_CHOICE;
        if (rawOptions == null || rawOptions.isEmpty()) {
            return expectsOptions ? null : new ArrayList<>();
        }
        List<String> cleaned = new ArrayList<>();
        for (String option : rawOptions) {
            if (StringUtils.hasText(option)) {
                cleaned.add(option.trim());
            }
        }
        if (cleaned.isEmpty()) {
            return expectsOptions ? null : new ArrayList<>();
        }
        if (!expectsOptions) {
            return new ArrayList<>();
        }
        return cleaned;
    }

    private String normalizeDataKey(String candidate, Set<String> usedKeys, int index) {
        String base;
        if (StringUtils.hasText(candidate)) {
            base = slugify(candidate);
        } else {
            base = "pergunta_" + (index + 1);
        }
        if (!base.matches("^[a-z][a-z0-9_-]*$")) {
            base = "pergunta_" + (index + 1);
        }
        String dataKey = base;
        int suffix = 2;
        while (usedKeys.contains(dataKey)) {
            dataKey = base + "_" + suffix++;
        }
        return dataKey;
    }

    private String generateUniqueSlug(String candidateSlug, String name, Long experimentId) {
        String base = StringUtils.hasText(candidateSlug) ? slugify(candidateSlug) : slugify(name);
        if (!StringUtils.hasText(base)) {
            base = "fluxo-" + experimentId;
        }
        base = truncate(base, 110);
        String slug = base;
        int suffix = 2;
        while (leadPortalFlowRepository.findBySlug(slug).isPresent()) {
            String suffixValue = "-" + suffix++;
            int maxBaseLength = Math.max(1, 120 - suffixValue.length());
            String truncatedBase = truncate(base, maxBaseLength);
            slug = truncatedBase + suffixValue;
        }
        return truncate(slug, 120);
    }

    private String slugify(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "");
        String slug = normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        return slug;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength));
    }
}
