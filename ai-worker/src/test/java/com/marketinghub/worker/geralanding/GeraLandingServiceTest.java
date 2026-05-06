package com.marketinghub.worker.geralanding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
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
