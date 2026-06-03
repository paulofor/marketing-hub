package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.htmlcapture;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.client.BackendClient;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.config.WorkerProperties;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.HtmlCaptureClaimRequest;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.HtmlCaptureCompleteRequest;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.HtmlCaptureFailRequest;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.StageBackendPort;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.StageExecution;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.StageResult;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Conecta a etapa htmlcapture ao backend sem acoplar o núcleo genérico a contratos HTTP concretos. */
@Component
@RequiredArgsConstructor
public class HtmlCaptureBackendPort implements StageBackendPort<HtmlCaptureInput, HtmlCaptureOutput> {

    private static final String STAGE_CODE = "HTML_CAPTURE";

    private final BackendClient backendClient;
    private final WorkerProperties properties;

    /** Reserva a próxima URL da biblioteca que precisa de HTML bruto versionado. */
    @Override
    public StageExecution<HtmlCaptureInput> claimNext() {
        var response = backendClient.claimHtmlCapture(new HtmlCaptureClaimRequest(
                properties.workspaceId(), properties.htmlCaptureLimit(), properties.htmlCaptureForce()));
        if (response == null || !response.claimed() || response.job() == null) {
            return null;
        }
        var job = response.job();
        return new StageExecution<>(job.snapshotId(), STAGE_CODE, new HtmlCaptureInput(job.pageId(), job.urlCanonical(), job.title()), Map.of());
    }

    /** Envia ao backend o HTML bruto capturado e seus metadados auditáveis. */
    @Override
    public void markCompleted(StageExecution<HtmlCaptureInput> execution, StageResult<HtmlCaptureOutput> result) {
        HtmlCaptureOutput output = result.output();
        backendClient.completeHtmlCapture(execution.idJob(), new HtmlCaptureCompleteRequest(
                output.rawHtml(), output.finalUrl(), output.httpStatus(), output.contentType(), output.sha256(), output.sizeBytes(), output.capturedAt()));
    }

    /** Envia ao backend a falha terminal da captura da URL reservada. */
    @Override
    public void markFailed(StageExecution<HtmlCaptureInput> execution, Exception error) {
        backendClient.failHtmlCapture(execution.idJob(), new HtmlCaptureFailRequest("HTML_CAPTURE_ERROR", error.getMessage()));
    }
}
