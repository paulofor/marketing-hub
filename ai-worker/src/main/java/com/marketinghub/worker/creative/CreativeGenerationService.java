package com.marketinghub.worker.creative;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.creative.dto.CreateCreativeRequest;
import com.marketinghub.experiment.CreativeGenerationMode;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.dto.ExperimentDto;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.worker.creative.pipeline.ExperimentPipelineAdExtractor;
import com.marketinghub.worker.creative.pipeline.PipelineAdCreativePlan;
import com.marketinghub.worker.creative.CreativeGenerationBackendClient.CreativeTaskExecutionAudit;
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
    private static final int META_PRIMARY_TEXT_MAX_LENGTH = 125;
    private static final int META_HEADLINE_MAX_LENGTH = 40;
    private static final int META_DESCRIPTION_MAX_LENGTH = 25;
    private static final int META_CALL_TO_ACTION_MAX_LENGTH = 32;
    private static final String DEFAULT_META_CALL_TO_ACTION = "LEARN_MORE";
    private final CreativeGenerationBackendClient backendClient;
    private final CreativeChatGptClient textClient;
    private final ObjectMapper objectMapper;
    private final ExperimentPipelineAdExtractor pipelineExtractor;
    private final LandingCreativeReferenceSelector referenceSelector;

    /** Inicializa o serviço com backend, texto e seleção dos entregáveis produzidos por Têmis. */
    public CreativeGenerationService(
            CreativeGenerationBackendClient backendClient,
            CreativeChatGptClient textClient,
            ObjectMapper objectMapper
    ) {
        this.backendClient = backendClient;
        this.textClient = textClient;
        this.objectMapper = objectMapper;
        this.pipelineExtractor = new ExperimentPipelineAdExtractor(objectMapper);
        this.referenceSelector = new LandingCreativeReferenceSelector(objectMapper);
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
                    backendClient.markFailed(experimentId, rootMessage(ex), failureAudit(ex));
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
        CreativeBatch batch = dto.getCreativeGenerationMode() == CreativeGenerationMode.PIPELINE_ADS
                ? generatePipelineCreatives(dto, quantity)
                : generateDefaultCreatives(dto, quantity);
        try {
            for (CreateCreativeRequest creative : batch.creatives()) {
                backendClient.createCreative(dto.getId(), creative);
            }
            backendClient.markCompleted(dto.getId(), batch.executionAudit());
        } catch (RuntimeException ex) {
            throw new AuditedProcessingException(ex, batch.executionAudit());
        }
        log.info("Geração de criativos concluída. experimentId={} total={}", dto.getId(), batch.creatives().size());
    }

    /** Gera criativos a partir dos pares de texto e briefing já produzidos pelo pipeline do experimento. */
    private CreativeBatch generatePipelineCreatives(ExperimentDto dto, int quantity) {
        Experiment experiment = toExperiment(dto);
        List<LandingCreativeReferenceSelector.ReferenceImage> references =
                referenceSelector.selectCommercialKit(dto.getCommercialPlanVisualAssets());
        if (references.isEmpty()) {
            throw new IllegalStateException(
                    "Têmis não encontrou exemplos APPROVED no Kit Visual do plano comercial; geração bloqueada antes de consumir tentativa");
        }
        List<PipelineAdCreativePlan> plans = pipelineExtractor.extract(experiment).stream()
                .limit(Math.max(1, quantity))
                .toList();
        if (plans.isEmpty()) {
            throw new IllegalStateException("Nenhum anúncio válido encontrado no pipeline do experimento");
        }
        List<CreateCreativeRequest> result = new ArrayList<>();
        for (int index = 0; index < plans.size(); index++) {
            PipelineAdCreativePlan plan = plans.get(index);
            CreateCreativeRequest request = new CreateCreativeRequest();
            request.setHeadline(plan.headline());
            request.setPrimaryText(plan.primaryText());
            request.setDescription(plan.description());
            request.setCta(normalizeMetaCallToAction(plan.ctaText()));
            request.setFormat(StringUtils.hasText(plan.format()) ? plan.format() : "IMAGE");
            normalizeCreativeContract(request);
            String imageUrl = references.get(index % references.size()).url();
            requireGeneratedImageUrl(dto.getId(), request.getHeadline(), imageUrl);
            request.setImageUrl(imageUrl);
            request.setStatus(CreativeStatus.DRAFT);
            result.add(request);
        }
        return new CreativeBatch(result, deterministicAudit(dto));
    }

    /** Gera criativos no modo padrão usando texto gerado por IA e imagem por prompt do experimento. */
    private CreativeBatch generateDefaultCreatives(ExperimentDto dto, int quantity) {
        Experiment experiment = toExperiment(dto);
        List<LandingCreativeReferenceSelector.ReferenceImage> references =
                referenceSelector.selectCommercialKit(dto.getCommercialPlanVisualAssets());
        if (references.isEmpty()) {
            throw new IllegalStateException(
                    "Geração bloqueada: o plano não possui entregável visual APPROVED produzido por Têmis");
        }
        CreativeBatch batch = generateAndValidateCopy(experiment, quantity);
        List<CreateCreativeRequest> creatives = batch.creatives();
        for (int index = 0; index < creatives.size(); index++) {
            CreateCreativeRequest creative = creatives.get(index);
            String imageUrl = references.get(index % references.size()).url();
            requireGeneratedImageUrl(dto.getId(), creative.getHeadline(), imageUrl);
            creative.setImageUrl(imageUrl);
            if (creative.getStatus() == null) {
                creative.setStatus(CreativeStatus.DRAFT);
            }
        }
        return new CreativeBatch(creatives, batch.executionAudit());
    }

    /** Reexecuta uma vez a criação textual quando o modelo viola o contrato comercial da Meta. */
    private CreativeBatch generateAndValidateCopy(Experiment experiment, int quantity) {
        IllegalArgumentException lastContractFailure = null;
        CreativeTaskExecutionAudit latestAudit = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            CreativeChatGptClient.Generation generation = attempt == 1
                    ? textClient.generateCreatives(experiment, quantity)
                    : textClient.generateCreatives(experiment, quantity, lastContractFailure.getMessage());
            latestAudit = modelAudit(generation.executionAudit());
            List<CreateCreativeRequest> creatives = generation.creatives().stream()
                    .limit(Math.max(1, quantity))
                    .toList();
            try {
                creatives.forEach(this::normalizeCreativeContract);
                return new CreativeBatch(creatives, latestAudit);
            } catch (IllegalArgumentException ex) {
                lastContractFailure = ex;
                log.warn("Copy Meta fora do contrato; solicitando reescrita completa. experimentId={} tentativa={}",
                        experiment.getId(), attempt, ex);
            }
        }
        throw new AuditedProcessingException(lastContractFailure, latestAudit);
    }

    /** Registra a entrada exata do pipeline que reutiliza artefatos sem chamar modelo. */
    private CreativeTaskExecutionAudit deterministicAudit(ExperimentDto dto) {
        try {
            return new CreativeTaskExecutionAudit(
                    "DETERMINISTIC",
                    "creative-pipeline-ads-v1",
                    "NOT_APPLICABLE",
                    objectMapper.writeValueAsString(dto),
                    List.of());
        } catch (JsonProcessingException ex) {
            log.error("Falha ao serializar a entrada determinística de Dédalo. experimentId={}", dto.getId(), ex);
            throw new IllegalStateException("Não foi possível auditar a entrada do pipeline de criativos", ex);
        }
    }

    /** Converte a chamada real de copy no contrato comum de auditoria da tarefa. */
    private CreativeTaskExecutionAudit modelAudit(CreativeChatGptClient.ExecutionAudit audit) {
        if (audit == null
                || !StringUtils.hasText(audit.modelCode())
                || !StringUtils.hasText(audit.reasoningEffort())
                || !StringUtils.hasText(audit.promptSent())) {
            throw new IllegalStateException(
                    "A geração de copy não informou modelo, raciocínio e prompt integral");
        }
        return new CreativeTaskExecutionAudit(
                "MODEL",
                audit.modelCode(),
                audit.reasoningEffort(),
                audit.promptSent(),
                List.of());
    }

    /** Recupera a chamada que falhou ou declara honestamente que o modelo não iniciou. */
    private CreativeTaskExecutionAudit failureAudit(RuntimeException error) {
        if (error instanceof AuditedProcessingException audited
                && audited.executionAudit() != null) {
            return audited.executionAudit();
        }
        if (error instanceof CreativeChatGptClient.AuditedCreativeGenerationException audited
                && audited.executionAudit() != null) {
            return modelAudit(audited.executionAudit());
        }
        return new CreativeTaskExecutionAudit(
                "NOT_STARTED", null, "NOT_APPLICABLE", null, List.of());
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

    /** Valida todos os campos textuais publicáveis sem truncamento e normaliza apenas o CTA técnico. */
    private void normalizeCreativeContract(CreateCreativeRequest creative) {
        if (creative == null) {
            return;
        }
        String normalizedCta = normalizeMetaCallToAction(creative.getCta());
        List<String> violations = new ArrayList<>();
        addMetaTextViolation(
                violations, "primaryText", creative.getPrimaryText(), META_PRIMARY_TEXT_MAX_LENGTH);
        addMetaTextViolation(violations, "headline", creative.getHeadline(), META_HEADLINE_MAX_LENGTH);
        addMetaTextViolation(
                violations, "description", creative.getDescription(), META_DESCRIPTION_MAX_LENGTH);
        addMetaTextViolation(violations, "cta", normalizedCta, META_CALL_TO_ACTION_MAX_LENGTH);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("Copy Meta inválida: " + String.join("; ", violations)
                    + "; reescrita obrigatória");
        }
        creative.setCta(normalizedCta);
    }

    /** Registra a contagem do campo que excede o contrato Meta para diagnóstico no painel. */
    private void addMetaTextViolation(
            List<String> violations, String field, String value, int maxLength) {
        if (value != null) {
            int actualLength = value.codePointCount(0, value.length());
            if (actualLength > maxLength) {
                violations.add(field + " excede " + maxLength + " caracteres (atual: " + actualLength + ")");
            }
        }
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

    /** Limita texto em fronteira de palavra para impedir falha de persistencia por coluna curta. */
    private String limitText(String value, int maxLength) {
        if (!StringUtils.hasText(value) || value.length() <= maxLength) {
            return value;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        int boundary = trimmed.lastIndexOf(' ', maxLength);
        if (boundary < maxLength / 2) {
            boundary = maxLength;
        }
        return trimmed.substring(0, boundary).trim();
    }

    /** Bloqueia criativo de imagem sem asset visual gerado para evitar aprovação/publicação falsa. */
    private void requireGeneratedImageUrl(Long experimentId, String headline, String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            throw new IllegalStateException(
                    "Imagem do criativo não foi gerada; experimento=" + experimentId
                            + " headline=" + limitText(headline, 80));
        }
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

    /** Une os criativos materializados à execução que deve aparecer no histórico. */
    private record CreativeBatch(
            List<CreateCreativeRequest> creatives, CreativeTaskExecutionAudit executionAudit) {
    }

    /** Transporta a auditoria quando a falha ocorre depois que a chamada já foi conhecida. */
    private static final class AuditedProcessingException extends RuntimeException {
        private final CreativeTaskExecutionAudit executionAudit;

        /** Preserva causa e auditoria para o callback terminal. */
        private AuditedProcessingException(
                RuntimeException cause, CreativeTaskExecutionAudit executionAudit) {
            super(cause.getMessage(), cause);
            this.executionAudit = executionAudit;
        }

        /** Retorna a chamada que antecedeu a falha. */
        private CreativeTaskExecutionAudit executionAudit() {
            return executionAudit;
        }
    }
}
