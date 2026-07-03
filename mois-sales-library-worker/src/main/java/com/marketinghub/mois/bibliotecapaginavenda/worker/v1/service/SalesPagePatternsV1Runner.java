package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.config.WorkerProperties;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service.DossierV1BackendClient.DossierPendingJob;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service.DossierV1BackendClient.DossierPendingRequest;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service.DossierV1BackendClient.DossierRecebeRequestRequest;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service.DossierV1BackendClient.DossierRecebeResponseRequest;
import com.marketinghub.pipelines.dossie.v1.PipelineWorker;
import com.marketinghub.pipelines.dossie.v1.StageResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** Executa somente o pipeline salespagepatterns.v1 para padrões de página de venda. */
@Service
@RequiredArgsConstructor
@Slf4j
public class SalesPagePatternsV1Runner {
    private static final List<String> STAGES = List.of("page-pattern-extraction");

    private final SalesPagePatternsV1BackendClient backendClient;
    private final PipelineWorker pipelineWorker;
    private final WorkerProperties properties;

    /** Consulta pendências de padrões e devolve resultados auditáveis ao backend. */
    @Scheduled(fixedDelay = 60000)
    public void runSalesPagePatternsV1Cycle() {
        for (String stageName : STAGES) {
            processStage(stageName);
        }
    }

    /** Processa trabalhos pendentes da etapa sem decidir próxima etapa fora do backend. */
    private void processStage(String stageName) {
        var response = backendClient.pending(stageName,
                new DossierPendingRequest(properties.workspaceId(), "mois-sales-library-worker-salespagepatterns-v1", 10));
        if (response == null || !response.claimed() || response.jobs() == null || response.jobs().isEmpty()) {
            return;
        }
        for (DossierPendingJob job : response.jobs()) {
            processJob(stageName, job);
        }
    }

    /** Executa um job e persiste request/response no backend. */
    private void processJob(String stageName, DossierPendingJob job) {
        try {
            backendClient.recebeRequest(stageName, job.idExterno(), job.jobId(),
                    new DossierRecebeRequestRequest(String.valueOf(job.input()), "mois-sales-library-worker", null, null));
            StageResult result = pipelineWorker.execute(job.toStageContext());
            if (result.errorMessage() != null && !result.errorMessage().isBlank()) {
                backendClient.recebeResponse(stageName, job.idExterno(), job.jobId(),
                        DossierRecebeResponseRequest.failure(result.errorMessage()));
            } else if (result.hasOpenAiInteractions()) {
                registrarInteracoesOpenAi(stageName, job, result);
            } else {
                backendClient.recebeResponse(stageName, job.idExterno(), job.jobId(),
                        DossierRecebeResponseRequest.success(String.valueOf(result.output())));
            }
            log.info("MOIS salespagepatterns.v1 job completed. stageName={}, idExterno={}, jobId={}, status={}",
                    stageName, job.idExterno(), job.jobId(), result.status());
        } catch (RuntimeException ex) {
            log.warn("MOIS salespagepatterns.v1 job failed. stageName={}, idExterno={}, jobId={}, errorClass={}, errorMessage={}",
                    stageName, job.idExterno(), job.jobId(), ex.getClass().getName(), ex.getMessage(), ex);
            backendClient.recebeResponse(stageName, job.idExterno(), job.jobId(),
                    DossierRecebeResponseRequest.failure(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
        }
    }

    /** Envia ao backend cada interação OpenAI preservando request e response brutos. */
    private void registrarInteracoesOpenAi(String stageName, DossierPendingJob job, StageResult result) {
        for (StageResult.OpenAiInteraction interaction : result.openAiInteractions()) {
            backendClient.recebeRequest(stageName, job.idExterno(), job.jobId(),
                    new DossierRecebeRequestRequest(interaction.rawRequestSent(), "openai", null, null));
            backendClient.recebeResponse(stageName, job.idExterno(), job.jobId(),
                    DossierRecebeResponseRequest.openAi(interaction));
        }
    }
}
