package com.marketinghub.oprm.market.estabelecimento.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.oprm.market.estabelecimento.dto.OprmEstabelecimentoCnaeRaizBatchRequestDto;
import com.marketinghub.oprm.market.estabelecimento.dto.OprmEstabelecimentoCnaeRaizUpsertDto;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Testa a persistência dedicada dos vínculos CNPJ raiz/CNAE/email de estabelecimentos OPRM.
 */
class OprmEstabelecimentoCnaeRaizServiceTest {

    private JdbcTemplate jdbcTemplate;
    private OprmEstabelecimentoCnaeRaizService service;

    /**
     * Prepara um banco H2 em modo MySQL com a tabela operacional usada pelo serviço.
     */
    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:oprm_estabelecimento_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("DROP TABLE IF EXISTS oprm_estabelecimento_cnae_raiz");
        jdbcTemplate.execute("""
                CREATE TABLE oprm_estabelecimento_cnae_raiz (
                  cnpj_raiz VARCHAR(8) NOT NULL,
                  cnae_code VARCHAR(7) NOT NULL,
                  email VARCHAR(254),
                  is_mei TINYINT(1) NOT NULL DEFAULT 0,
                  is_simples TINYINT(1) NOT NULL DEFAULT 0,
                  updated_at DATETIME NOT NULL,
                  PRIMARY KEY (cnpj_raiz, cnae_code)
                )
                """);
        service = new OprmEstabelecimentoCnaeRaizService(jdbcTemplate);
    }

    /**
     * Garante que o lote é inserido e atualizado sem apagar flags operacionais já existentes.
     */
    @Test
    void shouldUpsertBatchAndPreserveExistingFlags() {
        service.upsertBatch(new OprmEstabelecimentoCnaeRaizBatchRequestDto(List.of(
                new OprmEstabelecimentoCnaeRaizUpsertDto("12.345.678", "6201501", "CONTATO@EXEMPLO.COM")
        )));
        jdbcTemplate.update("UPDATE oprm_estabelecimento_cnae_raiz SET is_mei = 1, is_simples = 1 WHERE cnpj_raiz = ? AND cnae_code = ?",
                "12345678", "6201501");

        var response = service.upsertBatch(new OprmEstabelecimentoCnaeRaizBatchRequestDto(List.of(
                new OprmEstabelecimentoCnaeRaizUpsertDto("12345678", "6201501", "novo@exemplo.com")
        )));

        assertThat(response.received()).isEqualTo(1);
        assertThat(response.persisted()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT email FROM oprm_estabelecimento_cnae_raiz WHERE cnpj_raiz = ? AND cnae_code = ?",
                String.class, "12345678", "6201501")).isEqualTo("novo@exemplo.com");
        assertThat(jdbcTemplate.queryForObject("SELECT is_mei FROM oprm_estabelecimento_cnae_raiz WHERE cnpj_raiz = ? AND cnae_code = ?",
                Integer.class, "12345678", "6201501")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT is_simples FROM oprm_estabelecimento_cnae_raiz WHERE cnpj_raiz = ? AND cnae_code = ?",
                Integer.class, "12345678", "6201501")).isEqualTo(1);
    }
}
