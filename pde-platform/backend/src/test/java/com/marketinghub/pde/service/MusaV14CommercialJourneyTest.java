package com.marketinghub.pde.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.pde.dto.AiGuidanceCreateRequest;
import com.marketinghub.pde.dto.PublicPresenceDiagnosticRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Comprova entrada, continuidade, auditoria e isolamento da candidata comercial v14. */
class MusaV14CommercialJourneyTest {
    private static final String SLUG = "metodo-musa-7-dias";
    private static final String V14 = MusaV14EntryContract.VERSION;
    @TempDir Path directory;

    /** Mantém a raiz v12 e resolve a v14 somente quando solicitada de forma explícita. */
    @Test
    void preservesRootAndRedactsPaidContentsFromPublicContract() {
        var catalog = new ProductCatalogService();
        assertThat(catalog.getProductForHost(SLUG, "v8.clubemusa.com.br").experienceVersion())
                .isEqualTo("musa-pde-entry-v12-primeiro-ajuste-aplicavel");
        var candidate = catalog.getProductForRequest(SLUG, "v8.clubemusa.com.br", "", V14);
        assertThat(candidate.experienceVersion()).isEqualTo(V14);
        assertThat(catalog.getProductForHost(SLUG, "v9.clubemusa.com.br").experienceVersion()).isEqualTo(V14);
        assertThat(candidate.commercialBinding().experimentId()).isEqualTo(106);
        assertThat(candidate.commercialAccess().accessDays()).isEqualTo(90);
        assertThat(candidate.missions()).hasSize(7);
        assertThat(candidate.supportMaterials()).hasSize(3);
        assertThat(candidate.heroVideos()).isEmpty();
        var publicContract = catalog.getPublicProductForRequest(SLUG, "v8.clubemusa.com.br", "", V14);
        assertThat(publicContract.missions()).isEmpty();
        assertThat(publicContract.supportMaterials()).isEmpty();
    }

    /** Entrega a ação antes de cadastro, audita a versão e recupera o mesmo resultado sem fila. */
    @Test
    void deliversAndRecoversRealContextWithoutProvider() throws Exception {
        var catalog = new ProductCatalogService();
        var access = access(catalog);
        var guidance = guidance(catalog, access);
        var result = guidance.createPublicPresenceDiagnostic(new PublicPresenceDiagnosticRequest(
                Map.of("occasion", "reunião pela manhã", "existingSelection", "camisa azul e calça preta"), V14));
        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.microActions()).anyMatch(value -> value.contains("camisa azul e calça preta"))
                .anyMatch(value -> value.contains("reunião pela manhã"))
                .anyMatch(value -> value.contains("caimento"));
        assertThat(result.costUsd()).isZero();
        assertThat(result.inputTokens()).isZero();
        assertThat(result.outputTokens()).isZero();
        assertThat(guidance.getPendingGuidance()).isEmpty();
        assertThat(guidance(catalog, access).getPublicPresenceDiagnostic(result.requestId())).isEqualTo(result);
        assertThat(Files.readString(directory.resolve("guidance.json"))).contains(V14);
    }

    /** Rejeita entradas ausentes, excedentes e grandes antes de gerar resultado ou consumo. */
    @Test
    void refusesInvalidInputsBeforeCreatingRequests() {
        var catalog = new ProductCatalogService();
        var guidance = guidance(catalog, access(catalog));
        for (var answers : java.util.List.of(
                Map.of("occasion", "trabalho"),
                Map.of("occasion", " ", "existingSelection", "camisa"),
                Map.of("occasion", "x".repeat(161), "existingSelection", "camisa"),
                Map.of("occasion", "trabalho", "existingSelection", "x".repeat(301)),
                Map.of("occasion", "trabalho", "existingSelection", "camisa", "optionalNote", "x".repeat(301)),
                Map.of("occasion", "trabalho", "existingSelection", "camisa", "provider", "externo"))) {
            assertThatThrownBy(() -> guidance.createPublicPresenceDiagnostic(new PublicPresenceDiagnosticRequest(answers, V14)))
                    .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        }
        assertThat(guidance.getPendingGuidance()).isEmpty();
        assertThat(directory.resolve("guidance.json")).doesNotExist();
    }

    /** Percorre os sete dias, preserva progresso e suporte e mantém o prazo pago de 90 dias. */
    @Test
    void completesAndRestoresPaidJourneyWithoutMigratingPredecessor() {
        var catalog = new ProductCatalogService();
        var access = access(catalog);
        var grant = access.createInternalQaAccess(SLUG, "teste+vega-3709@sandbox.local", V14);
        assertThat(grant.accessUrl()).startsWith("/experiences/" + V14 + "/access#access=");
        var workspace = access.getWorkspace(grant.token());
        assertThat(Duration.between(Instant.now(), Instant.parse(workspace.accessExpiresAt())).toDays()).isBetween(89L, 90L);
        var guidance = guidance(catalog, access);
        for (var mission : workspace.product().missions()) {
            Map<String, String> answers = new LinkedHashMap<>();
            mission.interaction().fields().forEach(field -> answers.put(field.key(), field.options().getFirst()));
            var result = guidance.createGuidanceRequest(grant.token(), mission.id(),
                    new AiGuidanceCreateRequest(mission.interaction().guidanceType(), answers, V14));
            assertThat(result.status()).isEqualTo("COMPLETED");
            assertThat(result.costUsd()).isZero();
            access.completeMission(grant.token(), mission.id());
        }
        access.requestSupport(grant.token(), "Quero recuperar meu progresso.");
        var restored = access(catalog).getWorkspace(grant.token());
        assertThat(restored.completedMissions()).isEqualTo(7);
        assertThat(restored.product().supportMaterials()).hasSize(3);
        assertThat(restored.supportStatus()).isEqualTo("OPEN");
        assertThat(restored.experienceVersion()).isEqualTo(V14);
        var previous = access.createInternalQaAccess(SLUG, "teste+previous@sandbox.local", "musa-pde-entry-v12-primeiro-ajuste-aplicavel");
        access.createAccess(SLUG, previous.email(), "LOGIN", V14);
        assertThat(access.getWorkspace(previous.token()).experienceVersion()).isEqualTo("musa-pde-entry-v12-primeiro-ajuste-aplicavel");
        assertThatThrownBy(() -> access.getWorkspace("token-de-outra-sessao-inexistente"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** Envia a recuperação da v14 para o domínio produtivo reservado, sem usar a raiz v12. */
    @Test
    void sendsRecoveryToV14SurfaceWithoutSpringDefaults() {
        var access = access(new ProductCatalogService());
        var mail = org.mockito.Mockito.mock(PdeMailService.class);
        org.mockito.Mockito.when(mail.isConfigured()).thenReturn(true);
        org.springframework.test.util.ReflectionTestUtils.setField(access, "mailService", mail);
        var result = access.requestMagicLink(SLUG, "teste+vega-mail@sandbox.local", V14);
        assertThat(result.deliveryStatus()).isEqualTo("SENT");
        org.mockito.Mockito.verify(mail).sendMagicLink(
                org.mockito.ArgumentMatchers.eq("teste+vega-mail@sandbox.local"),
                org.mockito.ArgumentMatchers.startsWith("https://v9.clubemusa.com.br/experiences/" + V14 + "/access#access="));
    }

    /** Cria armazenamento exclusivo do teste sem mailer ou conexão externa. */
    private AccessService access(ProductCatalogService catalog) {
        return new AccessService(catalog, new ObjectMapper(), directory.resolve("access.json").toString());
    }

    /** Cria o executor local com a mesma persistência para comprovar retomada. */
    private AiGuidanceService guidance(ProductCatalogService catalog, AccessService access) {
        return new AiGuidanceService(access, catalog, new ObjectMapper(), directory.resolve("guidance.json").toString(),
                "", "", "", new PdeDatabaseMigrationService("", "", ""));
    }
}
