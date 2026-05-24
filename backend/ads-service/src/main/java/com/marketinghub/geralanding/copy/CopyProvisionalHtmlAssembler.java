package com.marketinghub.geralanding.copy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
/**
 * Conjunto exclusivo da etapa LANDING_PAGE_COPY: coordena o payload resolver e o processor
 * da etapa de copy para gerar HTML provisório sem invadir responsabilidades das demais etapas.
 */
public class CopyProvisionalHtmlAssembler {
    private static final Logger log = LoggerFactory.getLogger(CopyProvisionalHtmlAssembler.class);

    private final CopyProvisionalHtmlPayloadResolver payloadResolver;
    private final CopyProvisionalHtmlProcessor processor;
    private final ObjectMapper objectMapper;

    public CopyProvisionalHtmlAssembler(CopyProvisionalHtmlPayloadResolver payloadResolver,
                                        CopyProvisionalHtmlProcessor processor,
                                        ObjectMapper objectMapper) {
        this.payloadResolver = payloadResolver;
        this.processor = processor;
        this.objectMapper = objectMapper;
    }

    /**
     * Orquestra o resolver+processor para montar HTML provisório da etapa copy.
     */
    public String assemble(String copyModelResponse, String wireframeModelResponse, String jobId) {
        if (!StringUtils.hasText(copyModelResponse) || !StringUtils.hasText(wireframeModelResponse)) {
            log.warn(
                    "Montagem de HTML provisório ignorada por payload ausente (jobId={}, copyLength={}, wireframeLength={})",
                    normalizeJobId(jobId),
                    lengthOf(copyModelResponse),
                    lengthOf(wireframeModelResponse));
            return null;
        }
        log.info(
                "Iniciando montagem de HTML provisório da etapa copy (jobId={}, copyLength={}, wireframeLength={}, copyPreview={}, wireframePreview={})",
                normalizeJobId(jobId),
                lengthOf(copyModelResponse),
                lengthOf(wireframeModelResponse),
                preview(copyModelResponse),
                preview(wireframeModelResponse));
        try {
            CopyProvisionalHtmlPayloadResolver.CopyProvisionalHtmlPayload payload =
                    payloadResolver.resolve(copyModelResponse, wireframeModelResponse);

            String wireframeJson = payloadResolverToJson(payload.wireframe());
            String copyJson = payloadResolverToJson(payload.copy());
            log.info(
                    "Payload resolvido para processamento copy (jobId={}, resolvedWireframeLength={}, resolvedCopyLength={}, wireframeKeys={}, copyKeys={})",
                    normalizeJobId(jobId),
                    lengthOf(wireframeJson),
                    lengthOf(copyJson),
                    payload.wireframe() == null ? 0 : payload.wireframe().size(),
                    payload.copy() == null ? 0 : payload.copy().size());

            String html = processor.process(wireframeJson, copyJson);
            log.info(
                    "HTML provisório da etapa copy montado com sucesso (jobId={}, htmlLength={})",
                    normalizeJobId(jobId),
                    lengthOf(html));
            return html;
        } catch (Exception e) {
            String errorDetails = buildErrorDetails(e);
            log.error(
                    "Falha ao montar HTML provisório (jobId={}, copyLength={}, wireframeLength={}, copyPreview={}, wireframePreview={}, errorDetails={})",
                    jobId,
                    lengthOf(copyModelResponse),
                    lengthOf(wireframeModelResponse),
                    preview(copyModelResponse),
                    preview(wireframeModelResponse),
                    errorDetails,
                    e);
            throw new IllegalArgumentException(
                    "Falha ao montar HTML provisório a partir de wireframe + copy. "
                            + "jobId=" + normalizeJobId(jobId)
                            + ", copyLength=" + lengthOf(copyModelResponse)
                            + ", wireframeLength=" + lengthOf(wireframeModelResponse)
                            + ", errorDetails=" + errorDetails,
                    e);
        }
    }

    /**
     * Monta detalhes da exceção com causa-raiz para acelerar o diagnóstico de erros.
     */
    private String buildErrorDetails(Exception ex) {
        Throwable rootCause = ex;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }
        String rootMessage = StringUtils.hasText(rootCause.getMessage()) ? rootCause.getMessage() : "<sem-mensagem>";
        return rootCause.getClass().getSimpleName() + ": " + rootMessage.replaceAll("\\s+", " ").trim();
    }

    /**
     * Normaliza o jobId para logs e mensagens de erro.
     */
    private String normalizeJobId(String jobId) {
        return StringUtils.hasText(jobId) ? jobId : "<sem-jobId>";
    }

    /**
     * Retorna um preview seguro para logs de payloads longos.
     */
    private String preview(String payload) {
        if (!StringUtils.hasText(payload)) {
            return "";
        }
        return payload.substring(0, Math.min(payload.length(), 160)).replaceAll("\\s+", " ");
    }

    /**
     * Retorna o tamanho do texto para ajudar no diagnóstico do payload.
     */
    private int lengthOf(String payload) {
        return payload == null ? 0 : payload.length();
    }

    /**
     * Serializa o payload resolvido para JSON válido antes do processamento.
     */
    private String payloadResolverToJson(Object payload) throws Exception {
        return objectMapper.writeValueAsString(payload);
    }

}
