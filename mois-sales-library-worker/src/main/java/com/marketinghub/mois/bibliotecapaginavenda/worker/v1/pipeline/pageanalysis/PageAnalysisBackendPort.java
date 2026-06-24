package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.pageanalysis;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.client.BackendClient;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.config.WorkerProperties;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.ClaimRequest;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.CompleteRequest;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.FailRequest;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.StageBackendPort;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.StageExecution;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.StageResult;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Conecta a etapa pageanalysis ao backend sem acoplar o núcleo genérico a contratos HTTP concretos. */
@Component
@RequiredArgsConstructor
public class PageAnalysisBackendPort implements StageBackendPort<PageAnalysisInput, PageAnalysisOutput> {

    private static final String STAGE_CODE = "PAGE_ANALYSIS";

    private final BackendClient backendClient;
    private final WorkerProperties properties;
    private final AtomicInteger sourceCursor = new AtomicInteger(0);

    /** Reserva a próxima página com HTML capturado apta para análise comercial. */
    @Override
    public StageExecution<PageAnalysisInput> claimNext() {
        String sourceForCycle = resolveSourceForCycle();
        var response = backendClient.claim(new ClaimRequest(properties.workspaceId(), sourceForCycle));
        if (response == null || !response.claimed() || response.job() == null) {
            return null;
        }
        var job = response.job();
        return new StageExecution<>(job.jobId(), STAGE_CODE,
                new PageAnalysisInput(job.pageId(), job.urlCanonical(), job.title(), job.rawHtml()), Map.of("source", sourceForCycle));
    }

    /** Escolhe a fonte de marketplace do ciclo de análise, alternando quando houver múltiplas configuradas. */
    private String resolveSourceForCycle() {
        List<String> configuredSources = parseSources(properties.sources());
        if (!configuredSources.isEmpty()) {
            int index = Math.floorMod(sourceCursor.getAndIncrement(), configuredSources.size());
            return configuredSources.get(index);
        }
        return normalizeSource(properties.source());
    }

    /** Divide a configuração de fontes por vírgula e remove valores vazios. */
    private List<String> parseSources(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(this::normalizeSource)
                .filter(source -> !source.isBlank())
                .toList();
    }

    /** Normaliza fonte vazia para HOTMART, mantendo compatibilidade operacional. */
    private String normalizeSource(String value) {
        if (value == null || value.isBlank()) {
            return "HOTMART";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    /** Envia ao backend o diagnóstico comercial estruturado da página analisada. */
    @Override
    public void markCompleted(StageExecution<PageAnalysisInput> execution, StageResult<PageAnalysisOutput> result) {
        SalesPageAnalysisResult analysis = result.output().analysis();
        backendClient.complete(execution.idJob(), new CompleteRequest(
                analysis.scoreTotal(),
                analysis.sectionsJson(),
                analysis.copyJson(),
                analysis.visualJson(),
                analysis.imageJson(),
                analysis.geralandingWireframeJson(),
                analysis.geralandingCopyJson(),
                analysis.geralandingImagePromptJson(),
                analysis.geralandingDesignPresetJson(),
                analysis.analysisNotes(),
                analysis.requestPayloadJson(),
                analysis.responsePayloadJson(),
                analysis.parserVersion(),
                analysis.promptVersion(),
                analysis.modelName(),
                analysis.inputTokens(),
                analysis.outputTokens(),
                analysis.modelCostUsd(),
                Instant.now()));
    }

    /** Registra no backend a falha terminal da análise comercial reservada. */
    @Override
    public void markFailed(StageExecution<PageAnalysisInput> execution, Exception error) {
        backendClient.fail(execution.idJob(), new FailRequest("PIPELINE_ERROR", error.getMessage()));
    }
}
