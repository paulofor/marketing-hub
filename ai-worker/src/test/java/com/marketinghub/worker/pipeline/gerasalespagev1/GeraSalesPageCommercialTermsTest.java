package com.marketinghub.worker.pipeline.gerasalespagev1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.model.OpenAiDispatch;
import com.marketinghub.worker.openai.core.model.OpenAiResult;
import com.marketinghub.worker.openai.core.port.OpenAiClientPort;
import com.marketinghub.worker.pipeline.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Comprova preservação de termos cadastrados, segurança de texto e auditoria ponta a ponta do processor. */
class GeraSalesPageCommercialTermsTest {
    private final ObjectMapper json = new ObjectMapper();

    /** Termos ausentes não criam promessas e HTML legado permanece intacto. */
    @Test
    void keepsPagesWithoutExplicitTermsUnchanged() {
        String html = "<HTML><BODY><h1>Oferta</h1></BODY></HTML>";
        assertThat(GeraSalesPageCommercialTerms.render(html, Map.of(), json)).isEqualTo(html);
    }

    /** Reaplicação substitui fonte alterada e remoção explícita não deixa termos antigos. */
    @Test
    void replacesOwnSectionWithoutDuplicatingOrKeepingStaleTerms() {
        String original = "<html><body><main><h1>Oferta</h1></main></body></html>";
        String first = GeraSalesPageCommercialTerms.render(original, source("Formulário após pagar"), json);
        assertThat(first).contains("Formulário após pagar", "Política do vendedor", "Recebedor cadastrado");
        assertThat(first).contains("Primeira resposta em até 2 dias úteis", "Suporte por 14 dias corridos",
                "Entrega em até 4 dias úteis", "Reembolso integral: Até oito dias após receber",
                "Sem dados de cartão", "Prazo bancário informado pelo provedor");
        assertThat(GeraSalesPageCommercialTerms.render(first, source("Formulário após pagar"), json)).isEqualTo(first);
        String changed = GeraSalesPageCommercialTerms.render(first, source("Formulário atualizado"), json);
        assertThat(changed).containsOnlyOnce("data-mh-commercial-terms").contains("Formulário atualizado")
                .doesNotContain("Formulário após pagar");
        assertThat(GeraSalesPageCommercialTerms.render(changed, Map.of(), json)).isEqualTo(original);
    }

    /** Valores do contrato são texto visível e não executam scripts ou atributos HTML. */
    @Test
    void escapesUntrustedContractText() {
        String html = GeraSalesPageCommercialTerms.render("<body></body>",
                source("<img src=x onerror='alert(1)'> & \"dados\""), json);
        assertThat(html).contains("&lt;img", "&amp;", "&quot;dados&quot;")
                .doesNotContain("<img src=x");
    }

    /** Uma omissão do modelo é corrigida tanto no singular quanto com condições diferentes de suporte. */
    @ParameterizedTest
    @ValueSource(ints = {1, 3})
    void preservesSupportDeadlineAndReplacesThePreviousOne(int days) {
        var context = json.valueToTree(source("Formulário oficial"));
        ((com.fasterxml.jackson.databind.node.ObjectNode) context.path("experiment").path("product")
                .path("experienceContract").path("support")).put("firstResponseBusinessDays", days);
        String old = GeraSalesPageCommercialTerms.render("<body><main>Oferta sem prazo do suporte</main></body>",
                source("Formulário oficial"), json);
        String changed = GeraSalesPageCommercialTerms.render(old, json.convertValue(context, Map.class), json);
        assertThat(changed).contains("Primeira resposta em até " + days + (days == 1 ? " dia útil." : " dias úteis."))
                .doesNotContain("Primeira resposta em até 2 dias úteis").containsOnlyOnce("data-mh-commercial-terms");
    }

    /** Fonte incompleta ou malformada não inventa dias, marco inicial ou integralidade do reembolso. */
    @Test
    void doesNotInferUnknownCommercialCommitments() {
        Map<String, Object> input = Map.of("experiment", Map.of("product", Map.of("experienceContract", Map.of(
                "delivery", Map.of("businessDays", 5, "startsAfter", "UNKNOWN"),
                "support", Map.of("firstResponseBusinessDays", "1", "durationCalendarDaysAfterDelivery", -2),
                "refund", Map.of("fullRefund", false, "requestWindow", "Conforme política cadastrada")))));
        String result = GeraSalesPageCommercialTerms.render("<body>Oferta</body>", input, json);
        assertThat(result).contains("Solicitação de reembolso: Conforme política cadastrada")
                .doesNotContain("Entrega em até", "Primeira resposta", "Suporte por", "Reembolso integral");
    }

    /** Duas etapas preservam condições mesmo se o modelo omitir, mantendo bruto e custo originais. */
    @ParameterizedTest
    @ValueSource(strings = {"sales-page-html", "sales-page-publication-package"})
    void preservesTermsThroughModelArtifactsAndBackendResult(String stage) throws Exception {
        var ai = mock(OpenAiClientPort.class);
        var backend = mock(GeraSalesPageBackendClient.class);
        var processor = new GeraSalesPageProcessor(json, ai, new GeraSalesPageResponseValidator(json), backend, "flex");
        String raw = "{\"html\":\"<html><body><main>Oferta sem termos</main></body></html>\"}";
        var dispatch = new OpenAiDispatch("simulated", "prompt", "{}", "{}", "prompt", Instant.now());
        when(ai.dispatch(any())).thenReturn(dispatch);
        when(ai.awaitResult(dispatch)).thenReturn(new OpenAiResult<>("simulated", "raw-provider-response", raw,
                raw, 17, 23, new BigDecimal("0.01")));
        var input = new GeraSalesPageInput(246L, stage, "local-stage", "model-test", "schema-test", "prompt", "{}",
                source("Abra o formulário no retorno do pagamento"));
        var execution = new StageExecution<>("local-stage", 246L, stage, "PENDING", Instant.now(), input, Map.of());
        ArtifactStore store = (type, name, contentType, content, metadata) ->
                new StageArtifact(type, name, contentType, "local/" + name, "test-hash", metadata);
        var result = processor.process(new StageContext<>(execution, input, store, Map.of()));
        assertThat(result.output().payload().get("html").toString()).contains("Abra o formulário", "Política do vendedor");
        assertThat(result.output().payload().get("html").toString()).contains("Primeira resposta em até 2 dias úteis",
                "Suporte por 14 dias corridos", "Entrega em até 4 dias úteis", "Reembolso integral");
        var audited = (OpenAiResult<?>) result.metrics().get("openAiResult");
        assertThat(audited.rawResponse()).isEqualTo("raw-provider-response");
        assertThat(audited.modelResponse()).contains("data-mh-commercial-terms");
        assertThat(audited.costUsd()).isEqualByComparingTo("0.01");
        verify(backend).saveOpenAiRequest(eq(execution), any());
        verify(ai, times(1)).dispatch(any());
        assertThat(result.artifacts()).hasSize(2);
    }

    /** Permite reproduzir o HTML que falhou localmente sem publicar dados de um produto nos testes. */
    @Test
    @EnabledIfSystemProperty(named = "commercialTermsReplayDirectory", matches = ".+")
    void rendersOptionalLocalReplayForBrowserValidation() throws Exception {
        String directory = System.getProperty("commercialTermsReplayDirectory");
        Path root = Path.of(directory);
        Map<String, Object> context = json.readValue(Files.readString(root.resolve("context.json")), Map.class);
        Files.writeString(root.resolve("after.html"), GeraSalesPageCommercialTerms.render(
                Files.readString(root.resolve("before.html")), context, json));
    }

    /** Cria contrato independente de produto, sem valores presumidos pelo renderizador. */
    private Map<String, Object> source(String briefing) {
        return Map.of("experiment", Map.of("id", 246, "product", Map.of("id", 23,
                "experienceContract", Map.of("delivery", Map.of("briefingChannel", briefing,
                                "businessDays", 4, "startsAfter", "PAYMENT_APPROVED_AND_COMPLETE_BRIEFING",
                                "businessDaysDefinition", "Dias úteis conforme calendário cadastrado",
                                "channel", "Link enviado ao e-mail do briefing", "access", "Arquivo para baixar"),
                        "support", Map.of("email", "teste+suporte@sandbox.local", "firstResponseBusinessDays", 2,
                                "durationCalendarDaysAfterDelivery", 14, "scope", "Ajuda para acessar os arquivos",
                                "exclusions", "Sem serviços adicionais"),
                        "checkoutIdentity", Map.of("explanation", "Recebedor cadastrado"),
                        "refund", Map.of("providerProtectionDistinction", "Política do vendedor, distinta do provedor",
                                "fullRefund", true, "requestWindow", "Até oito dias após receber", "reasonRequired", false,
                                "instructions", "Sem dados de cartão", "processing", "Prazo bancário informado pelo provedor")))));
    }
}
