package com.marketinghub.leadportal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.leadportal.dto.FlowQuestionRequest;
import com.marketinghub.leadportal.dto.UpsertFlowRequest;
import com.marketinghub.leadportal.model.Flow;
import com.marketinghub.leadportal.model.FlowQuestionType;
import com.marketinghub.leadportal.service.FlowService;
import com.marketinghub.leadportal.entity.FlowAccessEntity;
import com.marketinghub.leadportal.repository.FlowAccessRepository;
import java.util.List;
import jakarta.servlet.http.Cookie;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

    @Autowired
    private FlowAccessRepository flowAccessRepository;

    @Autowired
    private MeterRegistry meterRegistry;

    @BeforeEach
    void clearFlows() {
        flowService.list().stream()
                .map(com.marketinghub.leadportal.model.Flow::slug)
                .forEach(flowService::delete);

        flowAccessRepository.deleteAll();
        meterRegistry.clear();
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
    void simpleFlowIsServedFromCatalogWithoutDatabaseRoundtrip() throws Exception {
        mockMvc.perform(get("/api/flows/formulario-simples-personal-trainer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("formulario-simples-personal-trainer"))
                .andExpect(jsonPath("$.name").value("Formulário simples para personal trainer"))
                .andExpect(jsonPath("$.questions", hasSize(6)))
                .andExpect(jsonPath("$.questions[0].title").value("Nome"))
                .andExpect(jsonPath("$.questions[2].type").value("SINGLE_CHOICE"))
                .andExpect(jsonPath("$.questions[2].options", hasSize(3)));

        assertThat(flowAccessRepository.findAll()).isEmpty();
    }


    @Test
    void getFlowRegistersVisitorCookieOnAccess() throws Exception {
        mockMvc.perform(put("/api/flows/diagnostico")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/flows/diagnostico")
                        .cookie(new Cookie("marketinghub_visitor_id", "visitor-123")))
                .andExpect(status().isOk());

        List<FlowAccessEntity> accesses = flowAccessRepository.findAll();
        assertThat(accesses)
                .singleElement()
                .extracting(FlowAccessEntity::getVisitorId)
                .isEqualTo("visitor-123");
    }

    @Test
    void flowAccessesAreExposedAsPrometheusMetrics() throws Exception {
        mockMvc.perform(put("/api/flows/diagnostico")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/flows/diagnostico"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/lead-portal/metrics"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("lead_portal_flow_access_total{slug=\"diagnostico\"")));

        double count = meterRegistry.counter("lead_portal_flow_access_total", "slug", "diagnostico").count();
        assertThat(count).isEqualTo(1d);
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
