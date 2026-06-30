package com.marketinghub.gerasalespage.v1;

import java.util.List;
import java.util.Optional;

/** Responsabilidade: definir a ordem canônica das etapas do GeraSalesPage v1. */
public enum GeraSalesPageStageCode {
    OFFER_BRIEF("sales-page-offer-brief"),
    WIREFRAME("sales-page-wireframe"),
    COPY("sales-page-copy"),
    VISUAL_PLAN("sales-page-visual-plan"),
    HTML("sales-page-html"),
    CHECKOUT_QUALITY_REVIEW("sales-page-checkout-quality-review"),
    PUBLICATION_PACKAGE("sales-page-publication-package");

    private final String code;

    /** Inicializa a etapa com o código operacional persistido. */
    GeraSalesPageStageCode(String code) {
        this.code = code;
    }

    /** Retorna o código operacional da etapa. */
    public String code() {
        return code;
    }

    /** Lista os códigos operacionais na ordem de execução do pipeline. */
    public static List<String> orderedCodes() {
        return List.of(values()).stream().map(GeraSalesPageStageCode::code).toList();
    }

    /** Localiza a próxima etapa canônica após a etapa informada. */
    public static Optional<String> nextAfter(String currentCode) {
        List<String> codes = orderedCodes();
        int index = codes.indexOf(currentCode);
        if (index < 0 || index + 1 >= codes.size()) {
            return Optional.empty();
        }
        return Optional.of(codes.get(index + 1));
    }

    /** Indica se o código pertence ao pipeline GeraSalesPage v1. */
    public static boolean contains(String code) {
        return orderedCodes().contains(code);
    }
}
