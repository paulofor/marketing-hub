package com.marketinghub.pde.mira.commercial.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.pde.dto.PrivacyActionRequest;
import com.marketinghub.pde.dto.PrivacyActionResponse;
import com.marketinghub.pde.service.AccessService;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Responsabilidade: comprovar entrega, limites e segregação comercial de Mira. */
class MiraCommercialServiceTest {
    @TempDir Path temporaryDirectory;
    private AccessService accessService;
    private MiraCommercialService service;

    /** Cria um acesso pago controlado e armazenamento isolado para cada teste. */
    @BeforeEach
    void setUp() {
        accessService = mock(AccessService.class);
        when(accessService.requirePaidAccessIdentity("paid-token", MiraCommercialService.PRODUCT_SLUG))
                .thenReturn(new AccessService.PaidAccessIdentity(
                        MiraCommercialService.PRODUCT_SLUG,
                        "cliente@example.com",
                        MiraCommercialService.EXPERIENCE_VERSION,
                        "MERCADO_PAGO",
                        Instant.parse("2026-09-26T10:00:00Z"),
                        null));
        service = new MiraCommercialService(
                accessService,
                new ObjectMapper(),
                temporaryDirectory.resolve("mira.json").toString());
    }

    /** Deve organizar produtos documentados e registrar o momento de valor. */
    @Test
    void organizesDocumentedProductsForPaidAccess() {
        service.session("paid-token");
        service.saveInput(
                "paid-token",
                new MiraCommercialService.InputRequest(
                        "Organizar os cuidados que já possuo",
                        List.of(
                                new MiraCommercialService.ProductInput(
                                        "Sabonete", "Limpar e enxaguar"),
                                new MiraCommercialService.ProductInput(
                                        "Hidratante", "Aplicar após a limpeza"))));

        var result = service.generate("paid-token");

        assertThat(result.status()).isEqualTo("READY");
        assertThat(result.attemptsUsed()).isEqualTo(1);
        assertThat(result.routine())
                .extracting(MiraCommercialService.RoutineCard::productName)
                .containsExactly("Sabonete", "Hidratante");
        verify(accessService).recordFunnelEvent(any());
    }

    /** Deve bloquear objetivo clínico sem consumir uma organização da compra. */
    @Test
    void blocksClinicalObjectiveWithoutConsumingAttempt() {
        service.session("paid-token");
        service.saveInput(
                "paid-token",
                new MiraCommercialService.InputRequest(
                        "Tratar uma doença de pele",
                        List.of(new MiraCommercialService.ProductInput(
                                "Sabonete", "Limpar e enxaguar"))));

        var result = service.generate("paid-token");

        assertThat(result.status()).isEqualTo("BLOCKED");
        assertThat(result.attemptsUsed()).isZero();
        assertThat(result.blocker()).contains("conclusão clínica");
    }

    /** Deve preservar o último resultado depois das duas organizações incluídas. */
    @Test
    void enforcesTwoAttemptLimit() {
        service.session("paid-token");
        for (int attempt = 1; attempt <= 2; attempt++) {
            service.saveInput(
                    "paid-token",
                    new MiraCommercialService.InputRequest(
                            "Organizar tentativa " + attempt,
                            List.of(new MiraCommercialService.ProductInput(
                                    "Produto " + attempt, "Limpar e enxaguar"))));
            service.generate("paid-token");
        }

        assertThatThrownBy(() -> service.saveInput(
                        "paid-token",
                        new MiraCommercialService.InputRequest(
                                "Terceira tentativa",
                                List.of(new MiraCommercialService.ProductInput(
                                        "Produto 3", "Limpar e enxaguar")))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("duas organizações");
    }

    /** Deve recuperar a rotina pronta depois de reconstruir o serviço com o mesmo armazenamento. */
    @Test
    void restoresReadyRoutineAfterServiceRestart() {
        service.session("paid-token");
        service.saveInput(
                "paid-token",
                new MiraCommercialService.InputRequest(
                        "Organizar os cuidados que já possuo",
                        List.of(new MiraCommercialService.ProductInput(
                                "Sabonete", "Limpar e enxaguar"))));
        service.generate("paid-token");

        MiraCommercialService restarted = new MiraCommercialService(
                accessService,
                new ObjectMapper(),
                temporaryDirectory.resolve("mira.json").toString());

        assertThat(restarted.session("paid-token"))
                .extracting(
                        MiraCommercialService.SessionResponse::status,
                        MiraCommercialService.SessionResponse::attemptsUsed)
                .containsExactly("READY", 1);
        assertThat(restarted.session("paid-token").routine())
                .extracting(MiraCommercialService.RoutineCard::productName)
                .containsExactly("Sabonete");
    }

    /** Deve incluir a sessão no acesso e removê-la junto da exclusão genérica. */
    @Test
    void mergesAndDeletesCommercialPrivacyData() {
        service.session("paid-token");
        when(accessService.executePrivacyAction(any(), any()))
                .thenReturn(new PrivacyActionResponse(
                        "ACCESS", "COMPLETED", "2026-09-26T11:00:00Z", Map.of("email", "masked")));

        var access = service.privacy("paid-token", new PrivacyActionRequest("ACCESS", null));

        assertThat(access.data()).containsKeys("email", "miraCommercial");
    }
}
