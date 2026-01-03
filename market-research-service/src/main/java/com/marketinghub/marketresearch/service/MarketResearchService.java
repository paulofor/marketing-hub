package com.marketinghub.marketresearch.service;

import com.marketinghub.marketresearch.config.MarketResearchProperties;
import com.marketinghub.marketresearch.domain.MarketResearchStatus;
import com.marketinghub.marketresearch.domain.MarketResearchTask;
import com.marketinghub.marketresearch.dto.MarketResearchRequest;
import com.marketinghub.marketresearch.openai.OpenAiClient;
import com.marketinghub.marketresearch.repository.MarketResearchTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MarketResearchService {

    private static final Logger log = LoggerFactory.getLogger(MarketResearchService.class);

    private final MarketResearchTaskRepository repository;
    private final WebClient webClient;
    private final OpenAiClient openAiClient;
    private final MarketResearchProperties properties;

    public MarketResearchService(MarketResearchTaskRepository repository,
                                 WebClient.Builder webClientBuilder,
                                 OpenAiClient openAiClient,
                                 MarketResearchProperties properties) {
        this.repository = repository;
        this.webClient = webClientBuilder.build();
        this.openAiClient = openAiClient;
        this.properties = properties;
    }

    public MarketResearchTask execute(MarketResearchRequest request) {
        MarketResearchTask task = new MarketResearchTask();
        task.setQuery(request.query());
        task.setAnalysisObjective(request.analysisObjective());
        task.setSources(request.normalizedSources());
        task.setStatus(MarketResearchStatus.RUNNING);
        repository.save(task);

        try {
            Map<String, String> contexts = fetchContexts(task.getSources());
            String snapshot = contexts.entrySet().stream()
                    .map(entry -> entry.getKey() + "\n" + entry.getValue())
                    .collect(Collectors.joining("\n\n"));
            task.setContextSnapshot(trim(snapshot, properties.getMaxContextLength()));

            String summary = openAiClient.summarize(task.getQuery(), task.getAnalysisObjective(), contexts);
            task.setSummary(summary);
            task.setModel(openAiClient.getModel());
            task.setStatus(MarketResearchStatus.COMPLETED);
        } catch (Exception e) {
            log.error("Falha ao processar pesquisa de mercado", e);
            task.setStatus(MarketResearchStatus.FAILED);
            task.setErrorMessage(e.getMessage());
        }

        return repository.save(task);
    }

    public MarketResearchTask findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pesquisa " + id + " não encontrada"));
    }

    private Map<String, String> fetchContexts(List<String> sources) {
        Map<String, String> contexts = new LinkedHashMap<>();
        for (String source : sources) {
            try {
                String body = webClient.get()
                        .uri(source)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block(Duration.ofMillis(properties.getHttpTimeout().toMillis()));
                contexts.put(source, summarizeBody(body));
            } catch (Exception e) {
                log.warn("Falha ao coletar fonte {}: {}", source, e.getMessage());
                contexts.put(source, "Não foi possível coletar a fonte: " + e.getMessage());
            }
        }
        return contexts;
    }

    private String summarizeBody(String body) {
        if (body == null) {
            return "";
        }
        String cleaned = body.replaceAll("<[^>]+>", " ")
                .replaceAll("\s+", " ")
                .trim();
        return trim(cleaned, properties.getPerSourceMaxLength());
    }

    private String trim(String text, int max) {
        if (text == null) {
            return null;
        }
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, Math.max(0, max - 3)) + "...";
    }
}
