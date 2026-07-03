package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service.DossierV1BackendClient.DossierPendingRequest;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service.DossierV1BackendClient.DossierPendingResponse;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service.DossierV1BackendClient.DossierRecebeRequestRequest;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service.DossierV1BackendClient.DossierRecebeResponseRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Executa os contratos HTTP canônicos do pipeline salespagepatterns.v1 no backend principal. */
@Component
@Slf4j
public class SalesPagePatternsV1BackendClient {
    private final RestClient restClient;

    /** Cria o cliente usando o RestClient configurado com a URL base do backend. */
    public SalesPagePatternsV1BackendClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /** Busca trabalhos pendentes da etapa informada no endpoint pending canônico. */
    public DossierPendingResponse pending(String stageName, DossierPendingRequest request) {
        log.info("MOIS salespagepatterns.v1 calling backend pending endpoint. stageName={}, workspaceId={}, workerId={}, limit={}",
                stageName, request.workspaceId(), request.workerId(), request.limit());
        return restClient.post()
                .uri("/api/internal/moissaleslibraryworker/salespagepatterns/v1/{stageName}/stage-executions/pending", stageName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(DossierPendingResponse.class);
    }

    /** Registra no backend o request bruto processado pelo worker. */
    public void recebeRequest(String stageName, String idExterno, String jobId, DossierRecebeRequestRequest request) {
        log.info("MOIS salespagepatterns.v1 sending recebeRequest. stageName={}, idExterno={}, jobId={}",
                stageName, idExterno, jobId);
        restClient.post()
                .uri("/api/internal/moissaleslibraryworker/salespagepatterns/v1/{stageName}/stage-executions/{idExterno}/{jobId}/recebeRequest",
                        stageName, idExterno, jobId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    /** Registra no backend a resposta funcional e auditável processada pelo worker. */
    public void recebeResponse(String stageName, String idExterno, String jobId, DossierRecebeResponseRequest request) {
        log.info("MOIS salespagepatterns.v1 sending recebeResponse. stageName={}, idExterno={}, jobId={}",
                stageName, idExterno, jobId);
        restClient.post()
                .uri("/api/internal/moissaleslibraryworker/salespagepatterns/v1/{stageName}/stage-executions/{idExterno}/{jobId}/recebeResponse",
                        stageName, idExterno, jobId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}
