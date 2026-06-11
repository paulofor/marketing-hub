package com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * Valida a leitura JDBC dos preços usados pela Biblioteca MOIS.
 */
@ExtendWith(MockitoExtension.class)
class MoisSalesLibraryPricingRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private MoisSalesLibraryPricingRepository repository;

    /**
     * Garante que o repositório consulta a tabela canônica openai_model com código normalizado e original.
     */
    @Test
    void shouldReadBatchPricesFromOpenAiModelTable() throws Exception {
        given(jdbcTemplate.query(
                any(String.class),
                any(RowMapper.class),
                eq("gpt-5.2"),
                eq("GPT-5.2"),
                eq("gpt-5.2")))
                .willAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    ResultSet row = org.mockito.Mockito.mock(ResultSet.class);
                    given(row.getBigDecimal("price_input_batch")).willReturn(new BigDecimal("1.25000"));
                    given(row.getBigDecimal("price_output_batch")).willReturn(new BigDecimal("10.00000"));
                    return List.of(mapper.mapRow(row, 0));
                });

        Optional<MoisSalesLibraryPricingGateway.ModelPricing> pricing = repository.findPricingByModelCode(" GPT-5.2 ");

        assertThat(pricing).isPresent();
        assertThat(pricing.get().priceInputBatch()).isEqualByComparingTo("1.25000");
        assertThat(pricing.get().priceOutputBatch()).isEqualByComparingTo("10.00000");
    }

    /**
     * Garante que código vazio não gera consulta desnecessária ao banco.
     */
    @Test
    void shouldReturnEmptyWhenModelCodeIsBlank() {
        Optional<MoisSalesLibraryPricingGateway.ModelPricing> pricing = repository.findPricingByModelCode(" ");

        assertThat(pricing).isEmpty();
    }
}
