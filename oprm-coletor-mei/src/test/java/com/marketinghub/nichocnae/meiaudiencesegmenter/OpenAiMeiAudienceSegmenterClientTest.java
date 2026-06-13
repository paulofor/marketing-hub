package com.marketinghub.nichocnae.meiaudiencesegmenter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/** Responsabilidade: validar a montagem operacional da chamada OpenAI da segmentação MEI/autônomo. */
class OpenAiMeiAudienceSegmenterClientTest {

    /** Garante que o modelo configurado no pipeline e enviado pelo backend tem prioridade sobre o fallback local. */
    @Test
    void shouldUsePipelineConfiguredModelBeforeLocalFallback() {
        OpenAiMeiAudienceSegmenterClient client = client("gpt-4.1-mini");

        assertThat(client.resolveModel(pending("gpt-5.4"))).isEqualTo("gpt-5.4");
    }

    /** Garante que a etapa continua operável com fallback local quando o backend ainda não enviar modelo. */
    @Test
    void shouldUseLocalFallbackWhenPendingHasNoConfiguredModel() {
        OpenAiMeiAudienceSegmenterClient client = client("gpt-4.1-mini");

        assertThat(client.resolveModel(pending(null))).isEqualTo("gpt-4.1-mini");
    }

    /** Cria cliente com dependências simuladas para validar apenas resolução de modelo. */
    private OpenAiMeiAudienceSegmenterClient client(String fallbackModel) {
        return new OpenAiMeiAudienceSegmenterClient(
                mock(RestClient.class),
                new ObjectMapper(),
                new MeiAudienceSegmenterOpenAiProperties("https://api.openai.com/v1", "key", "", fallbackModel,
                        "OPRM_MEI_AUDIENCE_SEGMENTER_OPENAI_API_KEY", "OPENAI_API_KEY"),
                mock(MeiAudienceSegmenterPromptBuilder.class),
                mock(MeiAudienceSegmenterSchema.class),
                mock(MeiAudienceSegmenterValidator.class));
    }

    /** Cria uma pendência mínima com modelo configurável para teste da chamada OpenAI. */
    private MeiAudienceSegmenterPending pending(String openAiModelCode) {
        return new MeiAudienceSegmenterPending(
                1001L, 2002L, 3003L, "9602501", "Cabeleireiros", "Serviços de beleza", "Beleza MEI",
                openAiModelCode, "Modelo configurado", "Rotina", "Comportamento", "Canais", "Dores operacionais",
                "Dores emocionais", "Sonhos", "Medos", "Linguagem", "Dores", "Resultados", "Evidências",
                "example.com", 80, 75, 70, 10, Instant.parse("2026-06-12T00:00:00Z"), List.of(), List.of());
    }
}
