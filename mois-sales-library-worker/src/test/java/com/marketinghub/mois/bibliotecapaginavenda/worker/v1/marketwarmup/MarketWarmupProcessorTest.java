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
     * Garante que uma rede social do produtor só entre no dossiê quando falar de conteúdo semelhante ao produto.
     */
    @Test
    void shouldKeepOnlyProducerSocialProfileWithSimilarProductContent() throws Exception {
        MarketWarmupProcessor processor = new MarketWarmupProcessor(new MarketWarmupQueryBuilder(), new ProducerSocialSearchClient());
        MarketWarmupClaimedJob job = new MarketWarmupClaimedJob(
                8L,
                80L,
                "workspace-001",
                "https://example.com/peptideos",
                "CAP - Certificação Avançada em Peptídeos",
                "Thiago Bechara dos Santos",
                "certificação para prescrição segura de peptídeos",
                "protocolo avançado de peptídeos",
                "dominar peptídeos com segurança clínica",
                "depoimentos de profissionais");

        var result = processor.process(job, 4);

        assertThat(result.sources())
                .extracting(source -> source.sourceUrl())
                .contains("https://www.instagram.com/thiagobechara.peptideos")
                .doesNotContain("https://www.instagram.com/thiagobechara.musica", "https://www.youtube.com/@outrothiagopeptideos");
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
                    new PublicSearchResult("Especialista do Sono ensina Sono Profundo", "https://www.youtube.com/watch?v=abc", "Especialista do Sono é professora especialista e faz live sobre método respiratório para dormir melhor; comentários perguntam se funciona e preço.", "<div>raw</div>"),
                    new PublicSearchResult("Review Sono Profundo vale a pena", "https://blog.example.com/review", "Depoimento de alunos mostra resultado e objeção sobre confiança no produto.", "<div>raw</div>"));
        }
    }

    /**
     * Simula resultados sociais com homônimo e perfil do mesmo produtor em outro assunto.
     */
    private static class ProducerSocialSearchClient implements PublicWebSearchClient {
        /**
         * Retorna fontes sociais para validar o filtro de mesmo produtor e conteúdo semelhante.
         */
        @Override
        public List<PublicSearchResult> search(String query, int limit) throws IOException {
            return List.of(
                    new PublicSearchResult("Thiago Bechara dos Santos | Peptídeos", "https://www.instagram.com/thiagobechara.peptideos", "Conteúdo sobre certificação avançada em peptídeos, protocolo seguro, prescrição clínica e depoimentos de profissionais.", "<div>raw</div>"),
                    new PublicSearchResult("Thiago Bechara dos Santos músico", "https://www.instagram.com/thiagobechara.musica", "Agenda de shows, violão e bastidores de música autoral.", "<div>raw</div>"),
                    new PublicSearchResult("Thiago Peptídeos", "https://www.youtube.com/@outrothiagopeptideos", "Canal sobre peptídeos sem relação com Thiago Bechara dos Santos.", "<div>raw</div>"));
        }
    }
}
