package com.marketinghub.mois.service;

import com.marketinghub.mois.dto.MoisHotmartProductDtos;
import java.sql.Timestamp;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MoisHotmartProductService {

    private final JdbcTemplate jdbcTemplate;

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
                        SELECT job_id, reference_id, product_name, product_url, producer_name,
                               hotmart_image_url, hotmart_price, sales_page_url, hotmart_temperature, collected_at
                        FROM mois_collected_reference
                        WHERE workspace_id = ?
                          AND source = 'HOTMART'
                          AND job_id = ?
                        ORDER BY collected_at DESC
                        LIMIT ?
                        """,
                (rs, rowNum) -> {
                    Timestamp collectedAt = rs.getTimestamp("collected_at");
                    return new MoisHotmartProductDtos.HotmartCollectedProductResponse(
                            rs.getString("job_id"),
                            rs.getString("reference_id"),
                            rs.getString("product_name"),
                            rs.getString("product_url"),
                            rs.getString("producer_name"),
                            rs.getString("hotmart_image_url"),
                            rs.getString("hotmart_price"),
                            "BRL",
                            rs.getString("sales_page_url"),
                            rs.getString("sales_page_url"),
                            rs.getObject("hotmart_temperature") == null ? null : rs.getDouble("hotmart_temperature"),
                            collectedAt == null ? null : collectedAt.toInstant());
                },
                workspaceId, latestJobId, limit
        );

        return new MoisHotmartProductDtos.HotmartCollectedProductListResponse(workspaceId, items);
    }
}
