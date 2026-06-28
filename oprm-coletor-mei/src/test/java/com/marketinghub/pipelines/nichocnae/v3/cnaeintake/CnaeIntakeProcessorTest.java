package com.marketinghub.pipelines.nichocnae.v3.cnaeintake;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marketinghub.pipelines.nichocnae.v3.core.StageContext;
import com.marketinghub.pipelines.nichocnae.v3.core.StageResult;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Valida a qualificação inicial do CNAE no pipeline NichoCNAE v3. */
class CnaeIntakeProcessorTest {
    /** Garante que a etapa 1 entrega código, nome do CNAE e recorte MEI/autônomo para a geração de personas. */
    @Test
    void shouldQualifyCnaeBeforePersonaGeneration() {
        CnaeIntakeProcessor processor = new CnaeIntakeProcessor();

        StageResult result = processor.process(new StageContext(
                "job-4781400",
                "107",
                Map.of("cnaeCode", "4781400", "cnaeDescription", "Comércio varejista de artigos do vestuário")));

        assertThat(result.status()).isEqualTo("CNAE_RECEBIDO");
        assertThat(result.output()).containsEntry("cnaeCode", "4781400");
        assertThat(result.output()).containsEntry("cnaeDescription", "Comércio varejista de artigos do vestuário");
        assertThat(result.output()).containsEntry("targetAudienceType", "MEI_PROFISSIONAIS_AUTONOMOS_NAO_CLT");
        assertThat(result.output()).containsEntry("targetAudienceDefinition", "Estamos falando de MEI, donos-operadores e profissionais autônomos que atuam por conta própria, sem contratação direta como CLT.");
        assertThat(result.output()).containsEntry("employmentBoundary", "NAO_ANALISAR_FUNCIONARIOS_CLT_CONTRATADOS_DIRETAMENTE");
        assertThat(result.output()).containsEntry("businessBoundary", "NAO_GERAR_OFERTA_CAMPANHA_LANDING");
        assertThat(result.output()).containsEntry("reportRole", "PERSONA_ROTINA_TAREFAS_DIARIAS");
        assertThat(result.output()).containsEntry("nextStageCode", "persona-candidate-generator");
    }

    /** Bloqueia avanço quando a entrada inicial ainda não tem o nome completo do CNAE. */
    @Test
    void shouldFailWithoutCnaeDescription() {
        CnaeIntakeProcessor processor = new CnaeIntakeProcessor();

        assertThatThrownBy(() -> processor.process(new StageContext("job-4781400", "107", Map.of("cnaeCode", "4781400"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cnaeDescription");
    }
}
