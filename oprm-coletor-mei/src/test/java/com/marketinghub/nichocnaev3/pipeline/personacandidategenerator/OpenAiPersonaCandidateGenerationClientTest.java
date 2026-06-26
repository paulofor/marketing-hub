package com.marketinghub.nichocnaev3.pipeline.personacandidategenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Valida o contrato de chamada da OpenAI para geração de personas candidatas v3. */
class OpenAiPersonaCandidateGenerationClientTest {
    /** Confirma que a requisição usa Responses API com JSON Schema e Flex Processing. */
    @Test
    void shouldCallOpenAiResponsesApiWithStrictJsonSchemaAndFlex() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        String modelJson = """
                {\"candidatePersonas\":[{\"name\":\"p1\"},{\"name\":\"p2\"},{\"name\":\"p3\"}],\"personaSummary\":\"p1;p2;p3\"}
                """.trim();
        String response = new ObjectMapper().writeValueAsString(Map.of("output_text", modelJson));
        server.expect(once(), requestTo(URI.create("https://api.openai.com/v1/responses")))
                .andExpect(jsonPath("$.model").value("gpt-test"))
                .andExpect(jsonPath("$.service_tier").value("flex"))
                .andExpect(jsonPath("$.text.format.type").value("json_schema"))
                .andExpect(jsonPath("$.text.format.strict").value(true))
                .andExpect(jsonPath("$.text.format.schema.required[6]").value("candidatePersonas"))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        OpenAiPersonaCandidateGenerationClient client = new OpenAiPersonaCandidateGenerationClient(
                builder.build(),
                new ObjectMapper(),
                new PersonaCandidateOpenAiProperties("https://api.openai.com/v1", "direct-key", "", "gpt-test", null),
                new PersonaCandidatePromptBuilder(),
                new PersonaCandidateSchemaLoader(new ObjectMapper()));

        Map<String, Object> output = client.generate(new PersonaCandidateGenerationRequest(
                "job-1",
                "72",
                "4781400",
                "Comércio varejista de artigos do vestuário",
                Map.of("cnaeCode", "4781400")));

        assertThat((List<?>) output.get("candidatePersonas")).hasSize(3);
        server.verify();
    }
}
