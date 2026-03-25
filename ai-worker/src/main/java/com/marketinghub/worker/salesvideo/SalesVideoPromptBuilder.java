package com.marketinghub.worker.salesvideo;

import com.marketinghub.product.dto.ProductDto;
import com.marketinghub.salesvideo.dto.SalesVideoProfileDto;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Constrói o prompt usado na geração de scripts de vídeo.
 */
@Component
public class SalesVideoPromptBuilder {

    public String buildPrompt(SalesVideoProfileDto profile, ProductDto product) {
        String language = profile != null && StringUtils.hasText(profile.getLanguage())
                ? profile.getLanguage()
                : "pt-BR";
        StringBuilder sb = new StringBuilder();
        sb.append("Contexto:\n");
        sb.append("- Idioma do vídeo: ").append(language).append('\n');
        if (profile != null && profile.getVideoKind() != null) {
            sb.append("- Tipo do vídeo: ").append(profile.getVideoKind()).append('\n');
        }
        if (profile != null && profile.getTargetDurationSeconds() != null) {
            sb.append("- Duração alvo: ")
                    .append(profile.getTargetDurationSeconds())
                    .append(" segundos\n");
        }
        sb.append('\n');

        appendSection(sb, "Resumo do produto", productFields(product));
        appendSection(sb, "Perfil do vídeo", profileFields(profile));

        sb.append("\nTarefas:\n");
        sb.append("1. Criar um hook forte e curto alinhado ao avatar.\n");
        sb.append("2. Escrever o script completo seguindo o estilo da persona.\n");
        sb.append("3. Sugerir um CTA claro e orientado à ação.\n");
        sb.append("4. Escrever uma legenda curta com até 2 hashtags estratégicas.\n");
        sb.append("5. Montar um storyboard com 3 a 6 cenas, cada uma com descrição visual, fala e duração aproximada.\n");
        sb.append("6. Respeitar o tempo alvo informado para ritmo e quantidade de cenas.\n");

        sb.append("\nInstruções adicionais:\n");
        sb.append("- Mantenha o texto no idioma indicado e no tom descrito.\n");
        sb.append("- Evite repetir frases e foque em benefícios e diferenciais reais.\n");
        sb.append("- Considere dores, promessa, prova social e mecanismo único quando disponíveis.\n");
        sb.append("- Distribua o storyboard de forma lógica: Abertura, desenvolvimento, prova/objeções e CTA.\n");

        sb.append("\nFormato de resposta obrigatório (JSON válido):\n");
        sb.append("{\n");
        sb.append("  \"hook\": \"texto curto do gancho\",\n");
        sb.append("  \"script\": \"script completo em parágrafos\",\n");
        sb.append("  \"cta\": \"chamada para ação\",\n");
        sb.append("  \"caption\": \"legenda pronta para redes sociais\",\n");
        sb.append("  \"storyboard\": [\n");
        sb.append("    {\n");
        sb.append("      \"scene\": 1,\n");
        sb.append("      \"visual\": \"descrição visual do take\",\n");
        sb.append("      \"voiceover\": \"fala correspondente\",\n");
        sb.append("      \"durationSeconds\": 5\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}\n");

        sb.append("\nNão retorne comentários fora do JSON.\n");
        return sb.toString();
    }

    private void appendSection(StringBuilder sb, String title, Map<String, String> fields) {
        if (fields.isEmpty()) {
            return;
        }
        sb.append(title).append(':').append('\n');
        fields.forEach((label, value) -> sb.append("- ").append(label).append(':').append(' ').append(value).append('\n'));
        sb.append('\n');
    }

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

    private void putIfNotBlank(Map<String, String> fields, String key, String value) {
        if (StringUtils.hasText(value)) {
            fields.put(key, value.trim());
        }
    }
}
