package com.marketinghub.geralanding.designpreset;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
/**
 * Monta o HTML provisório da etapa de preset de design a partir dos artefatos canônicos anteriores.
 */
public class DesignPresetProvisionalHtmlAssembler {

    private static final Logger log = LoggerFactory.getLogger(DesignPresetProvisionalHtmlAssembler.class);

    private final DesignPresetProvisionalHtmlProcessor processor;
    private final ObjectMapper objectMapper;

    public DesignPresetProvisionalHtmlAssembler(DesignPresetProvisionalHtmlProcessor processor,
                                                ObjectMapper objectMapper) {
        this.processor = processor;
        this.objectMapper = objectMapper;
    }


    /**
     * Monta o HTML provisório da etapa a partir do retorno direto do modelo.
     */
    public String assemble(String designPresetOutput, String jobId) {
        if (!StringUtils.hasText(designPresetOutput)) {
            return null;
        }
        return preserveCanonicalHtml(designPresetOutput, jobId);
    }

    /**
     * Consolida wireframe/copy com o resultado do design preset para produzir o HTML provisório da etapa.
     */
    @SuppressWarnings("unchecked")
    public String assemble(String wireframeJson,
                           String copyJson,
                           String imagePlanningJson,
                           String designPresetOutputJson,
                           String jobId) {
        if (!StringUtils.hasText(wireframeJson)
                || !StringUtils.hasText(copyJson)
                || !StringUtils.hasText(designPresetOutputJson)) {
            return null;
        }

        try {
            Map<String, Object> wireframePayload = normalizePayload(wireframeJson, "landingPageWireframe");
            Map<String, Object> copyPayload = normalizePayload(copyJson, "landingPageCopy");
            String html = processor.process(
                    objectMapper.writeValueAsString(wireframePayload),
                    objectMapper.writeValueAsString(copyPayload),
                    imagePlanningJson,
                    designPresetOutputJson);
            return preserveCanonicalHtml(html, jobId);
        } catch (Exception e) {
            String errorDetails = buildErrorDetails(e);
            log.error("Falha ao montar HTML provisório da fase landing-page-design-preset "
                            + "(jobId={}, wireframeLength={}, copyLength={}, imagePlanningLength={}, designPresetLength={}, errorDetails={})",
                    normalizeJobId(jobId),
                    lengthOf(wireframeJson),
                    lengthOf(copyJson),
                    lengthOf(imagePlanningJson),
                    lengthOf(designPresetOutputJson),
                    errorDetails,
                    e);
            throw new IllegalArgumentException(
                    "Falha ao montar HTML provisório da fase landing-page-design-preset. "
                            + "jobId=" + normalizeJobId(jobId)
                            + ", wireframeLength=" + lengthOf(wireframeJson)
                            + ", copyLength=" + lengthOf(copyJson)
                            + ", imagePlanningLength=" + lengthOf(imagePlanningJson)
                            + ", designPresetLength=" + lengthOf(designPresetOutputJson)
                            + ", errorDetails=" + errorDetails,
                    e);
        }
    }

    /**
     * Monta detalhes compactos da causa-raiz para facilitar diagnóstico operacional.
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
     * Normaliza identificador de job para mensagens e logs.
     */
    private String normalizeJobId(String jobId) {
        return StringUtils.hasText(jobId) ? jobId : "<sem-jobId>";
    }

    /**
     * Retorna o tamanho do payload para ampliar contexto de falhas.
     */
    private int lengthOf(String payload) {
        return payload == null ? 0 : payload.length();
    }

    /**
     * Normaliza o payload aceitando tanto raiz direta quanto raiz aninhada no nome canônico do artefato.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> normalizePayload(String sourceJson, String preferredRoot) throws Exception {
        Map<String, Object> root = objectMapper.readValue(sourceJson, Map.class);
        if (root.get(preferredRoot) instanceof Map<?, ?> nested) {
            return (Map<String, Object>) nested;
        }
        return new LinkedHashMap<>(root);
    }

    /**
     * Retorna o HTML sem anexar metadados técnicos para preservar aderência ao contrato canônico do artefato final.
     */
    private String preserveCanonicalHtml(String html, String jobId) {
        return html;
    }

}
