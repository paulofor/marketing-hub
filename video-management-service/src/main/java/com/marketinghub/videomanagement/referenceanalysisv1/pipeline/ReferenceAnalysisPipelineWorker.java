package com.marketinghub.videomanagement.referenceanalysisv1.pipeline;

import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.referenceanalysisv1.pipeline.analyze.ReferenceAnalysisBackendClient;
import com.marketinghub.videomanagement.service.AutomaticExecutionControl;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Orquestra localmente a etapa analyze sem decidir qualquer avanço de pipeline no backend. */
@Component
public class ReferenceAnalysisPipelineWorker {
    private static final Logger log = LoggerFactory.getLogger(ReferenceAnalysisPipelineWorker.class);
    private final VideoManagementProperties properties;
    private final ReferenceAnalysisBackendClient backend;
    private final ReferenceAnalysisStageProcessor processor;
    private final AutomaticExecutionControl automaticExecution;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /** Inicializa polling, processor plugável e gate administrativo de Apolo. */
    public ReferenceAnalysisPipelineWorker(VideoManagementProperties properties,
                                           ReferenceAnalysisBackendClient backend,
                                           ReferenceAnalysisStageProcessor processor,
                                           AutomaticExecutionControl automaticExecution) {
        this.properties = properties;
        this.backend = backend;
        this.processor = processor;
        this.automaticExecution = automaticExecution;
    }

    /** Consome uma pendência por vez e bloqueia polling sobreposto no mesmo executor. */
    @Scheduled(initialDelay = 7000,
            fixedDelayString = "#{@videoManagementProperties.referenceAnalysis.pollInterval.toMillis()}")
    public void poll() {
        if (!properties.getReferenceAnalysis().isEnabled()
                || !automaticExecution.allowsAutomaticExecution()
                || !running.compareAndSet(false, true)) {
            return;
        }
        try {
            List<ReferenceAnalysisStageContext> pending = backend.pending();
            if (!pending.isEmpty()) {
                process(pending.getFirst());
            }
        } catch (RuntimeException ex) {
            log.error("Falha no polling da análise de referências; workerId={}",
                    properties.getReferenceAnalysis().getWorkerId(), ex);
        } finally {
            running.set(false);
        }
    }

    /** Executa a etapa e reporta sucesso ou falha pelo contrato oficial do backend. */
    private void process(ReferenceAnalysisStageContext context) {
        try {
            ReferenceAnalysisStageResult result = processor.process(context);
            backend.complete(context, result);
        } catch (RuntimeException ex) {
            log.error("Falha ao analisar vídeo de referência; executionId={} referenceId={}",
                    context.executionId(), context.referenceId(), ex);
            backend.fail(context, ex);
        }
    }
}
