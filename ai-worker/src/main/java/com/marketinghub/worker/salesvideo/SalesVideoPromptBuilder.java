package com.marketinghub.worker.salesvideo;

import com.marketinghub.product.dto.ProductDto;
import com.marketinghub.salesvideo.dto.SalesVideoProfileDto;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.util.StreamUtils;

/**
 * Constrói o prompt usado na geração de scripts de vídeo.
 */
@Component
public class SalesVideoPromptBuilder {
    private static final String TEMPLATE_PATH = "prompts/salesvideo/sales-video-script.md";

    /** Monta o prompt final de roteiro a partir do template versionado e do contexto comercial. */
    public String buildPrompt(SalesVideoProfileDto profile, ProductDto product) {
        String language = profile != null && StringUtils.hasText(profile.getLanguage())
                ? profile.getLanguage()
                : "pt-BR";
        StringBuilder context = new StringBuilder();
        context.append("- Idioma do vídeo: ").append(language).append('\n');
        if (profile != null && profile.getVideoKind() != null) {
            context.append("- Tipo do vídeo: ").append(profile.getVideoKind()).append('\n');
        }
        if (profile != null && profile.getTargetDurationSeconds() != null) {
            context.append("- Duração alvo: ")
                    .append(profile.getTargetDurationSeconds())
                    .append(" segundos\n");
        }
        return loadTemplate()
                .replace("{{context}}", context.toString().trim())
                .replace("{{product_section}}", section("Resumo do produto", productFields(product)))
                .replace("{{profile_section}}", section("Perfil do vídeo", profileFields(profile)));
    }

    /** Renderiza uma seção do prompt com os campos disponíveis. */
    private String section(String title, Map<String, String> fields) {
        if (fields.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(title).append(':').append('\n');
        fields.forEach((label, value) -> sb.append("- ").append(label).append(':').append(' ').append(value).append('\n'));
        return sb.toString().trim();
    }

    /** Extrai os campos comerciais do produto para o prompt. */
    private Map<String, String> productFields(ProductDto product) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (product == null) {
            return fields;
        }
        putIfNotBlank(fields, "Promessa", product.getPromise());
        putIfNotBlank(fields, "Dor principal", product.getExplicitPain());
        putIfNotBlank(fields, "Mecanismo único", product.getUniqueMechanism());
        putIfNotBlank(fields, "Prova social", product.getSocialProof());
        putIfNotBlank(fields, "Risco reverso", product.getRiskReversal());
        putIfNotBlank(fields, "Tripwire", product.getTripwire());
        putIfNotBlank(fields, "Checkout", product.getCheckoutMonetization());
        return fields;
    }

    /** Extrai os campos do perfil de vídeo para o prompt. */
    private Map<String, String> profileFields(SalesVideoProfileDto profile) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (profile == null) {
            return fields;
        }
        putIfNotBlank(fields, "Título", profile.getTitle());
        putIfNotBlank(fields, "Persona", profile.getPersonaName());
        putIfNotBlank(fields, "Estilo da persona", profile.getPersonaStyle());
        putIfNotBlank(fields, "Estilo de voz", profile.getVoiceStyle());
        putIfNotBlank(fields, "Idioma", profile.getLanguage());
        return fields;
    }

    /** Adiciona um campo textual apenas quando houver conteúdo útil. */
    private void putIfNotBlank(Map<String, String> fields, String key, String value) {
        if (StringUtils.hasText(value)) {
            fields.put(key, value.trim());
        }
    }

    /** Carrega o template versionado do classpath. */
    private String loadTemplate() {
        try {
            return StreamUtils.copyToString(new ClassPathResource(TEMPLATE_PATH).getInputStream(),
                    StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Template de roteiro de vídeo não encontrado: " + TEMPLATE_PATH, ex);
        }
    }
}
