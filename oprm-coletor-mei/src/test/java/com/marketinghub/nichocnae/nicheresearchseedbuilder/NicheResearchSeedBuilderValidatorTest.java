package com.marketinghub.nichocnae.nicheresearchseedbuilder;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar as regras determinísticas da etapa dois antes da persistência no backend. */
class NicheResearchSeedBuilderValidatorTest {
    private final NicheResearchSeedBuilderValidator validator = new NicheResearchSeedBuilderValidator();

    /** Deve aceitar seed completo com doze queries específicas vinculadas a objetos comerciais do nicho. */
    @Test
    void shouldAcceptSpecificPendingQueries() {
        NicheResearchSeedBuilderPending pending = pending();
        NicheResearchSeedBuilderOutput output = outputWithQueries(List.of(
                "manicure rotina de atendimento",
                "manicure responsabilidades agenda",
                "cliente unha em gel dúvidas",
                "unha em gel quanto tempo dura",
                "pacote manicure mensal como funciona",
                "agenda manicure horários vazios",
                "salão beleza divulgar whatsapp",
                "hidratação cabelo cliente pergunta",
                "cabeleireiro rotina salão pequeno",
                "pedicure biossegurança atendimento",
                "manicure preço unha decorada",
                "salão beleza pacote fidelidade"));

        assertThatNoException().isThrownBy(() -> validator.validate(pending, output));
    }

    /** Deve rejeitar query genérica porque ela não ajuda a pesquisar a rotina concreta do nicho. */
    @Test
    void shouldRejectGenericQuery() {
        NicheResearchSeedBuilderPending pending = pending();
        List<String> texts = new ArrayList<>(List.of(
                "manicure rotina de atendimento",
                "manicure responsabilidades agenda",
                "cliente unha em gel dúvidas",
                "unha em gel quanto tempo dura",
                "pacote manicure mensal como funciona",
                "agenda manicure horários vazios",
                "salão beleza divulgar whatsapp",
                "hidratação cabelo cliente pergunta",
                "cabeleireiro rotina salão pequeno",
                "pedicure biossegurança atendimento",
                "manicure preço unha decorada",
                "salão beleza pacote fidelidade"));
        texts.set(0, "como vender mais");
        NicheResearchSeedBuilderOutput output = outputWithQueries(texts);

        assertThatThrownBy(() -> validator.validate(pending, output))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Query genérica proibida");
    }

    /** Deve rejeitar saída com menos de doze queries para manter cobertura mínima do MVP. */
    @Test
    void shouldRejectTooFewQueries() {
        NicheResearchSeedBuilderPending pending = pending();
        NicheResearchSeedBuilderOutput output = outputWithQueries(List.of("manicure rotina de atendimento"));

        assertThatThrownBy(() -> validator.validate(pending, output))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entre 12 e 15 queries");
    }

    /** Cria uma pendência padrão equivalente ao ciclo RUNNING retornado pelo backend. */
    private NicheResearchSeedBuilderPending pending() {
        return new NicheResearchSeedBuilderPending(
                1001L,
                55L,
                "9602501",
                "Cabeleireiros, manicure e pedicure",
                "Cabeleireiros, manicure e pedicure",
                BigDecimal.valueOf(92),
                "AUTO_SCORE_QUEUE",
                "RUNNING",
                Instant.now(),
                Instant.now());
    }

    /** Cria uma saída com seed de beleza e queries com objetivos alternados para validação. */
    private NicheResearchSeedBuilderOutput outputWithQueries(List<String> queryTexts) {
        NicheResearchSeed seed = new NicheResearchSeed(
                1001L,
                "9602501",
                "Cabeleireiros, manicure e pedicure",
                "Cabeleireiros, manicures e pedicures",
                "serviço local de beleza",
                "atendimento com agenda e recorrência por WhatsApp",
                "consumidor final recorrente",
                "manicure, unha em gel, salão beleza, hidratação cabelo, pedicure, cabeleireiro, pacote fidelidade, agenda",
                "O nicho depende de agenda cheia, recorrência e indicação.",
                "INFERRED_FROM_CNAE",
                "AI");
        List<ResearchQuery> queries = new ArrayList<>();
        for (int index = 0; index < queryTexts.size(); index++) {
            queries.add(new ResearchQuery(
                    1001L,
                    queryTexts.get(index),
                    index % 2 == 0 ? "ROUTINE_DISCOVERY" : "SALES_PAIN_DISCOVERY",
                    "GENERAL_WEB",
                    index + 1,
                    "PENDING",
                    "AI"));
        }
        return new NicheResearchSeedBuilderOutput(1001L, seed, queries);
    }
}
