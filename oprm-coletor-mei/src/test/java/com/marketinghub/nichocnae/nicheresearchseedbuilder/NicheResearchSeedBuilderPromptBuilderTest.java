package com.marketinghub.nichocnae.nicheresearchseedbuilder;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
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

    /** Cria uma pendência padrão para montagem determinística do prompt da etapa dois. */
    private NicheResearchSeedBuilderPending pending() {
        return new NicheResearchSeedBuilderPending(
                1001L,
                55L,
                "9602501",
                "Cabeleireiros, manicure e pedicure",
                "Cabeleireiros, manicure e pedicure",
                BigDecimal.valueOf(92),
                "gpt-5.4",
                "gpt-5.4 (gpt-5.4)",
                "AUTO_SCORE_QUEUE",
                "RUNNING",
                Instant.now(),
                Instant.now());
    }
}
