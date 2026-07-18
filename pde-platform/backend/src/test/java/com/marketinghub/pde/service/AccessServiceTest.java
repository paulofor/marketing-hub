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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;

/** Valida a liberação de acesso e o progresso da experiência PDE. */
class AccessServiceTest {

    @TempDir
    Path tempDir;

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

    /** Confirma que falha do provedor de e-mail nao quebra a criacao do acesso. */
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
                "https://clubemusa.com.br",
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

    /** Confirma que webhook Pepper aguardando pagamento nao libera acesso completo. */
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

    /** Confirma que retry do checkout nao altera o acesso ativo existente. */
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
                "CHECKOUT_STARTED",
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
                objectMapper,
                tempDir.resolve("ai-guidance.json").toString(),
                "",
                "",
                "");
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
                objectMapper,
                tempDir.resolve("ai-guidance.json").toString(),
                "",
                "",
                "");
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
                "Nao compre nada novo antes de testar a repeticao por uma semana.",
                "gpt-5.4-mini",
                "flex",
                "{\"model\":\"gpt-5.4-mini\"}",
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

    /** Simula provedor configurado que rejeita o envio do link magico. */
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
