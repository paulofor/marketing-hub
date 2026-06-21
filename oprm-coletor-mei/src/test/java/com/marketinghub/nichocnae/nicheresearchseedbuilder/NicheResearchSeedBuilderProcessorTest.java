package com.marketinghub.nichocnae.nicheresearchseedbuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.nichocnae.pipeline.StageContext;
import com.marketinghub.nichocnae.pipeline.StageExecution;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a orquestração da etapa dois entre OpenAI, backend e contrato StageProcessor. */
class NicheResearchSeedBuilderProcessorTest {

    /** Deve persistir no backend a saída gerada pela IA e devolver métricas estruturadas da etapa dois. */
    @Test
    void shouldGenerateAndCompleteSeedBuilderOutput() {
        OpenAiNicheResearchSeedBuilderClient openAiClient = mock(OpenAiNicheResearchSeedBuilderClient.class);
        NicheResearchSeedBuilderBackendClient backendClient = mock(NicheResearchSeedBuilderBackendClient.class);
        NicheResearchSeedBuilderProcessor processor = new NicheResearchSeedBuilderProcessor(openAiClient, backendClient);
        NicheResearchSeedBuilderPending pending = pending();
        NicheResearchSeedBuilderOutput output = output();
        OpenAiSeedBuilderResult generated = new OpenAiSeedBuilderResult(output, "{}", "{\"input\":true}", "{\"id\":\"resp_123\"}", 10, 20, "resp_123", "gpt-test");
        when(openAiClient.generate(pending)).thenReturn(generated);
        when(backendClient.completeStageExecution(generated)).thenReturn(output);

        var result = processor.process(new StageContext<>(
                new StageExecution<>("job-1", pending, Map.of()),
                pending,
                (artifact, content) -> artifact,
                Map.of()));

        assertThat(result.output()).isEqualTo(output);
        assertThat(result.metrics()).containsEntry("researchCycleId", 1001L).containsEntry("queryCount", 1);
        verify(backendClient).completeStageExecution(generated);
    }

    /** Cria uma pendência mínima para a etapa dois. */
    private NicheResearchSeedBuilderPending pending() {
        return new NicheResearchSeedBuilderPending(
                1001L,
                55L,
                "9602501",
                "Cabeleireiros, manicure e pedicure",
                "Cabeleireiros, manicure e pedicure",
                BigDecimal.valueOf(92),
                125000L,
                "gpt-5.2",
                "gpt-5.2 (gpt-5.2)",
                "AUTO_SCORE_QUEUE",
                null,
                null,
                null,
                null,
                List.of(),
                "RUNNING",
                Instant.now(),
                Instant.now());
    }

    /** Cria uma saída mínima suficiente para validar o processor. */
    private NicheResearchSeedBuilderOutput output() {
        NicheResearchSeed seed = new NicheResearchSeed(
                1001L,
                "9602501",
                "Cabeleireiros, manicure e pedicure",
                "Cabeleireiros, manicures e pedicures",
                "serviço local de beleza",
                "atendimento com agenda",
                "consumidor final",
                "manicure",
                "depende de recorrência",
                "INFERRED_FROM_CNAE",
                "AI");
        return new NicheResearchSeedBuilderOutput(
                1001L,
                seed,
                List.of(new ResearchQuery(1001L, "manicure MEI rotina Brasil", "MEI_ROUTINE_DISCOVERY", "GENERAL_WEB", 1, "PENDING", "AI")));
    }
}
