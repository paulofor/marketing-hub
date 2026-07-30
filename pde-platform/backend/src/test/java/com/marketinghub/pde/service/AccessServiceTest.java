package com.marketinghub.pde.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.marketinghub.pde.dto.FunnelEventRequest;
import com.marketinghub.pde.dto.AccessResponse;
import com.marketinghub.pde.dto.AiGuidanceCreateRequest;
import com.marketinghub.pde.dto.AiGuidanceResultRequest;
import com.marketinghub.pde.dto.MissionInteractionRequest;
import com.marketinghub.pde.dto.PepperWebhookRequest;
import com.marketinghub.pde.dto.WorkspaceResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;

/** Valida a liberação de acesso e o progresso da experiência PDE. */
class AccessServiceTest {

    @TempDir
    Path tempDir;

    /** Confirma que horários do funil PDE preservam a leitura operacional de Brasília. */
    @Test
    void convertsMysqlDatetimeAsBrazilOperationalTime() {
        Instant instant = AccessService.toOperationalInstant(Timestamp.valueOf("2026-07-22 23:13:00"));

        assertThat(instant.toString()).isEqualTo("2026-07-23T02:13:00Z");
    }

    /** Confirma que um acesso liberado retorna a experiência e progride ao concluir missão. */
    @Test
    void createsAccessAndTracksMissionProgress() {
        ProductCatalogService productCatalogService = new ProductCatalogService();
        AccessService accessService = new AccessService(
                productCatalogService,
                new ObjectMapper(),
                tempDir.resolve("access-grants.json").toString());

        AccessResponse access = accessService.createAccess("metodo-musa-7-dias", "cliente@sandbox.local", "DEV");
        WorkspaceResponse initialWorkspace = accessService.getWorkspace(access.token());

        assertThat(initialWorkspace.product().name()).contains("MUSA");
        assertThat(initialWorkspace.subscriptionStatus()).isEqualTo("TRIAL");
        assertThat(initialWorkspace.progressPercent()).isZero();

        accessService.completeMission(access.token(), "dia-1-ruido-visual");
        WorkspaceResponse updatedWorkspace = accessService.getWorkspace(access.token());

        assertThat(updatedWorkspace.completedMissionIds()).containsExactly("dia-1-ruido-visual");
        assertThat(updatedWorkspace.progressPercent()).isEqualTo(14);
    }

    /** Confirma que o progresso continua disponível depois de recriar o serviço. */
    @Test
    void persistsMissionProgressAcrossServiceRestart() {
        ProductCatalogService productCatalogService = new ProductCatalogService();
        String storagePath = tempDir.resolve("access-grants.json").toString();
        AccessService accessService = new AccessService(productCatalogService, new ObjectMapper(), storagePath);

        AccessResponse access = accessService.createAccess("metodo-musa-7-dias", "cliente@sandbox.local", "CHECKOUT");
        accessService.completeMission(access.token(), "dia-1-ruido-visual");

        AccessService restartedService = new AccessService(productCatalogService, new ObjectMapper(), storagePath);
        WorkspaceResponse workspace = restartedService.getWorkspace(access.token());

        assertThat(workspace.completedMissionIds()).containsExactly("dia-1-ruido-visual");
        assertThat(workspace.progressPercent()).isEqualTo(14);
    }

    /** Confirma que respostas de personalização do Dia 1 continuam disponíveis após reinício. */
    @Test
    void persistsMissionInteractionAcrossServiceRestart() {
        ProductCatalogService productCatalogService = new ProductCatalogService();
        String storagePath = tempDir.resolve("access-grants.json").toString();
        AccessService accessService = new AccessService(productCatalogService, new ObjectMapper(), storagePath);

        AccessResponse access = accessService.createAccess("metodo-musa-7-dias", "cliente@sandbox.local", "DEV");
        accessService.saveMissionInteraction(access.token(), "dia-1-ruido-visual", new MissionInteractionRequest(Map.of(
                "presenceFocus", "Trabalho ou reunião",
                "mainObstacle", "Roupa sem intenção",
                "evidencePhrase", "Eu me sinto arrumada, mas pouco marcante quando a roupa não conversa comigo.")));

        AccessService restartedService = new AccessService(productCatalogService, new ObjectMapper(), storagePath);
        WorkspaceResponse workspace = restartedService.getWorkspace(access.token());

        assertThat(workspace.missionInteractions())
                .extracting("questionKey")
                .contains("presenceFocus", "mainObstacle", "evidencePhrase");
    }

    /** Confirma que cadastro e login reutilizam o mesmo acesso por produto e e-mail. */
    @Test
    void registersCustomerAndLogsInWithSameAccess() {
        ProductCatalogService productCatalogService = new ProductCatalogService();
        AccessService accessService = new AccessService(
                productCatalogService,
                new ObjectMapper(),
                tempDir.resolve("access-grants.json").toString());

        AccessResponse registered = accessService.registerCustomer("metodo-musa-7-dias", "Cliente@Sandbox.Local");
        AccessResponse login = accessService.loginCustomer("metodo-musa-7-dias", "cliente@sandbox.local");
        AccessResponse duplicateRegister = accessService.registerCustomer("metodo-musa-7-dias", "cliente@sandbox.local");

        assertThat(login.token()).isEqualTo(registered.token());
        assertThat(duplicateRegister.token()).isEqualTo(registered.token());
        assertThat(login.accessUrl()).isEqualTo("/access/" + registered.token());
    }

    /** Confirma que o magic link cria acesso de entrada sem marcar assinatura ativa. */
    @Test
    void createsMagicLinkAccessAsTrial() {
        ProductCatalogService productCatalogService = new ProductCatalogService();
        AccessService accessService = new AccessService(
                productCatalogService,
                new ObjectMapper(),
                tempDir.resolve("access-grants.json").toString());

        var response = accessService.requestMagicLink("metodo-musa-7-dias", "cliente@sandbox.local");
        String token = response.accessUrl().replace("/access/", "");
        WorkspaceResponse workspace = accessService.getWorkspace(token);

        assertThat(response.deliveryStatus()).isEqualTo("EMAIL_NOT_CONFIGURED");
        assertThat(workspace.subscriptionStatus()).isEqualTo("TRIAL");
        assertThat(workspace.accessSource()).isEqualTo("MAGIC_LINK");
    }

    /** Confirma que login por link exige cadastro anterior e reutiliza o acesso existente. */
    @Test
    void sendsExistingCustomerMagicLinkWithoutCreatingNewAccess() {
        ProductCatalogService productCatalogService = new ProductCatalogService();
        AccessService accessService = new AccessService(
                productCatalogService,
                new ObjectMapper(),
                tempDir.resolve("access-grants.json").toString());

        var firstAccess = accessService.requestMagicLink("metodo-musa-7-dias", "Cliente@Sandbox.Local");
        var loginLink = accessService.requestExistingMagicLink("metodo-musa-7-dias", "cliente@sandbox.local");

        assertThat(loginLink.deliveryStatus()).isEqualTo("EMAIL_NOT_CONFIGURED");
        assertThat(loginLink.accessUrl()).isEqualTo(firstAccess.accessUrl());
    }

    /** Confirma que falha do provedor de e-mail não quebra a criação do acesso. */
    @Test
    void reportsEmailSendFailureWithoutBreakingAccessCreation() {
        ProductCatalogService productCatalogService = new ProductCatalogService();
        AccessService accessService = new AccessService(
                productCatalogService,
                new ObjectMapper(),
                tempDir.resolve("access-grants.json").toString(),
                "",
                "",
                "",
                false,
                "http://localhost:5176",
                false,
                new FailingMailService(),
                null);

        var response = accessService.requestMagicLink("metodo-musa-7-dias", "cliente@sandbox.local");

        assertThat(response.deliveryStatus()).isEqualTo("EMAIL_SEND_FAILED");
        assertThat(response.accessUrl()).isNull();
    }

    /** Confirma que a tentativa de login sem cadastro orienta a cliente a pedir primeiro acesso. */
    @Test
    void rejectsExistingCustomerMagicLinkWhenEmailIsUnknown() {
        ProductCatalogService productCatalogService = new ProductCatalogService();
        AccessService accessService = new AccessService(
                productCatalogService,
                new ObjectMapper(),
                tempDir.resolve("access-grants.json").toString());

        assertThrows(IllegalArgumentException.class, () ->
                accessService.requestExistingMagicLink("metodo-musa-7-dias", "cliente@sandbox.local"));
    }

    /** Confirma que checkout aprovado libera assinatura ativa para a cliente. */
    @Test
    void marksCheckoutAccessAsActiveSubscription() {
        ProductCatalogService productCatalogService = new ProductCatalogService();
        AccessService accessService = new AccessService(
                productCatalogService,
                new ObjectMapper(),
                tempDir.resolve("access-grants.json").toString());

        AccessResponse access = accessService.createAccess("metodo-musa-7-dias", "cliente@sandbox.local", "CHECKOUT");
        WorkspaceResponse workspace = accessService.getWorkspace(access.token());

        assertThat(workspace.subscriptionStatus()).isEqualTo("ACTIVE");
        assertThat(workspace.accessSource()).isEqualTo("CHECKOUT");
    }

    /** Confirma que uma assinatura aprovada promove acesso criado antes pelo magic link. */
    @Test
    void promotesMagicLinkAccessAfterCheckoutApproval() {
        ProductCatalogService productCatalogService = new ProductCatalogService();
        AccessService accessService = new AccessService(
                productCatalogService,
                new ObjectMapper(),
                tempDir.resolve("access-grants.json").toString());

        accessService.requestMagicLink("metodo-musa-7-dias", "cliente@sandbox.local");
        AccessResponse paidAccess = accessService.createAccess("metodo-musa-7-dias", "cliente@sandbox.local", "PEPPER");
        WorkspaceResponse workspace = accessService.getWorkspace(paidAccess.token());

        assertThat(workspace.subscriptionStatus()).isEqualTo("ACTIVE");
        assertThat(workspace.accessSource()).isEqualTo("PEPPER");
    }

    /** Confirma que o payload v2 da Pepper libera acesso somente quando a transação está paga. */
    @Test
    void releasesAccessFromPaidPepperWebhookV2() {
        ProductCatalogService productCatalogService = new ProductCatalogService();
        AccessService accessService = new AccessService(
                productCatalogService,
                new ObjectMapper(),
                tempDir.resolve("access-grants.json").toString());

        PepperWebhookRequest request = new PepperWebhookRequest(
                null,
                null,
                null,
                "paid",
                new PepperWebhookRequest.PepperCustomer(
                        "customer-1", "Cliente MUSA", "cliente@sandbox.local", "+5511999999999", "12345678901"),
                new PepperWebhookRequest.PepperTransaction(
                        "pepper-tx-67", "paid", "pix", "4700", "https://go.pepper.com.br/customer/pepper-tx-67"),
                new PepperWebhookRequest.PepperOffer("offer-musa", "Clube MUSA", "4700"),
                List.of(),
                new PepperWebhookRequest.PepperTracking(null, null, null, null, null, null, "metodo-musa-7-dias"));

        AccessResponse access = accessService.receivePepperWebhook(request);
        WorkspaceResponse workspace = accessService.getWorkspace(access.token());

        assertThat(workspace.subscriptionStatus()).isEqualTo("ACTIVE");
        assertThat(workspace.accessSource()).isEqualTo("PEPPER");
    }

    /** Confirma que a reconciliacao Pepper sem webhook usa a mesma liberacao paga. */
    @Test
    void releasesAccessFromPepperPaidTransactionFallback() {
        ProductCatalogService productCatalogService = new ProductCatalogService();
        AccessService accessService = new AccessService(
                productCatalogService,
                new ObjectMapper(),
                tempDir.resolve("access-grants.json").toString());

        AccessResponse access = accessService.releasePepperPaidTransaction(
                "metodo-musa-7-dias",
                "cliente@sandbox.local",
                "jft4eub7br",
                "owm6x");
        WorkspaceResponse workspace = accessService.getWorkspace(access.token());

        assertThat(workspace.subscriptionStatus()).isEqualTo("ACTIVE");
        assertThat(workspace.accessSource()).isEqualTo("PEPPER");
    }

    /** Confirma que webhook Pepper aguardando pagamento não libera acesso completo. */
    @Test
    void rejectsPepperWebhookWithoutPaidStatus() {
        ProductCatalogService productCatalogService = new ProductCatalogService();
        AccessService accessService = new AccessService(
                productCatalogService,
                new ObjectMapper(),
                tempDir.resolve("access-grants.json").toString());

        PepperWebhookRequest request = new PepperWebhookRequest(
                "metodo-musa-7-dias",
                "cliente@sandbox.local",
                "pepper-tx-67",
                "waiting_payment",
                null,
                null,
                null,
                List.of(),
                null);

        assertThrows(IllegalArgumentException.class, () -> accessService.receivePepperWebhook(request));
    }

    /** Confirma que retry do checkout não altera o acesso ativo existente. */
    @Test
    void keepsActiveSubscriptionStableOnCheckoutRetry() {
        ProductCatalogService productCatalogService = new ProductCatalogService();
        AccessService accessService = new AccessService(
                productCatalogService,
                new ObjectMapper(),
                tempDir.resolve("access-grants.json").toString());

        AccessResponse first = accessService.createAccess("metodo-musa-7-dias", "cliente@sandbox.local", "CHECKOUT");
        AccessResponse retry = accessService.createAccess("metodo-musa-7-dias", "cliente@sandbox.local", "CHECKOUT");
        WorkspaceResponse workspace = accessService.getWorkspace(retry.token());

        assertThat(retry.token()).isEqualTo(first.token());
        assertThat(workspace.subscriptionStatus()).isEqualTo("ACTIVE");
        assertThat(workspace.accessSource()).isEqualTo("CHECKOUT");
    }

    /** Confirma que o contrato de funil aceita eventos de liberação e ativação pós-compra. */
    @Test
    void acceptsAccessReleasedAndFirstUseFunnelEvents() {
        ProductCatalogService productCatalogService = new ProductCatalogService();
        AccessService accessService = new AccessService(
                productCatalogService,
                new ObjectMapper(),
                tempDir.resolve("access-grants.json").toString());

        var released = accessService.recordFunnelEvent(new FunnelEventRequest(
                "metodo-musa-7-dias",
                "ACCESS_RELEASED",
                null,
                "cliente@sandbox.local",
                "PEPPER",
                "test",
                null,
                Map.of("accessSource", "PEPPER")));
        var firstUse = accessService.recordFunnelEvent(new FunnelEventRequest(
                "metodo-musa-7-dias",
                "FIRST_USE",
                null,
                "cliente@sandbox.local",
                "PEPPER",
                "test",
                null,
                Map.of("activationType", "material_open")));

        assertThat(released.eventType()).isEqualTo("ACCESS_RELEASED");
        assertThat(firstUse.eventType()).isEqualTo("FIRST_USE");
    }

    /** Confirma que o contrato aceita eventos de analytics necessários para campanhas. */
    @Test
    void acceptsCampaignAnalyticsFunnelEvents() {
        ProductCatalogService productCatalogService = new ProductCatalogService();
        AccessService accessService = new AccessService(
                productCatalogService,
                new ObjectMapper(),
                tempDir.resolve("access-grants.json").toString());

        List<String> eventTypes = List.of(
                "PAGE_VIEW",
                "PAGE_LOAD",
                "PAGE_VISIBLE_TIME",
                "SECTION_VIEW",
                "CTA_VIEWED",
                "VIDEO_VIEWED",
                "VIDEO_PLAY",
                "VIDEO_PROGRESS_25",
                "VIDEO_PROGRESS_50",
                "VIDEO_PROGRESS_75",
                "VIDEO_COMPLETED",
                "VIDEO_ERROR",
                "VIDEO_CTA_CLICKED",
                "FUNNEL_EXPERIMENT_ASSIGNED",
                "PRESENCE_MAP_CHOICE_SELECTED",
                "DIAGNOSTIC_CHOICE_SELECTED",
                "DIAGNOSTIC_SUBMITTED",
                "DIAGNOSTIC_COMPLETED",
                "PROBLEM_RECOGNIZED",
                "CATEGORY_UNDERSTOOD",
                "MECHANISM_VIEWED",
                "MICRO_EXPERIENCE_STARTED",
                "REAL_INPUT_SUBMITTED",
                "MICRO_RESULT_RECEIVED",
                "PAID_CONTINUATION_VIEWED",
                "CHECKOUT_STARTED",
                "PURCHASE_COMPLETED",
                "MISSION_OPEN",
                "MISSION_COMPLETED",
                "MATERIAL_OPEN");

        eventTypes.forEach(eventType -> {
            var response = accessService.recordFunnelEvent(new FunnelEventRequest(
                    "metodo-musa-7-dias",
                    eventType,
                    null,
                    "cliente@sandbox.local",
                    "FRONTEND",
                    "test",
                    "https://clubemusa.com.br/?utm_campaign=musa-teste",
                    Map.of(
                            "visitorId", "visitor-1",
                            "sessionId", "session-1",
                            "utmCampaign", "musa-teste",
                            "deviceType", "mobile",
                            "visibleMs", 1200)));

            assertThat(response.eventType()).isEqualTo(eventType);
        });
    }

    /** Confirma que o resumo retorna vazio no modo local sem banco analítico. */
    @Test
    void returnsEmptyAnalyticsSummaryWithoutJdbcStorage() {
        ProductCatalogService productCatalogService = new ProductCatalogService();
        AccessService accessService = new AccessService(
                productCatalogService,
                new ObjectMapper(),
                tempDir.resolve("access-grants.json").toString());

        var summary = accessService.summarizeFunnelAnalytics("metodo-musa-7-dias");

        assertThat(summary.totalEvents()).isZero();
        assertThat(summary.events()).isEmpty();
    }

    /** Confirma que falha em breakdown auxiliar não derruba os KPIs principais do analytics. */
    @Test
    void keepsMainAnalyticsSummaryWhenOptionalBreakdownFails() throws SQLException {
        String jdbcUrl = "jdbc:h2:mem:pde_partial_analytics;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
        createPartialPdeFunnelEventSchema(jdbcUrl);
        try (var connection = DriverManager.getConnection(jdbcUrl, "sa", "sa");
                var statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO pde_funnel_event (
                      event_id, product_slug, experience_version, event_type, traffic_quality,
                      session_id, visitor_id, visible_ms, occurred_at
                    )
                    VALUES (
                      'event-partial-1', 'metodo-musa-7-dias', 'musa-pde-entry-v7-espelho-antes-de-sair',
                      'PAGE_VIEW', 'HUMAN', 'session-partial-1', 'visitor-partial-1', 3200,
                      TIMESTAMP '2026-07-30 12:00:00'
                    )
                    """);
        }
        AccessService accessService = new AccessService(
                new ProductCatalogService(),
                new ObjectMapper(),
                tempDir.resolve("access-grants.json").toString(),
                jdbcUrl,
                "sa",
                "sa",
                true,
                "http://localhost:5176",
                true,
                null,
                null);

        var summary = accessService.summarizeFunnelAnalytics("metodo-musa-7-dias");

        assertThat(summary.totalEvents()).isEqualTo(1);
        assertThat(summary.sessions()).isEqualTo(1);
        assertThat(summary.pageViews()).isEqualTo(1);
        assertThat(summary.events()).singleElement().satisfies(event -> {
            assertThat(event.eventType()).isEqualTo("PAGE_VIEW");
            assertThat(event.total()).isEqualTo(1);
        });
        assertThat(summary.experienceVersions())
                .singleElement()
                .satisfies(version -> assertThat(version.experienceVersion())
                        .isEqualTo("musa-pde-entry-v7-espelho-antes-de-sair"));
        assertThat(summary.trafficSources()).isEmpty();
        assertThat(summary.recentJourneys()).isEmpty();
    }

    /** Confirma que o resumo analítico expõe dispositivo e resolução gravados pelos eventos do PDE. */
    @Test
    void summarizesDeviceAndScreenSizeAnalyticsFromJdbcEvents() throws SQLException {
        String jdbcUrl = "jdbc:h2:mem:pde_device_analytics;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
        createPdeFunnelEventSchema(jdbcUrl);
        ProductCatalogService productCatalogService = new ProductCatalogService();
        AccessService accessService = new AccessService(
                productCatalogService,
                new ObjectMapper(),
                tempDir.resolve("access-grants.json").toString(),
                jdbcUrl,
                "sa",
                "sa",
                true,
                "http://localhost:5176",
                true,
                null,
                null);

        accessService.recordFunnelEvent(new FunnelEventRequest(
                "metodo-musa-7-dias",
                "PAGE_VIEW",
                null,
                "cliente@sandbox.local",
                "TEST",
                "test",
                "http://localhost:5176/?utm_source=ig&utm_campaign=campanha&utm_content=criativo",
                Map.ofEntries(
                        Map.entry("visitorId", "visitor-mobile"),
                        Map.entry("sessionId", "session-mobile"),
                        Map.entry("utmSource", "ig"),
                        Map.entry("utmCampaign", "campanha"),
                        Map.entry("utmContent", "criativo"),
                        Map.entry("userAgent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 Safari/604.1"),
                        Map.entry("deviceType", "mobile"),
                        Map.entry("screenWidth", 390),
                        Map.entry("screenHeight", 844),
                        Map.entry("viewportWidth", 390),
                        Map.entry("viewportHeight", 844))));

        var summary = accessService.summarizeFunnelAnalytics("metodo-musa-7-dias");

        assertThat(summary.deviceBreakdown())
                .anySatisfy(device -> {
                    assertThat(device.deviceType()).isEqualTo("mobile");
                    assertThat(device.label()).isEqualTo("Mobile");
                    assertThat(device.sessions()).isEqualTo(1);
                    assertThat(device.percentage()).isEqualTo(100.0);
                });
        assertThat(summary.screenSizeBreakdown())
                .singleElement()
                .satisfies(screen -> {
                    assertThat(screen.screenSize()).isEqualTo("390x844");
                    assertThat(screen.label()).isEqualTo("390x844");
                    assertThat(screen.sessions()).isEqualTo(1);
                    assertThat(screen.percentage()).isEqualTo(100.0);
                });
        assertThat(summary.trafficSources())
                .singleElement()
                .satisfies(source -> {
                    assertThat(source.trafficChannel()).isEqualTo("Meta");
                    assertThat(source.utmSource()).isEqualTo("ig");
                    assertThat(source.utmMedium()).isEqualTo("sem-meio");
                    assertThat(source.utmCampaign()).isEqualTo("campanha");
                    assertThat(source.utmContent()).isEqualTo("criativo");
                    assertThat(source.sessions()).isEqualTo(1);
                    assertThat(source.lastEventAt()).endsWith("-03:00");
                });
    }

    /** Confirma que remarketing nao se mistura com Meta frio na leitura por UTM. */
    @Test
    void classifiesRemarketingBeforeMetaTrafficSource() throws SQLException {
        String jdbcUrl = "jdbc:h2:mem:pde_remarketing_analytics;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
        createPdeFunnelEventSchema(jdbcUrl);
        ProductCatalogService productCatalogService = new ProductCatalogService();
        AccessService accessService = new AccessService(
                productCatalogService,
                new ObjectMapper(),
                tempDir.resolve("access-grants.json").toString(),
                jdbcUrl,
                "sa",
                "sa",
                true,
                "http://localhost:5176",
                true,
                null,
                null);

        accessService.recordFunnelEvent(new FunnelEventRequest(
                "metodo-musa-7-dias",
                "PAGE_VIEW",
                null,
                "cliente@sandbox.local",
                "TEST",
                "test",
                "http://localhost:5176/?utm_source=instagram&utm_medium=remarketing&utm_campaign=exp-71-remarketing",
                Map.of(
                        "visitorId", "visitor-remarketing",
                        "sessionId", "session-remarketing",
                        "userAgent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 Safari/604.1",
                        "utmSource", "instagram",
                        "utmMedium", "remarketing",
                        "utmCampaign", "exp-71-remarketing")));

        var summary = accessService.summarizeFunnelAnalytics("metodo-musa-7-dias");

        assertThat(summary.trafficSources())
                .singleElement()
                .satisfies(source -> assertThat(source.trafficChannel()).isEqualTo("Remarketing"));
    }

    /** Confirma que robôs ficam fora dos KPIs comerciais e seguem disponíveis para auditoria. */
    @Test
    void excludesBotTrafficFromCommercialAnalyticsAndKeepsAuditBreakdown() throws SQLException {
        String jdbcUrl = "jdbc:h2:mem:pde_traffic_quality;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
        createPdeFunnelEventSchema(jdbcUrl);
        AccessService accessService = new AccessService(
                new ProductCatalogService(),
                new ObjectMapper(),
                tempDir.resolve("access-grants.json").toString(),
                jdbcUrl,
                "sa",
                "sa",
                true,
                "http://localhost:5176",
                true,
                null,
                null);

        accessService.recordFunnelEvent(new FunnelEventRequest(
                "metodo-musa-7-dias",
                "PAGE_VIEW",
                null,
                "cliente@sandbox.local",
                "TEST",
                "test",
                "https://clubemusa.com.br/?utm_source=ig&utm_campaign=campanha",
                "201.10.10.10",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 Safari/604.1",
                Map.of(
                        "visitorId", "visitor-human",
                        "sessionId", "session-human",
                        "utmSource", "ig",
                        "utmCampaign", "campanha",
                        "deviceType", "mobile")));
        accessService.recordFunnelEvent(new FunnelEventRequest(
                "metodo-musa-7-dias",
                "PAGE_VIEW",
                null,
                null,
                "TEST",
                "test",
                "https://clubemusa.com.br/",
                "163.245.203.201",
                "Mozilla/5.0 (compatible; HeadlessChrome crawler)",
                Map.of(
                        "visitorId", "visitor-bot",
                        "sessionId", "session-bot",
                        "deviceType", "desktop")));

        var summary = accessService.summarizeFunnelAnalytics("metodo-musa-7-dias");

        assertThat(summary.totalEvents()).isEqualTo(1);
        assertThat(summary.rawTotalEvents()).isEqualTo(2);
        assertThat(summary.sessions()).isEqualTo(1);
        assertThat(summary.rawSessions()).isEqualTo(2);
        assertThat(summary.humanSessions()).isEqualTo(1);
        assertThat(summary.botSuspectedSessions()).isEqualTo(1);
        assertThat(summary.trafficSources())
                .singleElement()
                .satisfies(source -> assertThat(source.utmSource()).isEqualTo("ig"));
        assertThat(summary.trafficQualityBreakdown())
                .extracting("trafficQuality")
                .contains("HUMAN", "BOT_SUSPECTED");
        assertThat(summary.recentJourneys())
                .anySatisfy(journey -> {
                    assertThat(journey.sessionId()).isEqualTo("session-bot");
                    assertThat(journey.trafficQuality()).isEqualTo("BOT_SUSPECTED");
                    assertThat(journey.trafficProvider()).isEqualTo("META");
                });
    }

    /** Confirma que jornadas por sessão retornam vazio no modo local sem banco analítico. */
    @Test
    void returnsEmptySessionJourneysWithoutJdbcStorage() {
        ProductCatalogService productCatalogService = new ProductCatalogService();
        AccessService accessService = new AccessService(
                productCatalogService,
                new ObjectMapper(),
                tempDir.resolve("access-grants.json").toString());

        var journeys = accessService.summarizeSessionJourneys("metodo-musa-7-dias", 50);

        assertThat(journeys.productSlug()).isEqualTo("metodo-musa-7-dias");
        assertThat(journeys.totalSessions()).isZero();
        assertThat(journeys.sessions()).isEmpty();
    }

    /** Confirma que jornadas recentes expõem DATETIME do MySQL com offset explícito de Brasília. */
    @Test
    void summarizesSessionJourneysWithBrazilOperationalOffset() throws SQLException {
        String jdbcUrl = "jdbc:h2:mem:pde_session_journey_time;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
        createPdeFunnelEventSchema(jdbcUrl);
        try (var connection = DriverManager.getConnection(jdbcUrl, "sa", "sa");
                var statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO pde_funnel_event (
                      event_id, product_slug, event_type, source, page_url, session_id, visitor_id,
                      visible_ms, action_name, metadata_json, occurred_at
                    )
                    VALUES (
                      'event-session-time-1', 'metodo-musa-7-dias', 'PAGE_VISIBLE_TIME', 'test',
                      'https://clubemusa.com.br/acesso', 'session-time-1', 'visitor-time-1',
                      1200, 'page_visibility_flush', '{"screenName":"login_first_access"}',
                      TIMESTAMP '2026-07-24 21:07:16'
                    )
                    """);
        }
        AccessService accessService = new AccessService(
                new ProductCatalogService(),
                new ObjectMapper(),
                tempDir.resolve("access-grants.json").toString(),
                jdbcUrl,
                "sa",
                "sa",
                true,
                "http://localhost:5176",
                true,
                null,
                null);

        var journeys = accessService.summarizeSessionJourneys("metodo-musa-7-dias", 20);

        assertThat(journeys.sessions())
                .singleElement()
                .satisfies(journey -> {
                    assertThat(journey.lastEventAt()).isEqualTo("2026-07-24T21:07:16-03:00");
                    assertThat(journey.steps())
                            .singleElement()
                            .satisfies(step -> assertThat(step.occurredAt())
                                    .isEqualTo("2026-07-24T21:07:16-03:00"));
                });
    }

    /** Confirma que ambiente comercial não inicia sem persistência JDBC obrigatória. */
    @Test
    void rejectsStartupWhenJdbcStorageIsRequiredWithoutJdbcUrl() {
        ProductCatalogService productCatalogService = new ProductCatalogService();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> new AccessService(
                productCatalogService,
                new ObjectMapper(),
                tempDir.resolve("access-grants.json").toString(),
                "",
                "",
                "",
                true,
                "https://clubemusa.com.br",
                false,
                null,
                null));

        assertThat(exception.getMessage()).contains("URL, usuário e senha JDBC");
    }

    /** Confirma que o Clube MUSA público não aceita modo local sem persistência analítica. */
    @Test
    void rejectsCommercialMusaUrlWithoutJdbcEvenWhenRequireFlagIsMissing() {
        ProductCatalogService productCatalogService = new ProductCatalogService();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> new AccessService(
                productCatalogService,
                new ObjectMapper(),
                tempDir.resolve("access-grants.json").toString(),
                "",
                "",
                "",
                false,
                "https://clubemusa.com.br",
                false,
                null,
                null));

        assertThat(exception.getMessage()).contains("URL, usuário e senha JDBC");
    }

    /** Confirma que uma orientação por IA nasce pendente e é entregue ao worker PDE. */
    @Test
    void createsPendingAiGuidanceForDayTwoSignature() {
        ProductCatalogService productCatalogService = new ProductCatalogService();
        ObjectMapper objectMapper = new ObjectMapper();
        AccessService accessService = new AccessService(
                productCatalogService,
                objectMapper,
                tempDir.resolve("access-grants.json").toString());
        AiGuidanceService aiGuidanceService = new AiGuidanceService(
                accessService,
                productCatalogService,
                objectMapper,
                tempDir.resolve("ai-guidance.json").toString(),
                "",
                "",
                "",
                new PdeDatabaseMigrationService("", "", ""));
        AccessResponse access = accessService.createAccess("metodo-musa-7-dias", "cliente@sandbox.local", "CHECKOUT");

        var guidance = aiGuidanceService.createGuidanceRequest(
                access.token(),
                "dia-2-assinatura",
                new AiGuidanceCreateRequest("MUSA_DAY_2_SIGNATURE", Map.of(
                        "finishSignal", "Cabelo polido",
                        "baseColor", "Vinho discreto",
                        "memorableSignal", "Brinco luminoso")));
        var pending = aiGuidanceService.getPendingGuidance();

        assertThat(guidance.status()).isEqualTo("PENDING");
        assertThat(pending).isPresent();
        assertThat(pending.get().requestId()).isEqualTo(guidance.requestId());
        assertThat(pending.get().answers()).containsEntry("baseColor", "Vinho discreto");
        assertThat(pending.get().product().scientificEvidencePack().version()).isEqualTo("musa-evidence-pack-v1");
        assertThat(pending.get().product().scientificEvidencePack().references())
                .extracting("doi")
                .contains("10.1016/j.jesp.2012.02.008");
    }

    /** Confirma que todos os 7 dias possuem contrato de orientação por IA no backend. */
    @Test
    void acceptsAiGuidanceForAllSevenMusaDays() {
        ProductCatalogService productCatalogService = new ProductCatalogService();
        ObjectMapper objectMapper = new ObjectMapper();
        AccessService accessService = new AccessService(
                productCatalogService,
                objectMapper,
                tempDir.resolve("access-grants.json").toString());
        AiGuidanceService aiGuidanceService = new AiGuidanceService(
                accessService,
                productCatalogService,
                objectMapper,
                tempDir.resolve("ai-guidance.json").toString(),
                "",
                "",
                "",
                new PdeDatabaseMigrationService("", "", ""));
        AccessResponse access = accessService.createAccess("metodo-musa-7-dias", "cliente@sandbox.local", "CHECKOUT");

        Map<String, String> guidanceTypesByMission = Map.of(
                "dia-1-ruido-visual", "MUSA_DAY_1_PRESENCE_DIAGNOSIS",
                "dia-2-assinatura", "MUSA_DAY_2_SIGNATURE",
                "dia-3-base-acessivel", "MUSA_DAY_3_WARDROBE_REUSE",
                "dia-4-checklist-12-minutos", "MUSA_DAY_4_FINISHING_RITUAL",
                "dia-5-compra-inteligente", "MUSA_DAY_5_ANTI_IMPULSE_DECISION",
                "dia-6-situacao-chave", "MUSA_DAY_6_OCCASION_ENTRY",
                "dia-7-plano-pessoal", "MUSA_DAY_7_MAINTENANCE_PLAN");

        guidanceTypesByMission.forEach((missionId, guidanceType) -> {
            var guidance = aiGuidanceService.createGuidanceRequest(
                    access.token(),
                    missionId,
                    new AiGuidanceCreateRequest(guidanceType, Map.of(
                            "answerOne", "Resposta um",
                            "answerTwo", "Resposta dois",
                            "answerThree", "Resposta tres")));

            assertThat(guidance.status()).isEqualTo("PENDING");
            assertThat(guidance.missionId()).isEqualTo(missionId);
            assertThat(guidance.guidanceType()).isEqualTo(guidanceType);
        });
    }

    /** Confirma que o backend aceita resultado estruturado e auditoria do worker PDE. */
    @Test
    void receivesCompletedAiGuidanceResult() {
        ProductCatalogService productCatalogService = new ProductCatalogService();
        ObjectMapper objectMapper = new ObjectMapper();
        AccessService accessService = new AccessService(
                productCatalogService,
                objectMapper,
                tempDir.resolve("access-grants.json").toString());
        AiGuidanceService aiGuidanceService = new AiGuidanceService(
                accessService,
                productCatalogService,
                objectMapper,
                tempDir.resolve("ai-guidance.json").toString(),
                "",
                "",
                "",
                new PdeDatabaseMigrationService("", "", ""));
        AccessResponse access = accessService.createAccess("metodo-musa-7-dias", "cliente@sandbox.local", "CHECKOUT");
        var guidance = aiGuidanceService.createGuidanceRequest(
                access.token(),
                "dia-2-assinatura",
                new AiGuidanceCreateRequest("MUSA_DAY_2_SIGNATURE", Map.of(
                        "finishSignal", "Pele iluminada",
                        "baseColor", "Off-white",
                        "memorableSignal", "Perfume assinatura")));

        var completed = aiGuidanceService.receiveGuidanceResult(guidance.requestId(), new AiGuidanceResultRequest(
                "COMPLETED",
                "Sua assinatura MUSA ficou luminosa e limpa",
                "Repita pele iluminada, off-white e perfume assinatura para criar reconhecimento sem esforço.",
                List.of("Pele iluminada", "Off-white", "Perfume assinatura"),
                List.of("Separe a base off-white antes de sair.", "Finalize com perfume no ultimo passo."),
                "Não compre nada novo antes de testar a repetição por uma semana.",
                "gpt-5.5",
                "flex",
                "{\"model\":\"gpt-5.5\"}",
                "{\"output_text\":\"{}\"}",
                120,
                90,
                BigDecimal.valueOf(0.0012),
                null));
        var fetched = aiGuidanceService.getGuidance(access.token(), guidance.requestId());

        assertThat(completed.status()).isEqualTo("COMPLETED");
        assertThat(fetched.headline()).contains("assinatura MUSA");
        assertThat(fetched.signals()).containsExactly("Pele iluminada", "Off-white", "Perfume assinatura");
        assertThat(fetched.serviceTier()).isEqualTo("flex");
    }

    /** Confirma que o diagnóstico público usa o contrato de IA com plano de 7 dias. */
    @Test
    void acceptsPublicPresenceDiagnosticGuidanceType() {
        ProductCatalogService productCatalogService = new ProductCatalogService();
        ObjectMapper objectMapper = new ObjectMapper();
        AccessService accessService = new AccessService(
                productCatalogService,
                objectMapper,
                tempDir.resolve("access-grants.json").toString());
        AiGuidanceService aiGuidanceService = new AiGuidanceService(
                accessService,
                productCatalogService,
                objectMapper,
                tempDir.resolve("ai-guidance-public.json").toString(),
                "",
                "",
                "",
                new PdeDatabaseMigrationService("", "", ""));
        AccessResponse access = accessService.createAccess("metodo-musa-7-dias", "diagnostico+visitante@clubemusa.local", "DIAGNOSTIC");

        var guidance = aiGuidanceService.createGuidanceRequest(
                access.token(),
                "dia-1-ruido-visual",
                new AiGuidanceCreateRequest("MUSA_PUBLIC_PRESENCE_DIAGNOSTIC", Map.of(
                        "presenceFocus", "Trabalho ou reunião",
                        "mainObstacle", "Falta acabamento",
                        "desiredSignal", "Elegância discreta",
                        "mainConstraint", "Pouco tempo",
                        "startingResource", "Cabelo e pele")));
        var pending = aiGuidanceService.getPendingGuidance();

        assertThat(guidance.status()).isEqualTo("PENDING");
        assertThat(guidance.guidanceType()).isEqualTo("MUSA_PUBLIC_PRESENCE_DIAGNOSTIC");
        assertThat(pending).isPresent();
        assertThat(pending.get().missionId()).isEqualTo("dia-1-ruido-visual");
        assertThat(pending.get().product().slug()).isEqualTo("metodo-musa-7-dias");
    }

    /** Confirma que eventos comportamentais ricos da tela fazem parte do contrato analítico. */
    @Test
    void acceptsRichBehaviorAnalyticsEvents() {
        ProductCatalogService productCatalogService = new ProductCatalogService();
        AccessService accessService = new AccessService(
                productCatalogService,
                new ObjectMapper(),
                tempDir.resolve("access-grants.json").toString());

        List<String> eventTypes = List.of(
                "SCREEN_VIEW",
                "SCREEN_TIME",
                "SCROLL_DEPTH",
                "UI_CLICK",
                "LINK_CLICK",
                "FIELD_FOCUS",
                "FIELD_INPUT",
                "FIELD_FILLED",
                "FIELD_ABANDONED");

        eventTypes.forEach(eventType -> {
            var response = accessService.recordFunnelEvent(new FunnelEventRequest(
                    "metodo-musa-7-dias",
                    eventType,
                    null,
                    "cliente@sandbox.local",
                    "TEST",
                    "test",
                    "http://localhost:5176",
                    Map.of("screenName", "login_first_access", "actionName", eventType.toLowerCase())));

            assertThat(response.eventType()).isEqualTo(eventType);
            assertThat(response.status()).isEqualTo("RECORDED");
        });
    }

    /** Confirma que IP interno configurado não entra nas métricas comportamentais do PDE. */
    @Test
    void ignoresInternalIpFunnelEvents() {
        ProductCatalogService productCatalogService = new ProductCatalogService();
        AccessService accessService = new AccessService(
                productCatalogService,
                new ObjectMapper(),
                tempDir.resolve("access-grants.json").toString(),
                "",
                "",
                "",
                false,
                "http://localhost:5176",
                true,
                "179.210.58.3",
                null,
                null);

        var response = accessService.recordFunnelEvent(new FunnelEventRequest(
                "metodo-musa-7-dias",
                "SCREEN_TIME",
                null,
                "cliente@sandbox.local",
                "TEST",
                "test",
                "https://v6.clubemusa.com.br/",
                "179.210.58.3",
                "SamsungBrowser Android",
                Map.of("screenName", "login_first_access", "visibleMs", 4389000)));

        assertThat(response.eventType()).isEqualTo("SCREEN_TIME");
        assertThat(response.status()).isEqualTo("IGNORED_INTERNAL_IP");
    }

    /** Confirma que eventos fora do contrato continuam bloqueados. */
    @Test
    void rejectsUnsupportedFunnelEvent() {
        ProductCatalogService productCatalogService = new ProductCatalogService();
        AccessService accessService = new AccessService(
                productCatalogService,
                new ObjectMapper(),
                tempDir.resolve("access-grants.json").toString());

        assertThrows(IllegalArgumentException.class, () -> accessService.recordFunnelEvent(new FunnelEventRequest(
                "metodo-musa-7-dias",
                "INSTAGRAM_LOGIN_COMPLETED",
                null,
                "cliente@sandbox.local",
                "INSTAGRAM",
                "test",
                null,
                Map.of())));
    }

    /** Cria schema parcial para provar que breakdown auxiliar quebrado não impede o resumo principal. */
    private static void createPartialPdeFunnelEventSchema(String jdbcUrl) throws SQLException {
        try (var connection = DriverManager.getConnection(jdbcUrl, "sa", "sa");
                var statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE pde_access_grant (
                      token VARCHAR(120) PRIMARY KEY,
                      product_slug VARCHAR(120) NOT NULL,
                      email VARCHAR(191) NOT NULL,
                      source VARCHAR(80) NOT NULL,
                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            statement.execute("""
                    CREATE TABLE pde_access_mission_completion (
                      access_token VARCHAR(120) NOT NULL,
                      mission_id VARCHAR(120) NOT NULL,
                      completed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            statement.execute("""
                    CREATE TABLE pde_access_mission_interaction_answer (
                      access_token VARCHAR(120) NOT NULL,
                      mission_id VARCHAR(120) NOT NULL,
                      question_key VARCHAR(120) NOT NULL,
                      answer_text TEXT,
                      updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            statement.execute("""
                    CREATE TABLE pde_funnel_event (
                      id BIGINT AUTO_INCREMENT,
                      event_id VARCHAR(64) PRIMARY KEY,
                      product_slug VARCHAR(120) NOT NULL,
                      experience_version VARCHAR(80),
                      event_type VARCHAR(80) NOT NULL,
                      traffic_quality VARCHAR(40),
                      session_id VARCHAR(64),
                      visitor_id VARCHAR(64),
                      visible_ms BIGINT,
                      occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
        }
    }

    /** Cria a tabela mínima de eventos PDE usada pelas consultas analíticas do serviço. */
    private static void createPdeFunnelEventSchema(String jdbcUrl) throws SQLException {
        try (var connection = DriverManager.getConnection(jdbcUrl, "sa", "sa");
                var statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE pde_access_grant (
                      token VARCHAR(120) PRIMARY KEY,
                      product_slug VARCHAR(120) NOT NULL,
                      email VARCHAR(191) NOT NULL,
                      source VARCHAR(80) NOT NULL,
                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            statement.execute("""
                    CREATE TABLE pde_access_mission_completion (
                      access_token VARCHAR(120) NOT NULL,
                      mission_id VARCHAR(120) NOT NULL,
                      completed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            statement.execute("""
                    CREATE TABLE pde_access_mission_interaction_answer (
                      access_token VARCHAR(120) NOT NULL,
                      mission_id VARCHAR(120) NOT NULL,
                      question_key VARCHAR(120) NOT NULL,
                      answer_text TEXT,
                      updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            statement.execute("""
                    CREATE TABLE pde_funnel_event (
                      id BIGINT AUTO_INCREMENT,
                      event_id VARCHAR(64) PRIMARY KEY,
                      product_slug VARCHAR(120) NOT NULL,
                      experience_version VARCHAR(80),
                      access_token VARCHAR(120),
                      email VARCHAR(191),
                      normalized_email VARCHAR(191),
                      event_type VARCHAR(80) NOT NULL,
                      provider VARCHAR(80),
                      source VARCHAR(120),
                      page_url VARCHAR(1024),
                      client_ip VARCHAR(45),
                      user_agent VARCHAR(512),
                      traffic_quality VARCHAR(40),
                      traffic_quality_reason VARCHAR(120),
                      traffic_provider VARCHAR(80),
                      referrer_url VARCHAR(1024),
                      session_id VARCHAR(64),
                      visitor_id VARCHAR(64),
                      utm_source VARCHAR(120),
                      utm_medium VARCHAR(120),
                      utm_campaign VARCHAR(191),
                      utm_content VARCHAR(191),
                      utm_term VARCHAR(191),
                      device_type VARCHAR(40),
                      screen_width INT,
                      screen_height INT,
                      viewport_width INT,
                      viewport_height INT,
                      visible_ms BIGINT,
                      section_id VARCHAR(120),
                      action_name VARCHAR(120),
                      metadata_json TEXT,
                      occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
        }
    }

    /** Simula provedor configurado que rejeita o envio do link mágico. */
    private static class FailingMailService extends PdeMailService {

        /** Inicializa o serviço falso com transporte configurado para testes. */
        FailingMailService() {
            super("ses", "us-east-1", "", 1025, "acesso@clubemusa.com.br", "", "");
        }

        /** Informa que o provedor falso esta configurado. */
        @Override
        public boolean isConfigured() {
            return true;
        }

        /** Rejeita o envio para reproduzir falha do provedor externo. */
        @Override
        public void sendMagicLink(String to, String accessUrl) {
            throw new IllegalStateException("SES rejeitou o remetente");
        }
    }
}
