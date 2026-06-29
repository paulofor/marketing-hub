package com.marketinghub.repository.jpa.mois.dossieproduto;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/** Lê o contexto coletado da página/produto MOIS para alimentar o pipeline de dossiê. */
@Repository
public class DossierProductContextGateway {
    private final JdbcTemplate jdbcTemplate;

    /** Cria o gateway com acesso JDBC canônico centralizado no pacote de repositórios. */
    public DossierProductContextGateway(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Busca HTML bruto, descrições e resumos já coletados/analisados para uma página de venda. */
    public Optional<DossierProductContext> findContext(long pageId) {
        return jdbcTemplate.query("""
                SELECT p.id AS page_id, p.workspace_id, p.source, p.url_canonical, p.title, p.product_name,
                       COALESCE(cr_direct.producer_name, cr_url.producer_name) AS producer_name,
                       COALESCE(cr_direct.hotmart_price, cr_url.hotmart_price) AS hotmart_price,
                       COALESCE(cr_direct.hotmart_temperature, cr_url.hotmart_temperature) AS hotmart_temperature,
                       COALESCE(cr_direct.hotmart_producer, cr_url.hotmart_producer) AS hotmart_producer,
                       COALESCE(cr_direct.hotmart_description, cr_url.hotmart_description) AS hotmart_description,
                       p.url_final, p.http_status, p.html_sha256, p.html_bytes, p.score_total,
                       p.offer_summary, p.mechanism_summary, p.promise_summary, p.proof_summary,
                       cap.raw_html AS raw_html
                FROM mois_sales_page p
                LEFT JOIN mois_collected_reference cr_direct ON cr_direct.id = p.collected_reference_id
                LEFT JOIN (
                    SELECT workspace_id, COALESCE(sales_page_url, product_url) AS reference_url, MAX(id) AS latest_reference_id
                    FROM mois_collected_reference
                    WHERE source = 'HOTMART' AND COALESCE(sales_page_url, product_url) IS NOT NULL
                    GROUP BY workspace_id, COALESCE(sales_page_url, product_url)
                ) cr_latest ON cr_latest.workspace_id = p.workspace_id AND cr_latest.reference_url = p.url_canonical
                LEFT JOIN mois_collected_reference cr_url ON cr_url.id = cr_latest.latest_reference_id
                LEFT JOIN mois_sales_page_capture cap ON cap.id = (
                    SELECT MAX(cap2.id)
                    FROM mois_sales_page_capture cap2
                    WHERE cap2.sales_page_id = p.id
                      AND cap2.status = 'CAPTURED'
                      AND COALESCE(cap2.raw_html_bytes, 0) > 0
                )
                WHERE p.id = ?
                LIMIT 1
                """, rs -> {
                    if (!rs.next()) return Optional.<DossierProductContext>empty();
                    return Optional.of(new DossierProductContext(
                            rs.getLong("page_id"), rs.getString("workspace_id"), rs.getString("source"),
                            rs.getString("url_canonical"), rs.getString("title"), rs.getString("product_name"),
                            rs.getString("producer_name"), rs.getString("hotmart_price"), rs.getString("hotmart_temperature"),
                            rs.getString("hotmart_producer"), rs.getString("hotmart_description"), rs.getString("url_final"),
                            (Integer) rs.getObject("http_status"), rs.getString("html_sha256"), rs.getLong("html_bytes"),
                            rs.getString("score_total"), rs.getString("offer_summary"), rs.getString("mechanism_summary"),
                            rs.getString("promise_summary"), rs.getString("proof_summary"), rs.getString("raw_html")));
                }, pageId);
    }

    /** Indica se existe material coletado mínimo para permitir iniciar o dossiê. */
    public boolean hasCollectedProductContext(long pageId) {
        return findContext(pageId).map(DossierProductContext::hasCollectedPayload).orElse(false);
    }

    /** Representa o material coletado/analisado que deve entrar no prompt de entendimento do produto. */
    public record DossierProductContext(
            long pageId, String workspaceId, String source, String urlCanonical, String title, String productName,
            String producerName, String hotmartPrice, String hotmartTemperature, String hotmartProducer,
            String hotmartDescription, String finalUrl, Integer httpStatus, String htmlSha256, long htmlBytes,
            String scoreTotal, String offerSummary, String mechanismSummary, String promiseSummary, String proofSummary,
            String rawHtml) {

        /** Verifica se há HTML ou descrições/resumos suficientes para o dossiê trabalhar com evidência real. */
        public boolean hasCollectedPayload() {
            return htmlBytes > 0 || hasText(rawHtml) || hasText(hotmartDescription) || hasText(offerSummary)
                    || hasText(mechanismSummary) || hasText(promiseSummary) || hasText(proofSummary);
        }

        /** Converte o contexto para Map estável usado no contrato pending entregue ao executor. */
        public Map<String, Object> toInputMap() {
            Map<String, Object> values = new LinkedHashMap<>();
            put(values, "pageId", pageId);
            put(values, "workspaceId", workspaceId);
            put(values, "source", source);
            put(values, "urlCanonical", urlCanonical);
            put(values, "title", title);
            put(values, "productName", productName);
            put(values, "producerName", producerName);
            put(values, "hotmartPrice", hotmartPrice);
            put(values, "hotmartTemperature", hotmartTemperature);
            put(values, "hotmartProducer", hotmartProducer);
            put(values, "hotmartDescription", hotmartDescription);
            put(values, "finalUrl", finalUrl);
            put(values, "httpStatus", httpStatus);
            put(values, "htmlSha256", htmlSha256);
            put(values, "htmlBytes", htmlBytes);
            put(values, "scoreTotal", scoreTotal);
            put(values, "offerSummary", offerSummary);
            put(values, "mechanismSummary", mechanismSummary);
            put(values, "promiseSummary", promiseSummary);
            put(values, "proofSummary", proofSummary);
            put(values, "rawHtml", rawHtml);
            return values;
        }

        /** Adiciona valor textual ou numérico no mapa preservando apenas campos com conteúdo útil. */
        private static void put(Map<String, Object> values, String key, Object value) {
            if (value instanceof String text) {
                if (StringUtils.hasText(text)) values.put(key, text);
            } else if (value != null) {
                values.put(key, value);
            }
        }

        /** Verifica texto útil para validar existência de material coletado. */
        private static boolean hasText(String value) {
            return StringUtils.hasText(value);
        }
    }
}
