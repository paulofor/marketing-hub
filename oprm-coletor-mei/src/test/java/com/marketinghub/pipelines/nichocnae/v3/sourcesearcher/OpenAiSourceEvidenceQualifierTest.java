package com.marketinghub.pipelines.nichocnae.v3.sourcesearcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.pipelines.nichocnae.v3.core.StageContext;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Valida o contrato OpenAI de qualificação semântica do source-searcher. */
class OpenAiSourceEvidenceQualifierTest {
    /** Confirma que a requisição usa Responses API com schema estrito, Flex e auditoria no backend. */
    @Test
    void shouldCallOpenAiWithStrictSchemaFlexAndBackendAudit() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        String modelJson = """
                {"enoughEvidence":true,"decisionReason":"Fonte suficiente.","selectedSources":[{"url":"https://forum.example.com.br/troca","title":"Troca por WhatsApp","snippet":"Pedido, troca, entrega e atendimento por WhatsApp.","approved":true,"sourceIntent":"COMMUNITY_OR_QUESTION_EVIDENCE","sourceType":"REAL_PROFESSIONAL_QUESTION","routineEvidenceScore":82,"brazilRelevanceScore":75,"qualityScore":88,"evidenceReason":"Atrito real de atendimento e troca.","commercialPageRisk":false,"solutionLanguageRisk":false,"structuredBusinessDriftRisk":false,"matchedQuery":"loja roupas troca whatsapp"}],"rejectedEvidenceSummary":"Sem rejeicoes relevantes."}
                """.trim();
        String response = new ObjectMapper().writeValueAsString(Map.of("output_text", modelJson));
        AtomicReference<String> backendRequestAudit = new AtomicReference<>();
        AtomicReference<String> openAiRequest = new AtomicReference<>();
        server.expect(once(), requestTo(URI.create("http://backend.test/api/internal/oprmcoletormei/nichocnae/v3/source-searcher/stage-executions/4781400/job-1/recebeRequest")))
                .andExpect(jsonPath("$.plataforma").value("OPENAI_RESPONSES_API"))
                .andExpect(jsonPath("$.schema").exists())
                .andExpect(req -> backendRequestAudit.set(new ObjectMapper()
                        .readTree(((MockClientHttpRequest) req).getBodyAsString())
                        .get("request")
                        .asText()))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(URI.create("https://api.openai.com/v1/responses")))
                .andExpect(jsonPath("$.model").value("gpt-test"))
                .andExpect(jsonPath("$.service_tier").value("flex"))
                .andExpect(jsonPath("$.text.format.type").value("json_schema"))
                .andExpect(jsonPath("$.text.format.strict").value(true))
                .andExpect(jsonPath("$.text.format.schema.required[0]").value("enoughEvidence"))
                .andExpect(req -> openAiRequest.set(((MockClientHttpRequest) req).getBodyAsString()))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(URI.create("http://backend.test/api/internal/oprmcoletormei/nichocnae/v3/source-searcher/stage-executions/4781400/job-1/recebeResponse")))
                .andExpect(jsonPath("$.response").value(response))
                .andExpect(jsonPath("$.modelo").value("gpt-test"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        OpenAiSourceEvidenceQualifier qualifier = new OpenAiSourceEvidenceQualifier(
                builder.build(),
                new ObjectMapper(),
                new SourceSearcherOpenAiProperties("https://api.openai.com/v1", "direct-key", "", "gpt-test", null, true),
                new SourceSearcherPromptBuilder(),
                new SourceSearcherSchemaLoader(new ObjectMapper()),
                "http://backend.test");

        List<Map<String, Object>> selected = qualifier.qualify(
                new StageContext("job-1", "72", Map.of("cnaeCode", "4781400")),
                List.of(Map.of("query", "loja roupas troca whatsapp")),
                List.of(Map.of("qualifiedSources", List.of(Map.of("url", "https://forum.example.com.br/troca")))),
                List.of());

        assertThat(selected).hasSize(1);
        assertThat(selected.getFirst()).containsEntry("aiQualified", true);
        assertThat(backendRequestAudit.get()).isEqualTo(openAiRequest.get());
        server.verify();
    }

    /** Mantem seleção determinística quando a OpenAI classifica evidência como insuficiente. */
    @Test
    void shouldKeepDeterministicSelectionWhenOpenAiRejectsEvidence() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        String modelJson = "{\"enoughEvidence\":false,\"decisionReason\":\"Insuficiente.\",\"selectedSources\":[],\"rejectedEvidenceSummary\":\"Fontes genericas.\"}";
        String response = new ObjectMapper().writeValueAsString(Map.of("output_text", modelJson));
        server.expect(once(), requestTo(URI.create("http://backend.test/api/internal/oprmcoletormei/nichocnae/v3/source-searcher/stage-executions/4781400/job-2/recebeRequest")))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(URI.create("https://api.openai.com/v1/responses")))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(URI.create("http://backend.test/api/internal/oprmcoletormei/nichocnae/v3/source-searcher/stage-executions/4781400/job-2/recebeResponse")))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        OpenAiSourceEvidenceQualifier qualifier = new OpenAiSourceEvidenceQualifier(
                builder.build(),
                new ObjectMapper(),
                new SourceSearcherOpenAiProperties("https://api.openai.com/v1", "direct-key", "", "gpt-test", null, true),
                new SourceSearcherPromptBuilder(),
                new SourceSearcherSchemaLoader(new ObjectMapper()),
                "http://backend.test");
        List<Map<String, Object>> deterministic = List.of(Map.of("url", "https://fonte.example.com.br/rotina"));

        List<Map<String, Object>> selected = qualifier.qualify(
                new StageContext("job-2", "73", Map.of("cnaeCode", "4781400")),
                List.of(),
                List.of(),
                deterministic);

        assertThat(selected).isEqualTo(deterministic);
        server.verify();
    }
}
