package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service;

import com.marketinghub.pipelines.dossie.v1.StageContext;
import com.marketinghub.pipelines.dossie.v1.StageResult;
import com.marketinghub.pipelines.dossie.v1.StageResult.OpenAiInteraction;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Executa os contratos HTTP canônicos do pipeline de dossiê MOIS v1 no backend principal. */
@Component
@Slf4j
public class DossierV1BackendClient {
    private final RestClient restClient;

    /** Cria o cliente do pipeline usando o RestClient configurado com a URL base do backend. */
    public DossierV1BackendClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /** Busca trabalhos pendentes da etapa informada no endpoint pending canônico do backend. */
    public DossierPendingResponse pending(String stageName, DossierPendingRequest request) {
        log.info("MOIS dossie v1 calling backend pending endpoint. stageName={}, workspaceId={}, workerId={}, limit={}",
                stageName, request.workspaceId(), request.workerId(), request.limit());
        DossierPendingResponse response = restClient.post()
                .uri("/api/internal/moissaleslibraryworker/dossieproduto/v1/{stageName}/stage-executions/pending", stageName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(DossierPendingResponse.class);
        log.info("MOIS dossie v1 pending response received. stageName={}, claimed={}, jobs={}",
                stageName, response != null && response.claimed(), response == null || response.jobs() == null ? 0 : response.jobs().size());
        return response;
    }

    /** Registra no backend o request recebido pelo executor antes de processar a etapa. */
    public void recebeRequest(String stageName, String idExterno, String jobId, DossierRecebeRequestRequest request) {
        log.info("MOIS dossie v1 sending recebeRequest. stageName={}, idExterno={}, jobId={}, payload={}",
                stageName, idExterno, jobId, request);
        restClient.post()
                .uri("/api/internal/moissaleslibraryworker/dossieproduto/v1/{stageName}/stage-executions/{idExterno}/{jobId}/recebeRequest",
                        stageName, idExterno, jobId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    /** Registra no backend o resultado funcional da etapa executada pelo worker. */
    public void recebeResponse(String stageName, String idExterno, String jobId, DossierRecebeResponseRequest request) {
        log.info("MOIS dossie v1 sending recebeResponse. stageName={}, idExterno={}, jobId={}, payload={}",
                stageName, idExterno, jobId, request);
        restClient.post()
                .uri("/api/internal/moissaleslibraryworker/dossieproduto/v1/{stageName}/stage-executions/{idExterno}/{jobId}/recebeResponse",
                        stageName, idExterno, jobId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    /** Representa a solicitação do endpoint pending do pipeline v1. */
    public record DossierPendingRequest(String workspaceId, String workerId, Integer limit) {}

    /** Representa a resposta do endpoint pending do pipeline v1. */
    public record DossierPendingResponse(boolean claimed, List<DossierPendingJob> jobs) {}

    /** Representa um trabalho de etapa pendente entregue pelo backend ao executor. */
    public record DossierPendingJob(String jobId, long stageExecutionId, long dossierId, String workspaceId, String stageName, Map<String, Object> input) {
        /** Converte o contrato HTTP em contexto do núcleo do pipeline v1. */
        public StageContext toStageContext() {
            return new StageContext(stageExecutionId, dossierId, workspaceId, stageName, input == null ? Map.of() : input);
        }

        /** Resolve o identificador externo usado pelos callbacks do backend. */
        public String idExterno() {
            Object productKey = input == null ? null : input.get("productKey");
            return productKey == null ? String.valueOf(dossierId) : String.valueOf(productKey);
        }
    }

    /** Representa o payload de auditoria do request enviado ao backend. */
    public record DossierRecebeRequestRequest(String request, String plataforma, String prompt, String schema) {}

    /** Representa o payload de auditoria da resposta funcional enviada ao backend. */
    public record DossierRecebeResponseRequest(String response, Integer quantidadeTokenEntrada, Integer quantidadeTokenSaida, BigDecimal custo, String modelo, String descricaoErro) {
        /** Cria a resposta de sucesso a partir do resultado da etapa. */
        public static DossierRecebeResponseRequest success(String response) {
            return new DossierRecebeResponseRequest(response, null, null, null, "mois-dossie-v1-local", null);
        }

        /** Cria a resposta bruta de uma interação OpenAI preservando tokens, custo, modelo e erro informados pela etapa. */
        public static DossierRecebeResponseRequest openAi(OpenAiInteraction interaction) {
            return new DossierRecebeResponseRequest(
                    interaction.rawResponseReceived(),
                    interaction.quantidadeTokenEntrada(),
                    interaction.quantidadeTokenSaida(),
                    interaction.custo(),
                    interaction.modelo(),
                    interaction.descricaoErro());
        }

        /** Cria a resposta de falha operacional persistível. */
        public static DossierRecebeResponseRequest failure(String errorMessage) {
            return new DossierRecebeResponseRequest(null, null, null, null, "mois-dossie-v1-local", errorMessage);
        }
    }

    /** Serializa de forma simples o resultado da etapa para o contrato textual do backend. */
    public String responseFrom(StageResult result) {
        return String.valueOf(result.output());
    }
}
