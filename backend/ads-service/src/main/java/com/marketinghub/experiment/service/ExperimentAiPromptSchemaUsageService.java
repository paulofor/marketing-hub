package com.marketinghub.experiment.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentAiPromptSchemaUsage;
import com.marketinghub.gerasalespage.v1.GeraSalesPagePromptSchemaTemplate;
import com.marketinghub.hypothesis.pain.HypothesisPainStageExecution;
import com.marketinghub.repository.jpa.experiment.ExperimentAiPromptSchemaUsageRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPagePromptSchemaTemplateRepository;
import com.marketinghub.repository.jpa.hypothesis.HypothesisPainStageExecutionRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Responsabilidade: associar prompts e schemas de IA usados aos experimentos que dependem deles. */
@Service
public class ExperimentAiPromptSchemaUsageService {
    private static final String HYPOTHESIS_PIPELINE_CODE = "hypothesis-pipeline";
    private static final String HYPOTHESIS_USAGE_CONTEXT = "HYPOTHESIS_PIPELINE";
    private static final String SALES_PAGE_USAGE_CONTEXT = "GERA_SALES_PAGE_V1";

    private final ExperimentRepository experimentRepository;
    private final HypothesisPainStageExecutionRepository hypothesisExecutionRepository;
    private final GeraSalesPagePromptSchemaTemplateRepository templateRepository;
    private final ExperimentAiPromptSchemaUsageRepository usageRepository;

    /** Inicializa o serviço com os repositórios de experimento, execuções e templates. */
    public ExperimentAiPromptSchemaUsageService(
            ExperimentRepository experimentRepository,
            HypothesisPainStageExecutionRepository hypothesisExecutionRepository,
            GeraSalesPagePromptSchemaTemplateRepository templateRepository,
            ExperimentAiPromptSchemaUsageRepository usageRepository) {
        this.experimentRepository = experimentRepository;
        this.hypothesisExecutionRepository = hypothesisExecutionRepository;
        this.templateRepository = templateRepository;
        this.usageRepository = usageRepository;
    }

    /** Associa ao experimento todos os templates usados pelas etapas concluídas da hipótese origem. */
    @Transactional
    public void linkHypothesisTemplates(Long experimentId) {
        Experiment experiment = findExperiment(experimentId);
        if (experiment.getHypothesisRefIdForPending() == null) {
            return;
        }
        List<HypothesisPainStageExecution> executions =
                hypothesisExecutionRepository.findByHypothesisIdOrderByExecutionRequestedAtAsc(
                        experiment.getHypothesisRefIdForPending());
        executions.stream()
                .filter(execution -> StringUtils.hasText(execution.getPromptTemplateId()))
                .forEach(execution -> templateRepository.findById(execution.getPromptTemplateId())
                        .ifPresent(template -> upsertUsage(
                                experiment,
                                template,
                                HYPOTHESIS_USAGE_CONTEXT,
                                execution.getStageCode(),
                                fromDatabaseIdJob(execution.getIdJob()),
                                execution.getCompletedAt())));
    }

    /** Associa ao experimento o template ativo usado por uma etapa do GeraSalesPage. */
    @Transactional
    public void linkSalesPageTemplate(Long experimentId, GeraSalesPagePromptSchemaTemplate template, String stageCode, String sourceJobId) {
        Experiment experiment = findExperiment(experimentId);
        upsertUsage(experiment, template, SALES_PAGE_USAGE_CONTEXT, stageCode, sourceJobId, Instant.now());
    }

    /** Retorna a quantidade de templates de hipótese associados ao experimento. */
    @Transactional(readOnly = true)
    public long countHypothesisTemplates(Long experimentId) {
        return usageRepository.countByExperimentIdAndPipelineCode(experimentId, HYPOTHESIS_PIPELINE_CODE);
    }

    /** Cria ou atualiza o uso do template para o experimento informado. */
    private void upsertUsage(
            Experiment experiment,
            GeraSalesPagePromptSchemaTemplate template,
            String usageContext,
            String stageCode,
            String sourceJobId,
            Instant usedAt) {
        Instant now = Instant.now();
        Optional<ExperimentAiPromptSchemaUsage> existing =
                usageRepository.findByExperimentIdAndTemplateKeyAndUsageContextAndStageCode(
                        experiment.getId(),
                        template.getTemplateKey(),
                        usageContext,
                        stageCode);
        ExperimentAiPromptSchemaUsage usage = existing.orElseGet(() -> ExperimentAiPromptSchemaUsage.builder()
                .experiment(experiment)
                .templateKey(template.getTemplateKey())
                .usageContext(usageContext)
                .createdAt(now)
                .build());
        usage.setPipelineCode(template.getPipelineCode());
        usage.setStageCode(stageCode);
        usage.setTemplateVersion(template.getVersion());
        usage.setOpenAiModel(template.getOpenAiModel());
        usage.setSchemaName(template.getSchemaName());
        usage.setSourceJobId(sourceJobId);
        usage.setUsedAt(usedAt != null ? usedAt : now);
        usage.setUpdatedAt(now);
        usageRepository.save(usage);
    }

    /** Busca o experimento de forma transacional para registrar a associação. */
    private Experiment findExperiment(Long experimentId) {
        return experimentRepository.findById(experimentId)
                .orElseThrow(() -> new EntityNotFoundException("Experiment not found: " + experimentId));
    }

    /** Converte o idJob binário usado no pipeline de hipótese para texto UUID. */
    private String fromDatabaseIdJob(byte[] idJob) {
        if (idJob == null || idJob.length != 16) {
            return null;
        }
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(idJob);
        return new java.util.UUID(buffer.getLong(), buffer.getLong()).toString();
    }
}
