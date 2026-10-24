package com.marketinghub.videomanagement.service.provider;

import com.fasterxml.jackson.databind.JsonNode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Vincula a prova do produto ao trecho efetivamente narrado, sem alterar fala ou gerar mídia. */
final class PdeProofCaptionTiming {
    private static final java.util.regex.Pattern PROOF_WORD =
            java.util.regex.Pattern.compile("\\b(exemplo|amostra|demonstracao|prova)\\b");
    /** Identifica uma referência explícita à prova na copy aprovada e recusa ambiguidade. */
    static int cueIndex(JsonNode metadata) {
        if (!metadata.at("/technicalQualityGate/captionMustMatchNarration").asBoolean(false)) return -1;
        String text = metadata.path("captionText").asText(metadata.at("/post_production/caption_plan").asText());
        var segments = java.util.Arrays.stream(text.split("\\s*\\|\\s*")).map(String::trim)
                .filter(value -> !value.isBlank()).toList();
        var explicit = metadata.at("/post_production/product_proof/captionCueIndex");
        if (explicit.isIntegralNumber() && explicit.asInt() >= 0 && explicit.asInt() < segments.size())
            return explicit.asInt();
        if (!explicit.isMissingNode()) throw invalid();
        var matches = new ArrayList<Integer>();
        for (int index = 0; index < segments.size(); index++) {
            String normalized = Normalizer.normalize(segments.get(index), Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
            if (PROOF_WORD.matcher(normalized).find())
                matches.add(index);
        }
        if (matches.size() != 1) throw invalid();
        return matches.getFirst();
    }

    /** Mostra a prova desde o início do convite narrado e a mantém até o último frame. */
    static Window resolve(JsonNode metadata, List<Window> captions) {
        int index = cueIndex(metadata);
        if (index < 0) return null;
        if (index >= captions.size()) throw invalid();
        Window cue = captions.get(index);
        double duration = metadata.path("postProductionDurationSeconds")
                .asDouble(metadata.path("targetDurationSeconds").asDouble());
        if (!Double.isFinite(cue.start()) || !Double.isFinite(cue.end()) || cue.start() < 0
                || cue.end() <= cue.start() || cue.end() > duration + .1) throw invalid();
        double reading = metadata.at("/post_production/product_proof/sequenceShot").asBoolean(false)
                ? metadata.at("/post_production/product_proof/minimumSeconds").asDouble(6) : 6;
        if (!Double.isFinite(reading) || reading < 2 || reading > 12) throw invalid();
        if (metadata.at("/post_production/product_proof/sequenceShot").asBoolean(false)
                && index == captions.size() - 1) {
            double start = cue.start();
            if (reading > 3 || duration - cue.end() > 3 || duration - start < reading)
                throw new VideoProviderException("PDE_PROOF_CAPTION_TIMING_INVALID",
                        "O encerramento precisa de dois a três segundos legíveis, sem prolongar a chamada após a voz.");
            return new Window(start, duration, index);
        }
        double end = Math.max(cue.end(), cue.start() + reading);
        if (end > duration - 2) throw new VideoProviderException("PDE_PROOF_CAPTION_TIMING_INVALID",
                "A prova precisa do tempo de leitura declarado (seis segundos no contrato original) e dois segundos de encerramento; ajuste a finalização reutilizando a voz auditada.");
        return new Window(cue.start(), end, index);
    }

    /** Expõe um bloqueio de contrato antes de sintetizar voz ou montar prova no instante errado. */
    private static VideoProviderException invalid() {
        return new VideoProviderException("PDE_PROOF_CAPTION_TIMING_INVALID",
                "A locução precisa identificar um único trecho de exemplo, amostra ou demonstração da prova; informe captionCueIndex para uma associação explícita.");
    }

    /** Representa os segundos auditados e o índice editorial do trecho de prova. */
    record Window(double start, double end, int cueIndex) {}
}
