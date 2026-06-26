package com.marketinghub.pipelines.nichocnae.v3.personacandidategenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Valida o contrato de chamada da OpenAI para geração de personas candidatas v3. */
@ExtendWith(OutputCaptureExtension.class)
class OpenAiPersonaCandidateGenerationClientTest {
    /** Confirma que a requisição usa Responses API com JSON Schema e Flex Processing. */
    @Test
    void shouldCallOpenAiResponsesApiWithStrictJsonSchemaAndFlex(CapturedOutput logs) throws Exception {
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
        assertThat(logs.getOut())
                .contains("Request OpenAI persona-candidate-generator")
                .contains("Response OpenAI persona-candidate-generator")
                .contains("jobId=job-1")
                .contains("stageExecutionId=72")
                .contains("cnaeCode=4781400")
                .contains("\"service_tier\":\"flex\"")
                .contains("\"output_text\"")
                .doesNotContain("direct-key");
        server.verify();
    }

    /** Confirma que o cliente lê a chave OpenAI de arquivo seguro quando a variável direta não está presente. */
    @Test
    void shouldResolveApiKeyFromConfiguredSecureFile() throws Exception {
        Path apiKeyFile = Files.createTempFile("oprm-openai-key", ".txt");
        Files.writeString(apiKeyFile, " file-key ");
        OpenAiPersonaCandidateGenerationClient client = new OpenAiPersonaCandidateGenerationClient(
                RestClient.builder().build(),
                new ObjectMapper(),
                new PersonaCandidateOpenAiProperties("https://api.openai.com/v1", "", apiKeyFile.toString(), "gpt-test", null),
                new PersonaCandidatePromptBuilder(),
                new PersonaCandidateSchemaLoader(new ObjectMapper()));

        String apiKey = client.resolveApiKey(new PersonaCandidateGenerationRequest(
                "job-1",
                "72",
                "4781400",
                "Comércio varejista de artigos do vestuário",
                Map.of("cnaeCode", "4781400")));

        assertThat(apiKey).isEqualTo("file-key");
        Files.deleteIfExists(apiKeyFile);
    }

    /** Confirma que erros HTTP da OpenAI registram status e corpo para diagnóstico da causa-raiz. */
    @Test
    void shouldLogOpenAiHttpStatusAndBodyWhenProviderRejectsRequest(CapturedOutput logs) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        String errorBody = "{\"error\":{\"message\":\"invalid schema\",\"type\":\"invalid_request_error\"}}";
        server.expect(once(), requestTo(URI.create("https://api.openai.com/v1/responses")))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(errorBody));
        OpenAiPersonaCandidateGenerationClient client = new OpenAiPersonaCandidateGenerationClient(
                builder.build(),
                new ObjectMapper(),
                new PersonaCandidateOpenAiProperties("https://api.openai.com/v1", "direct-key", "", "gpt-test", null),
                new PersonaCandidatePromptBuilder(),
                new PersonaCandidateSchemaLoader(new ObjectMapper()));

        assertThatThrownBy(() -> client.generate(new PersonaCandidateGenerationRequest(
                        "job-err",
                        "87",
                        "4781400",
                        "Comércio varejista de artigos do vestuário",
                        Map.of("cnaeCode", "4781400"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Falha na OpenAI ao gerar personas candidatas do NichoCNAE v3.");

        assertThat(logs.getOut())
                .contains("Erro HTTP da OpenAI em persona-candidate-generator")
                .contains("jobId=job-err")
                .contains("stageExecutionId=87")
                .contains("statusCode=400")
                .contains("invalid schema")
                .doesNotContain("direct-key");
        server.verify();
    }

}
