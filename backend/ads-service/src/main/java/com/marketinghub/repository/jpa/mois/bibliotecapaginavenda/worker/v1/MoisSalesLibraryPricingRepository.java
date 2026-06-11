package com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * Executa a leitura JDBC dos preços de modelos OpenAI usados pela Biblioteca MOIS.
 */
@Repository
@RequiredArgsConstructor
public class MoisSalesLibraryPricingRepository implements MoisSalesLibraryPricingGateway {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Busca o preço batch por código normalizado e pelo código original para preservar compatibilidade operacional.
     */
    @Override
    public Optional<ModelPricing> findPricingByModelCode(String modelCode) {
        if (!StringUtils.hasText(modelCode)) {
            return Optional.empty();
        }
        String originalCode = modelCode.trim();
        String normalizedCode = originalCode.toLowerCase(Locale.ROOT);
        List<ModelPricing> rows = jdbcTemplate.query("""
                SELECT price_input_batch, price_output_batch
                FROM openai_model
                WHERE code = ? OR code = ?
                ORDER BY CASE WHEN code = ? THEN 0 ELSE 1 END
                LIMIT 1
                """, (rs, rowNum) -> new ModelPricing(
                rs.getBigDecimal("price_input_batch"),
                rs.getBigDecimal("price_output_batch")), normalizedCode, originalCode, normalizedCode);
        return rows.stream().findFirst();
    }
}
