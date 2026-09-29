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
            "(?is)\\s*<aside\\b[^>]*data-mh-commercial-terms=[\"']v1[\"'][^>]*>.*?</aside>");
    private static final Pattern OWN_STYLE = Pattern.compile(
            "(?is)\\s*<style\\b[^>]*data-mh-terms-style=[\"']v2[\"'][^>]*>.*?</style>");
    private static final Pattern BODY_END = Pattern.compile("(?i)</body\\s*>");
    private static final String RESPONSIVE_STYLE = """
            <style data-mh-terms-style="v2">
            .mh-commercial-terms{max-width:960px;margin:24px auto 32px;padding:clamp(20px,4vw,32px);box-sizing:border-box;overflow-wrap:anywhere;border:1px solid #e4dfe2;border-radius:20px;background:#fff;color:#292929;font:inherit;box-shadow:0 14px 36px rgba(35,24,31,.08)}
            .mh-commercial-terms .mh-terms-kicker{margin:0 0 8px;color:#7b3150;font-size:.78rem;font-weight:800;letter-spacing:.08em;text-transform:uppercase}
            .mh-commercial-terms h2{margin:0;font-size:clamp(1.45rem,4vw,2rem);line-height:1.15}
            .mh-commercial-terms .mh-terms-intro{max-width:720px;margin:10px 0 20px;line-height:1.55;color:#5f555a}
            .mh-commercial-terms .mh-terms-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px}
            .mh-commercial-terms details{border:1px solid #eadfe4;border-radius:14px;background:#fff9fb;overflow:hidden}
            .mh-commercial-terms summary{display:flex;align-items:center;justify-content:space-between;gap:12px;min-height:52px;padding:13px 16px;box-sizing:border-box;cursor:pointer;font-weight:750;line-height:1.35;list-style:none}
            .mh-commercial-terms summary::-webkit-details-marker{display:none}
            .mh-commercial-terms summary::after{content:"+";flex:0 0 auto;color:#7b3150;font-size:1.25rem;line-height:1}
            .mh-commercial-terms details[open] summary::after{content:"−"}
            .mh-commercial-terms ul{margin:0;padding:0 18px 16px 36px;color:#4f474b;line-height:1.55}
            .mh-commercial-terms li+li{margin-top:8px}
            @media(max-width:720px){
              .mh-commercial-terms{margin:20px 16px 28px;padding:20px 16px;border-radius:16px}
              .mh-commercial-terms .mh-terms-grid{grid-template-columns:1fr}
              .mobile-sticky,.mobile-sticky-cta,[data-mh-sticky-cta]{position:static!important;inset:auto!important;transform:none!important;width:auto!important;max-width:none!important;margin:0 16px 20px!important;box-shadow:none!important}
            }
            </style>
            """;

    /** Impede instâncias de um renderizador puro de condições comerciais. */
    private GeraSalesPageCommercialTerms() {}

    /** Renderiza somente fontes explícitas, escapadas e atualizadas na mesma publicação auditada. */
    static String render(String html, Map<String, Object> promptData, ObjectMapper json) {
        JsonNode product = json.valueToTree(promptData).path("experiment").path("product");
        JsonNode contract = product.path("experienceContract");
        Map<String, List<String>> terms = new LinkedHashMap<>();
        put(terms, "Prazo e acesso à entrega", deliveryTerms(contract.path("delivery")));
        add(terms, "Como enviar suas informações", contract.path("delivery").path("briefingChannel"));
        add(terms, "Personalização contratada", contract.path("delivery").path("personalizationScope"));
        put(terms, "Suporte e atendimento", supportTerms(contract.path("support")));
        put(terms, "Como solicitar reembolso", refundTerms(contract.path("refund")));
        add(terms, "Identificação no pagamento", contract.path("checkoutIdentity").path("explanation"));
        add(terms, "Reembolso da oferta e proteção do pagamento",
                contract.path("refund").path("providerProtectionDistinction"));
        String clean = OWN_STYLE.matcher(OWN_SECTION.matcher(html).replaceAll("")).replaceAll("");
        if (terms.isEmpty()) return clean;
        StringBuilder section = new StringBuilder(RESPONSIVE_STYLE)
                .append("<aside class=\"mh-commercial-terms\" data-mh-commercial-terms=\"v1\" "
                        + "aria-label=\"Condições de compra\">"
                        + "<p class=\"mh-terms-kicker\">Transparência antes da compra</p>"
                        + "<h2>Compra e atendimento</h2>"
                        + "<p class=\"mh-terms-intro\">Prazo, personalização, suporte, pagamento e reembolso "
                        + "organizados para consulta rápida.</p><div class=\"mh-terms-grid\">");
        terms.forEach((label, items) -> appendGroup(section, label, items));
        section.append("</div></aside>");
        var end = BODY_END.matcher(clean);
        return end.find() ? clean.substring(0, end.start()) + section + clean.substring(end.start())
                : clean + section;
    }

    /** Acrescenta apenas texto cadastrado, sem interpretar metadados como promessa comercial. */
    private static void add(Map<String, List<String>> terms, String label, JsonNode value) {
        if (value.isTextual() && !value.asText().isBlank()) terms.put(label, List.of(value.asText().strip()));
    }

    /** Expõe prazo somente com quantidade e marco inicial conhecidos, preservando acesso cadastrado. */
    private static List<String> deliveryTerms(JsonNode delivery) {
        List<String> parts = new ArrayList<>();
        int days = positiveInteger(delivery.path("businessDays"));
        if (days > 0 && "PAYMENT_APPROVED_AND_COMPLETE_BRIEFING".equals(delivery.path("startsAfter").asText())) {
            parts.add("Entrega em até " + days + (days == 1 ? " dia útil" : " dias úteis")
                    + " após pagamento aprovado e recebimento do briefing completo.");
            append(parts, delivery.path("businessDaysDefinition"));
        }
        append(parts, delivery.path("channel"));
        append(parts, delivery.path("access"));
        return parts;
    }

    /** Preserva canal, primeira resposta, período e limites de suporte sem assumir valores ausentes. */
    private static List<String> supportTerms(JsonNode support) {
        List<String> parts = new ArrayList<>();
        int responseDays = positiveInteger(support.path("firstResponseBusinessDays"));
        if (responseDays > 0) parts.add("Primeira resposta em até " + responseDays
                + (responseDays == 1 ? " dia útil." : " dias úteis."));
        int duration = positiveInteger(support.path("durationCalendarDaysAfterDelivery"));
        if (duration > 0) parts.add("Suporte por " + duration
                + (duration == 1 ? " dia corrido" : " dias corridos") + " após a entrega.");
        append(parts, support.path("email"));
        append(parts, support.path("scope"));
        append(parts, support.path("exclusions"));
        return parts;
    }

    /** Mantém a janela e o procedimento explícitos sem presumir reembolso integral ou prazo bancário. */
    private static List<String> refundTerms(JsonNode refund) {
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
        return parts;
    }

    /** Monta um grupo recolhível com cada compromisso em item próprio para leitura mobile. */
    private static void appendGroup(StringBuilder section, String label, List<String> items) {
        section.append("<details><summary>").append(escape(label)).append("</summary><ul>");
        items.forEach(item -> section.append("<li>").append(escape(item)).append("</li>"));
        section.append("</ul></details>");
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
    private static void put(Map<String, List<String>> terms, String label, List<String> items) {
        if (!items.isEmpty()) terms.put(label, List.copyOf(items));
    }

    /** Mantém conteúdo do contrato como texto, impedindo HTML, links ou scripts injetados. */
    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
