package com.marketinghub.mcpserver.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Responsabilidade: diagnosticar a cobertura financeira das tentativas do Estúdio. */
@Service
public class StudioLedgerCoverageService {

    private final JdbcTemplate jdbcTemplate;

    /** Inicializa o diagnóstico com acesso somente leitura ao banco principal. */
    public StudioLedgerCoverageService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Consolida tentativas, ausências e qualidade de custo por tipo de origem e provedor. */
    public Map<String, Object> diagnose() {
        List<Map<String, Object>> coverage = jdbcTemplate.queryForList("""
                SELECT source_type,
                       asset_type,
                       provider,
                       COUNT(*) AS attempts,
                       SUM(CASE WHEN ledger_id IS NOT NULL THEN 1 ELSE 0 END) AS ledger_entries,
                       SUM(CASE WHEN ledger_id IS NULL THEN 1 ELSE 0 END) AS missing_entries,
                       SUM(CASE WHEN ledger_id IS NOT NULL
                                     AND provider_cost_usd IS NULL
                                     AND estimated_cost_usd IS NULL THEN 1 ELSE 0 END) AS unknown_cost_entries,
                       SUM(CASE WHEN ledger_id IS NOT NULL
                                     AND commercial_plan_id IS NULL THEN 1 ELSE 0 END) AS unassigned_entries,
                       SUM(COALESCE(provider_cost_usd, estimated_cost_usd, 0)) AS known_cost_usd
                FROM (
                    SELECT 'SALES_VIDEO_JOB' AS source_type,
                           j.job_type AS asset_type,
                           COALESCE(NULLIF(j.provider_name, ''), j.provider_family, 'UNKNOWN') AS provider,
                           ledger.id AS ledger_id,
                           ledger.provider_cost_usd,
                           ledger.estimated_cost_usd,
                           ledger.commercial_plan_id
                    FROM sales_video_job j
                    LEFT JOIN studio_cost_ledger_entry ledger
                      ON ledger.source_type = 'SALES_VIDEO_JOB'
                     AND ledger.source_id = CONCAT('', j.id)
                    UNION ALL
                    SELECT 'MEDIA_ASSET',
                           a.type,
                           COALESCE(a.provider, 'UNKNOWN'),
                           ledger.id,
                           ledger.provider_cost_usd,
                           ledger.estimated_cost_usd,
                           ledger.commercial_plan_id
                    FROM asset a
                    LEFT JOIN studio_cost_ledger_entry ledger
                      ON ledger.source_type = 'MEDIA_ASSET'
                     AND ledger.source_id = CONCAT('', a.id)
                    WHERE a.type IN ('AUDIO', 'VIDEO')
                      AND a.provider IN ('SYNTHESIA', 'HEYGEN', 'ELEVENLABS', 'RUNWAY')
                    UNION ALL
                    SELECT 'IMAGE_GENERATION_REQUEST',
                           'IMAGE',
                           'OPENAI',
                           ledger.id,
                           ledger.provider_cost_usd,
                           ledger.estimated_cost_usd,
                           ledger.commercial_plan_id
                    FROM image_generation_request request
                    LEFT JOIN studio_cost_ledger_entry ledger
                      ON ledger.source_type = 'IMAGE_GENERATION_REQUEST'
                     AND ledger.source_id = request.job_id
                ) attempts
                GROUP BY source_type, asset_type, provider
                ORDER BY missing_entries DESC, unknown_cost_entries DESC, source_type, asset_type, provider
                """);

        long attempts = sum(coverage, "ATTEMPTS");
        long ledgerEntries = sum(coverage, "LEDGER_ENTRIES");
        long missingEntries = sum(coverage, "MISSING_ENTRIES");
        long unknownCostEntries = sum(coverage, "UNKNOWN_COST_ENTRIES");
        long unassignedEntries = sum(coverage, "UNASSIGNED_ENTRIES");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", coverageStatus(attempts, missingEntries, unknownCostEntries, unassignedEntries));
        result.put("attempts", attempts);
        result.put("ledgerEntries", ledgerEntries);
        result.put("missingEntries", missingEntries);
        result.put("unknownCostEntries", unknownCostEntries);
        result.put("unassignedEntries", unassignedEntries);
        result.put("groups", coverage);
        result.put("interpretation", "Custos ausentes ou desconhecidos nunca representam custo zero confirmado.");
        return result;
    }

    /** Soma uma métrica numérica retornada pelos agrupamentos do diagnóstico. */
    private long sum(List<Map<String, Object>> rows, String column) {
        return rows.stream()
                .map(row -> row.get(column))
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .mapToLong(Number::longValue)
                .sum();
    }

    /** Classifica a cobertura sem confundir ledger vazio com custo zero. */
    private String coverageStatus(long attempts, long missing, long unknown, long unassigned) {
        if (attempts == 0) {
            return "NO_ATTEMPTS_FOUND";
        }
        if (missing > 0) {
            return "MISSING_LEDGER_ENTRIES";
        }
        if (unknown > 0) {
            return "UNKNOWN_COSTS";
        }
        if (unassigned > 0) {
            return "UNASSIGNED_COSTS";
        }
        return "COMPLETE";
    }
}
