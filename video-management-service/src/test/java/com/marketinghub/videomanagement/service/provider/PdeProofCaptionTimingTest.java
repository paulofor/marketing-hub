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

    /** Mostra o convite durante toda a fala, preservando até três segundos posteriores. */
    @ParameterizedTest
    @ValueSource(longs = {10, 93010})
    void keepsFinalSequenceProofUntilLastFrame(long productId) {
        var input = metadata("Conheça o site. | Veja o resultado. | Experimente a demonstração.");
        input.put("productId", productId).put("targetDurationSeconds", 22);
        input.withObject("/post_production/product_proof").put("captionCueIndex", 2)
                .put("sequenceShot", true).put("minimumSeconds", 2);
        var captions = List.of(new PdeProofCaptionTiming.Window(0, 8.4, 0),
                new PdeProofCaptionTiming.Window(8.4, 17.184, 1),
                new PdeProofCaptionTiming.Window(17.184, 21.048, 2));
        assertThat(PdeProofCaptionTiming.resolve(input, captions))
                .isEqualTo(new PdeProofCaptionTiming.Window(17.184, 22, 2));
        input.withObject("/post_production/product_proof").put("minimumSeconds", 6);
        assertThatThrownBy(() -> PdeProofCaptionTiming.resolve(input, captions))
                .isInstanceOf(VideoProviderException.class);
    }

    /** Recusa um encerramento artificialmente prolongado ou sem tempo mínimo de leitura. */
    @Test
    void rejectsClosingWithoutTimingForReadingOrWithLongHold() {
        var input = metadata("Conheça o site. | Experimente.");
        input.withObject("/post_production/product_proof").put("captionCueIndex", 1)
                .put("sequenceShot", true).put("minimumSeconds", 2);
        for (var cue : List.of(new PdeProofCaptionTiming.Window(12, 16, 1),
                new PdeProofCaptionTiming.Window(29, 30, 1))) {
            assertThatThrownBy(() -> PdeProofCaptionTiming.resolve(input,
                    List.of(new PdeProofCaptionTiming.Window(0, 12, 0), cue)))
                    .isInstanceOf(VideoProviderException.class);
        }
    }

    /** Monta uma entrada segregada, sem prova nem custo comercial. */
    private ObjectNode metadata(String caption) {
        var input = new ObjectMapper().createObjectNode();
        input.put("captionText", caption).put("targetDurationSeconds", 30);
        input.putObject("technicalQualityGate").put("captionMustMatchNarration", true);
        return input;
    }
}
