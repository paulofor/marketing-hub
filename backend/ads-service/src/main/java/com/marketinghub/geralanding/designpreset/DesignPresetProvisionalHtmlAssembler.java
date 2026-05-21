package com.marketinghub.geralanding.designpreset;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
/**
 * Monta o HTML provisório da etapa de preset de design a partir dos artefatos canônicos anteriores.
 */
public class DesignPresetProvisionalHtmlAssembler {

    private final DesignPresetProvisionalHtmlProcessor processor;
    private final ObjectMapper objectMapper;

    public DesignPresetProvisionalHtmlAssembler(DesignPresetProvisionalHtmlProcessor processor, ObjectMapper objectMapper) {
        this.processor = processor;
        this.objectMapper = objectMapper;
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
            throw new IllegalArgumentException("Falha ao montar HTML provisório da fase landing-page-design-preset", e);
        }
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
