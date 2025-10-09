package com.marketinghub.ads;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.ads.dto.CreateFacebookInstantFormRequest;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import com.marketinghub.experiment.repository.ExperimentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class FacebookInstantFormControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    FacebookInstantFormRepository instantFormRepository;

    @Autowired
    FacebookPageRepository pageRepository;

    @Autowired
    FacebookAccountRepository accountRepository;

    @Autowired
    ExperimentRepository experimentRepository;

    @Autowired
    HypothesisRepository hypothesisRepository;

    @Autowired
    MarketNicheRepository nicheRepository;

    Hypothesis hypothesis;
    FacebookPage page;

    @BeforeEach
    void setup() {
        instantFormRepository.deleteAll();
        experimentRepository.deleteAll();
        pageRepository.deleteAll();
        accountRepository.deleteAll();
        hypothesisRepository.deleteAll();
        nicheRepository.deleteAll();

        FacebookAccount account = accountRepository.save(FacebookAccount.builder()
                .name("Account")
                .currency("BRL")
                .build());
        page = pageRepository.save(FacebookPage.builder()
                .account(account)
                .pageId("1234567890")
                .name("Página Teste")
                .build());
        MarketNiche niche = nicheRepository.save(MarketNiche.builder()
                .name("Niche")
                .build());
        hypothesis = hypothesisRepository.save(Hypothesis.builder()
                .marketNiche(niche)
                .title("Hipótese")
                .build());
    }

    @Test
    void shouldCreateAndListInstantForms() throws Exception {
        CreateFacebookInstantFormRequest request = new CreateFacebookInstantFormRequest(
                page.getId(),
                "FORM-123",
                "Formulário Principal",
                "ACTIVE",
                "pt_BR",
                42L,
                Instant.parse("2024-08-01T10:15:30Z"),
                Instant.parse("2024-08-05T12:00:00Z"),
                "https://example.com/thanks",
                "https://example.com/privacy",
                "gpt-4o",
                "Prompt de teste"
        );

        mockMvc.perform(post("/api/hypotheses/" + hypothesis.getId() + "/instant-forms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Formulário Principal"))
                .andExpect(jsonPath("$.facebookFormId").value("FORM-123"))
                .andExpect(jsonPath("$.model").value("gpt-4o"));

        assertThat(instantFormRepository.findAll()).hasSize(1)
                .first()
                .satisfies(form -> {
                    assertThat(form.getHypothesis().getId()).isEqualTo(hypothesis.getId());
                    assertThat(form.getPrompt()).isEqualTo("Prompt de teste");
                    assertThat(form.getModel()).isEqualTo("gpt-4o");
                });

        mockMvc.perform(get("/api/hypotheses/" + hypothesis.getId() + "/instant-forms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].facebookPageName").value("Página Teste"))
                .andExpect(jsonPath("$[0].leadsCount").value(42));
    }
}
