package com.marketinghub.nichocnae.nicheresearchseedbuilder;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar apenas a integridade mínima da etapa dois antes da persistência no backend. */
class NicheResearchSeedBuilderValidatorTest {
    private final NicheResearchSeedBuilderValidator validator = new NicheResearchSeedBuilderValidator();

    /** Deve aceitar seed completo com queries retornadas pelo modelo sem julgar conteúdo semântico. */
    @Test
    void shouldAcceptModelQueriesWithoutSemanticBlocking() {
        NicheResearchSeedBuilderPending pending = pending();
        NicheResearchSeedBuilderOutput output = outputWithQueries(modelQueryTexts());

        assertThatNoException().isThrownBy(() -> validator.validate(pending, output));
    }

    /** Deve aceitar query genérica porque a etapa passou a confiar no modelo e nas próximas fases do pipeline. */
    @Test
    void shouldAcceptGenericQueryFromModel() {
        NicheResearchSeedBuilderPending pending = pending();
        List<String> texts = new ArrayList<>(modelQueryTexts());
        texts.set(0, "como vender mais");
        NicheResearchSeedBuilderOutput output = outputWithQueries(texts);

        assertThatNoException().isThrownBy(() -> validator.validate(pending, output));
    }

    /** Deve aceitar linguagem de solução na query porque o bloqueio semântico foi removido desta etapa. */
    @Test
    void shouldAcceptSolutionLanguageQueryFromModel() {
        NicheResearchSeedBuilderPending pending = pending();
        List<String> texts = new ArrayList<>(modelQueryTexts());
        texts.set(0, "IA para crescimento de manicure MEI no Brasil");
        NicheResearchSeedBuilderOutput output = outputWithQueries(texts);

        assertThatNoException().isThrownBy(() -> validator.validate(pending, output));
    }

    /** Deve aceitar query sem marcador literal de MEI/autônomo porque a etapa não bloqueia mais semântica textual. */
    @Test
    void shouldAcceptQueryWithoutAudienceMarker() {
        NicheResearchSeedBuilderPending pending = pending();
        List<String> texts = new ArrayList<>(modelQueryTexts());
        texts.set(0, "manicure rotina de atendimento no Brasil");
        NicheResearchSeedBuilderOutput output = outputWithQueries(texts);

        assertThatNoException().isThrownBy(() -> validator.validate(pending, output));
    }

    /** Deve aceitar qualquer quantidade positiva de queries porque a cobertura passa a ser responsabilidade do modelo. */
    @Test
    void shouldAcceptPositiveQueryCountFromModel() {
        NicheResearchSeedBuilderPending pending = pending();
        NicheResearchSeedBuilderOutput output = outputWithQueries(List.of("manicure rotina de atendimento"));

        assertThatNoException().isThrownBy(() -> validator.validate(pending, output));
    }

    /** Deve rejeitar somente ausência total de queries para evitar payload impossível de persistir operacionalmente. */
    @Test
    void shouldRejectEmptyQueryList() {
        NicheResearchSeedBuilderPending pending = pending();
        NicheResearchSeedBuilderOutput output = outputWithQueries(List.of());

        assertThatThrownBy(() -> validator.validate(pending, output))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pelo menos uma query");
    }

    /** Cria queries de exemplo com linguagem natural do modelo. */
    private List<String> modelQueryTexts() {
        return List.of(
                "manicure MEI rotina de atendimento no Brasil",
                "profissionais autônomos cabeleireiros enfrentam dificuldades no atendimento",
                "cidades brasileiras comportamento de aquisição de clientes para salão pequeno",
                "unha em gel quanto tempo dura na rotina de atendimento",
                "tarefas semanais de manicure com agenda cheia",
                "horários vazios na agenda de salão de beleza",
                "comunicação com clientes pelo whatsapp em salão pequeno",
                "hidratação cabelo cliente pergunta frequente",
                "rotina de cabeleireiro em salão pequeno",
                "biossegurança no atendimento de pedicure",
                "preço de unha decorada e cobrança recorrente",
                "recorrência de clientes em salão de bairro");
    }

    /** Cria uma pendência padrão equivalente ao ciclo RUNNING retornado pelo backend. */
    private NicheResearchSeedBuilderPending pending() {
        return new NicheResearchSeedBuilderPending(
                1001L,
                55L,
                "9602501",
                "Cabeleireiros, manicure e pedicure",
                "Cabeleireiros, manicure e pedicure",
                BigDecimal.valueOf(90),
                "AUTO_SCORE_QUEUE",
                "RUNNING",
                Instant.parse("2026-06-06T10:00:00Z"),
                Instant.parse("2026-06-06T10:00:00Z"));
    }

    /** Monta a saída da etapa dois com as queries informadas. */
    private NicheResearchSeedBuilderOutput outputWithQueries(List<String> queryTexts) {
        NicheResearchSeed seed = new NicheResearchSeed(
                1001L,
                "9602501",
                "Cabeleireiros, manicure e pedicure",
                "Cabeleireiros, manicure e pedicure",
                "serviço local de beleza",
                "agenda e atendimento recorrente",
                "consumidor final recorrente",
                "manicure, pedicure, escova",
                "depende de agenda cheia",
                "INFERRED_FROM_CNAE",
                "AI");
        List<ResearchQuery> queries = new ArrayList<>();
        int priority = 1;
        for (String text : queryTexts) {
            queries.add(new ResearchQuery(
                    1001L,
                    text,
                    "MEI_ROUTINE_DISCOVERY",
                    "web",
                    priority++,
                    "PENDING",
                    "AI"));
        }
        return new NicheResearchSeedBuilderOutput(1001L, seed, queries);
    }
}
