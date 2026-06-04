package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.schema;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Protege o congelamento das tabelas legadas da Biblioteca de Páginas de Vendas.
 */
class MoisSalesLibraryLegacyFreezeTest {

    private static final List<String> LEGACY_TABLES = List.of(
            "mois_sales_library_url_ingest",
            "mois_sales_library_processing_job",
            "mois_sales_library_page_analysis",
            "mois_sales_library_page_snapshot",
            "mois_sales_library_snapshot_artifact",
            "mois_collected_reference_html_capture"
    );

    /**
     * Garante que o código Java principal não faça DML/DDL nas tabelas legadas congeladas.
     */
    @Test
    void mainJavaSourcesShouldNotWriteToFrozenLegacyTables() throws IOException {
        Path mainJava = Path.of("src/main/java");

        List<String> violations;
        try (Stream<Path> paths = Files.walk(mainJava)) {
            violations = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(MoisSalesLibraryLegacyFreezeTest::findFrozenLegacyWrites)
                    .toList();
        }

        assertThat(violations)
                .as("Tabelas legadas da Biblioteca de Páginas de Vendas estão congeladas "
                        + "e só podem ser lidas para auditoria/backfill")
                .isEmpty();
    }

    /**
     * Localiza comandos de escrita/DDL sobre tabelas legadas em um arquivo Java.
     */
    private static Stream<String> findFrozenLegacyWrites(Path path) {
        try {
            String content = Files.readString(path).toLowerCase(Locale.ROOT);
            return LEGACY_TABLES.stream()
                    .filter(table -> writePatternFor(table).matcher(content).find())
                    .map(table -> path + " escreve na tabela legada congelada " + table);
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível ler " + path, ex);
        }
    }

    /**
     * Cria o padrão que detecta comandos de alteração de dados/estrutura contra uma tabela legada.
     */
    private static Pattern writePatternFor(String table) {
        String normalizedTable = Pattern.quote(table);
        return Pattern.compile("(?s)\\b(insert\\s+into|update|delete\\s+from|merge\\s+into|truncate\\s+table|"
                + "alter\\s+table|drop\\s+table)\\s+`?" + normalizedTable + "`?\\b");
    }
}
