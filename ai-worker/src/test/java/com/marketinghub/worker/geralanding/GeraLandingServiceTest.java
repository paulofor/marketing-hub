package com.marketinghub.worker.geralanding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GeraLandingServiceTest {

    private final GeraLandingBackendClient backendClient = Mockito.mock(GeraLandingBackendClient.class);
    private final GeraLandingService service = new GeraLandingService(new ObjectMapper(), backendClient);

    @Test
    void deveMontarPromptEtapaComPromptEDados() throws Exception {
        GeraLandingPromptContext context = novoContexto();

        String prompt = service.montarPromptEtapa(context, "test-placeholder");

        assertThat(prompt)
                .contains("REGRAS GLOBAIS")
                .contains("Resultado claro")
                .doesNotContain("{prompt-")
                .doesNotContain("{dados-");
    }

    @Test
    void deveResolverPlaceholdersMustacheNoPromptDeWireframe() throws Exception {
        GeraLandingPromptContext context = new GeraLandingPromptContext(
                10L,
                "id-job-original",
                "landing-page-wireframe",
                Map.of(
                        "NICHE_NAME", "E-commerce",
                        "PAIN_JSON", Map.of("title", "Baixa conversão"),
                        "RESULT_JSON", Map.of("title", "Mais vendas")));

        String prompt = service.montarPromptEtapa(context, "landing-page-wireframe");

        assertThat(prompt).contains("Nicho: E-commerce");
        assertThat(prompt).contains("Baixa conversão");
        assertThat(prompt).contains("Mais vendas");
        assertThat(prompt).doesNotContain("{{NICHE_NAME}}");
        assertThat(prompt).doesNotContain("{{PAIN_JSON}}");
        assertThat(prompt).doesNotContain("{{RESULT_JSON}}");
    }

    @Test
    void deveDisponibilizarTodosOsItensCanonicosDosPipelinesNoPromptFinal() throws Exception {
        Map<String, Object> dadosPrompt = new LinkedHashMap<>();
        dadosPrompt.put("NICHE_NAME", "E-commerce");
        dadosPrompt.put("PAIN_JSON", Map.of("title", "Baixa conversão"));
        dadosPrompt.put("RESULT_JSON", Map.of("title", "Mais vendas"));
        dadosPrompt.put("MECHANISM_JSON", Map.of("title", "Mecanismo validado"));
        dadosPrompt.put("PROOF_JSON", Map.of("title", "Prova objetiva"));
        dadosPrompt.put("OFFER_JSON", Map.of("title", "Oferta principal"));
        dadosPrompt.put("campaignAngle", Map.of("hook", "Ganhe tempo"));
        dadosPrompt.put("adCopy", Map.of("headline", "Resultado claro"));
        dadosPrompt.put("adImageBriefing", Map.of("scene", "Pessoa trabalhando"));
        dadosPrompt.put("landingPageWireframe", Map.of("pageGoal", "capturar lead"));
        dadosPrompt.put("landingCopy", Map.of("primaryCTA", "Quero começar"));
        dadosPrompt.put("landingPromptImagem", "Prompt imagem");
        dadosPrompt.put("listaImagem", Map.of("items", java.util.List.of("img1", "img2")));
        dadosPrompt.put("landingPresetDesign", Map.of("palette", "neutral"));
        dadosPrompt.put("landingHtml", "<html></html>");
        dadosPrompt.put("experimentMetadata", Map.of("experimentId", 10));

        GeraLandingPromptContext context = new GeraLandingPromptContext(
                10L,
                "id-job-original",
                "landing-page-wireframe",
                dadosPrompt);

        String prompt = service.montarPromptEtapa(context, "landing-page-wireframe");

        assertThat(prompt)
                .contains("- NICHE_NAME: E-commerce")
                .contains("- PAIN_JSON:")
                .contains("- RESULT_JSON:")
                .contains("- MECHANISM_JSON:")
                .contains("- PROOF_JSON:")
                .contains("- OFFER_JSON:")
                .contains("## Pipeline de experimento")
                .contains("- campaignAngle:")
                .contains("- adCopy:")
                .contains("- adImageBriefing:")
                .contains("- landingPageWireframe:")
                .contains("- landingCopy:")
                .contains("- landingPromptImagem: Prompt imagem")
                .contains("- listaImagem:")
                .contains("- landingPresetDesign:")
                .contains("- landingHtml: <html></html>")
                .contains("- experimentMetadata:");
    }

    @Test
    void deveRegistrarPromptMontadoComChaveDeRastreio() throws Exception {
        GeraLandingPromptContext context = novoContexto();

        String prompt = service.montarERegistrarPromptEtapa(context, "test-placeholder");

        assertThat(prompt).contains("Resultado claro");
        verify(backendClient)
                .receivePrompt(
                        Mockito.eq("id-job-original"),
                        Mockito.eq(10L),
                        Mockito.eq("test-placeholder"),
                        Mockito.eq(prompt),
                        Mockito.isNull(),
                        Mockito.isNull(),
                        Mockito.isNull(),
                        Mockito.anyString());
    }

    @Test
    void deveFalharQuandoPromptDaEtapaNaoExiste() {
        assertThatThrownBy(() -> service.montarPromptEtapa(null, "etapa-inexistente"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Prompt não encontrado");
    }

    private GeraLandingPromptContext novoContexto() {
        return new GeraLandingPromptContext(
                10L,
                "id-job-original",
                "landing-page-wireframe",
                Map.of(
                        "adCopy", Map.of(
                                "headline", "Resultado claro",
                                "ctaText", "Começar agora")));
    }
}
