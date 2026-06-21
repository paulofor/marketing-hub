package com.marketinghub.nichocnae.nicheresearchseedbuilder;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar que o prompt da etapa dois guia a IA para pesquisa de rotina sem viés de solução. */
class NicheResearchSeedBuilderPromptBuilderTest {
    private final NicheResearchSeedBuilderPromptBuilder promptBuilder = new NicheResearchSeedBuilderPromptBuilder();

    /** Deve declarar rotina real, exigir aquisição operacional e proibir solução comercial. */
    @Test
    void shouldBuildRoutineRealityPromptWithoutSolutionSearch() {
        String prompt = promptBuilder.buildPrompt(pending());

        assertThat(prompt)
                .contains("especialista em Marketing e Comportamento do Consumidor no Digital")
                .contains("profissional brasileiro MEI/autônomo")
                .contains("usar o CNAE amplo apenas como fonte inicial de descoberta")
                .contains("quebrar esse CNAE em 3 a 7 subnichos operacionais mais vendáveis")
                .contains("não crie nem materialize o nicho amplo")
                .contains("Pontue cada subnicho de 1 a 5 por recorrência")
                .contains("urgência da dor")
                .contains("capacidade de pagar")
                .contains("clareza do resultado")
                .contains("compatibilidade com produto digital")
                .contains("seed.nicheName deve ser o subnicho específico vencedor")
                .contains("nunca o CNAE amplo")
                .contains("público, contexto operacional e dor/resultado observável")
                .contains("pré-gate comercial antes de gerar queries profundas")
                .contains("possibilidade de evidência pública")
                .contains("validar demanda comercial antes da pesquisa profunda")
                .contains("pagamento/cobrança/preço")
                .contains("meiVolume: 125000")
                .contains("português do Brasil")
                .contains("operação comercial real")
                .contains("rotina executada")
                .contains("aquisição de clientes")
                .contains("faltas, remarcações e clientes que somem")
                .contains("precificação, cobrança, pacotes e recorrência")
                .contains("materiais, tempo de atendimento e retrabalho")
                .contains("relatos reais em fóruns, vídeos, comentários e perguntas frequentes")
                .contains("manicure clientes pelo WhatsApp indicação Instagram")
                .contains("manicure cliente falta remarca some")
                .contains("manicure preço pacote cobrança sinal recorrência")
                .contains("manicure material tempo atendimento retrabalho")
                .contains("relatos manicure autônoma comentários dúvidas frequentes")
                .contains("priority menor")
                .contains("CBO, tabelas salariais e páginas institucionais com prioridade menor")
                .contains("não force marcador literal")
                .contains("limites maxLength")
                .contains("queryGoal curto")
                .contains("Não inclua metadado técnico")
                .contains("Não proponha solução")
                .contains("Não procure produto")
                .contains("Não procure oferta")
                .contains("Não procure ferramenta")
                .contains("captação de clientes")
                .contains("canais usados")
                .contains("indicação")
                .contains("redes sociais")
                .contains("WhatsApp")
                .contains("Instagram")
                .contains("orçamento")
                .contains("agenda vazia")
                .contains("retorno")
                .contains("fidelização")
                .contains("cancelamento")
                .contains("reativação")
                .contains("recorrência")
                .contains("realidade operacional do profissional")
                .contains("comportamento operacional observado")
                .contains("Evite que a pesquisa dependa demais de CBO, tabelas salariais, páginas institucionais")
                .contains("não como recomendação de marketing")
                .contains("Não transforme essas queries em aconselhamento de marketing")
                .contains("criação de campanha")
                .contains("funil")
                .contains("anúncio")
                .doesNotContain("construtor da etapa 2")
                .doesNotContain("executor de pipeline")
                .doesNotContain("fontes do Brasil")
                .doesNotContain("domínios .br")
                .doesNotContain("PRODUCT_SERVICE_DISCOVERY")
                .doesNotContain("OFFER_PATTERN_DISCOVERY")
                .doesNotContain("SALES_PAIN_DISCOVERY");
    }

    /** Deve transformar a reprovação anterior em orientação obrigatória para o novo ciclo. */
    @Test
    void shouldIncludeAutomaticLearningFromPreviousQualityGate() {
        NicheResearchSeedBuilderPending pending = new NicheResearchSeedBuilderPending(
                1001L,
                55L,
                "9602501",
                "Cabeleireiros, manicure e pedicure",
                "Cabeleireiros, manicure e pedicure",
                BigDecimal.valueOf(92),
                125000L,
                "gpt-5.2",
                "gpt-5.2 (gpt-5.2)",
                "MANUAL_REPROCESS",
                "SOLUTION_CONTAMINATED",
                "REFAZER_BUSCA_SEM_SOLUCAO",
                "Reexecutar busca removendo fontes de solucao",
                "riscoLinguagemSolucao=70; dominadoPorSolucao=true",
                List.of(),
                "RUNNING",
                Instant.now(),
                Instant.now());

        String prompt = promptBuilder.buildPrompt(pending);

        assertThat(prompt)
                .contains("Aprendizado automático do ciclo anterior")
                .contains("previousQualityStatus: SOLUTION_CONTAMINATED")
                .contains("previousNextMoveCode: REFAZER_BUSCA_SEM_SOLUCAO")
                .contains("não repita a mesma causa de reprovação")
                .contains("exclua termos de solução")
                .contains("priorize relatos manuais");
    }

    /** Deve orientar busca complementar para tarefas reais quando o gate pedir evidência da execução. */
    @Test
    void shouldPrioritizeExecutorRoutineQueriesWhenPreviousNextMoveRequestsRealTasks() {
        NicheResearchSeedBuilderPending pending = new NicheResearchSeedBuilderPending(
                1001L,
                55L,
                "9602501",
                "Cabeleireiros, manicure e pedicure",
                "Manicure autônoma domiciliar",
                BigDecimal.valueOf(92),
                125000L,
                "gpt-5.2",
                "gpt-5.2 (gpt-5.2)",
                "AUTO_QUALITY_REPROCESS",
                "NEEDS_EXECUTOR_ROUTINE_EVIDENCE",
                "BUSCAR_TAREFAS_REAIS_EXECUTOR",
                "Pesquisar relatos e tarefas concretas do executor",
                "rotinaRevelaTarefasReaisExecutor=false; tarefasConcretasDistintas=0",
                List.of(),
                "RUNNING",
                Instant.now(),
                Instant.now());

        String prompt = promptBuilder.buildPrompt(pending);

        assertThat(prompt)
                .contains("previousNextMoveCode: BUSCAR_TAREFAS_REAIS_EXECUTOR")
                .contains("priorizar execução manual real")
                .contains("rotina do atendimento em domicílio")
                .contains("materiais e maleta")
                .contains("deslocamento")
                .contains("tempo de atendimento")
                .contains("esterilização")
                .contains("cutilagem")
                .contains("esmalte descascado")
                .contains("retrabalho")
                .contains("reduza temporariamente a dominância de agenda, WhatsApp, Instagram")
                .contains("rotina real manicure atendimento em domicílio passo a passo Brasil");
    }

    /** Deve proibir subnichos já materializados para o mesmo CNAE e orientar escolha de recorte novo. */
    @Test
    void shouldExcludeExistingSubnichesForSameCnaeFromNewChoice() {
        NicheResearchSeedBuilderPending pending = new NicheResearchSeedBuilderPending(
                1001L,
                55L,
                "9602501",
                "Cabeleireiros, manicure e pedicure",
                "Cabeleireiros, manicure e pedicure",
                BigDecimal.valueOf(92),
                125000L,
                "gpt-5.2",
                "gpt-5.2 (gpt-5.2)",
                "MANUAL_REPROCESS",
                null,
                null,
                null,
                null,
                List.of(
                        "Manicure autônoma que atende em domicílio",
                        "Nail designer iniciante com agenda pelo Instagram"),
                "RUNNING",
                Instant.now(),
                Instant.now());

        String prompt = promptBuilder.buildPrompt(pending);

        assertThat(prompt)
                .contains("Subnichos já materializados para este CNAE e proibidos nesta nova escolha")
                .contains("- Manicure autônoma que atende em domicílio")
                .contains("- Nail designer iniciante com agenda pelo Instagram")
                .contains("não escolha, reescreva ou aproxime semanticamente esses subnichos já existentes")
                .contains("ampliar o portfólio do CNAE sem canibalização");
    }


    /** Cria uma pendência padrão para montagem determinística do prompt da etapa dois. */
    private NicheResearchSeedBuilderPending pending() {
        return new NicheResearchSeedBuilderPending(
                1001L,
                55L,
                "9602501",
                "Cabeleireiros, manicure e pedicure",
                "Cabeleireiros, manicure e pedicure",
                BigDecimal.valueOf(92),
                125000L,
                "gpt-5.2",
                "gpt-5.2 (gpt-5.2)",
                "AUTO_SCORE_QUEUE",
                null,
                null,
                null,
                null,
                List.of(),
                "RUNNING",
                Instant.now(),
                Instant.now());
    }
}
