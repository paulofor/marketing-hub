package com.marketinghub.geralanding.designpreset;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import jakarta.persistence.EntityNotFoundException;
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
    private final ExperimentRepository experimentRepository;

    public DesignPresetProvisionalHtmlAssembler(DesignPresetProvisionalHtmlProcessor processor,
                                                ObjectMapper objectMapper,
                                                ExperimentRepository experimentRepository) {
        this.processor = processor;
        this.objectMapper = objectMapper;
        this.experimentRepository = experimentRepository;
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
     * Recupera os artefatos do experimento e monta o HTML final do preset de design.
     */
    public String assemble(Long experimentId, String designPresetOutput, String jobId) {
        if (experimentId == null || !StringUtils.hasText(designPresetOutput)) {
            return null;
        }
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new EntityNotFoundException("Experiment not found: " + experimentId));
        return assemble(
                experiment.getLandingPageWireframe(),
                experiment.getLandingPageCopy(),
                experiment.getLandingPageImagePlanning(),
                designPresetOutput,
                jobId);
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
