package com.marketinghub.worker.creativeimprovement;

import com.marketinghub.worker.creative.CreativeImageClient;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Responsabilidade: materializar revisões visuais solicitadas pelo backend. */
@Service
public class CreativeImprovementService {
    private static final Logger log = LoggerFactory.getLogger(CreativeImprovementService.class);
    private final CreativeImprovementBackendClient backendClient;
    private final CreativeImageClient imageClient;

    /** Inicializa a etapa técnica com a fila oficial e o gerador visual homologado. */
    public CreativeImprovementService(CreativeImprovementBackendClient backendClient,
                                      CreativeImageClient imageClient) {
        this.backendClient = backendClient;
        this.imageClient = imageClient;
    }

    /** Gera uma nova versão sem revisar, pontuar ou aprovar o anúncio. */
    public Summary processPending(int limit) {
        int success = 0;
        int failed = 0;
        var pending = backendClient.listPending(limit);
        for (Map<String, Object> correction : pending) {
            Long creativeId = Long.valueOf(correction.get("creativeId").toString());
            try {
                String prompt = buildMandatoryPrompt(correction);
                if (prompt.isBlank()) {
                    throw new IllegalArgumentException("Correção sem prompt visual");
                }
                String imageUrl = imageClient.generateImage(
                        prompt,
                        "Revisão visual solicitada pelo backend",
                        "creative-improvement-" + creativeId);
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("imageUrl", imageUrl);
                result.put("requestJson", prompt);
                backendClient.report(creativeId, result);
                success++;
            } catch (RuntimeException ex) {
                failed++;
                log.error("Falha ao materializar correção do anúncio. creativeId={}", creativeId, ex);
                backendClient.report(creativeId, Map.of("error", rootMessage(ex)));
            }
        }
        return new Summary(pending.size(), success, failed);
    }

    /** Monta um prompt determinístico que torna cada item do parecer obrigatório e auditável. */
    private String buildMandatoryPrompt(Map<String, Object> correction) {
        String basePrompt = text(correction.get("revisedImagePrompt"));
        var mandatory = stringList(correction.get("mandatoryVisualRequirements"));
        var acceptance = stringList(correction.get("visualAcceptanceCriteria"));
        if (basePrompt.isBlank() || mandatory.isEmpty() || acceptance.isEmpty()) {
            throw new IllegalArgumentException("Correção sem contrato visual obrigatório completo");
        }
        StringBuilder prompt = new StringBuilder(basePrompt.trim());
        appendSection(prompt, "REQUISITOS OBRIGATÓRIOS — cumpra todos", mandatory);
        appendSection(prompt, "ELEMENTOS PROIBIDOS — não inclua nenhum", stringList(correction.get("forbiddenVisualElements")));
        appendSection(prompt, "CRITÉRIOS DE ACEITAÇÃO — a arte final deve permitir verificar todos", acceptance);
        prompt.append("\nENTREGA: gere exatamente uma arte final, sem explicações, variações, grade, mosaico ou mockup de interface.");
        return prompt.toString();
    }

    /** Acrescenta uma seção enumerada sem permitir que requisitos se percam no texto livre. */
    private void appendSection(StringBuilder prompt, String title, java.util.List<String> items) {
        if (items.isEmpty()) return;
        prompt.append("\n\n").append(title).append(':');
        for (int index = 0; index < items.size(); index++) {
            prompt.append("\n").append(index + 1).append(". ").append(items.get(index));
        }
    }

    /** Normaliza listas recebidas do backend sem aceitar itens vazios. */
    private java.util.List<String> stringList(Object value) {
        if (!(value instanceof java.util.Collection<?> collection)) return java.util.List.of();
        return collection.stream().map(this::text).filter(item -> !item.isBlank()).distinct().toList();
    }

    /** Normaliza texto do contrato recebido do backend. */
    private String text(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    /** Extrai a causa específica já preservada no log com stack trace. */
    private String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    /** Resumo operacional de um ciclo de melhoria. */
    public record Summary(int total, int success, int failed) {}
}
