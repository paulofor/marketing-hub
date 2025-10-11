package com.marketinghub.experiment.web;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import com.marketinghub.journey.model.Journey;
import com.marketinghub.journey.model.JourneyPhase;
import com.marketinghub.journey.model.JourneyStep;
import com.marketinghub.journey.model.JourneyStimulusType;
import com.marketinghub.journey.model.JourneyTemplate;
import com.marketinghub.journey.repository.JourneyRepository;
import com.marketinghub.journey.repository.JourneyStepRepository;
import com.marketinghub.journey.repository.JourneyTemplateRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb2",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.liquibase.enabled=false"
})
class ExperimentEmailControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ExperimentRepository experimentRepository;

    @Autowired
    JourneyRepository journeyRepository;

    @Autowired
    JourneyTemplateRepository journeyTemplateRepository;

    @Autowired
    JourneyStepRepository journeyStepRepository;

    @Autowired
    MarketNicheRepository nicheRepository;

    @Autowired
    HypothesisRepository hypothesisRepository;

    Experiment experiment;
    JourneyStep emailStep;
    Journey journey;

    @BeforeEach
    void setup() {
        journeyRepository.deleteAll();
        experimentRepository.deleteAll();
        journeyStepRepository.deleteAll();
        journeyTemplateRepository.deleteAll();
        hypothesisRepository.deleteAll();
        nicheRepository.deleteAll();

        MarketNiche niche = nicheRepository.save(MarketNiche.builder()
                .name("Finanças")
                .build());
        Hypothesis hypothesis = hypothesisRepository.save(Hypothesis.builder()
                .marketNiche(niche)
                .title("Hipótese A")
                .build());
        JourneyTemplate template = journeyTemplateRepository.save(JourneyTemplate.builder()
                .name("Template CRM")
                .build());
        emailStep = journeyStepRepository.save(JourneyStep.builder()
                .template(template)
                .position(1)
                .name("Boas-vindas")
                .phase(JourneyPhase.ATTENTION)
                .stimulusType(JourneyStimulusType.EMAIL)
                .metadata(Map.of("tone", "warm"))
                .build());
        experiment = experimentRepository.save(Experiment.builder()
                .niche(niche)
                .name("Experimento 1")
                .hypothesis("Testar onboarding")
                .hypothesisRef(hypothesis)
                .journeyTemplate(template)
                .platform(ExperimentPlatform.FACEBOOK)
                .status(ExperimentStatus.PLANNED)
                .creativeApproved(false)
                .build());
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put(key("subject"), "Bem-vindo à comunidade");
        metadata.put(key("templateId"), "tpl-123");
        metadata.put(key("status"), "draft");
        metadata.put(key("notes"), "Focar no benefício principal");
        metadata.put(key("preheader"), "Comece hoje mesmo");
        metadata.put(key("model"), "gpt-4o");
        metadata.put(key("prompt"), "Prompt de geração");
        journey = journeyRepository.save(Journey.builder()
                .template(template)
                .name("Jornada teste")
                .experiment(experiment)
                .metadata(metadata)
                .build());
    }

    @Test
    void shouldReturnEmailDetail() throws Exception {
        mockMvc.perform(get("/api/experiments/" + experiment.getId() + "/emails/" + emailStep.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stepName").value("Boas-vindas"))
                .andExpect(jsonPath("$.journeyId").value(journey.getId()))
                .andExpect(jsonPath("$.subject").value("Bem-vindo à comunidade"))
                .andExpect(jsonPath("$.model").value("gpt-4o"))
                .andExpect(jsonPath("$.approved").value(false));
    }

    @Test
    void shouldApproveAndRevokeEmail() throws Exception {
        mockMvc.perform(patch("/api/experiments/" + experiment.getId() + "/emails/" + emailStep.getId() + "/approval")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("approved"))
                .andExpect(jsonPath("$.approved").value(true));

        Journey updated = journeyRepository.findById(journey.getId()).orElseThrow();
        assertThat(updated.getMetadata().get(key("status"))).isEqualTo("approved");

        mockMvc.perform(patch("/api/experiments/" + experiment.getId() + "/emails/" + emailStep.getId() + "/approval")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("review"))
                .andExpect(jsonPath("$.approved").value(false));
    }

    @Test
    void shouldDeleteEmailMetadata() throws Exception {
        mockMvc.perform(delete("/api/experiments/" + experiment.getId() + "/emails/" + emailStep.getId()))
                .andExpect(status().isNoContent());

        Journey updated = journeyRepository.findById(journey.getId()).orElseThrow();
        assertThat(updated.getMetadata()).doesNotContainKeys(
                key("subject"), key("templateId"), key("status"), key("notes"), key("model"));
    }

    private String key(String field) {
        return "email.step." + emailStep.getId() + "." + field;
    }
}
