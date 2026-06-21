package com.marketinghub.nichocnae.meiaudiencesegmenter;

import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.marketinghub.oprmcoletormei.marketimport.config.OprmMarketImportCollectorProperties;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Responsabilidade: validar o contrato entre o coletor OPRM MEI e o backend da segmentação de público. */
class MeiAudienceSegmenterBackendClientTest {

    /** Garante que a falha da OpenAI é registrada no backend com causa-raiz operacional suficiente. */
    @Test
    void shouldSendOpenAiOperationalFailureMessageToBackend() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        String message = "Falha ao segmentar público MEI/autônomo com OpenAI; tipo=TooManyRequests; "
                + "causaRaiz=429 Too Many Requests; researchCycleId=1001; routineCardId=2002; "
                + "modeloOpenAI=gpt-5.2; endpoint=https://api.openai.com/v1/responses; httpStatus=429; "
                + "httpBody={error={message=rate limit reached}}";
        server.expect(once(), requestTo(URI.create(
                        "http://backend.test/api/internal/oprm/nichocnae/mei-audience-segmenter/stage-executions/1001/fail")))
                .andExpect(jsonPath("$.errorMessage").value(message))
                .andRespond(withSuccess());
        MeiAudienceSegmenterBackendClient client = new MeiAudienceSegmenterBackendClient(
                new OprmMarketImportCollectorProperties("http://backend.test", "manual"), builder.build());

        client.failStageExecution(pending(), new IllegalStateException(message));

        server.verify();
    }

    /** Cria uma pendência mínima para validar o endpoint de falha por ciclo. */
    private MeiAudienceSegmenterPending pending() {
        return new MeiAudienceSegmenterPending(
                1001L,
                2002L,
                3003L,
                "9602501",
                "Cabeleireiros",
                "Serviços de beleza",
                "Beleza MEI",
                "gpt-5.2",
                "Modelo configurado",
                "Rotina",
                "Comportamento",
                "Canais",
                "Dores operacionais",
                "Dores emocionais",
                "Sonhos",
                "Medos",
                "Linguagem",
                "Dores",
                "Resultados",
                "Evidências",
                "example.com",
                80,
                75,
                70,
                10,
                Instant.parse("2026-06-12T00:00:00Z"),
                List.of(),
                List.of());
    }
}
