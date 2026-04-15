package com.marketinghub.oprm.application;

import java.text.Normalizer;
import java.util.Locale;

public final class OccupationLabelNormalizer {

    private OccupationLabelNormalizer() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .trim();
        return cleaned.replaceAll("\\s+", " ");
    }
}
