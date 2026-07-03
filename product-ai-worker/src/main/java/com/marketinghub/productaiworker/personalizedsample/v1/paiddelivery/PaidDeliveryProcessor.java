package com.marketinghub.productaiworker.personalizedsample.v1.paiddelivery;

import com.marketinghub.productaiworker.core.StageContext;
import com.marketinghub.productaiworker.core.StageProcessor;
import com.marketinghub.productaiworker.infra.ProductAiBackendClient;
import com.marketinghub.productaiworker.infra.ProductAiOpenAiClient;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Responsabilidade: executar a entrega paga do pipeline personalizedsample.v1. */
@Component
public class PaidDeliveryProcessor implements StageProcessor {
    private static final String PIPELINE_CODE = "personalizedsample.v1";
    private static final String STAGE_CODE = "paid-delivery";

    private final ProductAiBackendClient backendClient;
    private final ProductAiOpenAiClient openAiClient;

    /** Inicializa processor com clientes de backend e OpenAI. */
    public PaidDeliveryProcessor(ProductAiBackendClient backendClient, ProductAiOpenAiClient openAiClient) {
        this.backendClient = backendClient;
        this.openAiClient = openAiClient;
    }

    /** Retorna o código do pipeline executado por esta etapa. */
    @Override
    public String pipelineCode() {
        return PIPELINE_CODE;
    }

    /** Retorna o código da etapa executada. */
    @Override
    public String stageCode() {
        return STAGE_CODE;
    }

    /** Processa uma entrega paga personalizada usando prompt/schema recebidos do backend. */
    @Override
    public void process(StageContext context) {
        String prompt = renderPrompt(context);
        String schemaJson = stringTemplateValue(context, "schemaJson");
        String model = stringTemplateValue(context, "model");
        try {
            ProductAiOpenAiClient.OpenAiCallResult result = openAiClient.generate(
                    model,
                    prompt,
                    schemaJson,
                    (tier, requestBody) -> backendClient.receiveRequest(context.idJob(), Map.of(
                            "prompt", prompt,
                            "schemaJson", schemaJson,
                            "requestBodyJson", backendClient.toJson(requestBody),
                            "openAiModel", model,
                            "serviceTier", tier)));
            ProductAiOpenAiClient.OpenAiResponse response = result.response();
            backendClient.receiveResponse(context.idJob(), responsePayload(model, result.serviceTier(), response));
        } catch (Exception ex) {
            Map<String, Object> errorPayload = new LinkedHashMap<>();
            errorPayload.put("errorMessage", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
            backendClient.receiveResponse(context.idJob(), errorPayload);
        }
    }

    /** Renderiza prompt final com dados comerciais e respostas do usuário. */
    private String renderPrompt(StageContext context) {
        return stringTemplateValue(context, "promptMarkdownContent")
                + "\n\nExperimento:\n" + backendClient.toJson(context.experiment())
                + "\n\nComprador:\n" + backendClient.toJson(context.buyer())
                + "\n\nDados de personalização coletados no funil:\n"
                + backendClient.toJson(context.personalizationInput());
    }

    /** Monta payload de resposta para o backend. */
    private Map<String, Object> responsePayload(
            String model,
            String serviceTier,
            ProductAiOpenAiClient.OpenAiResponse response) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("responseBodyJson", backendClient.toJson(response));
        payload.put("functionalOutputJson", response != null ? response.outputText() : null);
        payload.put("artifactUrl", null);
        payload.put("openAiModel", model);
        payload.put("serviceTier", serviceTier);
        payload.put("inputTokens", response != null && response.usage() != null ? response.usage().inputTokens() : null);
        payload.put("outputTokens", response != null && response.usage() != null ? response.usage().outputTokens() : null);
        return payload;
    }

    /** Lê valor textual do template enviado pelo backend. */
    private String stringTemplateValue(StageContext context, String key) {
        Object value = context.promptSchemaTemplate() != null ? context.promptSchemaTemplate().get(key) : null;
        return value != null ? value.toString() : "";
    }
}
