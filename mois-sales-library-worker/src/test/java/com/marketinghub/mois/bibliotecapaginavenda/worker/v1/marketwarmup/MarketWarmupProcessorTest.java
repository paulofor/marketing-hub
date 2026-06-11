package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.marketwarmup;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupClaimedJob;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupPlatform;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupSignalType;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Valida o dossiê V1 gerado pelo worker de aquecimento de mercado.
 */
class MarketWarmupProcessorTest {
    /**
     * Garante que fontes públicas e sinais básicos sejam enviados em contrato estruturado ao backend.
     */
    @Test
    void shouldBuildStructuredDossierFromPublicSearchResults() throws Exception {
        MarketWarmupProcessor processor = new MarketWarmupProcessor(new MarketWarmupQueryBuilder(), new FakeSearchClient());
        MarketWarmupClaimedJob job = new MarketWarmupClaimedJob(
                7L,
                70L,
                "workspace-001",
                "https://example.com/oferta",
                "Sono Profundo",
                "Especialista do Sono",
                "Oferta contra insônia",
                "método respiratório",
                "dormir melhor em 7 dias",
                "depoimentos");

        var result = processor.process(job, 3);

        assertThat(result.sources()).isNotEmpty();
        assertThat(result.signals()).extracting(signal -> signal.signalType()).contains(MarketWarmupSignalType.PAIN_EXPLICIT, MarketWarmupSignalType.BUYING_INTENT, MarketWarmupSignalType.CREATOR_AUTHORITY, MarketWarmupSignalType.SOCIAL_PROOF);
        assertThat(result.sources()).extracting(source -> source.platform()).contains(MarketWarmupPlatform.YOUTUBE);
        assertThat(result.summary().recommendation()).isNotNull();
        assertThat(result.summary().scoreTotal()).isPositive();
        assertThat(result.summary().opportunityRecommendation()).contains("A pesquisa pública encontrou evidência");
        assertThat(result.summary().opportunityRecommendation()).doesNotContain("não depende só da página de vendas");
        assertThat(result.summary().nextExperimentSuggestion()).contains("Solicitar ao modelo consolidação final");
    }

    /**
     * Simula uma fonte pública sem dependência de rede para manter o teste determinístico.
     */
    private static class FakeSearchClient implements PublicWebSearchClient {
        /**
         * Retorna resultados com dor, intenção de compra e canal público detectáveis.
         */
        @Override
        public List<PublicSearchResult> search(String query, int limit) throws IOException {
            return List.of(
                    new PublicSearchResult("Insônia: dor e dificuldade para dormir", "https://www.youtube.com/watch?v=abc", "Professora especialista faz live e comentários perguntam se funciona e preço do método.", "<div>raw</div>"),
                    new PublicSearchResult("Review Sono Profundo vale a pena", "https://blog.example.com/review", "Depoimento de alunos mostra resultado e objeção sobre confiança no produto.", "<div>raw</div>"));
        }
    }
}
