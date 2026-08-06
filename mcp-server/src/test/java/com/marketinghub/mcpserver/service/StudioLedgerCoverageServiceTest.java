package com.marketinghub.mcpserver.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

/** Responsabilidade: validar o diagnóstico de cobertura financeira do Estúdio. */
class StudioLedgerCoverageServiceTest {

  /** Garante que fontes com collations legadas distintas sejam normalizadas antes da comparação. */
  @Test
  void normalizesLegacyCollationsInCoverageQuery() {
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of());

    var result = new StudioLedgerCoverageService(jdbcTemplate).diagnose();

    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(jdbcTemplate).queryForList(sql.capture());
    assertThat(sql.getValue())
        .contains("CONVERT(j.job_type USING utf8mb4) COLLATE utf8mb4_unicode_ci")
        .contains("CONVERT(j.id USING utf8mb4) COLLATE utf8mb4_unicode_ci")
        .contains("CONVERT(a.id USING utf8mb4) COLLATE utf8mb4_unicode_ci")
        .contains("CONVERT(request.job_id USING utf8mb4) COLLATE utf8mb4_unicode_ci");
    assertThat(result.get("status")).isEqualTo("NO_ATTEMPTS_FOUND");
  }
}
