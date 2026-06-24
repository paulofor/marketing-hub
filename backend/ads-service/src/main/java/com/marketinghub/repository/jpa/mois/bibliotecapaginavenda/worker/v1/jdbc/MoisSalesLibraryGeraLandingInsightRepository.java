package com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.jdbc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesLibraryGeraLandingInsightGateway;
import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesLibraryGeraLandingInsightGateway.GeraLandingReferenceInsight;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Responsabilidade: consultar insumos GeraLanding já analisados na biblioteca MOIS usando somente acesso JDBC centralizado em repository. */
@Repository
public class MoisSalesLibraryGeraLandingInsightRepository implements MoisSalesLibraryGeraLandingInsightGateway {

    private static final Logger log = LoggerFactory.getLogger(MoisSalesLibraryGeraLandingInsightRepository.class);
    private static final TypeReference<Map<String, Object>> JSON_OBJECT_TYPE = new TypeReference<>() {};
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /** Inicializa o repository com o acesso JDBC canônico do backend e serializador para expor JSON estruturado. */
    public MoisSalesLibraryGeraLandingInsightRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /** Busca referências com maior score e todos os blocos de insumo preenchidos para alimentar prompts do GeraLanding. */
    @Override
    public List<GeraLandingReferenceInsight> findTopReferences(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 5));
        return jdbcTemplate.query("""
                SELECT e.sales_page_id,
                       p.url_canonical,
                       p.title,
                       e.score_total,
                       e.geralanding_wireframe_json,
                       e.geralanding_copy_json,
                       e.geralanding_image_prompt_json,
                       e.geralanding_design_preset_json
                FROM mois_sales_page_job_execution e
                INNER JOIN mois_sales_page p ON p.id = e.sales_page_id
                WHERE e.job_type = 'PAGE_ANALYSIS'
                  AND e.status = 'DONE'
                  AND e.score_total IS NOT NULL
                  AND e.geralanding_wireframe_json IS NOT NULL
                  AND e.geralanding_copy_json IS NOT NULL
                  AND e.geralanding_image_prompt_json IS NOT NULL
                  AND e.geralanding_design_preset_json IS NOT NULL
                ORDER BY e.score_total DESC, e.finished_at DESC, e.id DESC
                LIMIT ?
                """, (rs, rowNum) -> new GeraLandingReferenceInsight(
                rs.getLong("sales_page_id"),
                rs.getString("url_canonical"),
                rs.getString("title"),
                rs.getBigDecimal("score_total"),
                parseJsonObject(rs.getString("geralanding_wireframe_json"), rs.getLong("sales_page_id"), "geralanding_wireframe_json"),
                parseJsonObject(rs.getString("geralanding_copy_json"), rs.getLong("sales_page_id"), "geralanding_copy_json"),
                parseJsonObject(rs.getString("geralanding_image_prompt_json"), rs.getLong("sales_page_id"), "geralanding_image_prompt_json"),
                parseJsonObject(rs.getString("geralanding_design_preset_json"), rs.getLong("sales_page_id"), "geralanding_design_preset_json")
        ), safeLimit);
    }

    /** Converte o JSON textual do banco para objeto estruturado antes de entregar ao GeraLanding. */
    private Object parseJsonObject(String rawJson, long pageId, String fieldName) {
        if (rawJson == null || rawJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(rawJson, JSON_OBJECT_TYPE);
        } catch (Exception ex) {
            log.warn(
                    "Falha ao converter insumo MOIS para objeto JSON estruturado. modulo=GeraLanding, operacao=parseJsonObject, pageId={}, fieldName={}",
                    pageId,
                    fieldName,
                    ex
            );
            return Map.of("raw", rawJson);
        }
    }

}
