package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.marketwarmup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupClaimedJob;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.pageanalysis.OpenAiProperties;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Usa OpenAI como camada de planejamento para melhorar as queries de pesquisa do dossiê.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAiMarketWarmupQueryPlanner implements MarketWarmupQueryPlanner {
    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Gera consultas mais específicas para encontrar produtor, canais, prova social e distribuição.
     */
    @Override
    public List<String> planQueries(MarketWarmupClaimedJob job, List<String> baseQueries) {
        List<String> fallback = safeBaseQueries(baseQueries);
        String apiKey = properties.resolvedApiKey();
        if (apiKey.isBlank()) {
            log.info("MOIS market-warmup query planner usando fallback sem OpenAI. jobId={}, pageId={}, baseQueries={}",
                    job.jobId(), job.pageId(), fallback.size());
            return fallback;
        }
        try {
            Map<String, Object> request = buildRequest(job, fallback);
            String requestPayload = objectMapper.writeValueAsString(request);
            log.info("MOIS market-warmup enviando request cru para OpenAI. jobId={}, requestPayload={}",
                    job.jobId(), requestPayload);
            String rawResponse = RestClient.builder()
                    .baseUrl(properties.normalizedBaseUrl())
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .defaultHeader("Content-Type", "application/json")
                    .build()
                    .post()
                    .uri("/chat/completions")
                    .body(request)
                    .retrieve()
                    .body(String.class);
            log.info("MOIS market-warmup recebeu resposta crua da OpenAI. jobId={}, rawResponse={}",
                    job.jobId(), rawResponse);
            return mergeQueries(fallback, parseQueries(rawResponse));
        } catch (RuntimeException ex) {
            log.warn("MOIS market-warmup query planner falhou e usará fallback. jobId={}, pageId={}, errorClass={}, errorMessage={}",
                    job.jobId(), job.pageId(), ex.getClass().getName(), ex.getMessage(), ex);
            return fallback;
        } catch (Exception ex) {
            log.warn("MOIS market-warmup query planner falhou ao serializar resposta e usará fallback. jobId={}, pageId={}, errorClass={}, errorMessage={}",
                    job.jobId(), job.pageId(), ex.getClass().getName(), ex.getMessage(), ex);
            return fallback;
        }
    }

    /**
     * Monta o payload objetivo enviado ao modelo para produzir somente JSON com queries.
     */
    private Map<String, Object> buildRequest(MarketWarmupClaimedJob job, List<String> baseQueries) {
        String userPrompt = """
                Gere melhores consultas de busca web para descobrir por que este produto vende.
                Foque em produtor, pessoa pública, fundador, autoridade, Instagram, YouTube, TikTok, WhatsApp, lives, afiliados, reviews e prova social.
                Evite consultas genéricas por palavras soltas do nome do produto.
                Use aspas em nomes próprios e no título do produto quando útil.
                Responda somente JSON válido no formato {"queries":["..."]}.

                Produto: %s
                Produtor: %s
                URL: %s
                Oferta: %s
                Mecanismo: %s
                Promessa: %s
                Prova: %s
                Queries base: %s
                """.formatted(
                safe(job.title()),
                safe(job.producerName()),
                safe(job.urlCanonical()),
                safe(job.offerSummary()),
                safe(job.mechanismSummary()),
                safe(job.promiseSummary()),
                safe(job.proofSummary()),
                baseQueries);
        return Map.of(
                "model", properties.normalizedModel(),
                "temperature", 0.2,
                "messages", List.of(
                        Map.of("role", "system", "content", "Você é um planejador de pesquisa comercial para dossiês de produtos digitais."),
                        Map.of("role", "user", "content", userPrompt)));
    }

    /**
     * Extrai as queries do JSON retornado pelo modelo.
     */
    private List<String> parseQueries(String rawResponse) throws Exception {
        JsonNode root = objectMapper.readTree(rawResponse);
        String content = root.path("choices").path(0).path("message").path("content").asText("");
        if (content.isBlank()) {
            return List.of();
        }
        JsonNode planned = objectMapper.readTree(stripCodeFence(content));
        JsonNode queriesNode = planned.path("queries");
        if (!queriesNode.isArray()) {
            return List.of();
        }
        List<String> queries = new ArrayList<>();
        for (JsonNode queryNode : queriesNode) {
            String query = clean(queryNode.asText(""));
            if (isUseful(query)) {
                queries.add(query);
            }
        }
        return queries;
    }

    /**
     * Junta fallback e OpenAI preservando ordem, limite e foco comercial.
     */
    private List<String> mergeQueries(List<String> fallback, List<String> planned) {
        Set<String> queries = new LinkedHashSet<>();
        planned.stream().map(this::clean).filter(this::isUseful).forEach(queries::add);
        fallback.stream().map(this::clean).filter(this::isUseful).forEach(queries::add);
        return queries.stream().limit(10).toList();
    }

    /**
     * Garante que as queries base sejam sempre utilizáveis quando a camada OpenAI não responder.
     */
    private List<String> safeBaseQueries(List<String> baseQueries) {
        if (baseQueries == null) {
            return List.of();
        }
        return baseQueries.stream().map(this::clean).filter(this::isUseful).limit(10).toList();
    }

    /**
     * Remove cercas Markdown quando o modelo envolve o JSON em bloco de código.
     */
    private String stripCodeFence(String value) {
        String normalized = value.trim();
        if (normalized.startsWith("```")) {
            normalized = normalized.replaceFirst("^```(?:json)?", "").replaceFirst("```$", "").trim();
        }
        return normalized;
    }

    /**
     * Valida que a query carrega sinal específico de busca comercial.
     */
    private boolean isUseful(String query) {
        String normalized = query.toLowerCase();
        return query.length() >= 18
                && (normalized.contains("\"")
                || normalized.contains("instagram")
                || normalized.contains("youtube")
                || normalized.contains("tiktok")
                || normalized.contains("whatsapp")
                || normalized.contains("hotmart")
                || normalized.contains("depoimento")
                || normalized.contains("review")
                || normalized.contains("afiliado"));
    }

    /**
     * Normaliza espaços e limita tamanho para busca pública.
     */
    private String clean(String value) {
        String normalized = safe(value).replaceAll("\\s+", " ").trim();
        return normalized.length() > 180 ? normalized.substring(0, 180).trim() : normalized;
    }

    /**
     * Normaliza nulos em texto vazio para montar prompt seguro.
     */
    private String safe(String value) {
        return value == null ? "" : value;
    }
}
