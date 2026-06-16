package com.marketinghub.mois.service;

import com.marketinghub.mois.dto.MoisHotmartProductDtos;
import java.sql.Timestamp;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Consulta produtos Hotmart coletados e expõe os dados comerciais usados na tela e no ciclo 2. */
@Service
@RequiredArgsConstructor
public class MoisHotmartProductService {

    private final JdbcTemplate jdbcTemplate;

    /** Lista os produtos Hotmart da coleta mais recente do workspace informado. */
    public MoisHotmartProductDtos.HotmartCollectedProductListResponse listLatestByWorkspace(String workspaceId, int limit) {
        String latestJobId = jdbcTemplate.query(
                        """
                                SELECT job_id
                                FROM mois_collected_reference
                                WHERE workspace_id = ?
                                  AND source = 'HOTMART'
                                ORDER BY updated_at DESC
                                LIMIT 1
                                """,
                        (rs, rowNum) -> rs.getString("job_id"),
                        workspaceId)
                .stream()
                .findFirst()
                .orElse(null);

        if (latestJobId == null) {
            return new MoisHotmartProductDtos.HotmartCollectedProductListResponse(workspaceId, List.of());
        }

        List<MoisHotmartProductDtos.HotmartCollectedProductResponse> items = jdbcTemplate.query(
                """
                        SELECT job_id, reference_id, product_name, product_url, hotmart_description, producer_name,
                               hotmart_image_url, hotmart_price, sales_page_url, hotmart_temperature, collected_at
                        FROM mois_collected_reference
                        WHERE workspace_id = ?
                          AND source = 'HOTMART'
                          AND job_id = ?
                        ORDER BY collected_at DESC
                        LIMIT ?
                        """,
                (rs, rowNum) -> mapHotmartProduct(rs),
                workspaceId, latestJobId, limit
        );

        return new MoisHotmartProductDtos.HotmartCollectedProductListResponse(workspaceId, items);
    }

    /**
     * Lista produtos Hotmart do ciclo 1 ainda não processados pelo ciclo 2, evitando repetir o mesmo produto em novas
     * execuções.
     */
    public MoisHotmartProductDtos.HotmartCollectedProductListResponse listCycleTwoCandidatesByWorkspace(
            String workspaceId, int limit) {
        String latestFirstCycleJobId = jdbcTemplate.query(
                        """
                                SELECT s.job_id
                                FROM mois_collection_job_state s
                                WHERE s.workspace_id = ?
                                  AND s.status = 'COLLECTION_EXECUTED'
                                  AND s.payload_json LIKE '%Coleta executada via API da Hotmart%'
                                ORDER BY s.updated_at DESC
                                LIMIT 1
                                """,
                        (rs, rowNum) -> rs.getString("job_id"),
                        workspaceId)
                .stream()
                .findFirst()
                .orElse(null);

        if (latestFirstCycleJobId == null) {
            return new MoisHotmartProductDtos.HotmartCollectedProductListResponse(workspaceId, List.of());
        }

        List<MoisHotmartProductDtos.HotmartCollectedProductResponse> items = jdbcTemplate.query(
                """
                        SELECT r.job_id, r.reference_id, r.product_name, r.product_url, r.hotmart_description, r.producer_name,
                               r.hotmart_image_url, r.hotmart_price, r.sales_page_url, r.hotmart_temperature, r.collected_at
                        FROM mois_collected_reference r
                        LEFT JOIN mois_collected_reference processed
                          ON processed.workspace_id = r.workspace_id
                         AND processed.source = 'HOTMART'
                         AND processed.job_id IN (
                               SELECT s2.job_id
                               FROM mois_collection_job_state s2
                               WHERE s2.workspace_id = ?
                                 AND s2.status = 'COLLECTION_EXECUTED'
                                 AND s2.payload_json LIKE '%Ciclo 2 executado%'
                         )
                         AND (
                               (processed.reference_id IS NOT NULL AND processed.reference_id = r.reference_id)
                            OR (processed.product_url IS NOT NULL AND r.product_url IS NOT NULL AND processed.product_url = r.product_url)
                            OR (processed.sales_page_url IS NOT NULL AND r.sales_page_url IS NOT NULL AND processed.sales_page_url = r.sales_page_url)
                            OR (processed.title IS NOT NULL AND r.title IS NOT NULL AND processed.title = r.title
                                AND COALESCE(processed.producer_name, '') = COALESCE(r.producer_name, ''))
                         )
                        WHERE r.workspace_id = ?
                          AND r.source = 'HOTMART'
                          AND r.job_id = ?
                          AND processed.id IS NULL
                        ORDER BY r.collected_at DESC, r.id DESC
                        LIMIT ?
                        """,
                (rs, rowNum) -> mapHotmartProduct(rs),
                workspaceId, workspaceId, latestFirstCycleJobId, limit
        );

        return new MoisHotmartProductDtos.HotmartCollectedProductListResponse(workspaceId, items);
    }

    /** Converte a linha relacional Hotmart para o DTO usado pela tela e pelo coletor. */
    private MoisHotmartProductDtos.HotmartCollectedProductResponse mapHotmartProduct(java.sql.ResultSet rs)
            throws java.sql.SQLException {
        Timestamp collectedAt = rs.getTimestamp("collected_at");
        return new MoisHotmartProductDtos.HotmartCollectedProductResponse(
                rs.getString("job_id"),
                rs.getString("reference_id"),
                rs.getString("product_name"),
                rs.getString("product_url"),
                rs.getString("hotmart_description"),
                rs.getString("producer_name"),
                rs.getString("hotmart_image_url"),
                rs.getString("hotmart_price"),
                "BRL",
                rs.getString("sales_page_url"),
                rs.getString("sales_page_url"),
                rs.getObject("hotmart_temperature") == null ? null : rs.getDouble("hotmart_temperature"),
                collectedAt == null ? null : collectedAt.toInstant());
    }
}
