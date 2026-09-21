package com.marketinghub.worker.pipeline.gerasalespagev1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
        put(terms, "Prazo e acesso à entrega", deliveryTerms(contract.path("delivery")));
        add(terms, "Como enviar suas informações", contract.path("delivery").path("briefingChannel"));
        add(terms, "Personalização contratada", contract.path("delivery").path("personalizationScope"));
        put(terms, "Suporte e atendimento", supportTerms(contract.path("support")));
        put(terms, "Como solicitar reembolso", refundTerms(contract.path("refund")));
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

    /** Expõe prazo somente com quantidade e marco inicial conhecidos, preservando acesso cadastrado. */
    private static String deliveryTerms(JsonNode delivery) {
        List<String> parts = new ArrayList<>();
        int days = positiveInteger(delivery.path("businessDays"));
        if (days > 0 && "PAYMENT_APPROVED_AND_COMPLETE_BRIEFING".equals(delivery.path("startsAfter").asText())) {
            parts.add("Entrega em até " + days + (days == 1 ? " dia útil" : " dias úteis")
                    + " após pagamento aprovado e recebimento do briefing completo.");
            append(parts, delivery.path("businessDaysDefinition"));
        }
        append(parts, delivery.path("channel"));
        append(parts, delivery.path("access"));
        return String.join(" ", parts);
    }

    /** Preserva canal, primeira resposta, período e limites de suporte sem assumir valores ausentes. */
    private static String supportTerms(JsonNode support) {
        List<String> parts = new ArrayList<>();
        append(parts, support.path("email"));
        int responseDays = positiveInteger(support.path("firstResponseBusinessDays"));
        if (responseDays > 0) parts.add("Primeira resposta em até " + responseDays
                + (responseDays == 1 ? " dia útil." : " dias úteis."));
        int duration = positiveInteger(support.path("durationCalendarDaysAfterDelivery"));
        if (duration > 0) parts.add("Suporte por " + duration
                + (duration == 1 ? " dia corrido" : " dias corridos") + " após a entrega.");
        append(parts, support.path("scope"));
        append(parts, support.path("exclusions"));
        return String.join(" ", parts);
    }

    /** Mantém a janela e o procedimento explícitos sem presumir reembolso integral ou prazo bancário. */
    private static String refundTerms(JsonNode refund) {
        List<String> parts = new ArrayList<>();
        JsonNode window = refund.path("requestWindow");
        if (window.isTextual() && !window.asText().isBlank()) {
            parts.add((refund.path("fullRefund").isBoolean() && refund.path("fullRefund").asBoolean()
                    ? "Reembolso integral: " : "Solicitação de reembolso: ") + window.asText().strip() + ".");
        }
        append(parts, refund.path("email"));
        if (refund.path("reasonRequired").isBoolean() && !refund.path("reasonRequired").asBoolean())
            parts.add("Não é necessário justificar o pedido.");
        append(parts, refund.path("instructions"));
        append(parts, refund.path("processing"));
        return String.join(" ", parts);
    }

    /** Aceita somente dias inteiros positivos fornecidos pela fonte, sem converter texto ou frações. */
    private static int positiveInteger(JsonNode value) {
        return value.isIntegralNumber() && value.canConvertToInt() && value.intValue() > 0 ? value.intValue() : 0;
    }

    /** Junta somente trechos textuais cadastrados, sem serializar objetos técnicos na página. */
    private static void append(List<String> parts, JsonNode value) {
        if (value.isTextual() && !value.asText().isBlank()) parts.add(value.asText().strip());
    }

    /** Omite blocos vazios quando a fonte ainda não define o compromisso comercial. */
    private static void put(Map<String, String> terms, String label, String text) {
        if (!text.isBlank()) terms.put(label, text);
    }

    /** Mantém conteúdo do contrato como texto, impedindo HTML, links ou scripts injetados. */
    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
