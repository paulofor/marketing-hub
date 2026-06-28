package com.marketinghub.pipelines.nichocnae.v3.personaroutinematerializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marketinghub.pipelines.nichocnae.v3.core.StageContext;
import com.marketinghub.pipelines.nichocnae.v3.core.StageResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Valida a materialização final do perfil persona-rotina no pipeline NichoCNAE v3. */
class PersonaRoutineMaterializerProcessorTest {
    /** Garante que a etapa final entregue perfil funcional completo para persistência canônica no backend. */
    @Test
    void shouldMaterializeApprovedPersonaRoutineProfile() {
        PersonaRoutineMaterializerProcessor processor = new PersonaRoutineMaterializerProcessor();
        StageResult result = processor.process(new StageContext("job-1", "10", Map.of(
                "approved", true,
                "cnaeCode", "4781400",
                "cnaeDescription", "Comércio varejista de artigos do vestuário",
                "winningPersonaName", "lojista MEI de moda",
                "winnerPersona", Map.of("name", "lojista MEI de moda", "description", "Dono operador de loja pequena"),
                "dailyTasks", List.of(
                        Map.of(
                                "task", "controlar estoque de peças",
                                "pain", "CONTROLE_OPERACIONAL_MANUAL",
                                "buyingSignal", "PROCURA_FERRAMENTA_OU_MODELO",
                                "sourceUrl", "https://fonte.example/rotina",
                                "evidenceText", "relato público sobre controle de estoque",
                                "easeLever", "simplificar controle operacional"),
                        Map.of(
                                "task", "responder clientes no WhatsApp",
                                "pain", "PERDA_DE_TEMPO",
                                "buyingSignal", "SINAL_DE_COMPRA_A_VALIDAR",
                                "sourceUrl", "https://fonte.example/atendimento",
                                "evidenceText", "relato público sobre atendimento diário",
                                "easeLever", "economizar tempo em tarefa recorrente")))));

        assertThat(result.status()).isEqualTo("PERFIL_MATERIALIZAVEL");
        assertThat(result.output()).containsEntry("nextStageCode", "").containsEntry("materializationReadiness", "PRONTO_PARA_BACKEND_PERSISTIR_MARKET_NICHE_E_PROFILE");
        assertThat(result.output().get("materializedProfile")).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> profile = (Map<String, Object>) result.output().get("materializedProfile");
        assertThat(profile)
                .containsEntry("personaName", "lojista MEI de moda")
                .containsEntry("approvedByQualityGate", true);
        assertThat(profile.get("dailyTasks")).asList().hasSize(2);
        assertThat(profile.get("evidenceSources")).asList().hasSize(2);
        assertThat(result.output().get("marketNicheCandidate")).isInstanceOf(Map.class);
    }

    /** Impede materialização quando o quality-gate ainda não aprovou a rotina. */
    @Test
    void shouldBlockWhenQualityGateIsNotApproved() {
        PersonaRoutineMaterializerProcessor processor = new PersonaRoutineMaterializerProcessor();

        assertThatThrownBy(() -> processor.process(new StageContext("job-1", "10", Map.of("approved", false))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("quality-gate aprovado");
    }
}
