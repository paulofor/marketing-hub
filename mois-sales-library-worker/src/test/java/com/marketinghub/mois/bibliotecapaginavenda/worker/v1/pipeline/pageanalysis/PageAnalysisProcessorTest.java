package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.pageanalysis;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.config.WorkerProperties;
import org.junit.jupiter.api.Test;

class PageAnalysisProcessorTest {

    /** Garante que o resumo visual contabiliza imagens para evitar análise falsa de página sem imagem. */
    @Test
    void shouldSummarizeImageDensityAndProofSignals() {
        PageAnalysisProcessor processor = new PageAnalysisProcessor(properties(), null);
        String summary = processor.summarizeImagesForAnalysis("""
                <html><body>
                  <img src="depoimento-1.jpg" alt="print de depoimento">
                  <img src="antes-depois.jpg" alt="antes e depois">
                  <img src="hero.jpg" alt="capa do produto">
                </body></html>
                """, "https://example.com");

        assertTrue(summary.contains("total_img=3"));
        assertTrue(summary.contains("imagens_com_sinais_de_prova=2"));
        assertTrue(summary.contains("print de depoimento"));
    }

    /** Cria propriedades mínimas para instanciar o processador sem executar integrações externas. */
    private WorkerProperties properties() {
        return new WorkerProperties(
                "http://localhost",
                "workspace",
                "HOTMART",
                "HOTMART",
                "HOTMART",
                "HOTMART",
                1000,
                1000,
                1000,
                10,
                false,
                false,
                1000,
                "worker",
                "https://duckduckgo.com/html/",
                10,
                "Mozilla/5.0",
                1000
        );
    }
}
