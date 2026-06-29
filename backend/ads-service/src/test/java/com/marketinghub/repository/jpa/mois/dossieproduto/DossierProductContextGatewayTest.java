package com.marketinghub.repository.jpa.mois.dossieproduto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

/** Valida a leitura do contexto coletado usado pelo dossiê MOIS v1. */
class DossierProductContextGatewayTest {

    /** Garante que o gateway leia HTML da tabela operacional atual de jobs da página de venda. */
    @Test
    void findContextUsesCurrentSalesPageJobExecutionCaptureTable() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        when(jdbcTemplate.query(
                        sqlCaptor.capture(),
                        org.mockito.ArgumentMatchers.<ResultSetExtractor<Optional<DossierProductContextGateway.DossierProductContext>>>any(),
                        eq(280L)))
                .thenReturn(Optional.empty());
        DossierProductContextGateway gateway = new DossierProductContextGateway(jdbcTemplate);

        gateway.findContext(280L);

        String sql = sqlCaptor.getValue();
        assertThat(sql).contains("FROM mois_sales_page_job_execution cap2");
        assertThat(sql).contains("cap2.stage = 'CAPTURE'");
        assertThat(sql).contains("cap2.status IN ('CAPTURED', 'DUPLICATE')");
        assertThat(sql).doesNotContain("mois_sales_page_capture");
    }
}
