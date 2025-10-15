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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
    void allowsCreatingInstantFormWithoutFacebookIdentifier() throws Exception {
        CreateFacebookInstantFormRequest request = new CreateFacebookInstantFormRequest(
                page.getId(),
                null,
                "Lead Magnet",
                "DRAFT",
                "pt_BR",
                null,
                null,
                null,
                "https://example.com/thanks",
                "https://example.com/privacy",
                "gpt-4o",
                "Prompt IA"
        );

        mockMvc.perform(post("/api/hypotheses/" + hypothesis.getId() + "/instant-forms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.facebookFormId").value((Object) null))
                .andExpect(jsonPath("$.approved").value(false));

        FacebookInstantForm form = instantFormRepository.findAll().getFirst();

        mockMvc.perform(patch("/api/instant-forms/" + form.getId() + "/approval")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\":true}"))
                .andExpect(status().isOk());

        instantFormRepository.flush();

        mockMvc.perform(get("/api/instant-forms/ready-to-publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].facebookFormId").value((Object) null));
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
                .andExpect(jsonPath("$.model").value("gpt-4o"))
                .andExpect(jsonPath("$.approved").value(false))
                .andExpect(jsonPath("$.published").value(false));

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
                .andExpect(jsonPath("$[0].leadsCount").value(42))
                .andExpect(jsonPath("$[0].approved").value(false))
                .andExpect(jsonPath("$[0].published").value(false));

        FacebookInstantForm form = instantFormRepository.findAll().get(0);

        mockMvc.perform(get("/api/instant-forms/" + form.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(form.getId()))
                .andExpect(jsonPath("$.prompt").value("Prompt de teste"));

        mockMvc.perform(patch("/api/instant-forms/" + form.getId() + "/approval")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approved").value(true))
                .andExpect(jsonPath("$.approvedAt").exists());

        instantFormRepository.flush();

        mockMvc.perform(get("/api/instant-forms/ready-to-publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].facebookFormId").value("FORM-123"))
                .andExpect(jsonPath("$[0].shareLink").value((Object) null));

        mockMvc.perform(patch("/api/instant-forms/" + form.getId() + "/publication")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"published\":true,\"shareLink\":\"https://facebook.com/ads/leadgen/?id=FORM-123\",\"status\":\"ACTIVE\",\"publishedAt\":\"2024-08-06T10:00:00Z\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(true))
                .andExpect(jsonPath("$.shareLink").value("https://facebook.com/ads/leadgen/?id=FORM-123"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        instantFormRepository.flush();

        mockMvc.perform(patch("/api/instant-forms/" + form.getId() + "/publication")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"published\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(false))
                .andExpect(jsonPath("$.shareLink").value((Object) null));

        mockMvc.perform(patch("/api/instant-forms/" + form.getId() + "/approval")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approved").value(false))
                .andExpect(jsonPath("$.approvedAt").doesNotExist());

        mockMvc.perform(delete("/api/instant-forms/" + form.getId()))
                .andExpect(status().isNoContent());

        assertThat(instantFormRepository.findAll()).isEmpty();
    }
}
