package com.marketinghub.pipelines.nichocnae.v3.sourcesearcher;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.pipelines.nichocnae.v3.core.StageContext;
import com.marketinghub.pipelines.nichocnae.v3.core.StageResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Valida a decisão de avanço da etapa source-searcher no pipeline NichoCNAE v3. */
class SourceSearcherProcessorTest {
    /** Bloqueia avanço quando existem apenas queries planejadas, mas nenhuma fonte real auditável. */
    @Test
    void shouldBlockWithoutRealSources() {
        StageResult result = new SourceSearcherProcessor((query, limit) -> List.of()).process(new StageContext("job", "5", Map.of(
                "plannedQueries", List.of(Map.of("query", "gerente loja rotina estoque")))));

        assertThat(result.status()).isEqualTo("FONTES_NAO_COLETADAS");
        assertThat(result.output()).containsEntry("nextStageCode", "");
        assertThat(result.output()).containsEntry("blocked", true);
        assertThat(result.output()).containsEntry("recommendedCorrectionStage", "source-searcher");
        assertThat((List<?>) result.output().get("foundSources")).isEmpty();
    }

    /** Permite avanço somente quando a entrada traz fontes reais para o source-fetcher coletar. */
    @Test
    void shouldAdvanceWithRealFoundSources() {
        StageResult result = new SourceSearcherProcessor().process(new StageContext("job", "5", Map.of(
                "plannedQueries", List.of(Map.of("query", "gerente loja rotina estoque")),
                "foundSources", List.of(Map.of(
                        "url", "https://exemplo.com.br/rotina-loja",
                        "title", "Rotina de MEI autônomo em loja no Brasil",
                        "snippet", "Profissional autônomo acompanha estoque, agenda, atendimento, clientes e cobrança diariamente.")))));

        assertThat(result.status()).isEqualTo("FONTES_ENCONTRADAS");
        assertThat(result.output()).containsEntry("nextStageCode", "source-fetcher");
        assertThat(result.output()).containsEntry("blocked", false);
        assertThat(result.output()).containsEntry("foundSourceCount", 1);
        assertThat((List<?>) result.output().get("foundSources")).hasSize(1);
        assertThat((List<?>) result.output().get("selectedSources")).hasSize(1);
    }

    /** Busca fontes públicas a partir das queries e entrega somente fontes qualificadas para a próxima etapa. */
    @Test
    void shouldSearchAndDeliverQualifiedRoutineSources() {
        SourceSearchClient searchClient = (query, limit) -> List.of(
                new SourceSearchResult(
                        "Rotina de profissional MEI no Brasil",
                        "https://rotina.example.com.br/mei",
                        "Profissional autônomo relata agenda, atendimento, clientes, cobrança e retrabalho manual.",
                        "TEST_PROVIDER",
                        "<item />"),
                new SourceSearchResult(
                        "Sistema para automatizar vendas",
                        "https://software.example.com/app",
                        "Contrate plataforma, planos, CRM, automação e teste grátis.",
                        "TEST_PROVIDER",
                        "<item />"));

        StageResult result = new SourceSearcherProcessor(searchClient).process(new StageContext("job", "5", Map.of(
                "plannedQueries", List.of(Map.of(
                        "query", "manicure agenda clientes",
                        "intent", "TAREFA_DIARIA",
                        "objective", "Validar rotina de atendimento")))));

        assertThat(result.status()).isEqualTo("FONTES_ENCONTRADAS");
        assertThat(result.output()).containsEntry("nextStageCode", "source-fetcher");
        List<?> selectedSources = (List<?>) result.output().get("selectedSources");
        assertThat(selectedSources).hasSize(1);
        Map<?, ?> source = (Map<?, ?>) selectedSources.getFirst();
        assertThat(source.get("sourceIntent")).isEqualTo("ROUTINE_EVIDENCE");
        assertThat(source.get("searchProvider")).isEqualTo("TEST_PROVIDER");
        assertThat(source.containsKey("routineEvidenceScore")).isTrue();
        assertThat(source.containsKey("brazilRelevanceScore")).isTrue();
    }

    /** Usa variações curtas para encontrar fontes mesmo quando a query planejada vem longa e ruidosa. */
    @Test
    void shouldSearchWithSimplifiedQueryVariants() {
        SourceSearchClient searchClient = (query, limit) -> {
            if (query.toLowerCase().startsWith("atendimento") && query.toLowerCase().contains("provador")) {
                return List.of(new SourceSearchResult(
                        "Rotina de atendimento em loja de roupas no Brasil",
                        "https://varejo.example.com.br/rotina-loja-roupas",
                        "Dono MEI relata rotina de atendimento, provador, clientes, estoque, caixa e reposição manual.",
                        "TEST_PROVIDER",
                        "<item />"));
            }
            return List.of();
        };

        StageResult result = new SourceSearcherProcessor(searchClient).process(new StageContext("job", "5", Map.of(
                "plannedQueries", List.of(Map.of(
                        "query", "Dono-operador de loja física de roupas e acessórios (MEI/pequeno varejo) MEI autônomo dono operador Atendimento presencial e suporte no provador rotina problema frequência",
                        "intent", "TAREFA_DIARIA",
                        "objective", "Validar tarefa recorrente e frequência operacional")))));

        assertThat(result.status()).isEqualTo("FONTES_ENCONTRADAS");
        List<?> attempts = (List<?>) result.output().get("searchAttempts");
        Map<?, ?> attempt = (Map<?, ?>) attempts.getFirst();
        assertThat((List<?>) attempt.get("queryVariants")).anySatisfy(query ->
                assertThat(String.valueOf(query)).startsWith("atendimento"));
    }

    /** Usa termos operacionais de pedidos e trocas para evitar buscas genéricas que retornam calculadoras/dicionários. */
    @Test
    void shouldSearchOperationalCommerceTermsFromValidationQuery() {
        SourceSearchClient searchClient = (query, limit) -> List.of(new SourceSearchResult(
                "Rotina de MEI no Brasil com atendimento por WhatsApp",
                "https://varejo.example.com.br/whatsapp-pedidos",
                "MEI relata pedidos, reservas, trocas, clientes, atendimento por WhatsApp e controle manual de estoque.",
                "TEST_PROVIDER",
                "<item />"));

        StageResult result = new SourceSearcherProcessor(searchClient).process(new StageContext("job", "5", Map.of(
                "plannedQueries", List.of(Map.of(
                        "query", "dificuldade de Validar o volume típico de pedidos dia como a persona controla reservas e trocas devoluções à distância e quais canais digitais predominam WhatsApp Instagram Brasil",
                        "intent", "DOR_OPERACIONAL",
                        "objective", "Confirmar dor, esforço manual e consequência prática")))));

        assertThat(result.status()).isEqualTo("FONTES_ENCONTRADAS");
        List<?> attempts = (List<?>) result.output().get("searchAttempts");
        Map<?, ?> attempt = (Map<?, ?>) attempts.getFirst();
        assertThat((List<?>) attempt.get("queryVariants")).anySatisfy(query ->
                assertThat(String.valueOf(query)).startsWith("pedidos reservas trocas"));
    }

    /** Usa fallback de domínio quando queries planejadas atraem apenas resultados genéricos ou ambíguos. */
    @Test
    void shouldUseDomainFallbackWhenPlannedQueriesReturnOnlyNoise() {
        SourceSearchClient searchClient = (query, limit) -> {
            if (query.contains("relato rotina atendimento cliente Brasil")) {
                return List.of(new SourceSearchResult(
                        "Relato de rotina de loja de roupas com caixa e estoque",
                        "https://varejo.example.com.br/relato-loja-roupas",
                        "Dono MEI relata atendimento a clientes, pedidos por WhatsApp, trocas, cobrança, caixa e controle manual de estoque.",
                        "TEST_PROVIDER",
                        "<item />"));
            }
            return List.of(new SourceSearchResult(
                    "Controle para videogame em oferta",
                    "https://ecommerce.example.com.br/controle",
                    "Controle sem fio para console com promoção e entrega.",
                    "TEST_PROVIDER",
                    "<item />"));
        };

        StageResult result = new SourceSearcherProcessor(searchClient).process(new StageContext("job", "5", Map.of(
                "plannedQueries", List.of(Map.of(
                        "query", "Controle diário de fluxo de caixa com registro de recebimentos e pagamentos loja roupas",
                        "intent", "SINAL_DE_COMPRA",
                        "objective", "Encontrar evidência de organização de caixa e estoque")))));

        assertThat(result.status()).isEqualTo("FONTES_ENCONTRADAS");
        List<?> selectedSources = (List<?>) result.output().get("selectedSources");
        Map<?, ?> source = (Map<?, ?>) selectedSources.getFirst();
        assertThat(source.get("url")).isEqualTo("https://varejo.example.com.br/relato-loja-roupas");
        List<?> attempts = (List<?>) result.output().get("searchAttempts");
        assertThat(attempts).anySatisfy(attempt ->
                assertThat(String.valueOf(((Map<?, ?>) attempt).get("intent"))).isEqualTo("DOMAIN_FALLBACK"));
    }

    /** Gera buscas focadas em reclamações e aceita fonte pública com atrito real de pedido, troca e entrega. */
    @Test
    void shouldUseComplaintQueriesAndAcceptRealCustomerFrictionSources() {
        SourceSearchClient searchClient = (query, limit) -> {
            if (query.startsWith("site:reclameaqui.com.br")) {
                return List.of(new SourceSearchResult(
                        "Reclamação sobre troca e entrega de roupa comprada pelo WhatsApp",
                        "https://www.reclameaqui.com.br/loja-roupas/troca-entrega-whatsapp",
                        "Cliente relata pedido, troca, entrega, atendimento por WhatsApp e reserva de peça em loja de roupas.",
                        "TEST_PROVIDER",
                        "<item />"));
            }
            return List.of();
        };

        StageResult result = new SourceSearcherProcessor(searchClient).process(new StageContext("job", "5", Map.of(
                "plannedQueries", List.of(Map.of(
                        "query", "Dono-operador de loja de roupas atendimento estoque pedidos reservas trocas entrega WhatsApp Instagram Brasil",
                        "intent", "TAREFA_DIARIA",
                        "objective", "Confirmar atritos reais de pedido, troca e entrega")))));

        assertThat(result.status()).isEqualTo("FONTES_ENCONTRADAS");
        List<?> attempts = (List<?>) result.output().get("searchAttempts");
        Map<?, ?> attempt = (Map<?, ?>) attempts.getFirst();
        assertThat((List<?>) attempt.get("queryVariants")).anySatisfy(query ->
                assertThat(String.valueOf(query)).startsWith("site:reclameaqui.com.br"));
        List<?> selectedSources = (List<?>) result.output().get("selectedSources");
        Map<?, ?> source = (Map<?, ?>) selectedSources.getFirst();
        assertThat(source.get("sourceIntent")).isEqualTo("COMMUNITY_OR_QUESTION_EVIDENCE");
    }

    /** Rejeita dicionários comuns de rotina que antes consumiam resultados úteis das buscas públicas. */
    @Test
    void shouldRejectCommonDictionaryRoutineResults() {
        SourceSearchClient searchClient = (query, limit) -> List.of(new SourceSearchResult(
                "Rotina - Dicio, Dicionário Online de Português",
                "https://www.dicio.com.br/rotina/",
                "Significado de rotina: sequência dos procedimentos e costumes habituais.",
                "TEST_PROVIDER",
                "<item />"));

        StageResult result = new SourceSearcherProcessor(searchClient).process(new StageContext("job", "5", Map.of(
                "plannedQueries", List.of(Map.of("query", "rotina loja roupas atendimento")))));

        assertThat(result.status()).isEqualTo("FONTES_NAO_COLETADAS");
        List<?> attempts = (List<?>) result.output().get("searchAttempts");
        Map<?, ?> attempt = (Map<?, ?>) attempts.getFirst();
        assertThat((List<?>) attempt.get("rejectedSources")).anySatisfy(rejected -> {
            Map<?, ?> source = (Map<?, ?>) rejected;
            assertThat(source.get("sourceIntent")).isEqualTo("IRRELEVANT_UTILITY_RISK");
            assertThat(source.get("irrelevantUtilityRisk")).isEqualTo(true);
        });
    }

    /** Rejeita resultados utilitários irrelevantes que aparecem em buscas ruidosas de cobrança ou pagamento. */
    @Test
    void shouldRejectIrrelevantUtilityResults() {
        SourceSearchClient searchClient = (query, limit) -> List.of(new SourceSearchResult(
                "Calculator.net: Free Online Calculators",
                "https://www.calculator.net/",
                "Online calculator for quick calculations, finance and math.",
                "TEST_PROVIDER",
                "<item />"));

        StageResult result = new SourceSearcherProcessor(searchClient).process(new StageContext("job", "5", Map.of(
                "plannedQueries", List.of(Map.of("query", "pagamentos comprovantes cobrança")))));

        assertThat(result.status()).isEqualTo("FONTES_NAO_COLETADAS");
        List<?> attempts = (List<?>) result.output().get("searchAttempts");
        Map<?, ?> attempt = (Map<?, ?>) attempts.getFirst();
        assertThat((List<?>) attempt.get("rejectedSources")).anySatisfy(rejected -> {
            Map<?, ?> source = (Map<?, ?>) rejected;
            assertThat(source.get("sourceIntent")).isEqualTo("IRRELEVANT_UTILITY_RISK");
            assertThat(source.get("irrelevantUtilityRisk")).isEqualTo(true);
        });
    }

    /** Registra fontes rejeitadas com motivo para diagnosticar bloqueios sem depender apenas do log técnico. */
    @Test
    void shouldAuditRejectedSourcesWithReason() {
        SourceSearchClient searchClient = (query, limit) -> List.of(new SourceSearchResult(
                "Conteúdo genérico de moda",
                "",
                "Texto sem fonte rastreável para rotina operacional.",
                "TEST_PROVIDER",
                "<item />"));

        StageResult result = new SourceSearcherProcessor(searchClient).process(new StageContext("job", "5", Map.of(
                "plannedQueries", List.of(Map.of("query", "loja roupas estoque")))));

        List<?> attempts = (List<?>) result.output().get("searchAttempts");
        Map<?, ?> attempt = (Map<?, ?>) attempts.getFirst();
        assertThat((List<?>) attempt.get("rejectedSources")).anySatisfy(rejected ->
                assertThat(((Map<?, ?>) rejected).get("rejectionReason")).isEqualTo("URL_AUSENTE"));
    }


    /** Bloqueia fonte pública rastreável, mas fraca, para impedir que o gate receba ruído como evidência. */
    @Test
    void shouldBlockWeakNonCommercialSourcesInsteadOfAdvancingToGate() {
        SourceSearchClient searchClient = (query, limit) -> List.of(new SourceSearchResult(
                "Atendimento em loja de roupas",
                "https://varejo.example.com.br/atendimento-loja",
                "Loja de roupas relata atendimento ao cliente e organização manual.",
                "TEST_PROVIDER",
                "<item />"));

        StageResult result = new SourceSearcherProcessor(searchClient).process(new StageContext("job", "5", Map.of(
                "plannedQueries", List.of(Map.of("query", "loja roupas atendimento pedidos")))));

        assertThat(result.status()).isEqualTo("FONTES_NAO_COLETADAS");
        assertThat(result.output()).containsEntry("nextStageCode", "");
        List<?> selectedSources = (List<?>) result.output().get("selectedSources");
        assertThat(selectedSources).isEmpty();
    }

    /** Bloqueia fonte de rotina genérica fora do domínio do CNAE para não gerar tarefa inútil nas etapas seguintes. */
    @Test
    void shouldBlockOffDomainCurriculumRoutineSource() {
        SourceSearchClient searchClient = (query, limit) -> List.of(new SourceSearchResult(
                "ROTINAS PEDAGÓGICAS RPE 2026 – Currículo do Espírito Santo",
                "https://curriculo.sedu.es.gov.br/curriculo/rpe/",
                "Gostaria de esclarecer uma dúvida sobre a rotina do 2º trimestre de Língua Portuguesa.",
                "TEST_PROVIDER",
                "<item />"));

        StageResult result = new SourceSearcherProcessor(searchClient).process(new StageContext("job", "5", Map.of(
                "plannedQueries", List.of(Map.of("query", "loja roupas atendimento pedidos whatsapp")))));

        assertThat(result.status()).isEqualTo("FONTES_NAO_COLETADAS");
        assertThat(result.output()).containsEntry("nextStageCode", "");
        assertThat((List<?>) result.output().get("selectedSources")).isEmpty();
    }

    /** Não trata a palavra vendas isolada como contaminação, pois ela faz parte da rotina de loja. */
    @Test
    void shouldNotRejectRoutineRetailSourceOnlyBecauseItMentionsSales() {
        SourceSearchClient searchClient = (query, limit) -> List.of(new SourceSearchResult(
                "Rotina de vendas e atendimento em loja no Brasil",
                "https://varejo.example.com.br/rotina-vendas-loja",
                "MEI relata rotina de vendas, atendimento a clientes, pedidos, trocas, estoque, cobrança e WhatsApp.",
                "TEST_PROVIDER",
                "<item />"));

        StageResult result = new SourceSearcherProcessor(searchClient).process(new StageContext("job", "5", Map.of(
                "plannedQueries", List.of(Map.of("query", "loja roupas vendas atendimento clientes")))));

        assertThat(result.status()).isEqualTo("FONTES_ENCONTRADAS");
        List<?> selectedSources = (List<?>) result.output().get("selectedSources");
        Map<?, ?> source = (Map<?, ?>) selectedSources.getFirst();
        assertThat(source.get("sourceIntent")).isEqualTo("ROUTINE_EVIDENCE");
        assertThat(source.get("commercialPageRisk")).isEqualTo(false);
    }

    /** Bloqueia avanço quando a busca retorna apenas fonte comercial ou solução contaminada. */
    @Test
    void shouldBlockCommercialSolutionSources() {
        SourceSearchClient searchClient = (query, limit) -> List.of(new SourceSearchResult(
                "Sistema de agenda para vender mais",
                "https://software.example.com.br/precos",
                "Contrate software, aplicativo, CRM, automação, planos e teste grátis.",
                "TEST_PROVIDER",
                "<item />"));

        StageResult result = new SourceSearcherProcessor(searchClient).process(new StageContext("job", "5", Map.of(
                "plannedQueries", List.of(Map.of("query", "manicure agenda clientes")))));

        assertThat(result.status()).isEqualTo("FONTES_NAO_COLETADAS");
        assertThat(result.output()).containsEntry("nextStageCode", "");
        assertThat((List<?>) result.output().get("selectedSources")).isEmpty();
    }

    /** Usa qualificação semântica externa para recuperar candidato ambíguo sem relaxar a busca determinística. */
    @Test
    void shouldAllowEvidenceQualifierToSelectAuditedCandidate() {
        SourceSearchClient searchClient = (query, limit) -> List.of(new SourceSearchResult(
                "Perguntas de clientes sobre troca por WhatsApp",
                "https://forum.example.com.br/trocas-whatsapp",
                "Cliente relata dúvida sobre pedido, troca, entrega e atendimento por WhatsApp em loja de roupas.",
                "TEST_PROVIDER",
                "<item />"));
        SourceEvidenceQualifier qualifier = (context, plannedQueries, attempts, selectedSources) -> {
            Map<String, Object> candidate = (Map<String, Object>) ((List<?>) attempts.getFirst().get("qualifiedSources")).getFirst();
            Map<String, Object> promoted = new java.util.LinkedHashMap<>(candidate);
            promoted.put("aiQualified", true);
            promoted.put("evidenceReason", "Fonte pública rastreável com atrito real de pedido, troca e atendimento.");
            return List.of(promoted);
        };

        StageResult result = new SourceSearcherProcessor(searchClient, qualifier).process(new StageContext("job", "5", Map.of(
                "plannedQueries", List.of(Map.of("query", "loja roupas troca whatsapp atendimento")))));

        assertThat(result.status()).isEqualTo("FONTES_ENCONTRADAS");
        Map<?, ?> source = (Map<?, ?>) ((List<?>) result.output().get("selectedSources")).getFirst();
        assertThat(source.get("aiQualified")).isEqualTo(true);
        assertThat(result.output()).containsEntry("nextStageCode", "source-fetcher");
    }
}
