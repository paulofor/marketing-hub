package com.marketinghub.leadportal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.leadportal.dto.FlowQuestionRequest;
import com.marketinghub.leadportal.dto.UpsertFlowRequest;
import com.marketinghub.leadportal.model.Flow;
import com.marketinghub.leadportal.model.FlowQuestionType;
import com.marketinghub.leadportal.service.FlowService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FlowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FlowService flowService;

    @BeforeEach
    void clearFlows() {
        flowService.list().stream()
                .map(com.marketinghub.leadportal.model.Flow::slug)
                .forEach(flowService::delete);
    }

    @Test
    void upsertAndRetrieveFlow() throws Exception {
        UpsertFlowRequest request = buildRequest();

        mockMvc.perform(put("/api/flows/diagnostico")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("diagnostico"))
                .andExpect(jsonPath("$.questions[0].type").value("TEXT"));

        mockMvc.perform(get("/api/flows/diagnostico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Diagnóstico"))
                .andExpect(jsonPath("$.model").doesNotExist())
                .andExpect(jsonPath("$.prompt").doesNotExist());
    }

    @Test
    void deleteFlowRemovesDefinition() throws Exception {
        mockMvc.perform(put("/api/flows/excluir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/flows/excluir"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/flows/excluir"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getFlowWithoutQuestionsReturnsEmptyList() throws Exception {
        Flow flow = new Flow("sem-perguntas", "Sem perguntas", "Descrição", null, null, null);
        flowService.save(flow);

        mockMvc.perform(get("/api/flows/sem-perguntas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions").isArray())
                .andExpect(jsonPath("$.questions", hasSize(0)));
    }

    private UpsertFlowRequest buildRequest() {
        FlowQuestionRequest question = new FlowQuestionRequest();
        question.setTitle("Qual o seu nome?");
        question.setDataKey("nome");
        question.setType(FlowQuestionType.TEXT);
        question.setRequired(true);

        UpsertFlowRequest request = new UpsertFlowRequest();
        request.setName("Diagnóstico");
        request.setDescription("Descubra seu potencial");
        request.setQuestions(List.of(question));
        return request;
    }
}
