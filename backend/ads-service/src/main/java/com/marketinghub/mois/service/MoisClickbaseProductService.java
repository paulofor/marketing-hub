package com.marketinghub.mois.service;

import com.marketinghub.mois.dto.MoisClickbaseProductDtos;
import java.sql.Timestamp;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MoisClickbaseProductService {

    private final JdbcTemplate jdbcTemplate;

    public MoisClickbaseProductDtos.ClickbaseCollectedProductListResponse listLatestByWorkspace(String workspaceId, int limit) {
        String latestJobId = jdbcTemplate.query(
                        """
                                SELECT job_id
                                FROM mois_collected_reference
                                WHERE workspace_id = ?
                                  AND source = 'CLICKBANK'
                                ORDER BY updated_at DESC
                                LIMIT 1
                                """,
                        (rs, rowNum) -> rs.getString("job_id"),
                        workspaceId)
                .stream()
                .findFirst()
                .orElse(null);

        if (latestJobId == null) {
            return new MoisClickbaseProductDtos.ClickbaseCollectedProductListResponse(workspaceId, List.of());
        }

        List<MoisClickbaseProductDtos.ClickbaseCollectedProductResponse> items = jdbcTemplate.query(
                """
                        SELECT job_id, reference_id, title, product_name, product_url, producer_name, success_score, collected_at
                        FROM mois_collected_reference
                        WHERE workspace_id = ?
                          AND source = 'CLICKBANK'
                          AND job_id = ?
                        ORDER BY success_score DESC, collected_at DESC
                        LIMIT ?
                        """,
                (rs, rowNum) -> {
                    Timestamp collectedAt = rs.getTimestamp("collected_at");
                    String title = rs.getString("product_name");
                    if (title == null || title.isBlank()) {
                        title = rs.getString("title");
                    }
                    return new MoisClickbaseProductDtos.ClickbaseCollectedProductResponse(
                            rs.getString("job_id"),
                            rs.getString("reference_id"),
                            title,
                            rs.getString("product_url"),
                            rs.getString("producer_name"),
                            rs.getObject("success_score", Integer.class),
                            collectedAt == null ? null : collectedAt.toInstant());
                },
                workspaceId, latestJobId, limit
        );

        return new MoisClickbaseProductDtos.ClickbaseCollectedProductListResponse(workspaceId, items);
    }
}
