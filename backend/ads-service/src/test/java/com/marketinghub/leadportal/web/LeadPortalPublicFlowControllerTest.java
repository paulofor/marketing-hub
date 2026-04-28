package com.marketinghub.leadportal.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.LeadPortalFlowQuestion;
import com.marketinghub.leadportal.LeadPortalQuestionType;
import com.marketinghub.leadportal.repository.LeadPortalFlowRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    @BeforeEach
    void cleanDatabase() {
        flowRepository.deleteAll();
        marketNicheRepository.deleteAll();
    }

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

    @Test
    void getLandingPageBySlugReturnsHtmlDocumentFromStructuredJsonPayload() throws Exception {
        MarketNiche niche = marketNicheRepository.save(MarketNiche.builder().name("Nicho Teste").build());
        flowRepository.save(LeadPortalFlow.builder()
                .name("Fluxo Landing")
                .slug("exp-13-landing")
                .approved(true)
                .marketNiche(niche)
                .customFormHtml("""
                        {"landingPageHtml":"{\\"htmlDocument\\":\\"<!doctype html><html lang='pt-BR'><body><h1>Pare de vender por preço</h1></body></html>\\"}"}
                        """)
                .build());

        mockMvc.perform(get("/api/flows/exp-13-landing/page"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(content().string("<!doctype html><html lang='pt-BR'><body><h1>Pare de vender por preço</h1></body></html>"));
    }

    @Test
    void getLandingPageBySlugNormalizesJsonInsideHtmlWrapper() throws Exception {
        MarketNiche niche = marketNicheRepository.save(MarketNiche.builder().name("Nicho Teste").build());
        String nestedDocument = "<!doctype html><html lang=\"pt-BR\"><body><main><h1>Landing Normalizada</h1></main></body></html>";
        ObjectMapper mapper = new ObjectMapper();
        String jsonPayload = mapper.writeValueAsString(Map.of(
                "landingPageHtml", Map.of("htmlDocument", nestedDocument)
        ));
        String hybridHtml = """
                <html lang="pt-BR">
                  <head><title>Wrapper</title></head>
                  <body>
                    %s
                  </body>
                </html>
                """.formatted(jsonPayload);
        flowRepository.save(LeadPortalFlow.builder()
                .name("Fluxo Landing")
                .slug("exp-15-landing")
                .approved(true)
                .marketNiche(niche)
                .customFormHtml(hybridHtml)
                .build());

        mockMvc.perform(get("/api/flows/exp-15-landing/page"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(content().string(nestedDocument));
    }

    @Test
    void getLandingPageBySlugReturnsHtmlWhenLandingPayloadUsesArtifactEnvelope() throws Exception {
        MarketNiche niche = marketNicheRepository.save(MarketNiche.builder().name("Nicho Teste").build());
        String nestedDocument = "<!doctype html><html lang=\"pt-BR\"><body><main><h1>Landing via artifact.content</h1></main></body></html>";
        ObjectMapper mapper = new ObjectMapper();
        String payload = mapper.writeValueAsString(Map.of(
                "artifact", Map.of(
                        "content", Map.of(
                                "landingPageHtml", Map.of("htmlDocument", nestedDocument)
                        )
                )
        ));
        flowRepository.save(LeadPortalFlow.builder()
                .name("Fluxo Landing")
                .slug("exp-16-landing")
                .approved(true)
                .marketNiche(niche)
                .customFormHtml(payload)
                .build());

        mockMvc.perform(get("/api/flows/exp-16-landing/page"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(content().string(nestedDocument));
    }

    @Test
    void getLandingPageBySlugReturnsRawHtmlWhenLandingPageHtmlIsPlainText() throws Exception {
        MarketNiche niche = marketNicheRepository.save(MarketNiche.builder().name("Nicho Teste").build());
        String nestedDocument = "<!doctype html><html lang=\"pt-BR\"><body><main><h1>Landing raw html</h1></main></body></html>";
        ObjectMapper mapper = new ObjectMapper();
        String payload = mapper.writeValueAsString(Map.of(
                "landingPageHtml", nestedDocument
        ));
        flowRepository.save(LeadPortalFlow.builder()
                .name("Fluxo Landing")
                .slug("exp-17-landing")
                .approved(true)
                .marketNiche(niche)
                .customFormHtml(payload)
                .build());

        mockMvc.perform(get("/api/flows/exp-17-landing/page"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(content().string(nestedDocument));
    }

    @Test
    void getLandingPageBySlugExtractsInlineHtmlWhenPayloadHasJsonPrefix() throws Exception {
        MarketNiche niche = marketNicheRepository.save(MarketNiche.builder().name("Nicho Teste").build());
        String nestedDocument = "<!doctype html><html lang=\"pt-BR\"><body><main><h1>Landing extraída</h1></main></body></html>";
        String prefixedPayload = """
                {"landingPageHtml":"{\\"htmlDocument\\":\\"texto quebrado\\"}"}
                %s
                """.formatted(nestedDocument);
        flowRepository.save(LeadPortalFlow.builder()
                .name("Fluxo Landing")
                .slug("exp-18-landing")
                .approved(true)
                .marketNiche(niche)
                .customFormHtml(prefixedPayload)
                .build());

        mockMvc.perform(get("/api/flows/exp-18-landing/page"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(content().string(nestedDocument));
    }
}
