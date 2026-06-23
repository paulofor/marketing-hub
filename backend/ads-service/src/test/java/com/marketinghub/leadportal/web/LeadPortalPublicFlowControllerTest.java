package com.marketinghub.leadportal.web;

import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.LeadPortalFlowQuestion;
import com.marketinghub.leadportal.LeadPortalQuestionType;
import com.marketinghub.repository.jpa.leadportal.LeadPortalFlowRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica os endpoints públicos do Lead Portal e o contrato HTML renderizado da landing.
 */
@SpringBootTest(classes = AdsServiceApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:leadportal-public-flow;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.liquibase.enabled=false"
})
class LeadPortalPublicFlowControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    LeadPortalFlowRepository flowRepository;
    @Autowired
    MarketNicheRepository marketNicheRepository;

    /**
     * Limpa os dados persistidos antes de cada verificação dos endpoints públicos do fluxo.
     */
    @BeforeEach
    void cleanDatabase() {
        flowRepository.deleteAll();
        marketNicheRepository.deleteAll();
    }

    /**
     * Verifica que um fluxo aprovado é retornado pelo endpoint JSON público.
     */
    @Test
    void getBySlugReturnsApprovedFlow() throws Exception {
        MarketNiche niche = marketNicheRepository.save(MarketNiche.builder().name("Nicho Teste").build());
        LeadPortalFlow flow = LeadPortalFlow.builder()
                .name("Fluxo Landing")
                .slug("exp-10-landing")
                .approved(true)
                .marketNiche(niche)
                .build();

        LeadPortalFlowQuestion question = LeadPortalFlowQuestion.builder()
                .flow(flow)
                .title("Qual é o seu nome?")
                .dataKey("nome")
                .type(LeadPortalQuestionType.TEXT)
                .required(true)
                .position(0)
                .options(List.of())
                .build();
        flow.setQuestions(List.of(question));
        flowRepository.save(flow);

        mockMvc.perform(get("/api/flows/exp-10-landing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("exp-10-landing"))
                .andExpect(jsonPath("$.name").value("Fluxo Landing"))
                .andExpect(jsonPath("$.renderMode").value("fallback"))
                .andExpect(jsonPath("$.formSpec.questions[0].dataKey").value("nome"))
                .andExpect(jsonPath("$.questions[0].dataKey").value("nome"));
    }


    /**
     * Verifica que a landing pública injeta visitorId first-party sem contaminar o HTML com metadados técnicos.
     */
    @Test
    void getLandingPageBySlugInjectsVisitorAnalyticsWithoutTechnicalMetadata() throws Exception {
        MarketNiche niche = marketNicheRepository.save(MarketNiche.builder().name("Nicho Teste").build());
        flowRepository.save(LeadPortalFlow.builder()
                .name("Fluxo Landing")
                .slug("exp-10-landing")
                .approved(true)
                .customFormHtml("<html><head><title>Landing</title></head><body><section id=\"hero\"></section></body></html>")
                .marketNiche(niche)
                .build());

        String html = mockMvc.perform(get("/api/flows/exp-10-landing/page"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(html).contains("data-mh-landing-analytics=\"true\"");
        assertThat(html)
                .contains("var visitorId = safeGet('localStorage', 'mh_lp_visitor_' + slugValue) || randomId('visitor');");
        assertThat(html).contains("safeSet('localStorage', 'mh_lp_visitor_' + slugValue, visitorId);");
        assertThat(html)
                .contains("var sessionId = safeGet('sessionStorage', 'mh_lp_session_' + slugValue) || randomId('session');");
        assertThat(html).contains("safeSet('sessionStorage', 'mh_lp_session_' + slugValue, sessionId);");
        assertThat(html).contains("visitorId: visitorId");
        assertThat(html).contains("sessionId: sessionId");
        assertThat(html).contains("pageUrl: window.location.href");
        assertThat(html).contains("occurredAt: new Date().toISOString()");
        assertThat(html).contains("userAgent: navigator.userAgent || ''");
        assertThat(html).contains("window.crypto.randomUUID");
        assertThat(html).doesNotContain("<!-- AUTO:");
        assertThat(html).doesNotContain("debugInfo");
        assertThat(html).doesNotContain("legacyPreviewHtml");
        assertThat(html).doesNotContain("renderMode");
    }

    /**
     * Verifica que um fluxo não aprovado permanece indisponível no endpoint JSON público.
     */
    @Test
    void getBySlugReturnsNotFoundWhenFlowIsNotApproved() throws Exception {
        MarketNiche niche = marketNicheRepository.save(MarketNiche.builder().name("Nicho Teste").build());
        flowRepository.save(LeadPortalFlow.builder()
                .name("Fluxo Rascunho")
                .slug("exp-10-landing")
                .approved(false)
                .marketNiche(niche)
                .build());

        mockMvc.perform(get("/api/flows/exp-10-landing"))
                .andExpect(status().isNotFound());
    }

}
