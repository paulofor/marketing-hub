package com.marketinghub.worker.salesvideo;

import com.marketinghub.product.dto.ProductDto;
import com.marketinghub.salesvideo.dto.SalesVideoProfileDto;
import com.marketinghub.worker.salesvideo.dto.SalesVideoCommercialPlaybookResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
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

    /** Monta o prompt final de roteiro sem playbooks cadastrados. */
    public String buildPrompt(SalesVideoProfileDto profile, ProductDto product) {
        return buildPrompt(profile, product, List.of());
    }

    /** Monta o prompt final de roteiro a partir do template versionado e do contexto comercial. */
    public String buildPrompt(SalesVideoProfileDto profile,
                              ProductDto product,
                              List<SalesVideoCommercialPlaybookResponse> playbooks) {
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
                .replace("{{commercial_context_section}}", commercialContextSection(product))
                .replace("{{cinematic_brief_section}}", cinematicBriefSection(playbooks))
                .replace("{{product_section}}", section("Resumo do produto", productFields(product)))
                .replace("{{profile_section}}", section("Perfil do vídeo", profileFields(profile)));
    }

    /** Monta blocos comerciais reutilizáveis para adaptar o roteiro ao produto atual. */
    private String commercialContextSection(ProductDto product) {
        if (product == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        appendSection(sb, "Nicho e consumidor", nicheFields(product));
        appendSection(sb, "Hipótese e promessa", hypothesisFields(product));
        appendSection(sb, "Oferta, funil e conversão", offerFields(product));
        appendSection(sb, "Prova e experiência de valor", proofFields(product));
        return sb.toString().trim();
    }

    /** Monta a seção de briefs cinematográficos ativos cadastrados no playbook comercial. */
    private String cinematicBriefSection(List<SalesVideoCommercialPlaybookResponse> playbooks) {
        if (playbooks == null || playbooks.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int index = 1;
        for (SalesVideoCommercialPlaybookResponse playbook : playbooks) {
            if (playbook == null || !playbook.isActive()) {
                continue;
            }
            Map<String, String> fields = cinematicBriefFields(playbook);
            if (fields.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append("\n\n");
            }
            sb.append(section("Brief Cinematico PDE " + index, fields));
            index++;
        }
        return sb.toString().trim();
    }

    /** Extrai campos do Brief Cinematico PDE para orientar storyboard e prompts de video. */
    private Map<String, String> cinematicBriefFields(SalesVideoCommercialPlaybookResponse playbook) {
        Map<String, String> fields = new LinkedHashMap<>();
        putIfNotBlank(fields, "Nicho", playbook.getNicheKey());
        putIfNotBlank(fields, "Variacao", playbook.getVariantKey());
        putIfNotBlank(fields, "Papel no funil", playbook.getFunnelRole());
        putIfNotBlank(fields, "Promessa a tangibilizar", playbook.getPromiseToVisualize());
        putIfNotBlank(fields, "Dor visual", playbook.getVisualPain());
        putIfNotBlank(fields, "Cena principal", playbook.getMainScene());
        putIfNotBlank(fields, "Sujeito/personagem/produto", playbook.getSubjectDescription());
        putIfNotBlank(fields, "Movimento", playbook.getMotionDescription());
        putIfNotBlank(fields, "Camera/enquadramento", playbook.getCameraFraming());
        putIfNotBlank(fields, "Luz/estetica", playbook.getLightingStyle());
        putIfNotBlank(fields, "Emocao esperada", playbook.getExpectedEmotion());
        putIfNotBlank(fields, "CTA ou transicao", playbook.getTransitionOrCta());
        putIfNotBlank(fields, "Restricoes de qualidade", playbook.getQualityConstraints());
        putIfNotBlank(fields, "Prompt cinematografico final", playbook.getCinematicPrompt());
        putIfNotBlank(fields, "Objecao comercial", playbook.getObjectionText());
        putIfNotBlank(fields, "CTA comercial", playbook.getCtaText());
        return fields;
    }

    /** Adiciona uma seção comercial quando houver campos preenchidos. */
    private void appendSection(StringBuilder sb, String title, Map<String, String> fields) {
        String rendered = section(title, fields);
        if (!StringUtils.hasText(rendered)) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append("\n\n");
        }
        sb.append(rendered);
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
        putIfNotBlank(fields, "Nome", product.getName());
        putIfNotBlank(fields, "Tipo", product.getProductType());
        putIfNotBlank(fields, "Promessa", product.getPromise());
        putIfNotBlank(fields, "Dor principal", product.getExplicitPain());
        putIfNotBlank(fields, "Mecanismo único", product.getUniqueMechanism());
        putIfNotBlank(fields, "Prova social", product.getSocialProof());
        putIfNotBlank(fields, "Risco reverso", product.getRiskReversal());
        putIfNotBlank(fields, "Tripwire", product.getTripwire());
        putIfNotBlank(fields, "Checkout", product.getCheckoutMonetization());
        return fields;
    }

    /** Extrai informações de nicho e público para evitar roteiro genérico. */
    private Map<String, String> nicheFields(ProductDto product) {
        Map<String, String> fields = new LinkedHashMap<>();
        putIfNotBlank(fields, "Nicho", product.getNiche());
        putIfNotBlank(fields, "Público-alvo", product.getTargetAudience());
        putIfNotBlank(fields, "Avatar", product.getAvatar());
        putIfNotBlank(fields, "Estilo de linguagem", product.getLanguageStyle());
        return fields;
    }

    /** Extrai hipótese e cadeia de persuasão do produto para orientar a fala. */
    private Map<String, String> hypothesisFields(ProductDto product) {
        Map<String, String> fields = new LinkedHashMap<>();
        putIfNotBlank(fields, "Hipótese principal", product.getPrimaryHypothesis());
        putIfNotBlank(fields, "Dor explícita", product.getExplicitPain());
        putIfNotBlank(fields, "Promessa", product.getPromise());
        putIfNotBlank(fields, "Mecanismo único", product.getUniqueMechanism());
        putIfNotBlank(fields, "Storytelling", product.getStorytelling());
        return fields;
    }

    /** Extrai dados de oferta e funil para tornar o CTA específico. */
    private Map<String, String> offerFields(ProductDto product) {
        Map<String, String> fields = new LinkedHashMap<>();
        putIfNotBlank(fields, "CTA primário", product.getPrimaryCta());
        putIfNotBlank(fields, "Tripwire", product.getTripwire());
        putIfNotBlank(fields, "Funil", product.getFunnel());
        putIfNotBlank(fields, "Monetização no checkout", product.getCheckoutMonetization());
        putIfNotBlank(fields, "Preço atual", product.getCurrentPriceBrl() == null
                ? null
                : "R$ " + product.getCurrentPriceBrl());
        return fields;
    }

    /** Extrai prova, reversão de risco e experiência do produto para reduzir incerteza. */
    private Map<String, String> proofFields(ProductDto product) {
        Map<String, String> fields = new LinkedHashMap<>();
        putIfNotBlank(fields, "Prova social", product.getSocialProof());
        putIfNotBlank(fields, "Evidências científicas", product.getScientificEvidencePack());
        putIfNotBlank(fields, "Jornada de 7 dias", product.getSevenDayJourney());
        putIfNotBlank(fields, "Experiência PDE", product.getPdeExperienceJson());
        putIfNotBlank(fields, "Risco reverso", product.getRiskReversal());
        putIfNotBlank(fields, "Observações comerciais", product.getCommercialNotes());
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
