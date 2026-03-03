
package com.marketinghub.leadportal.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import com.marketinghub.journey.model.JourneyTemplate;
import com.marketinghub.journey.repository.JourneyTemplateRepository;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.LeadPortalQuestionType;
import com.marketinghub.leadportal.dto.CreateLeadPortalFlowRequest;
import com.marketinghub.leadportal.dto.LeadPortalFlowQuestionRequest;
import com.marketinghub.leadportal.integration.LeadPortalFlowPublisher;
import com.marketinghub.leadportal.repository.LeadPortalFlowRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
        "spring.liquibase.enabled=false",
        "integrations.lead-portal.enabled=true",
        "integrations.lead-portal.base-url=https://portal.example.com"
})
class LeadPortalFlowControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper mapper;
    @Autowired
    LeadPortalFlowRepository repository;
    @Autowired
    ExperimentRepository experimentRepository;
    @Autowired
    MarketNicheRepository marketNicheRepository;
    @Autowired
    HypothesisRepository hypothesisRepository;
    @Autowired
    JourneyTemplateRepository journeyTemplateRepository;

    @MockBean
    LeadPortalFlowPublisher flowPublisher;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
        experimentRepository.deleteAll();
        hypothesisRepository.deleteAll();
        marketNicheRepository.deleteAll();
        journeyTemplateRepository.deleteAll();
    }

    @Test
    void createFlowPersistsQuestions() throws Exception {
        Experiment experiment = createExperiment();
        CreateLeadPortalFlowRequest request = new CreateLeadPortalFlowRequest();
        request.setName("Fluxo Portal");
        request.setSlug("fluxo-portal");
        request.setDescription("Perguntas para leads vindos da campanha A");
        request.setMarketNicheId(experiment.getNiche().getId());
        request.setExperimentId(experiment.getId());
        request.setModel("gpt-4o");
        request.setQuestions(List.of(
                buildQuestion("Qual o seu nome?", "nome", LeadPortalQuestionType.TEXT, true, List.of()),
                buildQuestion("Envie uma imagem do produto", "imagem_produto", LeadPortalQuestionType.IMAGE_UPLOAD, false, List.of())
        ));

        mockMvc.perform(post("/api/lead-portal-flows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].dataKey").value("nome"))
                .andExpect(jsonPath("$.questions[1].type").value("IMAGE_UPLOAD"))
                .andExpect(jsonPath("$.experimentId").value(experiment.getId()))
                .andExpect(jsonPath("$.model").value("gpt-4o"))
                .andExpect(jsonPath("$.publicUrl").doesNotExist());

        assertThat(repository.count()).isEqualTo(1);
        LeadPortalFlow saved = repository.findAll().get(0);
        assertThat(saved.getQuestions()).hasSize(2);
        assertThat(saved.getQuestions().get(0).getPosition()).isZero();
        assertThat(saved.getExperiment()).isNotNull();
    }


    @Test
    void createFlowSupportsLongTextFields() throws Exception {
        Experiment experiment = createExperiment();
        String longText = "A".repeat(1200);

        CreateLeadPortalFlowRequest request = new CreateLeadPortalFlowRequest();
        request.setName("Fluxo Longo");
        request.setSlug("fluxo-longo");
        request.setDescription(longText);
        request.setMarketNicheId(experiment.getNiche().getId());
        request.setExperimentId(experiment.getId());
        request.setQuestions(List.of(
                buildQuestion(longText, longText, LeadPortalQuestionType.TEXTAREA, true, List.of(longText))
        ));
        request.getQuestions().get(0).setDescription(longText);
        request.getQuestions().get(0).setPlaceholder(longText);

        mockMvc.perform(post("/api/lead-portal-flows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].title").value(longText))
                .andExpect(jsonPath("$.questions[0].dataKey").value(longText))
                .andExpect(jsonPath("$.questions[0].description").value(longText))
                .andExpect(jsonPath("$.questions[0].placeholder").value(longText));

        LeadPortalFlow saved = repository.findBySlug("fluxo-longo").orElseThrow();
        assertThat(saved.getQuestions().get(0).getOptions()).containsExactly(longText);
    }

    @Test
    void duplicateSlugReturnsConflict() throws Exception {
        Experiment experiment = createExperiment();
        repository.save(LeadPortalFlow.builder()
                .name("Fluxo existente")
                .slug("fluxo-duplicado")
                .marketNiche(experiment.getNiche())
                .experiment(experiment)
                .build());

        CreateLeadPortalFlowRequest request = new CreateLeadPortalFlowRequest();
        request.setName("Fluxo Portal");
        request.setSlug("fluxo-duplicado");
        request.setMarketNicheId(experiment.getNiche().getId());
        request.setExperimentId(experiment.getId());
        request.setQuestions(List.of(
                buildQuestion("Qual o seu e-mail?", "email", LeadPortalQuestionType.EMAIL, true, List.of())
        ));

        mockMvc.perform(post("/api/lead-portal-flows")
                        .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void approvalEndpointUpdatesStatus() throws Exception {
        Experiment experiment = createExperiment();
        LeadPortalFlow flow = repository.save(LeadPortalFlow.builder()
                .name("Fluxo existente")
                .slug("fluxo-aprovacao")
                .marketNiche(experiment.getNiche())
                .experiment(experiment)
                .build());

        mockMvc.perform(patch("/api/lead-portal-flows/" + flow.getId() + "/approval")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approved").value(true))
                .andExpect(jsonPath("$.publicUrl").value("https://portal.example.com/flows/fluxo-aprovacao"));

        LeadPortalFlow updated = repository.findById(flow.getId()).orElseThrow();
        assertThat(updated.isApproved()).isTrue();
        assertThat(updated.getApprovedAt()).isNotNull();
    }

    private Experiment createExperiment() {
        MarketNiche niche = marketNicheRepository.save(MarketNiche.builder().name("Nicho Teste").build());
        Hypothesis hypothesis = hypothesisRepository.save(Hypothesis.builder()
                .title("Hipótese Teste")
                .marketNiche(niche)
                .build());
        JourneyTemplate template = journeyTemplateRepository.save(JourneyTemplate.builder()
                .name("Template Jornada")
                .build());
        Experiment experiment = Experiment.builder()
                .niche(niche)
                .name("Experimento Lead Portal")
                .hypothesis("Resumo")
                .hypothesisRef(hypothesis)
                .journeyTemplate(template)
                .status(ExperimentStatus.PLANNED)
                .platform(ExperimentPlatform.FACEBOOK)
                .build();
        return experimentRepository.save(experiment);
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
