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

    /** Deve aceitar seed completo com doze queries específicas vinculadas à rotina operacional do nicho. */
    @Test
    void shouldAcceptSpecificPendingQueries() {
        NicheResearchSeedBuilderPending pending = pending();
        NicheResearchSeedBuilderOutput output = outputWithQueries(validQueryTexts());

        assertThatNoException().isThrownBy(() -> validator.validate(pending, output));
    }

    /** Deve rejeitar query genérica porque ela não ajuda a pesquisar a rotina concreta do nicho. */
    @Test
    void shouldRejectGenericQuery() {
        NicheResearchSeedBuilderPending pending = pending();
        List<String> texts = new ArrayList<>(validQueryTexts());
        texts.set(0, "como vender mais");
        NicheResearchSeedBuilderOutput output = outputWithQueries(texts);

        assertThatThrownBy(() -> validator.validate(pending, output))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Query genérica proibida");
    }

    /** Deve rejeitar query contaminada por solução quando o termo não faz parte literal do CNAE. */
    @Test
    void shouldRejectSolutionLanguageQuery() {
        NicheResearchSeedBuilderPending pending = pending();
        List<String> texts = new ArrayList<>(validQueryTexts());
        texts.set(0, "IA para crescimento de manicure MEI no Brasil");
        NicheResearchSeedBuilderOutput output = outputWithQueries(texts);

        assertThatThrownBy(() -> validator.validate(pending, output))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("linguagem de solução proibida");
    }

    /** Deve rejeitar query sem marcador explícito de MEI/autônomo para não voltar a pesquisar apenas o CNAE. */
    @Test
    void shouldRejectQueryWithoutAudienceMarker() {
        NicheResearchSeedBuilderPending pending = pending();
        List<String> texts = new ArrayList<>(validQueryTexts());
        texts.set(0, "manicure rotina de atendimento no Brasil");
        NicheResearchSeedBuilderOutput output = outputWithQueries(texts);

        assertThatThrownBy(() -> validator.validate(pending, output))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("marcador de MEI/autônomo");
    }

    /** Deve rejeitar query sem marcador Brasil/pt-BR para manter a pesquisa aderente ao mercado brasileiro. */
    @Test
    void shouldRejectQueryWithoutBrazilMarker() {
        NicheResearchSeedBuilderPending pending = pending();
        List<String> texts = new ArrayList<>(validQueryTexts());
        texts.set(0, "manicure MEI rotina de atendimento agenda clientes");
        NicheResearchSeedBuilderOutput output = outputWithQueries(texts);

        assertThatThrownBy(() -> validator.validate(pending, output))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("marcador de Brasil/pt-BR");
    }

    /** Deve aceitar palavras repetidas nas queries porque repetição de conectivos não é duplicidade operacional. */
    @Test
    void shouldAcceptRepeatedCommonWordsInQueryText() {
        NicheResearchSeedBuilderPending pending = pending();
        List<String> texts = new ArrayList<>(validQueryTexts());
        NicheResearchSeedBuilderOutput output = outputWithQueries(texts);

        assertThatNoException().isThrownBy(() -> validator.validate(pending, output));
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

    /** Cria queries válidas com marcadores de MEI/autônomo e contexto brasileiro. */
    private List<String> validQueryTexts() {
        return List.of(
                "manicure MEI rotina de atendimento no Brasil",
                "profissional autônomo manicure responsabilidades agenda Brasil",
                "trabalhador por conta própria unha em gel dúvidas pt-BR",
                "MEI unha em gel quanto tempo dura Brasil",
                "profissional autônomo manicure tarefas atendimento semanal brasileiro",
                "MEI agenda manicure horários vazios Brasil",
                "dono-operador salão beleza comunicação clientes whatsapp Brasil",
                "profissional autônomo hidratação cabelo cliente pergunta brasileira",
                "MEI cabeleireiro rotina salão pequeno Brasil",
                "profissional autônomo pedicure biossegurança atendimento pt-BR",
                "MEI manicure preço unha decorada Brasil",
                "trabalhador por conta própria salão beleza recorrência clientes brasileiros");
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
                    index % 3 == 0
                            ? "MEI_ROUTINE_DISCOVERY"
                            : index % 3 == 1 ? "CUSTOMER_ACQUISITION_BEHAVIOR_DISCOVERY" : "DAILY_OPERATION_PAIN_DISCOVERY",
                    "GENERAL_WEB",
                    index + 1,
                    "PENDING",
                    "AI"));
        }
        return new NicheResearchSeedBuilderOutput(1001L, seed, queries);
    }
}
