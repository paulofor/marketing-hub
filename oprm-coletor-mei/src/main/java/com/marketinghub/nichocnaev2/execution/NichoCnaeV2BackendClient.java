package com.marketinghub.nichocnaev2.execution;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/** Cliente HTTP do executor NichoCNAE v2 para consumir pendências e reportar resultados ao backend. */
@Component
public class NichoCnaeV2BackendClient {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String backendBaseUrl;

    /** Inicializa o cliente com URL base do backend e mapper para contratos estruturados. */
    public NichoCnaeV2BackendClient(
            RestTemplateBuilder builder,
            ObjectMapper objectMapper,
            @Value("${backend.base-url:http://191.252.181.168}") String backendBaseUrl) {
        this.restTemplate = builder.build();
        this.objectMapper = objectMapper;
        this.backendBaseUrl = backendBaseUrl;
    }

    /** Consulta o endpoint pending da etapa e converte cada item para a unidade operacional genérica. */
    public List<NichoCnaeV2PendingExecution> listPending(NichoCnaeV2StageDefinition stage) {
        String url = backendBaseUrl + stage.backendPath() + "/pending";
        Map<String, Object>[] response = restTemplate.getForObject(url, Map[].class);
        return response == null ? List.of() : Arrays.stream(response).map(this::toPendingExecution).toList();
    }

    /** Registra no backend a conclusão da etapa processada pelo executor. */
    public void complete(NichoCnaeV2StageDefinition stage, NichoCnaeV2PendingExecution pending, Map<String, Object> request) {
        restTemplate.postForObject(
                backendBaseUrl + stage.backendPath() + "/" + pending.stageExecutionId() + "/complete",
                request,
                Object.class);
    }

    /** Registra no backend a próxima etapa pendente quando o contrato da etapa informar avanço operacional. */
    public void createNextStage(NichoCnaeV2StageDefinition nextStage, NichoCnaeV2PendingExecution pending, String inputPayload) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("jobId", pending.jobId());
        request.put("researchCycleId", pending.researchCycleId());
        request.put("sourceNicheId", pending.sourceNicheId());
        request.put("cnaeCode", pending.cnaeCode());
        request.put("attemptNumber", pending.attemptNumber());
        request.put("knowledgeVersion", pending.knowledgeVersion());
        request.put("materializationEnabled", pending.materializationEnabled());
        request.put("inputPayload", inputPayload);
        restTemplate.postForObject(backendBaseUrl + nextStage.backendPath(), request, Object.class);
    }

    /** Registra falha classificada pelo executor externo com contexto suficiente para diagnóstico no backend. */
    public void fail(
            NichoCnaeV2StageDefinition stage,
            NichoCnaeV2PendingExecution pending,
            RuntimeException ex,
            String failureType,
            String reasonCode) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("failureType", failureType);
        request.put("reasonCode", reasonCode);
        request.put("errorCode", reasonCode);
        request.put("errorMessage", detailedErrorMessage(stage, pending, ex, reasonCode));
        request.put("inputPayload", pending.inputPayload());
        restTemplate.postForObject(
                backendBaseUrl + stage.backendPath() + "/" + pending.stageExecutionId() + "/fail",
                request,
                Object.class);
    }

    /** Monta erro persistível com ponto de falha e stack trace para não perder a origem de NPE ou falhas similares. */
    static String detailedErrorMessage(
            NichoCnaeV2StageDefinition stage, NichoCnaeV2PendingExecution pending, RuntimeException ex, String reasonCode) {
        StringWriter stackTrace = new StringWriter();
        ex.printStackTrace(new PrintWriter(stackTrace));
        StackTraceElement failurePoint = firstApplicationFrame(ex);
        String message = ex.getMessage() == null || ex.getMessage().isBlank()
                ? ex.getClass().getSimpleName()
                : ex.getClass().getSimpleName() + ": " + ex.getMessage();
        return "reasonCode="
                + reasonCode
                + "; stage="
                + stage.stageCode()
                + "; stageExecutionId="
                + pending.stageExecutionId()
                + "; jobId="
                + pending.jobId()
                + "; cnaeCode="
                + pending.cnaeCode()
                + "; exception="
                + message
                + "; failurePoint="
                + failurePoint
                + "\n"
                + stackTrace;
    }

    /** Localiza o primeiro frame da aplicação para apontar rapidamente onde a falha nasceu. */
    private static StackTraceElement firstApplicationFrame(RuntimeException ex) {
        for (StackTraceElement frame : ex.getStackTrace()) {
            if (frame.getClassName().startsWith("com.marketinghub.")) {
                return frame;
            }
        }
        return ex.getStackTrace().length == 0 ? null : ex.getStackTrace()[0];
    }

    /** Serializa payload estruturado para armazenamento funcional no backend. */
    public String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao serializar payload NichoCNAE v2.", ex);
        }
    }

    /** Converte JSON textual recebido no pending para mapa estruturado usado pelo processor. */
    public Map<String, Object> parseInput(String inputPayload) {
        if (inputPayload == null || inputPayload.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(inputPayload, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Payload de entrada NichoCNAE v2 não é JSON estruturado válido.", ex);
        }
    }

    /** Converte mapa bruto do RestTemplate para contrato interno preservando o payload original auditável. */
    private NichoCnaeV2PendingExecution toPendingExecution(Map<String, Object> raw) {
        return new NichoCnaeV2PendingExecution(
                text(raw.get("stageExecutionId")),
                text(raw.get("jobId")),
                text(raw.get("cnaeCode")),
                text(raw.get("cnaeDescription")),
                longValue(raw.get("researchCycleId")),
                longValue(raw.get("sourceNicheId")),
                intValue(raw.get("attemptNumber")),
                intValue(raw.get("technicalRetryNumber")),
                intValue(raw.get("knowledgeVersion")),
                booleanValue(raw.get("materializationEnabled")),
                text(raw.get("inputPayload")),
                new LinkedHashMap<>(raw));
    }

    /** Extrai texto nullable sem quebrar campos ausentes. */
    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /** Extrai inteiro nullable aceitando números ou strings numéricas. */
    private Integer intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return value == null || String.valueOf(value).isBlank() ? null : Integer.valueOf(String.valueOf(value));
    }

    /** Extrai long nullable aceitando números ou strings numéricas. */
    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value == null || String.valueOf(value).isBlank() ? null : Long.valueOf(String.valueOf(value));
    }

    /** Extrai boolean nullable aceitando boolean ou string. */
    private Boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value == null || String.valueOf(value).isBlank() ? null : Boolean.valueOf(String.valueOf(value));
    }
}
