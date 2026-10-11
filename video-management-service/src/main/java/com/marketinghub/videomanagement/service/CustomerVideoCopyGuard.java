package com.marketinghub.videomanagement.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.videomanagement.service.provider.VideoProviderException;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Separa copy destinada à cliente de instruções internas antes de render ou síntese paga. */
public final class CustomerVideoCopyGuard {
    private static final Pattern INTERNAL = Pattern.compile(
            "\\b(acesso privado|demonstracao interna|homologacao|pipeline|gate tecnico|"
                    + "ciclo #[0-9]+|experimento #[0-9]+|fixture|callback|sha.?256)\\b");
    private static final List<String> COPY_PATHS = List.of(
            "/voiceOverScript", "/captionText", "/ctaText", "/voice_over_script", "/cta_text",
            "/premiumFinalization/voiceOverScript", "/premiumFinalization/captionText",
            "/premiumFinalization/ctaText", "/post_production/caption_plan", "/post_production/cta_text");

    /** Impede instanciação do validador sem estado. */
    private CustomerVideoCopyGuard() {}

    /** Confere somente campos exibidos ou falados, preservando auditoria, prova e instruções internas. */
    public static void validate(JsonNode metadata) {
        for (String path : COPY_PATHS) {
            String text = normalize(metadata.at(path).asText(""));
            var match = INTERNAL.matcher(text);
            if (match.find()) {
                throw new VideoProviderException("CUSTOMER_VIDEO_INTERNAL_LANGUAGE",
                        "A copy da cliente contém linguagem operacional em " + path
                                + ": " + match.group() + ". Corrija o roteiro antes de consumir produção ou voz.");
            }
        }
    }

    /** Normaliza acentos, caixa e espaços sem alterar o texto persistido ou a narração. */
    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
