package com.marketinghub.worker.openai.core.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a resolução comum de placeholders dos prompts do core OpenAI. */
class PromptTemplateResolverTest {

    /** Deve substituir placeholders prefixados usados pelos prompts editáveis das etapas OpenAI. */
    @Test
    void resolveShouldReplacePrefixedDataAndPromptPlaceholders() {
        PromptTemplateResolver resolver = new PromptTemplateResolver(
                this::loadPrompt,
                this::renderValue);
        String template = loadPrompt("prompts/geralanding/openai-core-placeholder-test.md");
        Map<String, Object> data = Map.of(
                "campaignAngle", Map.of("primaryPromise", "Agenda cheia"),
                "adCopy", Map.of("headline", "Sem desconto"),
                "adImageBriefing", Map.of("concept", "Calendário lotado"),
                "NICHE_NAME", "Clínicas");

        String prompt = resolver.resolve(template, data, "prompts/geralanding/openai-core-placeholder-test.md");

        assertThat(prompt)
                .contains("REGRAS GLOBAIS")
                .contains("campaignAngle={primaryPromise=Agenda cheia}")
                .contains("adCopy={headline=Sem desconto}")
                .contains("adImageBriefing={concept=Calendário lotado}")
                .contains("Nicho:\nClínicas")
                .doesNotContain("{{prompt-regras-globais}}")
                .doesNotContain("{{dados-campaignAngle}}")
                .doesNotContain("{{dados-adCopy}}")
                .doesNotContain("{{dados-adImageBriefing}}")
                .doesNotContain("{{NICHE_NAME}}");
    }

    /** Carrega prompts controlados pelo teste simulando arquivos irmãos da etapa. */
    private String loadPrompt(String path) {
        return switch (path) {
            case "prompts/geralanding/openai-core-placeholder-test.md" -> """
                    {{prompt-regras-globais}}

                    Ângulo da Campanha que vai ser publicada:
                    {{dados-campaignAngle}}

                    Copy do Anuncio:
                    {{dados-adCopy}}

                    Briefing das Imagens dos Anuncios:
                    {{dados-adImageBriefing}}

                    Nicho:
                    {{NICHE_NAME}}
                    """;
            case "prompts/geralanding/regras-globais.md" -> "REGRAS GLOBAIS";
            default -> throw new IllegalArgumentException("Prompt não encontrado: " + path);
        };
    }

    /** Renderiza valores de teste com prefixo da chave principal para facilitar asserção. */
    private String renderValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Map<?, ?> map && map.containsKey("primaryPromise")) {
            return "campaignAngle=" + value;
        }
        if (value instanceof Map<?, ?> map && map.containsKey("headline")) {
            return "adCopy=" + value;
        }
        if (value instanceof Map<?, ?> map && map.containsKey("concept")) {
            return "adImageBriefing=" + value;
        }
        return value.toString();
    }
}
