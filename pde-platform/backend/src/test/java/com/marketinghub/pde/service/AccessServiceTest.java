package com.marketinghub.pde.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.marketinghub.pde.dto.FunnelEventRequest;
import com.marketinghub.pde.dto.AccessResponse;
import com.marketinghub.pde.dto.PepperWebhookRequest;
import com.marketinghub.pde.dto.WorkspaceResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
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
}
