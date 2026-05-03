package com.marketinghub.worker.geralanding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.experimentpipeline.ExperimentPipelineBackendClient;
import com.marketinghub.worker.experimentpipeline.ExperimentPipelineJobDto;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GeraLandingServiceTest {

    private final ExperimentPipelineBackendClient backendClient = Mockito.mock(ExperimentPipelineBackendClient.class);
    private final GeraLandingService service = new GeraLandingService(new ObjectMapper(), backendClient);

    @Test
    void deveMontarPromptEtapaComPromptEDados() throws Exception {
        ExperimentPipelineJobDto job = novoJob();

        String prompt = service.montarPromptEtapa(job, "test-placeholder");

        assertThat(prompt)
                .contains("REGRAS GLOBAIS")
                .contains("Resultado claro")
                .doesNotContain("{prompt-")
                .doesNotContain("{dados-");
    }

    @Test
    void deveRegistrarPromptMontadoComChaveDeRastreio() throws Exception {
        ExperimentPipelineJobDto job = novoJob();

        String prompt = service.montarERegistrarPromptEtapa(job, "test-placeholder", "exec-7");

        assertThat(prompt).contains("Resultado claro");
        verify(backendClient).recordGenerationLog(
                Mockito.eq(job.id()),
                Mockito.eq(prompt),
                Mockito.contains("exp:10|etapa:test-placeholder|exec:exec-7|job:"),
                Mockito.eq("gpt-4.1"),
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.isNull());
    }

    @Test
    void deveFalharQuandoPromptDaEtapaNaoExiste() {
        assertThatThrownBy(() -> service.montarPromptEtapa(null, "etapa-inexistente"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Prompt não encontrado");
    }

    private ExperimentPipelineJobDto novoJob() {
        return new ExperimentPipelineJobDto(
                UUID.randomUUID(),
                10L,
                "landing-page-wireframe",
                "gpt-4.1",
                null,
                """
                        {
                          "adCopy": {
                            "headline": "Resultado claro",
                            "ctaText": "Começar agora"
                          }
                        }
                        """,
                Instant.now());
    }
}
