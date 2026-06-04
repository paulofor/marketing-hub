package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.schema;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Valida o contrato estrutural dos changelogs Liquibase do modelo consolidado de páginas de venda do MOIS.
 */
class MoisSalesPageSchemaChangelogTest {

    private static final Path CHANGELOG_ROOT = Path.of("src/main/resources/db/changelog");
    private static final Path SALES_PAGE_CHANGELOG = CHANGELOG_ROOT.resolve(
            "changesets/2026-06-04-mois-sales-page.yaml");
    private static final Path JOB_EXECUTION_CHANGELOG = CHANGELOG_ROOT.resolve(
            "changesets/2026-06-04-mois-sales-page-job-execution.yaml");
    private static final Path MASTER_CHANGELOG = CHANGELOG_ROOT.resolve("db.changelog-master.yaml");

    /**
     * Garante que a tabela de estado atual da página contém campos e índices canônicos da fase 1.
     */
    @Test
    void salesPageChangelogDefinesCurrentStateTableAndOperationalIndexes() throws IOException {
        String changelog = read(SALES_PAGE_CHANGELOG);

        assertThat(changelog)
                .contains("CREATE TABLE mois_sales_page")
                .contains("workspace_id VARCHAR(120) NOT NULL")
                .contains("url_canonical VARCHAR(1024) NOT NULL")
                .contains("current_stage VARCHAR(40) NOT NULL")
                .contains("current_status VARCHAR(40) NOT NULL")
                .contains("last_job_execution_id BIGINT NULL")
                .contains("UNIQUE KEY uk_mois_sales_page_workspace_url (workspace_id, url_canonical(512))")
                .contains("KEY idx_mois_sales_page_source_status (workspace_id, source, current_status, updated_at)")
                .contains("KEY idx_mois_sales_page_stage_status (workspace_id, current_stage, current_status, updated_at)")
                .contains("KEY idx_mois_sales_page_score (workspace_id, score_total)")
                .contains(
                        "KEY idx_mois_sales_page_source_reference (workspace_id, source, source_job_id, source_reference_id)");
    }

    /**
     * Garante que a tabela de histórico guarda auditoria, payloads e índices operacionais por página/job.
     */
    @Test
    void jobExecutionChangelogDefinesAuditTableAndOperationalIndexes() throws IOException {
        String changelog = read(JOB_EXECUTION_CHANGELOG);

        assertThat(changelog)
                .contains("CREATE TABLE mois_sales_page_job_execution")
                .contains("sales_page_id BIGINT NOT NULL")
                .contains("job_type VARCHAR(40) NOT NULL")
                .contains("stage VARCHAR(40) NOT NULL")
                .contains("status VARCHAR(40) NOT NULL")
                .contains("raw_html LONGTEXT NULL")
                .contains("request_payload_json LONGTEXT NULL")
                .contains("response_payload_json LONGTEXT NULL")
                .contains("CONSTRAINT fk_mois_sales_page_job_execution_page")
                .contains("KEY idx_mois_sales_page_job_page_created (sales_page_id, created_at)")
                .contains("KEY idx_mois_sales_page_job_status (workspace_id, stage, status, updated_at)")
                .contains("KEY idx_mois_sales_page_job_type_status (workspace_id, job_type, status, updated_at)");
    }

    /**
     * Garante que o changelog master aplica a tabela consolidada antes do histórico que depende dela.
     */
    @Test
    void masterChangelogIncludesSalesPageBeforeJobExecution() throws IOException {
        String master = read(MASTER_CHANGELOG);

        assertThat(master).contains("changesets/2026-06-04-mois-sales-page.yaml");
        assertThat(master).contains("changesets/2026-06-04-mois-sales-page-job-execution.yaml");
        assertThat(master.indexOf("changesets/2026-06-04-mois-sales-page.yaml"))
                .isLessThan(master.indexOf("changesets/2026-06-04-mois-sales-page-job-execution.yaml"));
    }

    /**
     * Lê um arquivo de changelog como texto UTF-8 para validações estruturais de contrato.
     */
    private String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
