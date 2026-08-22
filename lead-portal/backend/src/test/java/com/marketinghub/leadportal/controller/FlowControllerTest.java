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
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
                .andExpect(jsonPath("$.customFormRenderMode").value("STANDALONE_PAGE"))
                .andExpect(jsonPath("$.customFormHtml").value(containsString("data-mh-landing-analytics")))
                .andExpect(jsonPath("$.customFormHtml").value(containsString("data-mh-clarity-analytics")));

        mockMvc.perform(get("/api/flows/landing-direta/page"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Landing direta")))
                .andExpect(content().string(containsString("data-mh-landing-analytics=\"true\"")))
                .andExpect(content().string(containsString("data-mh-clarity-analytics=\"aggregate-v1\"")))
                .andExpect(content().string(containsString("consentv2")))
                .andExpect(content().string(containsString("analytics_Storage: 'denied'")))
                .andExpect(content().string(containsString("data-clarity-mask")))
                .andExpect(content().string(containsString("params.has('mh_audit')")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("clarity('identify'"))))
                .andExpect(content().string(containsString("/api/flows/")))
                .andExpect(content().string(containsString("/page-analytics")))
                .andExpect(content().string(containsString("mhAnalyticsDebug")))
                .andExpect(content().string(containsString("[MH Landing Analytics]")))
                .andExpect(content().string(containsString("operatingSystem")))
                .andExpect(content().string(containsString("screenWidth")))
                .andExpect(content().string(containsString("page_load_metric")))
                .andExpect(content().string(containsString("loadDurationMs")))
                .andExpect(content().string(containsString("largest-contentful-paint")))
                .andExpect(content().string(containsString("interactionToNextPaintMs")))
                .andExpect(content().string(containsString("timeToFirstByteMs")))
                .andExpect(content().string(containsString("resourceErrorCount")))
                .andExpect(content().string(containsString("marketinghub_visitor_id")))
                .andExpect(content().string(containsString("visitorId: visitorId")))
                .andExpect(content().string(containsString("automationSignal")))
                .andExpect(content().string(containsString("referrer: document.referrer")))
                .andExpect(content().string(containsString("isSelfReferentialLink")))
                .andExpect(content().string(containsString("form_start")))
                .andExpect(content().string(containsString("form_submit")));
    }

    /** Valida o comando interno idempotente que reprocessa ativos de uma landing histórica. */
    @Test
    void optimizeExistingFlowAssetsThroughInternalEndpoint() throws Exception {
        UpsertFlowRequest request = buildRequest();
        request.setCustomFormHtml("<!doctype html><html><body>Landing histórica</body></html>");

        mockMvc.perform(put("/api/flows/landing-historica")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/internal/flows/landing-historica/optimize-assets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("landing-historica"))
                .andExpect(jsonPath("$.customFormRenderMode").value("STANDALONE_PAGE"));
    }

    /** Impede que o comando de manutenção interna seja acionado pelo proxy público. */
    @Test
    void rejectExternalAssetMaintenanceRequest() throws Exception {
        mockMvc.perform(post("/api/internal/flows/landing-historica/optimize-assets")
                        .with(request -> {
                            request.setRemoteAddr("172.18.0.20");
                            return request;
                        }))
                .andExpect(status().isForbidden());
    }

    /**
     * Valida que auditorias internas não registram acesso nem carregam Clarity no navegador.
     */
    @Test
    void auditLinkIsExcludedFromCommercialTrackingContracts() throws Exception {
        UpsertFlowRequest request = buildRequest();
        request.setCustomFormHtml("<!doctype html><html><head></head><body>Auditoria</body></html>");
        mockMvc.perform(put("/api/flows/auditoria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/flows/auditoria").param("mh_audit", "visual"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customFormHtml").value(containsString("params.has('mh_audit')")));

        assertThat(flowAccessRepository.findAll()).isEmpty();
    }

    /**
     * Valida que o link de teste entrega a landing sem registrar acesso nem analytics comercial.
     */
    @Test
    void getStandaloneFlowPageWithTestParamDoesNotTrackAccess() throws Exception {
        UpsertFlowRequest request = buildRequest();
        request.setCustomFormHtml("<!doctype html><html><body>Landing teste</body></html>");

        mockMvc.perform(put("/api/flows/landing-teste")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/flows/landing-teste/page").param("mh_test", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("mh_internal_test")))
                .andExpect(content().string(containsString("Landing teste")));

        assertThat(flowAccessRepository.findAll()).isEmpty();
        assertThat(meterRegistry.counter("lead_portal_flow_access_total", "slug", "landing-teste").count())
                .isZero();
    }

    /**
     * Valida atualização de instrumentação legada para exibir diagnóstico no browser sem duplicar analytics.
     */
    @Test
    void getStandaloneFlowPageRefreshesLegacyAnalyticsScriptWithBrowserDebug() throws Exception {
        UpsertFlowRequest request = buildRequest();
        request.setCustomFormHtml("""
                <!doctype html><html><body>
                <main>Landing ja publicada</main>
                <script data-mh-landing-analytics="true">console.log('analytics antigo');</script>
                </body></html>
                """);

        mockMvc.perform(put("/api/flows/landing-legada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/flows/landing-legada/page"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("Landing ja publicada");
        assertThat(body).contains("mhAnalyticsDebug");
        assertThat(body).contains("[MH Landing Analytics]");
        assertThat(body).doesNotContain("analytics antigo");
        assertThat(body.split("data-mh-landing-analytics", -1).length - 1).isEqualTo(1);
    }

    /**
     * Garante que o coletor legado do GeraSalesPage não envie eventos para o slug incorreto `page`.
     */
    @Test
    void getStandaloneFlowPageRemovesLegacySalesPageCollector() throws Exception {
        UpsertFlowRequest request = buildRequest();
        request.setCustomFormHtml("""
                <!doctype html><html><body>
                <main>Landing com coletor legado</main>
                <script data-mh-sales-page-analytics="true">
                  var slug=(location.pathname.split('/').pop()||'');
                  var endpoint='/api/flows/'+encodeURIComponent(slug)+'/page-analytics';
                </script>
                </body></html>
                """);

        mockMvc.perform(put("/api/flows/landing-coletor-legado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/flows/landing-coletor-legado/page"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body)
                .contains("const slugValue = \"landing-coletor-legado\"")
                .contains("data-mh-landing-analytics=\"true\"")
                .doesNotContain("data-mh-sales-page-analytics")
                .doesNotContain("location.pathname.split('/').pop()");
    }

    /**
     * Garante que CTAs de checkout continuem mensuráveis mesmo quando o HTML não traz marcador semântico.
     */
    @Test
    void getStandaloneFlowPageTracksCheckoutLinksByMarkerOrDestination() throws Exception {
        UpsertFlowRequest request = buildRequest();
        request.setCustomFormHtml("""
                <!doctype html><html><body>
                <section id="oferta">
                  <a href="https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=preferencia-ativa">Comprar</a>
                  <a data-analytics-role="primary-checkout" href="https://checkout.example.com/order/1">Comprar marcado</a>
                </section>
                </body></html>
                """);

        mockMvc.perform(put("/api/flows/landing-checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/flows/landing-checkout/page"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .contains("anchor.getAttribute('data-analytics-role')")
                .contains("targetUrl.searchParams.has('pref_id')")
                .contains("host.endsWith('.mercadopago.com.br')")
                .contains("sendEvent('checkout_click', resolveCheckoutSection(anchor), null)");
    }

    /**
     * Garante que uma landing com coletor anterior seja atualizada quando ainda não mede checkout.
     */
    @Test
    void getStandaloneFlowPageRefreshesCollectorWithoutCheckoutTracking() throws Exception {
        UpsertFlowRequest request = buildRequest();
        request.setCustomFormHtml("""
                <!doctype html><html><body>
                <a href="https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=ativa">Comprar</a>
                <script data-mh-landing-analytics="true">
                  var mhAnalyticsDebug = true;
                  var mh_internal_test = true;
                  var visitorId = 'anterior';
                  var payload = {visitorId: visitorId, automationSignal: false};
                  console.log('coletor-sem-checkout', payload);
                </script>
                </body></html>
                """);

        mockMvc.perform(put("/api/flows/landing-coletor-sem-checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/flows/landing-coletor-sem-checkout/page"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .contains("checkout_click")
                .doesNotContain("console.log('coletor-sem-checkout'");
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
