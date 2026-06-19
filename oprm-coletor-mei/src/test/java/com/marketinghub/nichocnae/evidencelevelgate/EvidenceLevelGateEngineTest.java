package com.marketinghub.nichocnae.evidencelevelgate;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EvidenceLevelGateEngineTest {
    private final EvidenceLevelGateEngine engine = new EvidenceLevelGateEngine();

    /** Garante que a etapa aprova materialização apenas quando há camadas comerciais mínimas. */
    @Test
    void approvesMaterializationWithCommercialEvidence() {
        EvidenceLevelGateDecision decision = engine.evaluate(new EvidenceLevelGatePending(
                1L, 10L, "Nicho", "agenda visitas e entrega serviço", "dor com cancelamento e retrabalho",
                "perda de cliente e custo de atraso", "clientes pedem orçamento pelo whatsapp e indicação", "a.com,b.com",
                80, 70, 72, 80, 75, 76));

        assertThat(decision.evidenceLevel()).isEqualTo("E5");
        assertThat(decision.approvedForMaterialization()).isTrue();
    }

    /** Garante que atividade sem dor e sem fontes independentes não vira evidência comercial vendável. */
    @Test
    void rejectsWhenOnlyActivityExists() {
        EvidenceLevelGateDecision decision = engine.evaluate(new EvidenceLevelGatePending(
                1L, 10L, "Nicho", "rotina simples", "", "", "", "a.com",
                40, 50, 10, 10, 40, 40));

        assertThat(decision.evidenceLevel()).isEqualTo("E1");
        assertThat(decision.approvedForMaterialization()).isFalse();
        assertThat(decision.rejectionReasons()).contains("dor_pratica_insuficiente");
    }
}
