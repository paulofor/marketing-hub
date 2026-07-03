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
    public record DossierPendingJob(
            String jobId,
            long stageExecutionId,
            long dossierId,
            String workspaceId,
            String stageName,
            Map<String, Object> input,
            StageContext.PromptSchemaTemplate promptSchemaTemplate) {
        /** Converte o contrato HTTP em contexto do núcleo do pipeline v1. */
        public StageContext toStageContext() {
            return new StageContext(stageExecutionId, dossierId, workspaceId, stageName, input == null ? Map.of() : input,
                    promptSchemaTemplate);
        }

        /** Resolve o identificador externo usado pelos callbacks do backend. */
        public String idExterno() {
            Object productKey = input == null ? null : input.get("productKey");
            return productKey == null ? String.valueOf(dossierId) : String.valueOf(productKey);
        }
    }

    /** Representa o payload de auditoria do request enviado ao backend. */
    public record DossierRecebeRequestRequest(
            String request,
            String plataforma,
            String prompt,
            String schema,
            String promptTemplateKey,
            String promptTemplateVersion,
            String schemaName) {

        /** Cria auditoria de request com vínculo ao template recebido do backend. */
        public static DossierRecebeRequestRequest fromTemplate(
                String request,
                String platform,
                StageContext.PromptSchemaTemplate template) {
            return new DossierRecebeRequestRequest(
                    request,
                    platform,
                    template == null ? null : template.promptMarkdownContent(),
                    template == null ? null : template.schemaJson(),
                    template == null ? null : template.templateKey(),
                    template == null ? null : template.version(),
                    template == null ? null : template.schemaName());
        }
    }

    /** Representa o payload de auditoria da resposta funcional enviada ao backend. */
    public record DossierRecebeResponseRequest(
            String response,
            Integer quantidadeTokenEntrada,
            Integer quantidadeTokenSaida,
            BigDecimal custo,
            String modelo,
            String descricaoErro,
            String promptTemplateKey,
            String promptTemplateVersion,
            String schemaName) {
        /** Cria a resposta de sucesso a partir do resultado da etapa. */
        public static DossierRecebeResponseRequest success(String response) {
            return success(response, null);
        }

        /** Cria a resposta de sucesso local com vínculo ao template quando existir. */
        public static DossierRecebeResponseRequest success(String response, StageContext.PromptSchemaTemplate template) {
            return new DossierRecebeResponseRequest(response, null, null, null, "mois-dossie-v1-local", null,
                    templateKey(template), templateVersion(template), schemaName(template));
        }

        /** Cria a resposta bruta de uma interação OpenAI preservando tokens, custo, modelo e erro informados pela etapa. */
        public static DossierRecebeResponseRequest openAi(OpenAiInteraction interaction) {
            return openAi(interaction, null);
        }

        /** Cria a resposta bruta de OpenAI com vínculo ao template usado na chamada. */
        public static DossierRecebeResponseRequest openAi(OpenAiInteraction interaction, StageContext.PromptSchemaTemplate template) {
            return new DossierRecebeResponseRequest(
                    interaction.rawResponseReceived(),
                    interaction.quantidadeTokenEntrada(),
                    interaction.quantidadeTokenSaida(),
                    interaction.custo(),
                    interaction.modelo(),
                    interaction.descricaoErro(),
                    templateKey(template),
                    templateVersion(template),
                    schemaName(template));
        }

        /** Cria a resposta de falha operacional persistível. */
        public static DossierRecebeResponseRequest failure(String errorMessage) {
            return failure(errorMessage, null);
        }

        /** Cria a resposta de falha operacional persistível com vínculo ao template quando existir. */
        public static DossierRecebeResponseRequest failure(String errorMessage, StageContext.PromptSchemaTemplate template) {
            return new DossierRecebeResponseRequest(null, null, null, null, "mois-dossie-v1-local", errorMessage,
                    templateKey(template), templateVersion(template), schemaName(template));
        }

        /** Lê a chave do template preservando nulo para etapas sem IA. */
        private static String templateKey(StageContext.PromptSchemaTemplate template) {
            return template == null ? null : template.templateKey();
        }

        /** Lê a versão do template preservando nulo para etapas sem IA. */
        private static String templateVersion(StageContext.PromptSchemaTemplate template) {
            return template == null ? null : template.version();
        }

        /** Lê o nome do schema preservando nulo para etapas sem IA. */
        private static String schemaName(StageContext.PromptSchemaTemplate template) {
            return template == null ? null : template.schemaName();
        }
    }

    /** Serializa de forma simples o resultado da etapa para o contrato textual do backend. */
    public String responseFrom(StageResult result) {
        return String.valueOf(result.output());
    }
}
