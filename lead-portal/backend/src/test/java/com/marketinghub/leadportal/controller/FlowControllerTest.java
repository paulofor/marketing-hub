package com.marketinghub.leadportal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

/**
 * Testa o contrato público de criação, consulta e entrega standalone de fluxos do Lead Portal.
 */
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

    /**
     * Limpa fluxos, acessos e métricas entre os cenários de teste.
     */
    @BeforeEach
    void clearFlows() {
        flowService.list().stream()
                .map(com.marketinghub.leadportal.model.Flow::slug)
                .forEach(flowService::delete);

        flowAccessRepository.deleteAll();
        meterRegistry.clear();
    }

    /**
     * Valida criação e consulta básica de fluxo com perguntas.
     */
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

    /**
     * Valida entrega de fluxo simples do catálogo e registro de acesso.
     */
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

        assertThat(flowAccessRepository.findAll())
                .singleElement()
                .extracting(FlowAccessEntity::getFlowSlug)
                .isEqualTo("formulario-simples-personal-trainer");
    }


    /**
     * Valida captura do cookie de visitante no acesso ao fluxo.
     */
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

    /**
     * Valida captura do código de campanha vindo da query string.
     */
    @Test
    void getFlowCapturesCampaignCodeFromQueryParameter() throws Exception {
        mockMvc.perform(put("/api/flows/diagnostico")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/flows/diagnostico").param("campaign", "campanha-01"))
                .andExpect(status().isOk());

        assertThat(flowAccessRepository.findAll())
                .singleElement()
                .extracting(FlowAccessEntity::getCampaignCode)
                .isEqualTo("campanha-01");
    }


    /**
     * Valida exposição de acessos do fluxo em métricas Prometheus.
     */
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
        Flow flow = new Flow("sem-perguntas", "Sem perguntas", "Descrição", null, null, null, null, null, null, List.of(), null, null, null, null);
        flowService.save(flow);

        mockMvc.perform(get("/api/flows/sem-perguntas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions").isArray())
                .andExpect(jsonPath("$.questions", hasSize(0)));
    }


    @Test
    void upsertFlowIgnoresUnknownSimpleFormStyleFields() throws Exception {
        ObjectNode payload = (ObjectNode) objectMapper.readTree(objectMapper.writeValueAsString(buildRequest()));
        ObjectNode style = payload.putObject("simpleFormStyle");
        style.put("slug", "hero-style");
        style.put("name", "Hero Style");
        ObjectNode definition = style.putObject("definition");
        definition.put("backgroundColor", "#fff");
        definition.put("buttonBackground", "#000");
        style.put("previewImageUrl", "https://cdn.example.com/style.png");

        mockMvc.perform(put("/api/flows/diagnostico-com-estilo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("diagnostico-com-estilo"));
    }

    /**
     * Valida publicação de HTML customizado sem perguntas.
     */
    @Test
    void upsertFlowAllowsCustomFormWithoutQuestions() throws Exception {
        UpsertFlowRequest request = new UpsertFlowRequest();
        request.setName("Exp 10 Landing");
        request.setDescription("Landing personalizada");
        request.setCustomFormHtml("<section>Exp 10</section>");
        request.setQuestions(List.of());

        mockMvc.perform(put("/api/flows/exp-10-landing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customFormHtml").value(containsString("Exp 10")))
                .andExpect(jsonPath("$.customFormRenderMode").value("IFRAME"))
                .andExpect(jsonPath("$.questions", hasSize(0)));
    }

    /**
     * Valida bloqueio de fluxo sem perguntas quando não há HTML customizado.
     */
    @Test
    void upsertFlowRequiresQuestionsWhenCustomFormMissing() throws Exception {
        UpsertFlowRequest request = new UpsertFlowRequest();
        request.setName("Fluxo sem perguntas");

        mockMvc.perform(put("/api/flows/fluxo-sem-perguntas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Ao menos uma pergunta")));
    }


    /**
     * Valida entrega HTML standalone com analytics injetado sem fetch JSON adicional.
     */
    @Test
    void getStandaloneFlowPageReturnsHtmlDocumentWithoutJsonFetch() throws Exception {
        UpsertFlowRequest request = buildRequest();
        request.setCustomFormHtml("<!doctype html><html><body>Landing direta</body></html>");

        mockMvc.perform(put("/api/flows/landing-direta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/flows/landing-direta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customFormRenderMode").value("STANDALONE_PAGE"));

        mockMvc.perform(get("/api/flows/landing-direta/page"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Landing direta")))
                .andExpect(content().string(containsString("data-mh-landing-analytics=\"true\"")))
                .andExpect(content().string(containsString("/api/flows/")))
                .andExpect(content().string(containsString("/page-analytics")));
    }

    /**
     * Valida conflito quando a página standalone é solicitada para HTML de iframe.
     */
    @Test
    void getStandaloneFlowPageReturnsConflictWhenFlowUsesIframeHtml() throws Exception {
        UpsertFlowRequest request = buildRequest();
        request.setCustomFormHtml("<section>Flow em iframe</section>");

        mockMvc.perform(put("/api/flows/flow-iframe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/flows/flow-iframe/page"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(containsString("HTML standalone")));
    }

    /**
     * Monta um payload mínimo reutilizável para criação de fluxo.
     */
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
