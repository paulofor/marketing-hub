package com.marketinghub.pde.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.pde.dto.AiGuidanceCreateRequest;
import com.marketinghub.pde.dto.MissionInteractionRequest;
import com.marketinghub.pde.dto.PublicPresenceDiagnosticRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Comprova preferência aplicada, memória, autorização e isolamento da jornada de estilo. */
class MusaPersonalStyleJourneyTest {
    private static final String SLUG = "metodo-musa-7-dias";
    private static final String VERSION = MusaPersonalStyleContract.VERSION;
    @TempDir Path directory;

    /** As mesmas roupas recebem ações diferentes conforme o gosto declarado, sem inferir personalidade. */
    @Test
    void differentiatesPreferencesWithSameClothesAndPreservesNeutralChoice() {
        var catalog = new ProductCatalogService();
        var service = guidance(catalog, access(catalog));
        var comfort = service.createPublicPresenceDiagnostic(new PublicPresenceDiagnosticRequest(answers("Conforto e praticidade"), VERSION));
        var color = service.createPublicPresenceDiagnostic(new PublicPresenceDiagnosticRequest(answers("Cor e criatividade"), VERSION));
        assertThat(comfort.microActions()).isNotEqualTo(color.microActions());
        assertThat(comfort.microActions().getFirst()).contains("caminhe", "camiseta azul e jeans").doesNotContain("elegância");
        assertThat(color.microActions().getFirst()).contains("cor, estampa ou textura", "camiseta azul e jeans");
        var neutral = service.createPublicPresenceDiagnostic(new PublicPresenceDiagnosticRequest(answers("Quero manter como está"), VERSION));
        assertThat(neutral.microActions().getFirst()).contains("Não há correção obrigatória");
        assertThat(neutral.model()).isEqualTo(MusaPersonalStyleContract.MODEL);
        assertThat(neutral.costUsd()).isZero();
        assertThat(service.getPendingGuidance()).isEmpty();
    }

    /** Rejeita preferência ausente ou inventada e campos fora do contrato antes de criar uma resposta. */
    @Test
    void rejectsInvalidInputsAndKeepsLegacyWorking() {
        var catalog = new ProductCatalogService();
        var service = guidance(catalog, access(catalog));
        for (var changed : java.util.List.of(Map.of("stylePreference", "formal obrigatório"), Map.of("occasion", "x".repeat(161)), Map.of("extra", "injetado"))) {
            var input = new LinkedHashMap<>(answers("Discrição")); input.putAll(changed);
            assertThatThrownBy(() -> service.createPublicPresenceDiagnostic(new PublicPresenceDiagnosticRequest(input, VERSION)))
                    .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        }
        assertThatThrownBy(() -> service.createPublicPresenceDiagnostic(new PublicPresenceDiagnosticRequest(Map.of("occasion", "trabalho", "existingSelection", "camisa"), VERSION)))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        var legacy = service.createPublicPresenceDiagnostic(new PublicPresenceDiagnosticRequest(Map.of("occasion", "trabalho", "existingSelection", "camisa"), MusaV14EntryContract.VERSION));
        assertThat(legacy.status()).isEqualTo("COMPLETED");
        assertThat(legacy.model()).isEqualTo("MUSA_LOCAL_RULES_V1");
        assertThat(catalog.getProduct("pausa-de-transicao").slug()).isEqualTo("pausa-de-transicao");
    }

    /** Recupera o resultado persistido e seu contexto auditado sem repetir geração. */
    @Test
    void restoresResultAfterRestartWithVersionAndPreference() throws Exception {
        var catalog = new ProductCatalogService(); var access = access(catalog);
        var result = guidance(catalog, access).createPublicPresenceDiagnostic(new PublicPresenceDiagnosticRequest(answers("Ainda estou descobrindo"), VERSION));
        assertThat(guidance(catalog, access).getPublicPresenceDiagnostic(result.requestId())).isEqualTo(result);
        assertThat(Files.readString(directory.resolve("guidance.json"))).contains(VERSION, "Ainda estou descobrindo");
    }

    /** Completa os sete dias com registros próprios e conserva a ordem e a origem das escolhas. */
    @Test
    void completesSevenDaysAndReusesPersistedHistory() throws Exception {
        var catalog = new ProductCatalogService(); var access = access(catalog); var service = guidance(catalog, access);
        var grant = access.createInternalQaAccess(SLUG, "teste+estilo@sandbox.local", VERSION);
        for (var mission : access.getWorkspace(grant.token()).product().missions()) {
            var input = new LinkedHashMap<>(answers(mission.day() % 2 == 0 ? "Discrição" : "Cor e criatividade"));
            input.put("reflection", "Registro real de teste do dia " + mission.day());
            var result = service.createGuidanceRequest(grant.token(), mission.id(), new AiGuidanceCreateRequest(mission.interaction().guidanceType(), input, VERSION));
            if (mission.day() == 7) assertThat(result.signals()).anyMatch(value -> value.contains("Registro real de teste do dia 1"))
                    .anyMatch(value -> value.contains("Registro real de teste do dia 6"));
            access.completeMission(grant.token(), mission.id());
        }
        assertThat(access(catalog).getWorkspace(grant.token()).progressPercent()).isEqualTo(100);
        assertThat(Files.readString(directory.resolve("guidance.json"))).contains("estilo-dia-1.reflection", "estilo-dia-6.reflection");
    }

    /** Uma nova candidata não migra o acesso pago antigo nem recebe sua autorização comercial. */
    @Test
    void isolatesVersionsAndPaidEntitlementsForSameEmail() {
        var catalog = new ProductCatalogService(); var access = access(catalog);
        var old = access.createInternalQaAccess(SLUG, "teste+isolation@sandbox.local", MusaV14EntryContract.VERSION);
        var current = access.createAccess(SLUG, old.email(), "MAGIC_LINK", VERSION);
        assertThat(current.token()).isNotEqualTo(old.token());
        assertThat(access.requestExistingMagicLink(SLUG, old.email()).accessUrl()).contains(MusaV14EntryContract.VERSION);
        assertThat(access.getWorkspace(old.token()).experienceVersion()).isEqualTo(MusaV14EntryContract.VERSION);
        assertThat(access.getWorkspace(current.token()).subscriptionStatus()).isEqualTo("TRIAL");
        assertThatThrownBy(() -> guidance(catalog, access).createGuidanceRequest(old.token(), "dia-1-ruido-visual", new AiGuidanceCreateRequest("MUSA_V7_DAY_1_VISUAL_MESSAGE", answers("Elegância"), VERSION)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("versão");
        assertThatThrownBy(() -> access.saveMissionInteraction(current.token(), "estilo-dia-2", new MissionInteractionRequest(answers("Discrição"))))
                .isInstanceOf(IllegalArgumentException.class);
        var product = catalog.getProductForHost(SLUG, "v10.clubemusa.com.br");
        assertThat(product.commercialBinding()).isNull(); assertThat(product.commercialCheckout()).isNull();
        assertThat(product.commercialAccess().accessDays()).isEqualTo(90);
        var publicProduct = catalog.getPublicProductForRequest(SLUG, "v10.clubemusa.com.br", "", VERSION);
        assertThat(publicProduct.missions()).isEmpty(); assertThat(publicProduct.supportMaterials()).isEmpty();
    }

    /** A conclusão depende de um registro da cliente, inclusive quando ela mantém a roupa como está. */
    @Test
    void requiresReflectionBeforeCompletingAndRecoversCorrectVersion() {
        var catalog = new ProductCatalogService(); var access = access(catalog);
        var grant = access.createAccess(SLUG, "teste+reflection@sandbox.local", "MAGIC_LINK", VERSION);
        guidance(catalog, access).createGuidanceRequest(grant.token(), "estilo-dia-1", new AiGuidanceCreateRequest("MUSA_STYLE_DAY_1", answers("Quero manter como está"), VERSION));
        assertThatThrownBy(() -> access.completeMission(grant.token(), "estilo-dia-1")).isInstanceOf(IllegalArgumentException.class);
        var saved = new LinkedHashMap<>(answers("Quero manter como está")); saved.put("reflection", "Prefiro manter como está.");
        access.saveMissionInteraction(grant.token(), "estilo-dia-1", new MissionInteractionRequest(saved));
        access.completeMission(grant.token(), "estilo-dia-1");
        assertThat(access.getWorkspace(grant.token()).completedMissionIds()).containsExactly("estilo-dia-1");
        assertThat(access.requestExistingMagicLink(SLUG, grant.email(), VERSION).accessUrl()).contains("/experiences/" + VERSION);
    }

    /** Cria as mesmas roupas e ocasião para isolar o efeito da preferência. */
    private Map<String, String> answers(String preference) {
        return Map.of("occasion", "trabalho", "existingSelection", "camiseta azul e jeans", "stylePreference", preference);
    }

    /** Usa arquivos exclusivos do teste e nenhuma credencial real. */
    private AccessService access(ProductCatalogService catalog) {
        return new AccessService(catalog, new ObjectMapper(), directory.resolve("access.json").toString());
    }

    /** Reabre a mesma persistência para comprovar recuperação sem nova chamada. */
    private AiGuidanceService guidance(ProductCatalogService catalog, AccessService access) {
        return new AiGuidanceService(access, catalog, new ObjectMapper(), directory.resolve("guidance.json").toString(), "", "", "", new PdeDatabaseMigrationService("", "", ""));
    }
}
