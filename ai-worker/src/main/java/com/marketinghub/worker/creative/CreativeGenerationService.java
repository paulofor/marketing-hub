package com.marketinghub.worker.creative;

import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.creative.dto.CreateCreativeRequest;
import com.marketinghub.experiment.CreativeGenerationMode;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.dto.ExperimentDto;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.worker.creative.pipeline.ExperimentPipelineAdExtractor;
import com.marketinghub.worker.creative.pipeline.PipelineAdCreativePlan;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Responsabilidade: processar solicitações pendentes de geração de criativos de experimentos.
 */
@Service
public class CreativeGenerationService {
    private static final Logger log = LoggerFactory.getLogger(CreativeGenerationService.class);
    private static final int META_CALL_TO_ACTION_MAX_LENGTH = 32;
    private static final String DEFAULT_META_CALL_TO_ACTION = "LEARN_MORE";

    private final CreativeGenerationBackendClient backendClient;
    private final CreativeChatGptClient textClient;
    private final CreativeImageClient imageClient;
    private final ExperimentPipelineAdExtractor pipelineExtractor;

    /** Inicializa o serviço com clientes de backend, texto, imagem e extração dos anúncios do pipeline. */
    public CreativeGenerationService(
            CreativeGenerationBackendClient backendClient,
            CreativeChatGptClient textClient,
            CreativeImageClient imageClient,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper
    ) {
        this.backendClient = backendClient;
        this.textClient = textClient;
        this.imageClient = imageClient;
        this.pipelineExtractor = new ExperimentPipelineAdExtractor(objectMapper);
    }

    /** Processa até o limite informado de experimentos com geração de criativos pendente. */
    public ProcessingSummary processPending(int limit) {
        List<ExperimentDto> pending = backendClient.listPending(limit);
        int succeeded = 0;
        int failed = 0;
        for (ExperimentDto experiment : pending) {
            try {
                processExperiment(experiment);
                succeeded++;
            } catch (RuntimeException ex) {
                failed++;
                Long experimentId = experiment != null ? experiment.getId() : null;
                log.error("Falha ao processar geração de criativos. experimentId={}", experimentId, ex);
                if (experimentId != null) {
                    backendClient.markFailed(experimentId, rootMessage(ex));
                }
            }
        }
        return new ProcessingSummary(pending.size(), succeeded, failed);
    }

    /** Executa a geração de criativos para um experimento específico. */
    private void processExperiment(ExperimentDto dto) {
        if (dto == null || dto.getId() == null) {
            return;
        }
        int quantity = dto.getCreativesToGenerate() == null ? 0 : dto.getCreativesToGenerate();
        if (quantity <= 0) {
            return;
        }
        backendClient.markStarted(dto.getId());
        List<CreateCreativeRequest> creatives = dto.getCreativeGenerationMode() == CreativeGenerationMode.PIPELINE_ADS
                ? generatePipelineCreatives(dto, quantity)
                : generateDefaultCreatives(dto, quantity);
        for (CreateCreativeRequest creative : creatives) {
            backendClient.createCreative(dto.getId(), creative);
        }
        backendClient.markCompleted(dto.getId());
        log.info("Geração de criativos concluída. experimentId={} total={}", dto.getId(), creatives.size());
    }

    /** Gera criativos a partir dos pares de texto e briefing já produzidos pelo pipeline do experimento. */
    private List<CreateCreativeRequest> generatePipelineCreatives(ExperimentDto dto, int quantity) {
        Experiment experiment = toExperiment(dto);
        List<PipelineAdCreativePlan> plans = pipelineExtractor.extract(experiment).stream()
                .limit(Math.max(1, quantity))
                .toList();
        if (plans.isEmpty()) {
            throw new IllegalStateException("Nenhum anúncio válido encontrado no pipeline do experimento");
        }
        List<CreateCreativeRequest> result = new ArrayList<>();
        for (PipelineAdCreativePlan plan : plans) {
            String prompt = buildPipelineImagePrompt(plan);
            String imageUrl = imageClient.generateImage(prompt, null,
                    "pipeline-creative-experiment-" + dto.getId() + "-" + plan.variantKey());
            CreateCreativeRequest request = new CreateCreativeRequest();
            request.setHeadline(plan.headline());
            request.setPrimaryText(plan.primaryText());
            request.setDescription(plan.description());
            request.setCta(normalizeMetaCallToAction(plan.ctaText()));
            request.setFormat(StringUtils.hasText(plan.format()) ? plan.format() : "IMAGE");
            request.setImageUrl(imageUrl);
            request.setStatus(CreativeStatus.DRAFT);
            result.add(request);
        }
        return result;
    }

    /** Gera criativos no modo padrão usando texto gerado por IA e imagem por prompt do experimento. */
    private List<CreateCreativeRequest> generateDefaultCreatives(ExperimentDto dto, int quantity) {
        CreativeChatGptClient.Generation generation = textClient.generateCreatives(toExperiment(dto), quantity);
        List<CreateCreativeRequest> creatives = generation.creatives().stream()
                .limit(Math.max(1, quantity))
                .toList();
        for (CreateCreativeRequest creative : creatives) {
            String prompt = defaultImagePrompt(dto, creative);
            creative.setImageUrl(imageClient.generateImage(prompt, null, "creative-experiment-" + dto.getId()));
            if (creative.getStatus() == null) {
                creative.setStatus(CreativeStatus.DRAFT);
            }
            creative.setCta(normalizeMetaCallToAction(creative.getCta()));
        }
        return creatives;
    }

    /** Converte o DTO do backend em entidade mínima para reutilizar os geradores existentes. */
    private Experiment toExperiment(ExperimentDto dto) {
        Experiment experiment = new Experiment();
        experiment.setId(dto.getId());
        experiment.setName(dto.getName());
        experiment.setHypothesis(dto.getHypothesis());
        experiment.setCreativeTextPrompt(dto.getCreativeTextPrompt());
        experiment.setCreativeImagePrompt(dto.getCreativeImagePrompt());
        experiment.setAdCopy(dto.getAdCopy());
        experiment.setAdImageBriefing(dto.getAdImageBriefing());
        Hypothesis hypothesis = new Hypothesis();
        hypothesis.setId(dto.getHypothesisId());
        hypothesis.setTitle(dto.getHypothesis());
        hypothesis.setProblem(dto.getSinglePain());
        hypothesis.setPromise(dto.getFunnelPromise());
        experiment.setHypothesisRef(hypothesis);
        return experiment;
    }

    /** Monta o prompt visual final de um criativo do pipeline. */
    private String buildPipelineImagePrompt(PipelineAdCreativePlan plan) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Crie uma imagem de anúncio para Meta Ads. ");
        if (plan.imageBriefing() != null && StringUtils.hasText(plan.imageBriefing().visualBriefing())) {
            prompt.append(plan.imageBriefing().visualBriefing()).append(' ');
        }
        if (StringUtils.hasText(plan.primaryText())) {
            prompt.append("Mensagem do anúncio: ").append(plan.primaryText()).append(' ');
        }
        if (StringUtils.hasText(plan.headline())) {
            prompt.append("Headline: ").append(plan.headline()).append(' ');
        }
        return prompt.toString().trim();
    }

    /** Define o prompt de imagem do modo padrão usando prompt customizado ou texto do criativo. */
    private String defaultImagePrompt(ExperimentDto dto, CreateCreativeRequest creative) {
        if (StringUtils.hasText(dto.getCreativeImagePrompt())) {
            return dto.getCreativeImagePrompt();
        }
        return "Crie uma imagem de anúncio para Meta Ads alinhada ao texto: " + creative.getPrimaryText();
    }

    /** Normaliza CTA livre para o tipo canônico aceito pelo backend e pela Meta. */
    private String normalizeMetaCallToAction(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String normalized = value.trim();
        if (normalized.length() <= META_CALL_TO_ACTION_MAX_LENGTH) {
            return normalized;
        }
        return DEFAULT_META_CALL_TO_ACTION;
    }

    /** Extrai a mensagem raiz para gravar erro operacional legível no backend. */
    private String rootMessage(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage();
    }

    /** Resultado resumido do ciclo de geração de criativos. */
    public record ProcessingSummary(int total, int succeeded, int failed) {
    }
}
