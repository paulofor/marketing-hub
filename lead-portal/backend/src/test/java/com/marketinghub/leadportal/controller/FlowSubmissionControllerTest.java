package com.marketinghub.leadportal.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.leadportal.model.Flow;
import com.marketinghub.leadportal.model.FlowQuestion;
import com.marketinghub.leadportal.model.FlowQuestionType;
import com.marketinghub.leadportal.repository.FlowSubmissionRepository;
import com.marketinghub.leadportal.service.FlowService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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

    @BeforeEach
    void setup() {
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
                                List.of())));

        flowService.save(flow);
    }

    @AfterEach
    void cleanup() {
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
}
