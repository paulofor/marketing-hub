package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.client;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Encapsula chamadas HTTP do worker MOIS para o backend principal.
 */
@Component
@Slf4j
public class BackendClient {
    private final RestClient restClient;

    /**
     * Cria o cliente usando o RestClient configurado com a URL base do backend.
     */
    public BackendClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Reserva um job de análise de página de vendas na biblioteca.
     */
    public ClaimResponse claim(ClaimRequest request) {
        log.info("MOIS sales-library worker calling backend claim endpoint. workspaceId={}, source={}", request.workspaceId(), request.source());
        ClaimResponse response = restClient.post()
                .uri("/api/mois/sales-library/jobs:claim")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ClaimResponse.class);
        log.info("MOIS sales-library worker claim response received. claimed={}, hasJob={}",
                response != null && response.claimed(),
                response != null && response.job() != null);
        return response;
    }

    /**
     * Reserva uma referência coletada para captura de HTML bruto.
     */
    public CollectedReferenceHtmlClaimResponse claimCollectedReferenceHtml(CollectedReferenceHtmlClaimRequest request) {
        log.info("MOIS raw-html worker calling backend claim endpoint. workspaceId={}, source={}", request.workspaceId(), request.source());
        CollectedReferenceHtmlClaimResponse response = restClient.post()
                .uri("/api/mois/sales-library/collected-reference-html:claim")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(CollectedReferenceHtmlClaimResponse.class);
        log.info("MOIS raw-html worker claim response received. claimed={}, hasJob={}",
                response != null && response.claimed(),
                response != null && response.job() != null);
        return response;
    }

    /**
     * Persiste no backend o HTML bruto capturado de uma referência coletada.
     */
    public void completeCollectedReferenceHtml(long captureId, CollectedReferenceHtmlCompleteRequest request) {
        log.info("MOIS raw-html worker calling backend complete endpoint. captureId={}, httpStatus={}, bytes={}",
                captureId, request.httpStatus(), request.rawHtml() == null ? 0 : request.rawHtml().length());
        var entity = restClient.post()
                .uri("/api/mois/sales-library/collected-reference-html/{captureId}:complete", captureId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
        log.info("MOIS raw-html worker complete response received. captureId={}, status={}", captureId, entity.getStatusCode());
    }

    /**
     * Registra no backend uma falha na captura de HTML bruto.
     */
    public void failCollectedReferenceHtml(long captureId, CollectedReferenceHtmlFailRequest request) {
        log.warn("MOIS raw-html worker calling backend fail endpoint. captureId={}, errorCategory={}, errorMessage={}",
                captureId, request.errorCategory(), request.errorMessage());
        var entity = restClient.post()
                .uri("/api/mois/sales-library/collected-reference-html/{captureId}:fail", captureId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
        log.info("MOIS raw-html worker fail response received. captureId={}, status={}", captureId, entity.getStatusCode());
    }

    /**
     * Reserva no backend uma URL normalizada da biblioteca para captura de HTML bruto versionado.
     */
    public HtmlCaptureClaimResponse claimHtmlCapture(HtmlCaptureClaimRequest request) {
        log.info("MOIS htmlcapture worker calling backend claim endpoint. workspaceId={}, limit={}, force={}",
                request.workspaceId(), request.limit(), request.force());
        HtmlCaptureClaimResponse response = restClient.post()
                .uri("/api/mois/sales-library/html-captures:claim")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(HtmlCaptureClaimResponse.class);
        log.info("MOIS htmlcapture worker claim response received. claimed={}, hasJob={}",
                response != null && response.claimed(),
                response != null && response.job() != null);
        return response;
    }

    /**
     * Persiste no backend o HTML bruto versionado capturado para uma página da biblioteca.
     */
    public void completeHtmlCapture(long snapshotId, HtmlCaptureCompleteRequest request) {
        log.info("MOIS htmlcapture worker calling backend complete endpoint. snapshotId={}, httpStatus={}, bytes={}, sha256={}",
                snapshotId, request.httpStatus(), request.sizeBytes(), request.sha256());
        var entity = restClient.post()
                .uri("/api/mois/sales-library/html-captures/{snapshotId}:complete", snapshotId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
        log.info("MOIS htmlcapture worker complete response received. snapshotId={}, status={}", snapshotId, entity.getStatusCode());
    }

    /**
     * Registra no backend uma falha de captura de HTML bruto para uma página da biblioteca.
     */
    public void failHtmlCapture(long snapshotId, HtmlCaptureFailRequest request) {
        log.warn("MOIS htmlcapture worker calling backend fail endpoint. snapshotId={}, errorCategory={}, errorMessage={}",
                snapshotId, request.errorCategory(), request.errorMessage());
        var entity = restClient.post()
                .uri("/api/mois/sales-library/html-captures/{snapshotId}:fail", snapshotId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
        log.info("MOIS htmlcapture worker fail response received. snapshotId={}, status={}", snapshotId, entity.getStatusCode());
    }

    /**
     * Persiste no backend o resultado final de análise de um job.
     */
    public void complete(long jobId, CompleteRequest request) {
        log.info("MOIS sales-library worker calling backend complete endpoint. jobId={}, scoreTotal={}", jobId, request.scoreTotal());
        var entity = restClient.post()
                .uri("/api/mois/sales-library/jobs/{jobId}:complete", jobId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
        log.info("MOIS sales-library worker complete response received. jobId={}, status={}", jobId, entity.getStatusCode());
    }

    /**
     * Registra no backend uma falha terminal de análise de job.
     */
    public void fail(long jobId, FailRequest request) {
        log.warn("MOIS sales-library worker calling backend fail endpoint. jobId={}, errorCategory={}, errorMessage={}",
                jobId, request.errorCategory(), request.errorMessage());
        var entity = restClient.post()
                .uri("/api/mois/sales-library/jobs/{jobId}:fail", jobId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
        log.info("MOIS sales-library worker fail response received. jobId={}, status={}", jobId, entity.getStatusCode());
    }
}
