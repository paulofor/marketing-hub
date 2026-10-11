package com.marketinghub.videomanagement.service.provider;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Reproduz a fala antes da prova de Mira e protege a sincronização reutilizável entre produtos. */
class PdeProofCaptionTimingTest {
    /** A prova usa o terceiro áudio real, nunca o corte visual planejado para o fim. */
    @ParameterizedTest
    @ValueSource(longs = {10, 93010})
    void selectsMeasuredCaptionInsteadOfPlannedProof(long productId) {
        var metadata = metadata("Seus produtos organizados. | Informe nomes. | Veja o exemplo, com fontes e limites. | Conheça o produto.");
        metadata.put("productId", productId);
        metadata.putArray("cut_plan").addObject().put("role", "PROVA").put("duration_seconds", 30);
        var windows = List.of(new PdeProofCaptionTiming.Window(0, 4.32, -1),
                new PdeProofCaptionTiming.Window(4.32, 7.488, -1),
                new PdeProofCaptionTiming.Window(7.488, 11.136, -1),
                new PdeProofCaptionTiming.Window(11.136, 30, -1));
        assertThat(PdeProofCaptionTiming.resolve(metadata, windows))
                .isEqualTo(new PdeProofCaptionTiming.Window(7.488, 13.488, 2));
    }

    /** Contratos ambíguos bloqueiam antes de consumir voz; associação explícita resolve a ambiguidade. */
    @Test
    void rejectsAmbiguousOrMissingCueAndAcceptsExplicitIndex() {
        for (String caption : List.of("Conheça o produto.", "Exemplo inicial. | Veja outra amostra."))
            assertThatThrownBy(() -> PdeProofCaptionTiming.cueIndex(metadata(caption)))
                    .hasFieldOrPropertyWithValue("code", "PDE_PROOF_CAPTION_TIMING_INVALID");
        var input = metadata("Conheça o produto. | Veja sua organização.");
        input.withObject("/post_production/product_proof").put("captionCueIndex", 1);
        assertThat(PdeProofCaptionTiming.cueIndex(input)).isEqualTo(1);
        input.withObject("/post_production/product_proof").put("captionCueIndex", 3);
        assertThatThrownBy(() -> PdeProofCaptionTiming.cueIndex(input)).isInstanceOf(VideoProviderException.class);
    }

    /** Mantém o contrato sem locução e recusa relógios incompatíveis com a duração aprovada. */
    @Test
    void preservesLegacyAndRejectsOutOfBoundsAudio() {
        var input = metadata("Veja uma demonstração.");
        assertThatThrownBy(() -> PdeProofCaptionTiming.resolve(input,
                List.of(new PdeProofCaptionTiming.Window(0, 31, -1)))).isInstanceOf(VideoProviderException.class);
        input.remove("technicalQualityGate");
        assertThat(PdeProofCaptionTiming.resolve(input, List.of())).isNull();
    }

    /** Monta uma entrada segregada, sem prova nem custo comercial. */
    private ObjectNode metadata(String caption) {
        var input = new ObjectMapper().createObjectNode();
        input.put("captionText", caption).put("targetDurationSeconds", 30);
        input.putObject("technicalQualityGate").put("captionMustMatchNarration", true);
        return input;
    }
}
