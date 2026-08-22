package com.marketinghub.productdiscovery.v1.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/** Responsabilidade: proteger a relevância SQL das ofertas comerciais entregues a Argos. */
class ProductDiscoveryMarketplaceEvidenceServiceTest {

  /** Exige dois termos específicos e ignora público, serviço e canal na consulta dirigida. */
  @Test
  void requiresTwoSpecificTermsBeforeReturningMarketplaceOffers() {
    CapturingJdbcTemplate jdbc = new CapturingJdbcTemplate();
    ProductDiscoveryMarketplaceEvidenceService service =
        new ProductDiscoveryMarketplaceEvidenceService(jdbc);

    service.search(
        "HOTMART", "gerador proposta comercial para prestadores de serviços no WhatsApp", 10);

    assertThat(jdbc.sql).contains(") >= ?");
    assertThat(jdbc.parameters).containsExactly("HOTMART", "%gerador%", "%proposta%", 2, 250);
  }

  /** Responsabilidade: capturar a consulta sem depender de um banco durante o teste unitário. */
  private static class CapturingJdbcTemplate extends JdbcTemplate {
    private String sql;
    private List<Object> parameters;

    /** Registra SQL e parâmetros e devolve uma lista vazia de ofertas. */
    @Override
    public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
      this.sql = sql;
      this.parameters = List.of(args);
      return List.of();
    }
  }
}
