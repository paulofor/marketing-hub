package com.marketinghub.leadportal.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.LeadPortalQuestionType;
import com.marketinghub.leadportal.dto.CreateLeadPortalFlowRequest;
import com.marketinghub.leadportal.dto.LeadPortalFlowQuestionRequest;
import com.marketinghub.leadportal.repository.LeadPortalFlowRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AdsServiceApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.liquibase.enabled=false"
})
class LeadPortalFlowControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper mapper;
    @Autowired
    LeadPortalFlowRepository repository;

    @Test
    void createFlowPersistsQuestions() throws Exception {
        CreateLeadPortalFlowRequest request = new CreateLeadPortalFlowRequest();
        request.setName("Fluxo Portal");
        request.setSlug("fluxo-portal");
        request.setDescription("Perguntas para leads vindos da campanha A");
        request.setQuestions(List.of(
                buildQuestion("Qual o seu nome?", "nome", LeadPortalQuestionType.TEXT, true, List.of()),
                buildQuestion("Envie uma imagem do produto", "imagem_produto", LeadPortalQuestionType.IMAGE_UPLOAD, false, List.of())
        ));

        mockMvc.perform(post("/api/lead-portal-flows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].dataKey").value("nome"))
                .andExpect(jsonPath("$.questions[1].type").value("IMAGE_UPLOAD"));

        assertThat(repository.count()).isEqualTo(1);
        LeadPortalFlow saved = repository.findAll().get(0);
        assertThat(saved.getQuestions()).hasSize(2);
        assertThat(saved.getQuestions().get(0).getPosition()).isZero();
    }

    @Test
    void duplicateSlugReturnsConflict() throws Exception {
        repository.save(LeadPortalFlow.builder()
                .name("Fluxo existente")
                .slug("fluxo-duplicado")
                .build());

        CreateLeadPortalFlowRequest request = new CreateLeadPortalFlowRequest();
        request.setName("Fluxo Portal");
        request.setSlug("fluxo-duplicado");
        request.setQuestions(List.of(
                buildQuestion("Qual o seu e-mail?", "email", LeadPortalQuestionType.EMAIL, true, List.of())
        ));

        mockMvc.perform(post("/api/lead-portal-flows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    private LeadPortalFlowQuestionRequest buildQuestion(String title,
                                                        String dataKey,
                                                        LeadPortalQuestionType type,
                                                        boolean required,
                                                        List<String> options) {
        LeadPortalFlowQuestionRequest request = new LeadPortalFlowQuestionRequest();
        request.setTitle(title);
        request.setDataKey(dataKey);
        request.setType(type);
        request.setRequired(required);
        request.setOptions(options);
        return request;
    }
}
