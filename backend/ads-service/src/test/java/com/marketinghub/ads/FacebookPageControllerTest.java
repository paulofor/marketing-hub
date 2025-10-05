package com.marketinghub.ads;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import com.marketinghub.ads.InstagramAccount;
import com.marketinghub.ads.InstagramAccountRepository;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.liquibase.enabled=false"
})
class FacebookPageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    FacebookAccountRepository accountRepository;

    @Autowired
    FacebookPageRepository pageRepository;

    @Autowired
    ExperimentRepository experimentRepository;

    @Autowired
    MarketNicheRepository nicheRepository;

    @Autowired
    HypothesisRepository hypothesisRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    InstagramAccountRepository instagramAccountRepository;

    FacebookAccount account;

    @BeforeEach
    void setup() {
        experimentRepository.deleteAll();
        hypothesisRepository.deleteAll();
        nicheRepository.deleteAll();
        pageRepository.deleteAll();
        accountRepository.deleteAll();
        instagramAccountRepository.deleteAll();
        account = accountRepository.save(FacebookAccount.builder()
                .name("Account")
                .currency("BRL")
                .build());
    }

    @Test
    void shouldCreateListAndDeletePages() throws Exception {
        FacebookPageController.UpsertFacebookPageRequest request = new FacebookPageController.UpsertFacebookPageRequest(
                "123456",
                "Página Principal"
        );

        mockMvc.perform(post("/api/accounts/facebook/" + account.getId() + "/pages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageId").value("123456"))
                .andExpect(jsonPath("$.name").value("Página Principal"));

        FacebookPage saved = pageRepository.findAll().getFirst();

        Experiment experiment = createExperiment(saved);

        FacebookPageController.UpsertFacebookPageRequest update = new FacebookPageController.UpsertFacebookPageRequest(
                "654321",
                "Página Atualizada"
        );

        mockMvc.perform(put("/api/accounts/facebook/" + account.getId() + "/pages/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageId").value("654321"))
                .andExpect(jsonPath("$.name").value("Página Atualizada"));

        mockMvc.perform(get("/api/accounts/facebook/" + account.getId() + "/pages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].pageId").value("654321"));

        Experiment updatedExperiment = experimentRepository.findById(experiment.getId()).orElseThrow();
        assertThat(updatedExperiment.getFacebookPage()).isNotNull();
        assertThat(updatedExperiment.getFacebookPage().getId()).isEqualTo(saved.getId());
        assertThat(updatedExperiment.getFacebookPage().getPageId()).isEqualTo("654321");

        mockMvc.perform(delete("/api/accounts/facebook/" + account.getId() + "/pages/" + saved.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/accounts/facebook/" + account.getId() + "/pages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        Experiment clearedExperiment = experimentRepository.findById(experiment.getId()).orElseThrow();
        assertThat(clearedExperiment.getFacebookPage()).isNull();
    }

    private Experiment createExperiment(FacebookPage page) {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder()
                .name("Niche " + page.getPageId())
                .build());
        Hypothesis hypothesis = hypothesisRepository.save(Hypothesis.builder()
                .marketNiche(niche)
                .title("Hypothesis " + page.getPageId())
                .build());
        InstagramAccount instagramAccount = instagramAccountRepository.save(InstagramAccount.builder()
                .name("IG " + page.getPageId())
                .handle("@" + page.getPageId())
                .code("IG-" + page.getPageId())
                .build());
        Experiment experiment = Experiment.builder()
                .niche(niche)
                .name("Experiment " + page.getPageId())
                .hypothesis("Hypothesis " + page.getPageId())
                .hypothesisRef(hypothesis)
                .status(ExperimentStatus.PLANNED)
                .platform(ExperimentPlatform.FACEBOOK)
                .creativeApproved(true)
                .facebookPage(page)
                .instagramAccount(instagramAccount)
                .build();
        experiment.setCreativesToGenerate(0);
        experiment.setKpiTargetCpl(BigDecimal.ONE);
        return experimentRepository.save(experiment);
    }
}
