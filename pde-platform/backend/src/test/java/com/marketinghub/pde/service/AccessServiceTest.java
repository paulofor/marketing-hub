package com.marketinghub.pde.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.pde.dto.FunnelEventRequest;
import com.marketinghub.pde.dto.AccessResponse;
import com.marketinghub.pde.dto.AiGuidanceCreateRequest;
import com.marketinghub.pde.dto.AiGuidanceResultRequest;
import com.marketinghub.pde.dto.MissionInteractionRequest;
import com.marketinghub.pde.dto.OperationalMissionCompletionRequest;
import com.marketinghub.pde.dto.OperationalMissionCompletionRequest.DeliverySectionRequest;
import com.marketinghub.pde.dto.PublicPresenceDiagnosticRequest;
import com.marketinghub.pde.dto.WorkspaceResponse;
import com.marketinghub.pde.dto.ProductExperienceResponse;
import com.marketinghub.pde.dto.PrivacyActionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Files;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
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

    /** Impede que a cliente conclua um marco operacional da experiência assistida. */
    @Test
    void rejectsCustomerCompletionOfOperationalMission() {
        ProductCatalogService catalog = roleAwareCatalog();
        AccessService accessService = new AccessService(
                catalog, new ObjectMapper(), tempDir.resolve("role-customer.json").toString());
        AccessResponse access = accessService.createAccess("kit-role-aware", "cliente@sandbox.local", "DEV");

        accessService.completeMission(access.token(), "entrada");

        assertThrows(IllegalArgumentException.class,
                () -> accessService.completeMission(access.token(), "entrega"));
        assertThat(accessService.getWorkspace(access.token()).completedMissionIds()).containsExactly("entrada");
    }

    /** Impede que a operação pule a entrada que pertence à cliente. */
    @Test
    void rejectsOperationalMissionBeforeItsPredecessor() {
        ProductCatalogService catalog = roleAwareCatalog();
        AccessService accessService = new AccessService(
                catalog, new ObjectMapper(), tempDir.resolve("role-order.json").toString());
        AccessResponse access = accessService.createAccess("kit-role-aware", "cliente@sandbox.local", "DEV");

        assertThrows(IllegalArgumentException.class,
                () -> accessService.completeOperationalMission(access.token(), "entrega"));
        assertThat(accessService.getWorkspace(access.token()).completedMissionIds()).isEmpty();
    }

    /** Preserva a sequência entre entrada, operação e primeira aplicação da cliente. */
    @Test
    void completesRoleAwareJourneyInCanonicalOrder() {
        ProductCatalogService catalog = roleAwareCatalog();
        AccessService accessService = new AccessService(
                catalog, new ObjectMapper(), tempDir.resolve("role-happy.json").toString());
        AccessResponse access = accessService.createAccess("kit-role-aware", "cliente@sandbox.local", "DEV");

        accessService.completeMission(access.token(), "entrada");
        accessService.completeOperationalMission(access.token(), "entrega");
        accessService.completeMission(access.token(), "aplicacao");

        WorkspaceResponse workspace = accessService.getWorkspace(access.token());
        assertThat(workspace.completedMissionIds()).containsExactlyInAnyOrder("entrada", "entrega", "aplicacao");
        assertThat(workspace.progressPercent()).isEqualTo(100);
    }

    /** Exige e restaura microentrega e pacote individual antes de liberar os marcos prometidos. */
    @Test
    void persistsPersonalizedDeliveriesForCompletedOperationalMilestones() {
        ProductCatalogService catalog = assistedDeliveryCatalog();
        String storagePath = tempDir.resolve("personal-deliveries.json").toString();
        AccessService accessService = new AccessService(catalog, new ObjectMapper(), storagePath);
        AccessResponse access = accessService.createAccess("kit-delivery", "cliente@sandbox.local", "DEV");

        saveCompleteBriefing(accessService, access.token());
        accessService.completeMission(access.token(), "entrada-guiada");
        accessService.completeOperationalMission(access.token(), "conferencia-de-completude");
        accessService.completeOperationalMission(access.token(), "diagnostico-humano");
        assertThrows(IllegalArgumentException.class,
                () -> accessService.completeOperationalMission(access.token(), "microvalor-12h"));
        accessService.completeOperationalMission(
                access.token(),
                "microvalor-12h",
                new OperationalMissionCompletionRequest(
                        "Microentrega Studio Aurora",
                        "micro-v1",
                        "Conteúdo individual com três cenários, duas perguntas e uma resposta ajustada ao briefing da cliente. "
                                + "A mensagem exige revisão humana antes de qualquer uso no WhatsApp."));
        accessService.completeOperationalMission(
                access.token(),
                "entrega-completa-48h",
                new OperationalMissionCompletionRequest(
                        "Kit completo Studio Aurora",
                        "kit-v1",
                        null,
                        completeDeliverySections()));
        accessService.requestSupport(access.token(), "Quero revisar a resposta de preço antes do uso.");

        AccessService restarted = new AccessService(catalog, new ObjectMapper(), storagePath);
        WorkspaceResponse workspace = restarted.getWorkspace(access.token());

        assertThat(workspace.deliveryArtifacts()).hasSize(2);
        assertThat(workspace.deliveryArtifacts()).extracting("missionId")
                .containsExactlyInAnyOrder("microvalor-12h", "entrega-completa-48h");
        assertThat(restarted.getDeliveryArtifact(access.token(), "microvalor-12h").content())
                .contains("três cenários", "revisão humana");
        assertThat(workspace.supportStatus()).isEqualTo("OPEN");
    }

    /** Bloqueia uma entrega que apenas declara quantidades sem materializar cada componente prometido. */
    @Test
    void rejectsDeclarativeFullDeliveryWithoutStructuredItems() {
        ProductCatalogService catalog = assistedDeliveryCatalog();
        AccessService accessService = new AccessService(
                catalog, new ObjectMapper(), tempDir.resolve("declarative-delivery.json").toString());
        AccessResponse access = accessService.createAccess("kit-delivery", "cliente@sandbox.local", "DEV");

        saveCompleteBriefing(accessService, access.token());
        accessService.completeMission(access.token(), "entrada-guiada");
        accessService.completeOperationalMission(access.token(), "conferencia-de-completude");
        accessService.completeOperationalMission(access.token(), "diagnostico-humano");
        accessService.completeOperationalMission(
                access.token(),
                "microvalor-12h",
                new OperationalMissionCompletionRequest(
                        "Microentrega Studio Aurora",
                        "micro-v1",
                        "Conteúdo individual com três cenários, duas perguntas e uma resposta ajustada ao briefing da cliente. "
                                + "A mensagem exige revisão humana antes de qualquer uso no WhatsApp."));

        assertThrows(IllegalArgumentException.class,
                () -> accessService.completeOperationalMission(
                        access.token(),
                        "entrega-completa-48h",
                        new OperationalMissionCompletionRequest(
                                "Kit completo Studio Aurora",
                                "kit-v1",
                                "Declara incluir quinze respostas, oito perguntas, quatro follow-ups, guia e checklist.")));
    }

    /** Monta um catálogo mínimo com autoridade explícita para cliente e operação. */
    private ProductCatalogService roleAwareCatalog() {
        ProductCatalogService catalog = mock(ProductCatalogService.class);
        ProductExperienceResponse product = mock(ProductExperienceResponse.class);
        when(product.missions()).thenReturn(List.of(
                new ProductExperienceResponse.MissionDto(
                        "entrada", 1, "Entrada", "Objetivo", "Ação", "Evidência", "Dica", "CUSTOMER"),
                new ProductExperienceResponse.MissionDto(
                        "entrega", 2, "Entrega", "Objetivo", "Ação", "Evidência", "Dica", "OPERATION"),
                new ProductExperienceResponse.MissionDto(
                        "aplicacao", 3, "Aplicação", "Objetivo", "Ação", "Evidência", "Dica", "CUSTOMER")));
        when(catalog.getProduct("kit-role-aware")).thenReturn(product);
        return catalog;
    }

    /** Monta a sequência canônica mínima que exige duas entregas personalizadas. */
    private ProductCatalogService assistedDeliveryCatalog() {
        ProductCatalogService catalog = mock(ProductCatalogService.class);
        ProductExperienceResponse product = mock(ProductExperienceResponse.class);
        when(product.missions()).thenReturn(List.of(
                new ProductExperienceResponse.MissionDto(
                        "entrada-guiada", 1, "Entrada", "Objetivo", "Ação", "Evidência", "Dica", "CUSTOMER"),
                new ProductExperienceResponse.MissionDto(
                        "conferencia-de-completude", 2, "Conferência", "Objetivo", "Ação", "Evidência", "Dica", "OPERATION"),
                new ProductExperienceResponse.MissionDto(
                        "diagnostico-humano", 3, "Diagnóstico", "Objetivo", "Ação", "Evidência", "Dica", "OPERATION"),
                new ProductExperienceResponse.MissionDto(
                        "microvalor-12h", 4, "Microvalor", "Objetivo", "Ação", "Evidência", "Dica", "OPERATION"),
                new ProductExperienceResponse.MissionDto(
                        "entrega-completa-48h",
                        5,
                        "Entrega",
                        "Objetivo",
                        "Ação",
                        "Evidência",
                        "Dica",
                        "OPERATION",
                        completeDeliveryContract()),
                new ProductExperienceResponse.MissionDto(
                        "primeira-aplicacao-e-revisao", 6, "Aplicação", "Objetivo", "Ação", "Evidência", "Dica", "CUSTOMER")));
        when(catalog.getProduct("kit-delivery")).thenReturn(product);
        return catalog;
    }

    /** Define o contrato mínimo do kit completo usado nos testes da entrega material. */
    private ProductExperienceResponse.DeliveryContractDto completeDeliveryContract() {
        return new ProductExperienceResponse.DeliveryContractDto(List.of(
                new ProductExperienceResponse.DeliverySectionDto("responses", "Respostas", 10, 20),
                new ProductExperienceResponse.DeliverySectionDto("qualificationQuestions", "Perguntas", 5, 10),
                new ProductExperienceResponse.DeliverySectionDto("followUps", "Follow-ups", 3, 5),
                new ProductExperienceResponse.DeliverySectionDto("escalationRules", "Regras", 1, 8),
                new ProductExperienceResponse.DeliverySectionDto("usageGuide", "Guia", 3, 10),
                new ProductExperienceResponse.DeliverySectionDto("checklist", "Checklist", 5, 20)));
    }

    /** Monta uma entrega completa com todos os itens exigidos pelo contrato. */
    private List<DeliverySectionRequest> completeDeliverySections() {
        return List.of(
                new DeliverySectionRequest("responses", numberedItems("Resposta personalizada", 10)),
                new DeliverySectionRequest("qualificationQuestions", numberedItems("Pergunta personalizada", 5)),
                new DeliverySectionRequest("followUps", numberedItems("Follow-up manual", 3)),
                new DeliverySectionRequest("escalationRules", List.of("Revisar pessoalmente toda exceção")),
                new DeliverySectionRequest("usageGuide", numberedItems("Passo de uso", 3)),
                new DeliverySectionRequest("checklist", numberedItems("Item de revisão", 5)));
    }

    /** Gera itens distintos para provar quantidade material em cada seção. */
    private List<String> numberedItems(String prefix, int quantity) {
        return java.util.stream.IntStream.rangeClosed(1, quantity)
                .mapToObj(index -> prefix + " " + index + " ajustado ao Studio Aurora")
                .toList();
    }

    /** Salva o briefing completo antes de concluir a entrada da cliente. */
    private void saveCompleteBriefing(AccessService accessService, String token) {
        accessService.saveMissionInteraction(token, "entrada-guiada", new MissionInteractionRequest(Map.of(
                "services", "Manicure, alongamento e manutenção",
                "repeatedQuestions", "Preço, duração e agenda",
                "policies", "Hora marcada na região central",
                "tone", "Acolhedor, profissional e direto",
                "anonymousScenarios", "Cinco situações anonimizadas")));
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
                "mainObstacle", "Pareço comum",
                "desiredSignal", "Elegância discreta")));

        AccessService restartedService = new AccessService(productCatalogService, new ObjectMapper(), storagePath);
        WorkspaceResponse workspace = restartedService.getWorkspace(access.token());

        assertThat(workspace.missionInteractions())
                .extracting("questionKey")
                .contains("presenceFocus", "mainObstacle", "desiredSignal");
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

    /** Confirma que o Kit recebe link mágico no próprio domínio e nunca no domínio MUSA. */
    @Test
    void sendsNonMusaMagicLinkToProductDomain() {
        ProductCatalogService productCatalogService = mock(ProductCatalogService.class);
        when(productCatalogService.getProduct("kit-whatsapp-pronto"))
                .thenReturn(mock(ProductExperienceResponse.class));
        CapturingMailService mailService = new CapturingMailService();
        AccessService accessService = new AccessService(
                productCatalogService,
                new ObjectMapper(),
                tempDir.resolve("kit-magic-link.json").toString(),
                "",
                "",
                "",
                false,
                "http://localhost:5176",
                false,
                mailService,
                null);

        var response = accessService.requestMagicLink(
                "kit-whatsapp-pronto", "prestador@sandbox.local");

        assertThat(response.deliveryStatus()).isEqualTo("SENT");
        assertThat(mailService.accessUrl)
                .startsWith("https://kit-whatsapp-pronto.digicomdigital.com.br/access/")
                .doesNotContain("clubemusa.com.br");
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

    /** Confirma que a compra da v7 preserva a versão e libera exatamente noventa dias. */
    @Test
    void grantsMusaV7PaidAccessForNinetyDays() {
        ProductCatalogService productCatalogService = new ProductCatalogService();
        AccessService accessService = new AccessService(
                productCatalogService,
                new ObjectMapper(),
                tempDir.resolve("access-grants-v7.json").toString());
        Instant beforePurchase = Instant.now();

        AccessResponse access = accessService.createAccess(
                "metodo-musa-7-dias",
                "cliente-v7@sandbox.local",
                "CHECKOUT",
                "musa-pde-entry-v7-espelho-antes-de-sair");
        WorkspaceResponse workspace = accessService.getWorkspace(access.token());

        assertThat(workspace.subscriptionStatus()).isEqualTo("ACTIVE");
        assertThat(workspace.experienceVersion()).isEqualTo("musa-pde-entry-v7-espelho-antes-de-sair");
        assertThat(Instant.parse(workspace.accessExpiresAt()))
                .isBetween(beforePurchase.plusSeconds(89L * 24 * 3600), beforePurchase.plusSeconds(91L * 24 * 3600));
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
                "owm6x",
                6700,
                "BRL",
                "paid");
        WorkspaceResponse workspace = accessService.getWorkspace(access.token());

        assertThat(workspace.subscriptionStatus()).isEqualTo("ACTIVE");
        assertThat(workspace.accessSource()).isEqualTo("PEPPER");
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
                "TASTING_STARTED",
                "VALUE_MOMENT",
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

    /** Confirma que falha no consolidado principal preserva a causa técnica para stack trace. */
    @Test
    void preservesRootCauseWhenAnalyticsSummaryFails() throws SQLException {
        String jdbcUrl = "jdbc:h2:mem:pde_broken_analytics;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
        createPartialPdeFunnelEventSchema(jdbcUrl);
        try (var connection = DriverManager.getConnection(jdbcUrl, "sa", "sa");
                var statement = connection.createStatement()) {
            statement.execute("DROP TABLE pde_funnel_event");
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

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> accessService.summarizeFunnelAnalytics("metodo-musa-7-dias"));

        assertThat(exception).hasMessage("Não foi possível consolidar analytics PDE");
        assertThat(exception).hasCauseInstanceOf(SQLException.class);
    }

    /** Confirma que falha em breakdown auxiliar bloqueia relatório parcial de analytics. */
    @Test
    void failsAnalyticsSummaryWhenAuxiliaryBreakdownSchemaIsIncomplete() throws SQLException {
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

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> accessService.summarizeFunnelAnalytics("metodo-musa-7-dias"));

        assertThat(exception).hasMessage("Não foi possível consolidar analytics PDE");
        assertThat(exception).hasCauseInstanceOf(SQLException.class);
    }

    /** Confirma que schema legado sem qualidade de tráfego não vira relatório comercial parcial. */
    @Test
    void failsAnalyticsSummaryWhenCanonicalSchemaIsMissingTrafficQuality() throws SQLException {
        String jdbcUrl = "jdbc:h2:mem:pde_legacy_analytics;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
        createLegacyPdeFunnelEventSchema(jdbcUrl);
        try (var connection = DriverManager.getConnection(jdbcUrl, "sa", "sa");
                var statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO pde_funnel_event (
                      event_id, product_slug, event_type, session_id, visitor_id, visible_ms, occurred_at
                    )
                    VALUES
                      ('event-legacy-1', 'metodo-musa-7-dias', 'PAGE_VIEW', 'session-legacy-1',
                        'visitor-legacy-1', 1200, TIMESTAMP '2026-07-30 12:00:00'),
                      ('event-legacy-2', 'metodo-musa-7-dias', 'PED_ENTRY', 'session-legacy-1',
                        'visitor-legacy-1', 800, TIMESTAMP '2026-07-30 12:00:02')
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

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> accessService.summarizeFunnelAnalytics("metodo-musa-7-dias"));

        assertThat(exception).hasMessage("Não foi possível consolidar analytics PDE");
        assertThat(exception).hasCauseInstanceOf(SQLException.class);
    }

    /** Confirma que o resumo aciona a migração canônica antes de consultar analytics. */
    @Test
    void migratesOperationalSchemaBeforeAnalyticsSummary() throws SQLException {
        String jdbcUrl = "jdbc:h2:mem:pde_analytics_schema_ready;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
        createPdeFunnelEventSchema(jdbcUrl);
        PdeDatabaseMigrationService migrationService = mock(PdeDatabaseMigrationService.class);
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
                "",
                migrationService,
                null,
                null);
        reset(migrationService);

        accessService.summarizeFunnelAnalytics("metodo-musa-7-dias");

        verify(migrationService).migrateIfNeeded();
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

    /** Confirma que origem de tráfego consolida em Java sem duplicar sessão por múltiplos eventos. */
    @Test
    void summarizesTrafficSourcesWithoutDatabaseGrouping() throws SQLException {
        String jdbcUrl = "jdbc:h2:mem:pde_traffic_source_java_summary;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
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

        Map<String, Object> metaTrafficA = Map.of(
                "visitorId", "visitor-a",
                "sessionId", "session-a",
                "utmSource", "ig",
                "utmMedium", "paid",
                "utmCampaign", "musa-v6",
                "utmContent", "criativo-a",
                "userAgent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 Safari/604.1",
                "deviceType", "mobile");
        accessService.recordFunnelEvent(new FunnelEventRequest(
                "metodo-musa-7-dias",
                "PED_ENTRY",
                null,
                null,
                "TEST",
                "test",
                "https://v6.clubemusa.com.br/?utm_source=ig&utm_medium=paid&utm_campaign=musa-v6&utm_content=criativo-a",
                metaTrafficA));
        accessService.recordFunnelEvent(new FunnelEventRequest(
                "metodo-musa-7-dias",
                "VIDEO_COMPLETED",
                null,
                null,
                "TEST",
                "test",
                "https://v6.clubemusa.com.br/?utm_source=ig&utm_medium=paid&utm_campaign=musa-v6&utm_content=criativo-a",
                metaTrafficA));
        accessService.recordFunnelEvent(new FunnelEventRequest(
                "metodo-musa-7-dias",
                "CHECKOUT_STARTED",
                null,
                null,
                "TEST",
                "test",
                "https://v6.clubemusa.com.br/?utm_source=google&utm_medium=cpc&utm_campaign=musa-v6&utm_content=search",
                Map.of(
                        "visitorId", "visitor-b",
                        "sessionId", "session-b",
                        "utmSource", "google",
                        "utmMedium", "cpc",
                        "utmCampaign", "musa-v6",
                        "utmContent", "search",
                        "userAgent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 Safari/604.1",
                        "deviceType", "mobile")));

        var summary = accessService.summarizeFunnelAnalytics("metodo-musa-7-dias");

        assertThat(summary.trafficSources()).hasSize(2);
        assertThat(summary.trafficSources())
                .anySatisfy(source -> {
                    assertThat(source.trafficChannel()).isEqualTo("Meta");
                    assertThat(source.utmSource()).isEqualTo("ig");
                    assertThat(source.sessions()).isEqualTo(1);
                    assertThat(source.pdeEntries()).isEqualTo(1);
                    assertThat(source.videoComplete()).isEqualTo(1);
                    assertThat(source.checkoutStarted()).isZero();
                })
                .anySatisfy(source -> {
                    assertThat(source.trafficChannel()).isEqualTo("Google Search");
                    assertThat(source.sessions()).isEqualTo(1);
                    assertThat(source.checkoutStarted()).isEqualTo(1);
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

    /** Isola eventos, origem e jornada quando o monitor pede uma versão produtiva específica. */
    @Test
    void filtersCommercialAnalyticsByExperienceVersion() throws SQLException {
        String jdbcUrl = "jdbc:h2:mem:pde_version_analytics;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
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

        accessService.recordFunnelEvent(versionedEvent("musa-v6", "session-v6", "campaign-v6"));
        accessService.recordFunnelEvent(versionedEvent("musa-v7", "session-v7", "campaign-v7"));

        var summary = accessService.summarizeFunnelAnalytics("metodo-musa-7-dias", false, "musa-v7");

        assertThat(summary.currentExperienceVersion()).isEqualTo("musa-v7");
        assertThat(summary.totalEvents()).isEqualTo(1);
        assertThat(summary.sessions()).isEqualTo(1);
        assertThat(summary.events()).extracting("eventType").containsExactly("PED_ENTRY");
        assertThat(summary.trafficSources())
                .singleElement()
                .satisfies(source -> assertThat(source.utmCampaign()).isEqualTo("campaign-v7"));
        assertThat(summary.recentJourneys())
                .singleElement()
                .satisfies(journey -> assertThat(journey.sessionId()).isEqualTo("session-v7"));
    }

    /** Cria um evento humano identificado por versão para validar isolamento do analytics. */
    private FunnelEventRequest versionedEvent(String experienceVersion, String sessionId, String campaign) {
        return new FunnelEventRequest(
                "metodo-musa-7-dias",
                "PED_ENTRY",
                null,
                null,
                "TEST",
                "test",
                "https://clubemusa.com.br",
                Map.of(
                        "visitorId", "visitor-" + sessionId,
                        "sessionId", sessionId,
                        "experienceVersion", experienceVersion,
                        "utmSource", "instagram",
                        "utmMedium", "paid_social",
                        "utmCampaign", campaign,
                        "userAgent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)",
                        "deviceType", "mobile"));
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
                "https://clubemusa.com.br/?utm_source=codex&utm_medium=fake&utm_campaign=mh_fake_exp_78",
                "163.245.203.201",
                "Mozilla/5.0 (compatible; HeadlessChrome crawler)",
                Map.of(
                        "visitorId", "visitor-bot",
                        "sessionId", "session-bot",
                        "utmSource", "codex",
                        "utmMedium", "fake",
                        "utmCampaign", "mh_fake_exp_78",
                        "deviceType", "desktop")));

        var summary = accessService.summarizeFunnelAnalytics("metodo-musa-7-dias");
        var diagnosticSummary = accessService.summarizeFunnelAnalytics("metodo-musa-7-dias", true);

        assertThat(summary.totalEvents()).isEqualTo(1);
        assertThat(summary.rawTotalEvents()).isEqualTo(2);
        assertThat(summary.sessions()).isEqualTo(1);
        assertThat(summary.rawSessions()).isEqualTo(2);
        assertThat(summary.humanSessions()).isEqualTo(1);
        assertThat(summary.botSuspectedSessions()).isEqualTo(1);
        assertThat(summary.trafficSources())
                .singleElement()
                .satisfies(source -> assertThat(source.utmSource()).isEqualTo("ig"));
        assertThat(diagnosticSummary.totalEvents()).isEqualTo(1);
        assertThat(diagnosticSummary.trafficSources())
                .anySatisfy(source -> {
                    assertThat(source.utmSource()).isEqualTo("codex");
                    assertThat(source.utmCampaign()).isEqualTo("mh_fake_exp_78");
                    assertThat(source.sessions()).isEqualTo(1);
                });
        assertThat(summary.trafficQualityBreakdown())
                .extracting("trafficQuality")
                .contains("HUMAN", "BOT_SUSPECTED");
        assertThat(summary.recentJourneys())
                .singleElement()
                .satisfies(journey -> assertThat(journey.sessionId()).isEqualTo("session-human"));
    }

    /** Mantém evento funcional de homologação fora dos indicadores humanos e comerciais. */
    @Test
    void excludesExplicitTestMarkerBeforeFunctionalEventClassification() throws SQLException {
        String jdbcUrl = "jdbc:h2:mem:pde_explicit_test;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
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
                "FIRST_USE",
                "test-token",
                "teste+round@sandbox.local",
                "DEV",
                "mh_test",
                "http://localhost:5176/?mh_test=1",
                "127.0.0.1",
                "Mozilla/5.0",
                Map.of("visitorId", "test-visitor", "sessionId", "test-session")));

        var summary = accessService.summarizeFunnelAnalytics("metodo-musa-7-dias");

        assertThat(summary.totalEvents()).isZero();
        assertThat(summary.rawTotalEvents()).isEqualTo(1);
        assertThat(summary.humanSessions()).isZero();
        assertThat(summary.trafficQualityBreakdown())
                .singleElement()
                .satisfies(metric -> {
                    assertThat(metric.trafficQuality()).isEqualTo("INTERNAL_QA");
                    assertThat(metric.events()).isEqualTo(1);
                });
    }

    /** Mantém a ativação gerada internamente por um acesso DEV fora dos indicadores comerciais. */
    @Test
    void excludesDevAccessEventsWithoutDependingOnPageMarkers() throws SQLException {
        String jdbcUrl = "jdbc:h2:mem:pde_dev_access;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
        createPdeFunnelEventSchema(jdbcUrl);
        AccessService accessService = new AccessService(
                new ProductCatalogService(),
                new ObjectMapper(),
                tempDir.resolve("access-grants-dev.json").toString(),
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
                "FIRST_USE",
                "dev-token",
                "teste+dev@sandbox.local",
                "DEV",
                "pde-platform",
                null,
                Map.of("missionId", "dia-1-ruido-visual")));

        var summary = accessService.summarizeFunnelAnalytics("metodo-musa-7-dias");

        assertThat(summary.totalEvents()).isZero();
        assertThat(summary.humanSessions()).isZero();
        assertThat(summary.internalQaSessions()).isEqualTo(1);
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
                      event_id, product_slug, event_type, source, page_url, traffic_quality, session_id, visitor_id,
                      visible_ms, action_name, metadata_json, occurred_at
                    )
                    VALUES (
                      'event-session-time-1', 'metodo-musa-7-dias', 'PAGE_VISIBLE_TIME', 'test',
                      'https://clubemusa.com.br/acesso', 'HUMAN', 'session-time-1', 'visitor-time-1',
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

    /** Confirma que o resumo carrega jornadas recentes sem derrubar o analytics por ordenação pesada. */
    @Test
    void summarizesRecentSessionJourneysWithBoundedQuery() throws SQLException {
        String jdbcUrl = "jdbc:h2:mem:pde_session_journey_bounded;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
        createPdeFunnelEventSchema(jdbcUrl);
        try (var connection = DriverManager.getConnection(jdbcUrl, "sa", "sa");
                var statement = connection.createStatement()) {
            for (int sessionIndex = 1; sessionIndex <= 35; sessionIndex++) {
                for (int eventIndex = 1; eventIndex <= 3; eventIndex++) {
                    statement.execute("""
                            INSERT INTO pde_funnel_event (
                              event_id, product_slug, event_type, source, page_url, traffic_quality,
                              session_id, visitor_id, visible_ms, action_name, metadata_json, occurred_at
                            )
                            VALUES (
                              'event-bounded-%d-%d', 'metodo-musa-7-dias', 'PAGE_VISIBLE_TIME', 'test',
                              'https://clubemusa.com.br/acesso', 'HUMAN', 'session-bounded-%d',
                              'visitor-bounded-%d', 1000, 'page_visibility_flush', '{"screenName":"login_first_access"}',
                              TIMESTAMP '2026-07-24 21:%02d:%02d'
                            )
                            """.formatted(
                            sessionIndex,
                            eventIndex,
                            sessionIndex,
                            sessionIndex,
                            sessionIndex,
                            eventIndex));
                }
            }
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

        assertThat(summary.totalEvents()).isEqualTo(105);
        assertThat(summary.sessions()).isEqualTo(35);
        assertThat(summary.recentJourneys()).hasSize(20);
        assertThat(summary.recentJourneys().get(0).sessionId()).isEqualTo("session-bounded-35");
        assertThat(summary.recentJourneys().get(19).sessionId()).isEqualTo("session-bounded-16");
        assertThat(summary.recentJourneys().get(0).steps()).hasSize(3);
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

    /** Confirma que todos os sete dias da v7 usam regras locais sem fila, tokens ou texto livre. */
    @Test
    void completesAllMusaV7GuidanceLocallyAndRejectsFreeText() {
        ProductCatalogService productCatalogService = new ProductCatalogService();
        ObjectMapper objectMapper = new ObjectMapper();
        AccessService accessService = new AccessService(
                productCatalogService,
                objectMapper,
                tempDir.resolve("access-grants-v7-local.json").toString());
        AiGuidanceService aiGuidanceService = new AiGuidanceService(
                accessService,
                productCatalogService,
                objectMapper,
                tempDir.resolve("ai-guidance-v7-local.json").toString(),
                "",
                "",
                "",
                new PdeDatabaseMigrationService("", "", ""));
        AccessResponse access = accessService.createAccess(
                "metodo-musa-7-dias",
                "local-v7@sandbox.local",
                "CHECKOUT",
                "musa-pde-entry-v7-espelho-antes-de-sair");

        Map<String, AiGuidanceCreateRequest> requestsByMission = new LinkedHashMap<>();
        requestsByMission.put(
                "dia-1-ruido-visual",
                new AiGuidanceCreateRequest(
                        "MUSA_DAY_1_PRESENCE_DIAGNOSIS",
                        Map.of(
                                "presenceFocus", "Trabalho ou reunião",
                                "mainObstacle", "Falta presença",
                                "desiredSignal", "Elegância discreta"),
                        "musa-pde-entry-v7-espelho-antes-de-sair"));
        requestsByMission.put(
                "dia-2-assinatura",
                new AiGuidanceCreateRequest(
                        "MUSA_DAY_2_SIGNATURE",
                        Map.of(
                                "finishSignal", "Cabelo polido",
                                "baseColor", "Vinho discreto",
                                "memorableSignal", "Brinco luminoso"),
                        "musa-pde-entry-v7-espelho-antes-de-sair"));
        requestsByMission.put(
                "dia-3-base-acessivel",
                new AiGuidanceCreateRequest(
                        "MUSA_DAY_3_WARDROBE_REUSE",
                        Map.of(
                                "pieces", "Calça e camisa",
                                "accessories", "Brinco e perfume",
                                "realOccasion", "Rotina comum"),
                        "musa-pde-entry-v7-espelho-antes-de-sair"));
        requestsByMission.put(
                "dia-4-checklist-12-minutos",
                new AiGuidanceCreateRequest(
                        "MUSA_DAY_4_FINISHING_RITUAL",
                        Map.of(
                                "availableMinutes", "10 minutos",
                                "weakestFinish", "Cabelo",
                                "desiredFeeling", "Mais segura"),
                        "musa-pde-entry-v7-espelho-antes-de-sair"));
        requestsByMission.put(
                "dia-5-compra-inteligente",
                new AiGuidanceCreateRequest(
                        "MUSA_DAY_5_ANTI_IMPULSE_DECISION",
                        Map.of(
                                "desiredItem", "Roupa",
                                "buyingReason", "Impulso ou novidade",
                                "fitWithSignature", "Ainda não sei"),
                        "musa-pde-entry-v7-espelho-antes-de-sair"));
        requestsByMission.put(
                "dia-6-situacao-chave",
                new AiGuidanceCreateRequest(
                        "MUSA_DAY_6_OCCASION_ENTRY",
                        Map.of(
                                "occasion", "Evento",
                                "plannedLook", "Base neutra e detalhe",
                                "presenceRisk", "Desconforto"),
                        "musa-pde-entry-v7-espelho-antes-de-sair"));
        requestsByMission.put(
                "dia-7-plano-pessoal",
                new AiGuidanceCreateRequest(
                        "MUSA_DAY_7_MAINTENANCE_PLAN",
                        Map.of(
                                "bestSignal", "Acabamento",
                                "hardestPoint", "Pouco tempo",
                                "weeklyRitual", "Separar 3 combinações"),
                        "musa-pde-entry-v7-espelho-antes-de-sair"));

        requestsByMission.forEach((missionId, request) -> {
            var guidance = aiGuidanceService.createGuidanceRequest(access.token(), missionId, request);
            assertThat(guidance.status()).isEqualTo("COMPLETED");
            assertThat(guidance.model()).isEqualTo("MUSA_LOCAL_RULES_V1");
            assertThat(guidance.inputTokens()).isZero();
            assertThat(guidance.outputTokens()).isZero();
            assertThat(guidance.summary()).doesNotContainIgnoringCase("ciência", "garantia", "aprovação");
            assertThat(guidance.caution()).contains("não avalia", "reação de terceiros");
            accessService.completeMission(access.token(), missionId);
        });
        var neutralGuidance = aiGuidanceService.createPublicPresenceDiagnostic(
                new PublicPresenceDiagnosticRequest(
                        Map.of(
                                "mainObstacle", "Minha imagem está coerente; quero apenas organizar minhas escolhas",
                                "presenceFocus", "Rotina comum",
                                "desiredSignal", "Elegância discreta",
                                "startingResource", "Roupa que já tenho"),
                        "musa-pde-entry-v7-espelho-antes-de-sair"));
        assertThat(neutralGuidance.status()).isEqualTo("COMPLETED");
        assertThat(neutralGuidance.headline()).isEqualTo("Sua escolha atual foi preservada");
        assertThat(neutralGuidance.summary()).contains("não precisa corrigir sua imagem");
        assertThat(neutralGuidance.microActions()).contains("Você pode seguir para o próximo dia sem realizar uma microação.");
        assertThat(neutralGuidance.inputTokens()).isZero();
        assertThat(neutralGuidance.outputTokens()).isZero();
        assertThat(aiGuidanceService.getPendingGuidance()).isEmpty();
        assertThrows(IllegalArgumentException.class, () -> aiGuidanceService.createGuidanceRequest(
                access.token(),
                "dia-3-base-acessivel",
                new AiGuidanceCreateRequest(
                        "MUSA_DAY_3_WARDROBE_REUSE",
                        Map.of("pieces", "minha roupa favorita"),
                        "musa-pde-entry-v7-espelho-antes-de-sair")));
        assertThat(accessService.getWorkspace(access.token()).missionInteractions())
                .noneMatch(interaction -> "minha roupa favorita".equals(interaction.answerText()));
    }

    /** Confirma que a v7 bloqueia salto de dia no backend e preserva a versão de uma compra existente. */
    @Test
    void blocksMusaV7DayJumpAndFreezesPaidExperienceVersion() {
        ProductCatalogService catalog = new ProductCatalogService();
        AccessService accessService = new AccessService(
                catalog,
                new ObjectMapper(),
                tempDir.resolve("access-grants-v7-sequence.json").toString());
        AccessResponse access = accessService.createInternalQaAccess(
                "metodo-musa-7-dias",
                "sequencia@sandbox.local",
                "musa-pde-entry-v7-espelho-antes-de-sair");

        assertThrows(IllegalArgumentException.class, () -> accessService.saveMissionInteraction(
                access.token(),
                "dia-2-assinatura",
                new MissionInteractionRequest(Map.of(
                        "finishSignal", "Cabelo polido",
                        "baseColor", "Vinho discreto",
                        "memorableSignal", "Brinco luminoso"))));

        AccessResponse retryOnOldHost = accessService.createAccess(
                "metodo-musa-7-dias",
                "sequencia@sandbox.local",
                "CUSTOMER_REGISTRATION",
                "musa-pde-entry-v5-video-explicativo");
        WorkspaceResponse workspace = accessService.getWorkspace(retryOnOldHost.token());
        assertThat(workspace.experienceVersion()).isEqualTo("musa-pde-entry-v7-espelho-antes-de-sair");
    }

    /** Impede texto livre, chave desconhecida e valor de outra pergunta pela rota direta da v7. */
    @Test
    void enforcesMusaV7CategoricalContractOnDirectMissionInteraction() {
        AccessService accessService = new AccessService(
                new ProductCatalogService(),
                new ObjectMapper(),
                tempDir.resolve("access-grants-v7-categorical.json").toString());
        AccessResponse access = accessService.createInternalQaAccess(
                "metodo-musa-7-dias",
                "categorias@sandbox.local",
                "musa-pde-entry-v7-espelho-antes-de-sair");

        assertThrows(IllegalArgumentException.class, () -> accessService.saveMissionInteraction(
                access.token(),
                "dia-1-ruido-visual",
                new MissionInteractionRequest(Map.of("freeText", "conteúdo arbitrário"))));
        assertThrows(IllegalArgumentException.class, () -> accessService.saveMissionInteraction(
                access.token(),
                "dia-1-ruido-visual",
                new MissionInteractionRequest(Map.of("mainObstacle", "conteúdo arbitrário"))));
        assertThrows(IllegalArgumentException.class, () -> accessService.saveMissionInteraction(
                access.token(),
                "dia-1-ruido-visual",
                new MissionInteractionRequest(Map.of("mainObstacle", "Trabalho ou reunião"))));
        assertThat(accessService.getWorkspace(access.token()).missionInteractions()).isEmpty();
    }

    /** Confirma que QA pago fica segregado e que material exige entitlement ainda ativo. */
    @Test
    void segregatesInternalQaAccessAndProtectsPaidMaterials() {
        AccessService accessService = new AccessService(
                new ProductCatalogService(),
                new ObjectMapper(),
                tempDir.resolve("access-grants-v7-qa.json").toString());
        AccessResponse access = accessService.createInternalQaAccess(
                "metodo-musa-7-dias",
                "material@sandbox.local",
                "musa-pde-entry-v7-espelho-antes-de-sair");

        WorkspaceResponse workspace = accessService.getWorkspace(access.token());
        assertThat(workspace.subscriptionStatus()).isEqualTo("ACTIVE");
        assertThat(workspace.accessSource()).isEqualTo("INTERNAL_QA");
        accessService.authorizeMaterialAccess(access.token());
        assertThrows(SecurityException.class, () -> accessService.authorizeMaterialAccess(""));
        assertThrows(IllegalArgumentException.class, () -> accessService.createInternalQaAccess(
                "metodo-musa-7-dias",
                "cliente@exemplo.com",
                "musa-pde-entry-v7-espelho-antes-de-sair"));
    }

    /** Confirma que o navegador não pode fabricar compra ou liberação por evento público. */
    @Test
    void rejectsFinalCommercialEventsFromPublicIngestion() {
        AccessService accessService = new AccessService(
                new ProductCatalogService(),
                new ObjectMapper(),
                tempDir.resolve("access-grants-public-events.json").toString());

        assertThrows(SecurityException.class, () -> accessService.recordPublicFunnelEvent(new FunnelEventRequest(
                "metodo-musa-7-dias",
                "SUBSCRIPTION_APPROVED",
                null,
                "teste@sandbox.local",
                "browser",
                "frontend",
                null,
                Map.of())));
    }

    /** Confirma exportação, exclusão e retenção auditáveis sem conservar respostas identificáveis. */
    @Test
    void executesPrivacyRightsAndRetention() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Path activeStore = tempDir.resolve("access-grants-privacy.json");
        AccessService accessService = new AccessService(
                new ProductCatalogService(), objectMapper, activeStore.toString());
        AccessResponse access = accessService.createInternalQaAccess(
                "metodo-musa-7-dias",
                "privacidade@sandbox.local",
                "musa-pde-entry-v7-espelho-antes-de-sair");
        accessService.saveMissionInteraction(
                access.token(),
                "dia-1-ruido-visual",
                new MissionInteractionRequest(Map.of("mainObstacle", "Manter como está por enquanto")));

        var exported = accessService.executePrivacyAction(
                access.token(), new PrivacyActionRequest("ACCESS", null));
        assertThat(exported.status()).isEqualTo("COMPLETED");
        assertThat(exported.data()).containsEntry("email", "privacidade@sandbox.local");

        var deleted = accessService.executePrivacyAction(
                access.token(), new PrivacyActionRequest("DELETION", null));
        assertThat(deleted.status()).isEqualTo("COMPLETED");
        assertThrows(IllegalArgumentException.class, () -> accessService.getWorkspace(access.token()));
        assertThat(Files.readString(activeStore))
                .doesNotContain("privacidade@sandbox.local", "Manter como está por enquanto")
                .contains("PRIVACY_DELETED", "DELETION");

        Path expiredStore = tempDir.resolve("access-grants-expired-retention.json");
        String expiredToken = "expired-retention-token";
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(expiredStore.toFile(), Map.of(
                expiredToken,
                Map.of(
                        "productSlug", "metodo-musa-7-dias",
                        "email", "expirada@sandbox.local",
                        "source", "PEPPER",
                        "createdAt", "2025-01-01T00:00:00Z",
                        "experienceVersion", "musa-pde-entry-v7-espelho-antes-de-sair",
                        "paidAt", "2025-01-01T00:00:00Z",
                        "expiresAt", "2025-04-01T00:00:00Z",
                        "completedMissionIds", List.of("dia-1-ruido-visual"),
                        "missionInteractions", Map.of("dia-1-ruido-visual", Map.of("mainObstacle", "Falta presença")))));
        AccessService retentionService = new AccessService(
                new ProductCatalogService(), objectMapper, expiredStore.toString());
        assertThat(retentionService.enforceDataRetention(Instant.parse("2026-01-01T00:00:00Z"))).isEqualTo(1);
        assertThat(Files.readString(expiredStore))
                .doesNotContain("expirada@sandbox.local", "Falta presença")
                .contains("RETENTION_EXPIRED");
    }

    /** Prova em persistência SQL que exclusão troca o token e apaga todos os correlatores do funil. */
    @Test
    void irreversiblyScrubsJdbcPrivacyCorrelators() throws Exception {
        String jdbcUrl = "jdbc:h2:mem:pde_privacy_scrub;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
        createPrivacyPersistenceSchema(jdbcUrl);
        AccessService accessService = new AccessService(
                new ProductCatalogService(),
                new ObjectMapper(),
                tempDir.resolve("privacy-jdbc.json").toString(),
                jdbcUrl,
                "sa",
                "sa",
                true,
                "http://localhost:5176",
                true,
                "",
                mock(PdeDatabaseMigrationService.class),
                null,
                null);
        AccessResponse access = accessService.createInternalQaAccess(
                "metodo-musa-7-dias",
                "correlacao@sandbox.local",
                "musa-pde-entry-v7-espelho-antes-de-sair");
        accessService.recordFunnelEvent(new FunnelEventRequest(
                "metodo-musa-7-dias",
                "MISSION_OPEN",
                access.token(),
                "correlacao@sandbox.local",
                "INTERNAL_QA",
                "playwright",
                "https://v7.clubemusa.com.br/access/" + access.token(),
                Map.of(
                        "sessionId", "session-identificavel",
                        "visitorId", "visitor-identificavel",
                        "referrerUrl", "https://example.test/access/" + access.token(),
                        "clickId", "click-identificavel")));

        accessService.executePrivacyAction(access.token(), new PrivacyActionRequest("DELETION", null));

        try (var connection = DriverManager.getConnection(jdbcUrl, "sa", "sa");
                var oldGrant = connection.prepareStatement("SELECT COUNT(*) FROM pde_access_grant WHERE token = ?");
                var auditGrant = connection.prepareStatement(
                        "SELECT token, email, source FROM pde_access_grant WHERE source = 'PRIVACY_DELETED'");
                var events = connection.prepareStatement("""
                        SELECT access_token, email, normalized_email, page_url, client_ip, user_agent,
                               referrer_url, session_id, visitor_id, metadata_json
                        FROM pde_funnel_event
                        """)) {
            oldGrant.setString(1, access.token());
            try (var result = oldGrant.executeQuery()) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isZero();
            }
            try (var result = auditGrant.executeQuery()) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString("token")).isNotEqualTo(access.token());
                assertThat(result.getString("email")).endsWith("@privacy.invalid");
                assertThat(result.getString("source")).isEqualTo("PRIVACY_DELETED");
            }
            try (var result = events.executeQuery()) {
                assertThat(result.next()).isTrue();
                for (String column : List.of(
                        "access_token", "email", "normalized_email", "page_url", "client_ip", "user_agent",
                        "referrer_url", "session_id", "visitor_id", "metadata_json")) {
                    assertThat(result.getString(column)).as(column).isNull();
                }
            }
        }
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
                      experience_version VARCHAR(80) NOT NULL DEFAULT '',
                      paid_at TIMESTAMP NULL,
                      expires_at TIMESTAMP NULL,
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

    /** Cria schema legado de analytics anterior à classificação de tráfego humano. */
    private static void createLegacyPdeFunnelEventSchema(String jdbcUrl) throws SQLException {
        try (var connection = DriverManager.getConnection(jdbcUrl, "sa", "sa");
                var statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE pde_access_grant (
                      token VARCHAR(120) PRIMARY KEY,
                      product_slug VARCHAR(120) NOT NULL,
                      email VARCHAR(191) NOT NULL,
                      source VARCHAR(80) NOT NULL,
                      experience_version VARCHAR(80) NOT NULL DEFAULT '',
                      paid_at TIMESTAMP NULL,
                      expires_at TIMESTAMP NULL,
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
                      event_type VARCHAR(80) NOT NULL,
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
                      experience_version VARCHAR(80) NOT NULL DEFAULT '',
                      paid_at TIMESTAMP NULL,
                      expires_at TIMESTAMP NULL,
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

    /** Cria o schema relacional completo exigido pelo teste de anonimização irreversível. */
    private static void createPrivacyPersistenceSchema(String jdbcUrl) throws SQLException {
        try (var connection = DriverManager.getConnection(jdbcUrl, "sa", "sa");
                var statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE pde_access_grant (
                      token VARCHAR(36) PRIMARY KEY,
                      product_slug VARCHAR(191) NOT NULL,
                      email VARCHAR(320) NOT NULL,
                      normalized_email VARCHAR(320) NOT NULL,
                      source VARCHAR(40) NOT NULL,
                      experience_version VARCHAR(80) NOT NULL DEFAULT '',
                      paid_at TIMESTAMP NULL,
                      expires_at TIMESTAMP NULL,
                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      UNIQUE (product_slug, normalized_email)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE pde_access_mission_completion (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      access_token VARCHAR(36) NOT NULL,
                      mission_id VARCHAR(191) NOT NULL,
                      completed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            statement.execute("""
                    CREATE TABLE pde_access_mission_interaction_answer (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      access_token VARCHAR(36) NOT NULL,
                      product_slug VARCHAR(191) NOT NULL,
                      mission_id VARCHAR(191) NOT NULL,
                      question_key VARCHAR(100) NOT NULL,
                      answer_text TEXT NOT NULL,
                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      UNIQUE (access_token, mission_id, question_key)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE pde_ai_guidance_request (
                      request_id VARCHAR(36) PRIMARY KEY,
                      access_token VARCHAR(120) NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE pde_funnel_event (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      event_id VARCHAR(64) NOT NULL UNIQUE,
                      product_slug VARCHAR(120) NOT NULL,
                      experience_version VARCHAR(80),
                      access_token VARCHAR(120), email VARCHAR(191), normalized_email VARCHAR(191),
                      event_type VARCHAR(80) NOT NULL, provider VARCHAR(80), source VARCHAR(120),
                      page_url VARCHAR(1024), client_ip VARCHAR(45), user_agent VARCHAR(512),
                      traffic_quality VARCHAR(40), traffic_quality_reason VARCHAR(120), traffic_provider VARCHAR(80),
                      referrer_url VARCHAR(1024), session_id VARCHAR(64), visitor_id VARCHAR(64),
                      utm_source VARCHAR(120), utm_medium VARCHAR(120), utm_campaign VARCHAR(191),
                      utm_content VARCHAR(191), utm_term VARCHAR(191), device_type VARCHAR(40),
                      screen_width INT, screen_height INT, viewport_width INT, viewport_height INT,
                      visible_ms BIGINT, section_id VARCHAR(120), action_name VARCHAR(120), metadata_json TEXT,
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

    /** Captura o link mágico sem enviar e-mail externo durante a homologação. */
    private static class CapturingMailService extends PdeMailService {

        private String accessUrl;

        /** Inicializa o provedor local com transporte marcado como configurado. */
        CapturingMailService() {
            super("ses", "us-east-1", "", 1025, "acesso@digicomdigital.com.br", "", "");
        }

        /** Informa que o provedor de homologação aceita envios. */
        @Override
        public boolean isConfigured() {
            return true;
        }

        /** Guarda o link que seria enviado para permitir a asserção do domínio. */
        @Override
        public void sendMagicLink(String to, String value) {
            this.accessUrl = value;
        }
    }
}
