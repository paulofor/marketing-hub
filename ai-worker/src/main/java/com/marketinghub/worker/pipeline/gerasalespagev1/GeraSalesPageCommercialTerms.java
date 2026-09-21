package com.marketinghub.worker.pipeline.gerasalespagev1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Preserva no HTML condições de compra explícitas do contrato, sem depender da síntese do modelo. */
final class GeraSalesPageCommercialTerms {
    private static final Pattern OWN_SECTION = Pattern.compile(
            "(?is)<aside\\b[^>]*data-mh-commercial-terms=[\"']v1[\"'][^>]*>.*?</aside>");
    private static final Pattern BODY_END = Pattern.compile("(?i)</body\\s*>");

    /** Impede instâncias de um renderizador puro de condições comerciais. */
    private GeraSalesPageCommercialTerms() {}

    /** Renderiza somente fontes explícitas, escapadas e atualizadas na mesma publicação auditada. */
    static String render(String html, Map<String, Object> promptData, ObjectMapper json) {
        JsonNode product = json.valueToTree(promptData).path("experiment").path("product");
        JsonNode contract = product.path("experienceContract");
        Map<String, String> terms = new LinkedHashMap<>();
        add(terms, "Como enviar suas informações", contract.path("delivery").path("briefingChannel"));
        add(terms, "Identificação no pagamento", contract.path("checkoutIdentity").path("explanation"));
        add(terms, "Reembolso da oferta e proteção do pagamento",
                contract.path("refund").path("providerProtectionDistinction"));
        String clean = OWN_SECTION.matcher(html).replaceAll("");
        if (terms.isEmpty()) return clean;
        StringBuilder section = new StringBuilder("<aside data-mh-commercial-terms=\"v1\" "
                + "aria-label=\"Condições de compra\" style=\"max-width:960px;margin:24px auto 128px;padding:24px;"
                + "box-sizing:border-box;overflow-wrap:anywhere;border:1px solid #ddd;border-radius:16px;"
                + "background:#fff;color:#292929;font:inherit\"><h2>Compra e atendimento</h2><dl>");
        terms.forEach((label, text) -> section.append("<dt style=\"font-weight:700;margin-top:16px\">")
                .append(escape(label)).append("</dt><dd style=\"margin:8px 0 0;line-height:1.6\">")
                .append(escape(text)).append("</dd>"));
        section.append("</dl></aside>");
        var end = BODY_END.matcher(clean);
        return end.find() ? clean.substring(0, end.start()) + section + clean.substring(end.start())
                : clean + section;
    }

    /** Acrescenta apenas texto cadastrado, sem interpretar metadados como promessa comercial. */
    private static void add(Map<String, String> terms, String label, JsonNode value) {
        if (value.isTextual() && !value.asText().isBlank()) terms.put(label, value.asText().strip());
    }

    /** Mantém conteúdo do contrato como texto, impedindo HTML, links ou scripts injetados. */
    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
