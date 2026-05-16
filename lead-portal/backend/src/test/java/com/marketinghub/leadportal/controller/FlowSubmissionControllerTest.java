package com.marketinghub.leadportal.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.leadportal.model.Flow;
import com.marketinghub.leadportal.model.FlowQuestion;
import com.marketinghub.leadportal.model.FlowQuestionType;
import com.marketinghub.leadportal.repository.FlowSubmissionImagePackageRepository;
import com.marketinghub.leadportal.repository.FlowSubmissionRepository;
import com.marketinghub.leadportal.service.FlowService;
import com.marketinghub.leadportal.service.ExperimentFunnelTrackingClient;
import com.marketinghub.leadportal.service.ExperimentFunnelTrackingClient.TrackingResult;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FlowSubmissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FlowService flowService;

    @Autowired
    private FlowSubmissionRepository submissionRepository;

    @Autowired
    private FlowSubmissionImagePackageRepository imagePackageRepository;

    @MockBean
    private S3Client s3Client;

    @MockBean
    private ExperimentFunnelTrackingClient trackingClient;

    @BeforeEach
    void setup() {
        Mockito.lenient()
                .when(s3Client.putObject(Mockito.any(PutObjectRequest.class), Mockito.any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        Mockito.lenient()
                .when(s3Client.getObjectAsBytes(Mockito.any(GetObjectRequest.class)))
                .thenAnswer(invocation ->
                        ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), "conteudo".getBytes()));

        Mockito.lenient()
                .when(trackingClient.registerSubmission(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(TrackingResult.FORWARDED);

        imagePackageRepository.deleteAll();
        submissionRepository.deleteAll();
        flowService.list().stream()
                .map(Flow::slug)
                .forEach(flowService::delete);

        Flow flow = new Flow(
                "planejamento-acao-21-dias",
                "Planejamento de 21 dias",
                "Coleta de dados para planejamento",
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(
                        new FlowQuestion(
                                "Qual é o seu nome?",
                                "nome",
                                FlowQuestionType.TEXT,
                                true,
                                "", null,
                                List.of()),
                        new FlowQuestion(
                                "Qual é o seu e-mail?",
                                "email",
                                FlowQuestionType.EMAIL,
                                true,
                                "", null,
                                List.of()),
                        new FlowQuestion(
                                "Qual é o seu objetivo?",
                                "objetivo",
                                FlowQuestionType.TEXT,
                                true,
                                "", null,
                                List.of()),
                        new FlowQuestion(
                                "Envie uma imagem",
                                "referencia",
                                FlowQuestionType.IMAGE_UPLOAD,
                                true,
                                "", null,
                                List.of())), null, null, null, null);

        flowService.save(flow);
    }

    @AfterEach
    void cleanup() {
        imagePackageRepository.deleteAll();
        submissionRepository.deleteAll();
        flowService.list().stream()
                .map(Flow::slug)
                .forEach(flowService::delete);
    }

    @Test
    void submitFlowStoresAnswersAndImage() throws Exception {
        Map<String, Object> payload = Map.of(
                "name", "Cliente",
                "email", "cliente@example.com",
                "imageKey", "referencia",
                "answers",
                Map.of(
                        "nome", "Cliente",
                        "email", "cliente@example.com",
                        "objetivo", "Vender todos os dias"));

        MockMultipartFile payloadPart = new MockMultipartFile(
                "payload", "payload", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(payload));
        MockMultipartFile imagePart = new MockMultipartFile(
                "image", "referencia.png", MediaType.IMAGE_PNG_VALUE, "conteudo".getBytes());

        MvcResult result = mockMvc.perform(multipart("/api/flows/planejamento-acao-21-dias/submissions")
                        .file(payloadPart)
                        .file(imagePart))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.flowSlug").value("planejamento-acao-21-dias"))
                .andExpect(jsonPath("$.imageUrl").isNotEmpty())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        UUID id = UUID.fromString(objectMapper.readTree(body).get("id").asText());

        mockMvc.perform(get("/api/flows/submissions/" + id + "/image"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));

        assertThat(imagePackageRepository.count()).isZero();
    }

    @Test
    void acceptsMultipartWithoutPayloadAndPersistsCustomFields() throws Exception {
        MockMultipartFile imagePart = new MockMultipartFile(
                "image", "referencia.png", MediaType.IMAGE_PNG_VALUE, "conteudo".getBytes());

        mockMvc.perform(multipart("/api/flows/planejamento-acao-21-dias/submissions")
                        .file(imagePart)
                        .param("nome", "Cliente HTML")
                        .param("email", "clientehtml@example.com")
                        .param("objetivo", "Gerar autoridade")
                        .param("imageKey", "referencia")
                        .param("campo_extra", "valor livre")
                        .param("preferencias", "Yoga", "Pilates"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.flowSlug").value("planejamento-acao-21-dias"));

        assertThat(submissionRepository.findAll())
                .hasSize(1)
                .first()
                .satisfies(entity -> {
                    assertThat(entity.getName()).isEqualTo("Cliente HTML");
                    assertThat(entity.getEmail()).isEqualTo("clientehtml@example.com");
                    assertThat(entity.getAnswers()).containsEntry("campo_extra", "valor livre");
                    Object preferences = entity.getAnswers().get("preferencias");
                    assertThat(preferences).isInstanceOf(List.class);
                    @SuppressWarnings("unchecked")
                    List<String> prefList = (List<String>) preferences;
                    assertThat(prefList).containsExactly("Yoga", "Pilates");
                });
    }
    @Test
    void submissionSavesCampaignCodeWhenProvided() throws Exception {
        Map<String, Object> payload = Map.of(
                "name", "Cliente",
                "email", "cliente@example.com",
                "imageKey", "referencia",
                "campaignCode", "meta-campanha-42",
                "answers",
                Map.of(
                        "nome", "Cliente",
                        "email", "cliente@example.com",
                        "objetivo", "Vender todos os dias"));

        MockMultipartFile payloadPart = new MockMultipartFile(
                "payload", "payload", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(payload));
        MockMultipartFile imagePart = new MockMultipartFile(
                "image", "referencia.png", MediaType.IMAGE_PNG_VALUE, "conteudo".getBytes());

        mockMvc.perform(multipart("/api/flows/planejamento-acao-21-dias/submissions")
                        .file(payloadPart)
                        .file(imagePart))
                .andExpect(status().isCreated());

        assertThat(submissionRepository.findAll())
                .first()
                .extracting(entity -> entity.getCampaignCode())
                .isEqualTo("meta-campanha-42");
    }

    @Test
    void acceptsPayloadPartSentAsPlainTextJson() throws Exception {
        String payload = """
                {
                  "name": "Cliente Script",
                  "email": "cliente.script@example.com",
                  "imageKey": "referencia",
                  "answers": {
                    "nome": "Cliente Script",
                    "email": "cliente.script@example.com",
                    "objetivo": "Aumentar conversão"
                  }
                }
                """;

        MockMultipartFile payloadPart = new MockMultipartFile(
                "payload", "payload", MediaType.TEXT_PLAIN_VALUE, payload.getBytes());
        MockMultipartFile imagePart = new MockMultipartFile(
                "image", "referencia.png", MediaType.IMAGE_PNG_VALUE, "conteudo".getBytes());

        mockMvc.perform(multipart("/api/flows/planejamento-acao-21-dias/submissions")
                        .file(payloadPart)
                        .file(imagePart))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.flowSlug").value("planejamento-acao-21-dias"));
    }


    @Test
    void missingRequiredImageReturnsBadRequest() throws Exception {
        Map<String, Object> payload = Map.of(
                "name", "Cliente",
                "email", "cliente@example.com",
                "answers",
                Map.of(
                        "nome", "Cliente",
                        "email", "cliente@example.com",
                        "objetivo", "Vender todos os dias"));

        MockMultipartFile payloadPart = new MockMultipartFile(
                "payload", "payload", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(payload));

        mockMvc.perform(multipart("/api/flows/planejamento-acao-21-dias/submissions").file(payloadPart))
                .andExpect(status().isBadRequest());
    }


    @Test
    void simplePersonalTrainerSubmissionAllowsMissingOptionalWorkplaceField() throws Exception {
        Map<String, Object> payload = Map.of(
                "name", "Cliente",
                "email", "cliente@example.com",
                "answers",
                Map.of(
                        "nome", "Cliente",
                        "email", "cliente@example.com",
                        "forma_contato", "WhatsApp",
                        "tipo_aulas", List.of("Musculação")));

        MockMultipartFile payloadPart = new MockMultipartFile(
                "payload", "payload", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(payload));

        mockMvc.perform(multipart("/api/flows/formulario-simples-personal-trainer/submissions").file(payloadPart))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.flowSlug").value("formulario-simples-personal-trainer"));
    }

    @Test
    void submissionIgnoresQuestionsWithoutType() throws Exception {
        Flow flowWithNullType = new Flow(
                "fluxo-sem-tipo",
                "Fluxo com pergunta sem tipo",
                "",
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(
                        new FlowQuestion("Pergunta sem tipo", "semTipo", null, true, "", null, List.of()),
                        new FlowQuestion(
                                "Qual é o seu e-mail?",
                                "email",
                                FlowQuestionType.EMAIL,
                                true,
                                "",
                                null,
                                List.of())), null, null, null, null);

        flowService.save(flowWithNullType);

        Map<String, Object> payload = Map.of(
                "name", "Cliente",
                "email", "cliente@example.com",
                "answers",
                Map.of(
                        "semTipo", "qualquer coisa",
                        "email", "cliente@example.com"));

        MockMultipartFile payloadPart = new MockMultipartFile(
                "payload", "payload", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(payload));

        mockMvc.perform(multipart("/api/flows/fluxo-sem-tipo/submissions").file(payloadPart))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.flowSlug").value("fluxo-sem-tipo"));
    }

    @Test
    void customTemplateFlowsSkipQuestionValidation() throws Exception {
        Flow customTemplateFlow = new Flow(
                "fluxo-custom-html",
                "Fluxo com HTML personalizado",
                "",
                "<html></html>",
                null,
                null,
                null,
                null,
                null,
                List.of(
                        new FlowQuestion(
                                "Forma de contato",
                                "forma_contato",
                                FlowQuestionType.SINGLE_CHOICE,
                                true,
                                "",
                                null,
                                List.of("Instagram", "WhatsApp")),
                        new FlowQuestion(
                                "Especialidades",
                                "lista_opcoes",
                                FlowQuestionType.MULTIPLE_CHOICE,
                                true,
                                "",
                                null,
                                List.of("Musculação", "Cardio"))),
                null, null, null, null);

        flowService.save(customTemplateFlow);

        mockMvc.perform(multipart("/api/flows/fluxo-custom-html/submissions")
                        .param("nome", "Cliente Custom")
                        .param("email", "cliente.custom@example.com")
                        .param("instagram", "@cliente"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.flowSlug").value("fluxo-custom-html"));
    }
}
