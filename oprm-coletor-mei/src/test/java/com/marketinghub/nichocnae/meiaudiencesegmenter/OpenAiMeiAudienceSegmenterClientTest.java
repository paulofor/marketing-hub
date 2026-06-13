package com.marketinghub.nichocnae.meiaudiencesegmenter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
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

    /** Garante que falha HTTP da OpenAI vira mensagem operacional persistível com causa-raiz e contexto do ciclo. */
    @Test
    void shouldBuildOperationalMessageWhenOpenAiHttpFails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo(URI.create("https://api.openai.com/v1/responses")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .body("{\"error\":{\"message\":\"rate limit reached for organization\"}}"));
        MeiAudienceSegmenterPromptBuilder promptBuilder = mock(MeiAudienceSegmenterPromptBuilder.class);
        MeiAudienceSegmenterSchema schema = mock(MeiAudienceSegmenterSchema.class);
        MeiAudienceSegmenterValidator validator = mock(MeiAudienceSegmenterValidator.class);
        MeiAudienceSegmenterPending pending = pending("gpt-5.4");
        when(promptBuilder.buildPrompt(pending)).thenReturn("prompt segmentação");
        when(schema.buildSchema()).thenReturn(Map.of("type", "object"));
        OpenAiMeiAudienceSegmenterClient client = new OpenAiMeiAudienceSegmenterClient(
                builder.build(),
                new ObjectMapper(),
                new MeiAudienceSegmenterOpenAiProperties("https://api.openai.com/v1", "key", "", "gpt-4.1-mini",
                        "OPRM_MEI_AUDIENCE_SEGMENTER_OPENAI_API_KEY", "OPENAI_API_KEY"),
                promptBuilder,
                schema,
                validator);

        assertThatThrownBy(() -> client.segment(pending))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tipo=TooManyRequests")
                .hasMessageContaining("researchCycleId=1001")
                .hasMessageContaining("routineCardId=2002")
                .hasMessageContaining("modeloOpenAI=gpt-5.4")
                .hasMessageContaining("endpoint=https://api.openai.com/v1/responses")
                .hasMessageContaining("httpStatus=429")
                .hasMessageContaining("rate limit reached for organization");
        server.verify();
    }


    /** Garante pré-validação no worker e uma regeneração corretiva antes de registrar falha por contaminação. */
    @Test
    void shouldRegenerateOnceWhenPreValidationDetectsForbiddenTerm() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo(URI.create("https://api.openai.com/v1/responses")))
                .andRespond(withSuccess(openAiResponse("Profissionais com software de agenda"), MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(URI.create("https://api.openai.com/v1/responses")))
                .andRespond(withSuccess(openAiResponse("Profissionais com agenda instável"), MediaType.APPLICATION_JSON));
        MeiAudienceSegmenterPromptBuilder promptBuilder = mock(MeiAudienceSegmenterPromptBuilder.class);
        MeiAudienceSegmenterPending pending = pending("gpt-5.4");
        when(promptBuilder.buildPrompt(pending)).thenReturn("prompt original");
        when(promptBuilder.buildCorrectivePrompt(pending, "software")).thenReturn("prompt corretivo");
        MeiAudienceSegmenterSchema schema = mock(MeiAudienceSegmenterSchema.class);
        when(schema.buildSchema()).thenReturn(Map.of("type", "object"));
        OpenAiMeiAudienceSegmenterClient client = new OpenAiMeiAudienceSegmenterClient(
                builder.build(),
                new ObjectMapper(),
                new MeiAudienceSegmenterOpenAiProperties("https://api.openai.com/v1", "key", "", "gpt-4.1-mini",
                        "OPRM_MEI_AUDIENCE_SEGMENTER_OPENAI_API_KEY", "OPENAI_API_KEY"),
                promptBuilder,
                schema,
                new MeiAudienceSegmenterValidator());

        MeiAudienceSegmentDraft draft = client.segment(pending);

        assertThat(draft.audienceName()).isEqualTo("Profissionais com agenda instável");
        server.verify();
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


    /** Cria resposta mínima da OpenAI contendo JSON válido no campo output_text. */
    private String openAiResponse(String audienceName) {
        String json = """
                {
                  \"audienceName\": \"%s\",
                  \"occupationTerms\": \"termos ocupacionais usados nas fontes\",
                  \"workMode\": \"trabalha por conta própria e organiza agenda manualmente\",
                  \"customerAcquisitionBehavior\": \"consegue clientes por indicação e retorno de clientes antigos\",
                  \"dailyRoutineSummary\": \"rotina com atendimento, orçamento, compras e remarcações\",
                  \"recurringTasksSummary\": \"tarefas recorrentes de agenda, atendimento, cobrança e reposição\",
                  \"operationalPainsSummary\": \"dor prática com cancelamento, atraso e fluxo irregular\",
                  \"emotionalPainsSummary\": \"dor emocional com insegurança de renda e reputação local\",
                  \"dreamsSummary\": \"busca estabilidade, reconhecimento e agenda previsível\",
                  \"fearsSummary\": \"teme perder clientes, receber calote e ficar sem movimento\",
                  \"languagePatterns\": \"frases observadas sobre agenda, cliente sumido e indicação\",
                  \"channelsUsed\": \"usa WhatsApp, indicação, redes sociais locais e agenda própria\",
                  \"recentSourceSummary\": \"fontes brasileiras recentes com evidências curtas\",
                  \"autonomousProfessionalFitScore\": 85,
                  \"behavioralEvidenceScore\": 75,
                  \"sourceFreshnessScore\": 80,
                  \"outdatedSourceRiskScore\": 10,
                  \"structuredBusinessDriftRiskScore\": 5,
                  \"solutionLanguageRiskScore\": 0
                }
                """.formatted(audienceName).replace("\n", " ").replace("\"", "\\\"");
        return "{\"output_text\":\"" + json + "\"}";
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
