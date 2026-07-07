package com.marketinghub.gerasalespage.v1;

import org.springframework.util.StringUtils;

/** Responsabilidade: validar o contrato minimo de analytics publico das paginas do GeraSalesPage v1. */
public final class GeraSalesPageAnalyticsContract {
    public static final String TRACK_SECTION_ATTRIBUTE = "data-track-section";
    public static final String TRACKING_SCRIPT_ATTRIBUTE = "data-mh-sales-page-analytics";

    private GeraSalesPageAnalyticsContract() {
    }

    /** Confirma que o HTML possui coletores e pelo menos uma secao rastreavel para tempo de visualizacao. */
    public static boolean hasRequiredCollectors(String html) {
        if (!StringUtils.hasText(html)) {
            return false;
        }
        return html.contains(TRACKING_SCRIPT_ATTRIBUTE)
                && html.contains(TRACK_SECTION_ATTRIBUTE)
                && html.contains("page_view")
                && html.contains("page_load_metric")
                && html.contains("section_view_time")
                && html.contains("checkout_click");
    }
}
