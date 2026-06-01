package com.marketinghub.geralanding.presetdesign.provisorio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Monta o HTML provisório da etapa de preset de design a partir dos artefatos canônicos anteriores.
 */
@Component
public class DesignPresetProvisionalHtmlAssembler {

    private static final Logger log = LoggerFactory.getLogger(DesignPresetProvisionalHtmlAssembler.class);

    private final DesignPresetProvisionalHtmlProcessor processor;
    private final ObjectMapper objectMapper;

    /** Inicializa o montador com o processador tokenizado e o mapper JSON usados na consolidação do HTML. */
    public DesignPresetProvisionalHtmlAssembler(
            DesignPresetProvisionalHtmlProcessor processor,
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
    public String assemble(String wireframeJson,
                           String copyJson,
                           String imagePlanningJson,
                           String designPresetOutputJson,
                           String jobId) {
        return assemble(wireframeJson, copyJson, imagePlanningJson, null, designPresetOutputJson, jobId);
    }

    /**
     * Consolida os artefatos da landing aplicando o manifesto de imagens finais antes de montar o HTML provisório.
     */
    @SuppressWarnings("unchecked")
    public String assemble(String wireframeJson,
                           String copyJson,
                           String imagePlanningJson,
                           String imageAssetsJson,
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
            String enrichedImagePlanningJson = enrichImagePlanningWithAssets(imagePlanningJson, imageAssetsJson);
            String html = processor.process(
                    objectMapper.writeValueAsString(wireframePayload),
                    objectMapper.writeValueAsString(copyPayload),
                    enrichedImagePlanningJson,
                    designPresetOutputJson);
            return preserveCanonicalHtml(html, jobId);
        } catch (Exception e) {
            String errorDetails = buildErrorDetails(e);
            log.error("Falha ao montar HTML provisório da fase landing-page-design-preset "
                            + "(jobId={}, wireframeLength={}, copyLength={}, imagePlanningLength={}, imageAssetsLength={}, designPresetLength={}, errorDetails={})",
                    normalizeJobId(jobId),
                    lengthOf(wireframeJson),
                    lengthOf(copyJson),
                    lengthOf(imagePlanningJson),
                    lengthOf(imageAssetsJson),
                    lengthOf(designPresetOutputJson),
                    errorDetails,
                    e);
            throw new IllegalArgumentException(
                    "Falha ao montar HTML provisório da fase landing-page-design-preset. "
                            + "jobId=" + normalizeJobId(jobId)
                            + ", wireframeLength=" + lengthOf(wireframeJson)
                            + ", copyLength=" + lengthOf(copyJson)
                            + ", imagePlanningLength=" + lengthOf(imagePlanningJson)
                            + ", imageAssetsLength=" + lengthOf(imageAssetsJson)
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

    /** Enriquecer o planejamento de imagens com URLs definitivas vindas do manifesto consolidado do experimento. */
    private String enrichImagePlanningWithAssets(String imagePlanningJson, String imageAssetsJson) throws Exception {
        if (!StringUtils.hasText(imagePlanningJson) || !StringUtils.hasText(imageAssetsJson)) {
            return imagePlanningJson;
        }
        JsonNode planningRoot = objectMapper.readTree(imagePlanningJson);
        if (!(planningRoot instanceof ObjectNode planningRootObject)) {
            return imagePlanningJson;
        }
        ObjectNode planningNode = resolvePlanningNode(planningRootObject);
        JsonNode imagesNode = planningNode.path("images");
        if (!(imagesNode instanceof ArrayNode planningImages)) {
            return imagePlanningJson;
        }
        Map<String, JsonNode> assetsByKey = collectImageAssetsByKey(imageAssetsJson);
        if (assetsByKey.isEmpty()) {
            return imagePlanningJson;
        }
        boolean changed = false;
        for (JsonNode imageNode : planningImages) {
            if (!(imageNode instanceof ObjectNode imageObject)) {
                continue;
            }
            JsonNode asset = resolveAssetForPlanningImage(imageObject, assetsByKey);
            String resolvedUrl = firstText(asset, "resolvedUrl", "webUrl", "sourceUrl", "imageUrl", "url", "src");
            if (!StringUtils.hasText(resolvedUrl)) {
                continue;
            }
            imageObject.put("imageUrl", resolvedUrl.trim());
            copyTextIfAbsent(imageObject, asset, "sourceUrl");
            copyTextIfAbsent(imageObject, asset, "webUrl");
            changed = true;
        }
        return changed ? objectMapper.writeValueAsString(planningRootObject) : imagePlanningJson;
    }

    /** Localiza o objeto canônico do planejamento de imagens dentro dos formatos aceitos pelo pipeline. */
    private ObjectNode resolvePlanningNode(ObjectNode planningRootObject) {
        JsonNode directPlanning = planningRootObject.path("landingPageImagePlanning");
        if (directPlanning instanceof ObjectNode objectNode) {
            return objectNode;
        }
        JsonNode imagePlan = planningRootObject.path("imagePlan");
        if (imagePlan instanceof ObjectNode objectNode) {
            return objectNode;
        }
        return planningRootObject;
    }

    /** Indexa assets do manifesto por chaves úteis para casar seção, elemento e item planejado. */
    private Map<String, JsonNode> collectImageAssetsByKey(String imageAssetsJson) throws Exception {
        JsonNode root = objectMapper.readTree(imageAssetsJson);
        JsonNode imagesNode = root.path("images");
        if (!(imagesNode instanceof ArrayNode images)) {
            return Map.of();
        }
        Map<String, JsonNode> result = new LinkedHashMap<>();
        for (JsonNode asset : images) {
            addAssetKey(result, asset.path("planningItemKey").asText(null), asset);
            addAssetKey(result, asset.path("sectionId").asText(null), asset);
            addAssetKey(result, asset.path("elementId").asText(null), asset);
        }
        return result;
    }

    /** Registra uma chave normalizada do manifesto quando ela estiver preenchida. */
    private void addAssetKey(Map<String, JsonNode> result, String rawKey, JsonNode asset) {
        String key = normalizeAssetKey(rawKey);
        if (StringUtils.hasText(key)) {
            result.putIfAbsent(key, asset);
        }
    }

    /** Resolve o asset correspondente a uma imagem planejada por elementId, sectionId ou planningItemKey. */
    private JsonNode resolveAssetForPlanningImage(ObjectNode imageObject, Map<String, JsonNode> assetsByKey) {
        String[] candidateFields = {"elementId", "sectionId", "planningItemKey", "imageBindingKey", "slotId"};
        for (String field : candidateFields) {
            JsonNode asset = assetsByKey.get(normalizeAssetKey(imageObject.path(field).asText(null)));
            if (asset != null) {
                return asset;
            }
        }
        return null;
    }

    /** Retorna o primeiro campo textual preenchido dentro de um nó JSON. */
    private String firstText(JsonNode node, String... fields) {
        if (node == null) {
            return null;
        }
        for (String field : fields) {
            String value = node.path(field).asText(null);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    /** Copia metadado textual do asset para o planejamento sem sobrescrever valores já definidos. */
    private void copyTextIfAbsent(ObjectNode target, JsonNode source, String fieldName) {
        if (StringUtils.hasText(target.path(fieldName).asText(null))) {
            return;
        }
        String value = firstText(source, fieldName);
        if (StringUtils.hasText(value)) {
            target.put(fieldName, value.trim());
        }
    }

    /** Normaliza chaves de casamento entre planejamento e manifesto de imagens. */
    private String normalizeAssetKey(String rawKey) {
        return rawKey == null ? "" : rawKey.trim().toLowerCase();
    }

    /**
     * Retorna o HTML sem anexar metadados técnicos para preservar aderência ao contrato canônico do artefato final.
     */
    private String preserveCanonicalHtml(String html, String jobId) {
        return html;
    }

}
