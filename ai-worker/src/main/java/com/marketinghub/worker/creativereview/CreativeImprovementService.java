package com.marketinghub.worker.creativereview;

import com.marketinghub.worker.creative.CreativeImageClient;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Responsabilidade: materializar as correções visuais decididas pelo agente aprovador. */
@Service
public class CreativeImprovementService {
    private static final Logger log = LoggerFactory.getLogger(CreativeImprovementService.class);
    private final CreativeImprovementBackendClient backendClient;
    private final CreativeImageClient imageClient;

    /** Inicializa a etapa com a fila oficial e o gerador visual homologado. */
    public CreativeImprovementService(CreativeImprovementBackendClient backendClient,
                                      CreativeImageClient imageClient) {
        this.backendClient = backendClient;
        this.imageClient = imageClient;
    }

    /** Gera uma nova versão para cada correção e deixa a próxima revisão sob controle do backend. */
    public Summary processPending(int limit) {
        int success = 0;
        int failed = 0;
        var pending = backendClient.listPending(limit);
        for (Map<String, Object> correction : pending) {
            Long creativeId = Long.valueOf(correction.get("creativeId").toString());
            String prompt = text(correction.get("revisedImagePrompt"));
            try {
                if (prompt.isBlank()) {
                    throw new IllegalArgumentException("Correção sem prompt visual");
                }
                String imageUrl = imageClient.generateImage(
                        prompt,
                        "Correção definida pelo Agente Especialista em Aprovação de Anúncios",
                        "agent-improvement-creative-" + creativeId);
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
